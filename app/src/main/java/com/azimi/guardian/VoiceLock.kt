package com.azimi.guardian

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * AZIMI Voice Lock
 *
 * Local defensive voice factor. No raw recordings are stored and no audio is
 * sent to a server. This is NOT a certified biometric implementation and
 * does not claim perfect replay/spoof resistance.
 *
 * The verifier uses a fixed owner phrase and a compact normalized acoustic
 * feature vector. Enrollment uses three samples; verification uses a fresh
 * sample after the biometric factor has succeeded.
 */
object VoiceLock {

    const val RECORD_AUDIO_REQUEST_CODE = 9107

    private const val PREFS = "azimi_voice_lock"
    private const val KEY_ENROLLED = "enrolled"
    private const val KEY_ENROLLED_AT = "enrolled_at"
    private const val KEY_SAMPLE_COUNT = "sample_count"
    private const val KEY_TEMPLATE = "template"
    private const val KEY_VERIFIED_AT = "verified_at"
    private const val KEY_VERIFIED_SESSION = "verified_session"

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "AZIMI_VOICE_LOCK_KEY_V1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private const val SAMPLE_RATE = 16000
    private const val RECORD_SECONDS = 2
    private const val FRAME = 256
    private const val BANDS = 20
    private const val COEFFICIENTS = 13
    private const val ENROLLMENT_SAMPLES = 3
    private const val VERIFICATION_THRESHOLD = 0.82

    // Keep the phrase constant so the enrolled samples are comparable.
    const val OWNER_PHRASE = "AZIMI Guardian. I am the owner."

    data class State(
        val enrolled: Boolean,
        val sampleCount: Int,
        val enrolledAt: Long?,
        val verifiedAt: Long?,
        val verifiedForSession: Boolean
    )

    data class Result(
        val success: Boolean,
        val message: String,
        val score: Double? = null
    )

    @Volatile
    private var verificationSession: String? = null

    fun getState(context: Context): State {
        val p = prefs(context)
        return State(
            enrolled = p.getBoolean(KEY_ENROLLED, false),
            sampleCount = p.getInt(KEY_SAMPLE_COUNT, 0),
            enrolledAt = p.getLongOrNull(KEY_ENROLLED_AT),
            verifiedAt = p.getLongOrNull(KEY_VERIFIED_AT),
            verifiedForSession = verificationSession != null &&
                p.getString(KEY_VERIFIED_SESSION, null) == verificationSession
        )
    }

    fun hasEnrollment(context: Context): Boolean =
        getState(context).enrolled && getState(context).sampleCount >= ENROLLMENT_SAMPLES

