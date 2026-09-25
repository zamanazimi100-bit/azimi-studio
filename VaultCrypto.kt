package com.azimi.guardian

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

object VaultCrypto {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "azimi_vault_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS = "azimi_vault"

    private fun loadKeyStore(): KeyStore {
        return KeyStore.getInstance(KEYSTORE).apply {
            load(null)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        var keyStore = loadKeyStore()

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(
                KEY_ALIAS,
                null
            )

            if (entry is KeyStore.SecretKeyEntry) {
                return entry.secretKey
            }
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(128)
            .setBlockModes(
                KeyProperties.BLOCK_MODE_GCM
            )
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .build()

        generator.init(spec)

        val generatedKey = generator.generateKey()

        keyStore = loadKeyStore()

        val refreshedEntry = keyStore.getEntry(
            KEY_ALIAS,
            null
        )

        if (refreshedEntry is KeyStore.SecretKeyEntry) {
            return refreshedEntry.secretKey
        }

        return generatedKey
    }

    fun encrypt(
        value: String
    ): String {
        val cipher = Cipher.getInstance(
            TRANSFORMATION
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val iv = cipher.iv

        val encrypted = cipher.doFinal(
            value.toByteArray(
                StandardCharsets.UTF_8
            )
        )

        val combined = iv + encrypted

        return Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )
    }

    fun decrypt(
        value: String
    ): String {
        val combined = Base64.decode(
            value,
            Base64.NO_WRAP
        )

        require(combined.size > 12) {
            "Invalid encrypted data"
        }

        val iv = combined.copyOfRange(
            0,
            12
        )

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
            GCMParameterSpec(
                128,
                iv
            )
        )

        val decrypted = cipher.doFinal(
            encrypted
        )

        return String(
            decrypted,
            StandardCharsets.UTF_8
        )
    }

    fun put(
        context: Context,
        name: String,
        value: String
    ): Boolean {
        return runCatching {
            val encrypted = encrypt(value)

            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .putString(
                    name,
                    encrypted
                )
                .commit()
        }.getOrDefault(false)
    }

    fun get(
        context: Context,
        name: String
    ): String? {
        return runCatching {
            val encrypted =
                context.getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                    .getString(
                        name,
                        null
                    )
                    ?: return null

            decrypt(encrypted)
        }.getOrNull()
    }

    fun delete(
        context: Context,
        name: String
    ): Boolean {
        return runCatching {
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .remove(name)
                .commit()
        }.getOrDefault(false)
    }

    fun testEncryption(
        context: Context
    ): Boolean {
        return runCatching {
            val testValue =
                "AZIMI_TEST_VALUE"

            val encrypted =
                encrypt(testValue)

            val decrypted =
                decrypt(encrypted)

            decrypted == testValue
        }.getOrDefault(false)
    }

    fun clearAll(
        context: Context
    ): Boolean {
        return runCatching {
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .clear()
                .commit()
        }.getOrDefault(false)
    }
}
