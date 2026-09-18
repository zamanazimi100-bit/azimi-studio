package com.azimi.guardian

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object VaultCrypto {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "azimi_vault_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS = "azimi_vault"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply {
            load(null)
        }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance(
                "AES",
                KEYSTORE
            )

            generator.init(256)
            generator.generateKey()
        }

        return (
            keyStore.getEntry(
                KEY_ALIAS,
                null
            ) as KeyStore.SecretKeyEntry
        ).secretKey
    }

    fun encrypt(value: String): String {

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val iv = cipher.iv

        val encrypted = cipher.doFinal(
            value.toByteArray(StandardCharsets.UTF_8)
        )

        val combined = iv + encrypted

        return Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )
    }

    fun decrypt(value: String): String {

        val combined = Base64.decode(
            value,
            Base64.NO_WRAP
        )

        require(combined.size > 12) {
            "Invalid encrypted data"
        }

        val iv = combined.copyOfRange(0, 12)

        val encrypted = combined.copyOfRange(
            12,
            combined.size
        )

        val cipher = Cipher.getInstance(
            TRANSFORMATION
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, iv)
        )

        val decrypted = cipher.doFinal(encrypted)

        return String(
            decrypted,
            StandardCharsets.UTF_8
        )
    }

    fun put(
        context: Context,
        name: String,
        value: String
    ) {

        val encrypted = encrypt(value)

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(name, encrypted)
            .apply()
    }

    fun get(
        context: Context,
        name: String
    ): String? {

        val encrypted =
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(name, null)
                ?: return null

        return runCatching {
            decrypt(encrypted)
        }.getOrNull()
    }

    fun delete(
        context: Context,
        name: String
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(name)
            .apply()
    }
}