    fun ensureRecordPermission(activity: Activity): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 23) return true
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        return false
    }

    fun enroll(activity: Activity, onResult: (Result) -> Unit) {
        if (!ensureRecordPermission(activity)) return
        runEnrollment(activity, 0, mutableListOf(), onResult)
    }

    fun verify(activity: Activity, onResult: (Result) -> Unit) {
        if (!hasEnrollment(activity)) {
            onResult(Result(false, "Voice Lock is not enrolled. Enroll the owner voice factor first."))
            return
        }
        if (!ensureRecordPermission(activity)) return

        Toast.makeText(activity, "Speak the phrase clearly:\n$OWNER_PHRASE", Toast.LENGTH_LONG).show()
        capture(activity) { feature, error ->
            if (feature == null) {
                onResult(Result(false, error ?: "Voice capture failed."))
                return@capture
            }
            val template = loadTemplate(activity)
            if (template == null) {
                onResult(Result(false, "Voice Lock template is unavailable. Enrollment is required again."))
                return@capture
            }
            val score = similarity(template, feature)
            if (score >= VERIFICATION_THRESHOLD) {
                val session = currentFactorSession(activity)
                if (session.isNullOrBlank()) {
                    onResult(Result(false, "The biometric owner factor session is missing. Verify biometrics again."))
                    return@capture
                }
                verificationSession = session
                prefs(activity).edit()
                    .putLong(KEY_VERIFIED_AT, System.currentTimeMillis())
                    .putString(KEY_VERIFIED_SESSION, session)
                    .apply()
                onResult(Result(true, "Voice Lock verified for the current owner-authentication session.", score))
            } else {
                clearVerification(activity)
                onResult(Result(false, "Voice verification did not meet the local similarity threshold. Try again using the enrolled voice and phrase.", score))
            }
        }
    }

    /** Called only by AtlasOwnerAuthority after both factors are expected. */
    internal fun consumeVerifiedFactor(context: Context): Boolean {
        val state = getState(context)
        val session = currentFactorSession(context)
        val valid = state.verifiedForSession && !session.isNullOrBlank() && session == state.verifiedSessionId(context)
        if (valid) {
            prefs(context).edit().remove(KEY_VERIFIED_SESSION).apply()
            verificationSession = null
        }
        return valid
    }

    fun clearVerification(context: Context) {
        verificationSession = null
        prefs(context).edit()
            .remove(KEY_VERIFIED_AT)
            .remove(KEY_VERIFIED_SESSION)
            .apply()
    }

    fun clear(context: Context) {
        clearVerification(context)
        prefs(context).edit()
            .clear()
            .apply()
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    fun diagnostics(context: Context): Map<String, String> {
        val state = getState(context)
        return linkedMapOf(
            "enrolled" to state.enrolled.toString(),
            "sample_count" to state.sampleCount.toString(),
            "verified_for_session" to state.verifiedForSession.toString(),
            "raw_recordings_stored" to "false",
            "cloud_audio_upload" to "false",
            "spoof_resistance" to "defensive_limited",
            "status" to if (state.enrolled) "READY" else "NOT_ENROLLED"
        )
    }

    private fun runEnrollment(
        activity: Activity,
        index: Int,
        features: MutableList<FloatArray>,
        onResult: (Result) -> Unit
    ) {
        if (index >= ENROLLMENT_SAMPLES) {
            val template = average(features)
            saveTemplate(activity, template)
            prefs(activity).edit()
                .putBoolean(KEY_ENROLLED, true)
                .putLong(KEY_ENROLLED_AT, System.currentTimeMillis())
                .putInt(KEY_SAMPLE_COUNT, ENROLLMENT_SAMPLES)
                .apply()
            clearVerification(activity)
            onResult(Result(true, "Voice Lock enrollment completed with $ENROLLMENT_SAMPLES local samples. A fresh voice verification is still required."))
            return
        }

        Toast.makeText(
            activity,
            "Voice enrollment ${index + 1}/$ENROLLMENT_SAMPLES. Speak clearly:\n$OWNER_PHRASE",
            Toast.LENGTH_LONG
        ).show()

        capture(activity) { feature, error ->
            if (feature == null) {
                onResult(Result(false, error ?: "Voice enrollment capture failed."))
                return@capture
            }
            features.add(feature)
            Handler(Looper.getMainLooper()).postDelayed({
                runEnrollment(activity, index + 1, features, onResult)
            }, 350L)
        }
    }

    private fun capture(activity: Activity, callback: (FloatArray?, String?) -> Unit) {
        Thread {
            var recorder: AudioRecord? = null
            try {
                val minBuffer = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuffer <= 0) {
                    post(activity) { callback(null, "This device does not expose a usable microphone configuration.") }
                    return@Thread
                }

                val bufferSize = maxOf(minBuffer, FRAME * 8)
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    post(activity) { callback(null, "Microphone initialization failed.") }
                    return@Thread
                }

                val samples = ShortArray(SAMPLE_RATE * RECORD_SECONDS)
                var offset = 0
                recorder.startRecording()
                while (offset < samples.size) {
                    val read = recorder.read(samples, offset, samples.size - offset)
                    if (read <= 0) {
                        post(activity) { callback(null, "Microphone capture returned no usable audio.") }
                        return@Thread
                    }
                    offset += read
                }
                recorder.stop()

                val feature = extractFeatures(samples)
                if (feature == null) {
                    post(activity) { callback(null, "Voice sample was too quiet or too short. Please speak clearly and try again.") }
                } else {
                    post(activity) { callback(feature, null) }
                }
            } catch (security: SecurityException) {
                post(activity) { callback(null, "Microphone permission is required for Voice Lock.") }
            } catch (t: Throwable) {
                post(activity) { callback(null, "Voice capture failed safely: ${t.javaClass.simpleName}.") }
            } finally {
                runCatching { recorder?.release() }
            }
        }.start()
    }

    private fun extractFeatures(samples: ShortArray): FloatArray? {
        val frames = ArrayList<FloatArray>()
        var pos = 0
        while (pos + FRAME <= samples.size) {
            val frame = DoubleArray(FRAME)
            var energy = 0.0
            var zcr = 0
            for (i in 0 until FRAME) {
                val s = samples[pos + i] / 32768.0
                val w = 0.5 - 0.5 * cos(2.0 * PI * i / (FRAME - 1))
                frame[i] = s * w
                energy += s * s
                if (i > 0 && ((samples[pos + i] >= 0) != (samples[pos + i - 1] >= 0))) zcr++
            }
            if (energy > 0.00002) {
                val bands = DoubleArray(BANDS)
                for (k in 1 until FRAME / 2) {
                    var re = 0.0
                    var im = 0.0
                    for (n in 0 until FRAME) {
                        val angle = 2.0 * PI * k * n / FRAME
                        re += frame[n] * cos(angle)
                        im -= frame[n] * kotlin.math.sin(angle)
                    }
                    val power = re * re + im * im
                    val band = ((k.toDouble() / (FRAME / 2.0)) * BANDS).toInt().coerceIn(0, BANDS - 1)
                    bands[band] += power
                }
                val logBands = FloatArray(BANDS) { ln(bands[it] + 1e-9).toFloat() }
                val coeff = FloatArray(COEFFICIENTS)
                for (c in 0 until COEFFICIENTS) {
                    var sum = 0.0
                    for (b in 0 until BANDS) {
                        sum += logBands[b] * cos(PI * c * (b + 0.5) / BANDS)
                    }
                    coeff[c] = sum.toFloat()
                }
                val out = FloatArray(COEFFICIENTS + 2)
                for (i in 0 until COEFFICIENTS) out[i] = coeff[i]
                out[COEFFICIENTS] = ln(energy / FRAME + 1e-9).toFloat()
                out[COEFFICIENTS + 1] = (zcr.toDouble() / FRAME).toFloat()
                frames.add(out)
            }
            pos += FRAME
        }
        if (frames.size < 8) return null
        val vector = FloatArray(COEFFICIENTS + 2)
        for (f in frames) for (i in vector.indices) vector[i] += f[i]
        for (i in vector.indices) vector[i] /= frames.size
        normalize(vector)
        return vector
    }

    private fun normalize(v: FloatArray) {
        var mean = 0.0
        for (x in v) mean += x
        mean /= v.size
        var variance = 0.0
        for (i in v.indices) {
            v[i] = (v[i] - mean).toFloat()
            variance += v[i] * v[i]
        }
        val sd = sqrt(variance / v.size + 1e-8)
        for (i in v.indices) v[i] = (v[i] / sd).coerceIn(-4f, 4f)
    }

    private fun average(samples: List<FloatArray>): FloatArray {
        val out = FloatArray(COEFFICIENTS + 2)
        for (s in samples) for (i in out.indices) out[i] += s[i]
        for (i in out.indices) out[i] /= samples.size
        normalize(out)
        return out
    }

    private fun similarity(a: FloatArray, b: FloatArray): Double {
        var dot = 0.0
        var aa = 0.0
        var bb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            aa += a[i] * a[i]
            bb += b[i] * b[i]
        }
        if (aa <= 0 || bb <= 0) return -1.0
        return (dot / (sqrt(aa) * sqrt(bb))).coerceIn(-1.0, 1.0)
    }

    private fun saveTemplate(context: Context, template: FloatArray) {
        val raw = ByteBuffer.allocate(template.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        template.forEach { raw.putFloat(it) }
        val encrypted = encrypt(raw.array())
        prefs(context).edit().putString(KEY_TEMPLATE, android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)).apply()
    }

    private fun loadTemplate(context: Context): FloatArray? {
        val encoded = prefs(context).getString(KEY_TEMPLATE, null) ?: return null
        return runCatching {
            val bytes = decrypt(android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP))
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val out = FloatArray(COEFFICIENTS + 2)
            for (i in out.indices) out[i] = bb.float
            out
        }.getOrNull()
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return ByteArrayOutputStream().apply {
            write(iv.size)
            write(iv)
            write(ct)
        }.toByteArray()
    }

    private fun decrypt(data: ByteArray): ByteArray {
        val ivLength = data[0].toInt()
        val iv = data.copyOfRange(1, 1 + ivLength)
        val ct = data.copyOfRange(1 + ivLength, data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    private fun getKey(): SecretKey {
        val ks = keyStore()
        val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance("AES", KEYSTORE)
        generator.init(256)
        generator.generateKey()
        return ks.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun Context.getLongOrNull(key: String): Long? {
        val p = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (p.contains(key)) p.getLong(key, 0L) else null
    }

    private fun State.verifiedSessionId(context: Context): String? =
        prefs(context).getString(KEY_VERIFIED_SESSION, null)

    private fun currentFactorSession(context: Context): String? =
        context.applicationContext.getSharedPreferences("azimi_owner_authority", Context.MODE_PRIVATE)
            .getString("factor_session_id", null)

    private fun post(activity: Activity, block: () -> Unit) {
        activity.runOnUiThread(block)
    }
}
