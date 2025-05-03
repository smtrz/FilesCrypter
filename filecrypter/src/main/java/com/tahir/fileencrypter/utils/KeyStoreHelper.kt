/**
 * @Authors: Tahir Raza
 * @Date: 18/04/2025
 */

package com.tahir.fileencrypter.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.tahir.fileencrypter.ICryptoManager
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Internal helper for KeyStore operations.
 *
 * @property keyAlias The key alias in Android KeyStore
 * @property requireUserAuth Whether biometric/PIN is needed
 */
internal class KeyStoreHelper private constructor(
    private val keyAlias: String,
    private val requireAuth: Boolean
) {

    class Builder {
        private var keyAlias = KEY_ALIAS
        private var requireAuth = false
        fun setAlias(alias: String) = apply { this.keyAlias = alias }
        fun requireUserAuth(required: Boolean) = apply { this.requireAuth = required }
        fun build(): KeyStoreHelper {
            return KeyStoreHelper(keyAlias, requireAuth)
        }
    }

    private fun generateKey(): SecretKey {
        return KeyGenerator.getInstance(ALGO).apply {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(PADDING)
                    .setUserAuthenticationRequired(requireAuth)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }

    /**
     * Retrieves the secret key from the Android KeyStore.
     *
     * This function attempts to retrieve a secret key from the Android KeyStore using the specified [keyAlias].
     * If the key doesn't exist, it generates a new one using [generateKey] and stores it in the KeyStore.
     * If the key exists, it retrieves the [SecretKeyEntry] and returns the [SecretKey].
     *
     * @return The [SecretKey] associated with the [keyAlias].
     * @throws IllegalStateException If the retrieved entry is not a [KeyStore.SecretKeyEntry].
     * This indicates an inconsistency in the KeyStore.
     */
    fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        // Check if key already exists
        if (!keyStore.containsAlias(keyAlias)) {
            return generateKey()
        }
        val entry = keyStore.getEntry(keyAlias, null)
        return if (entry is KeyStore.SecretKeyEntry) {
            entry.secretKey
        } else {
            throw IllegalStateException("cast mismatched")
        }

    }
}
