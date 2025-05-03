/**
 * Internal implementation of [ICryptoManager] that handles the actual encryption/decryption operations.
 *
 * This class is marked as internal and should not be used directly by library consumers.
 * Instead, use the public [Files] class which provides a safer, more convenient API.
 *
 * @Authors: Tahir Raza
 * @Date: 18/04/2025
 */
package com.tahir.fileencrypter

import com.tahir.fileencrypter.utils.BYTE_SIZE
import com.tahir.fileencrypter.utils.KEY_ALIAS
import com.tahir.fileencrypter.utils.KeyStoreHelper
import com.tahir.fileencrypter.utils.MB_FROM_KB
import com.tahir.fileencrypter.utils.START
import com.tahir.fileencrypter.utils.TOTAL_PROGRESS
import com.tahir.fileencrypter.utils.TRANSFORMATION
import org.koin.core.annotation.Single
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec


@Single
internal class CryptoManagerImpl : ICryptoManager {

    private val secretKey = KeyStoreHelper.Builder()
        .setAlias(KEY_ALIAS)
        .requireUserAuth(false)
        .build().getKey()

    /**
     * Encrypts data from an input stream to an output stream.
     *
     * @param inputStream The source stream to encrypt
     * @param outputStream The destination stream for encrypted data
     * @param totalSize Total size of input data for progress calculation
     * @param onProgress Callback for progress updates (0-100)
     *
     * @throws SecurityException if there are key access issues
     * @throws IOException if there are I/O errors during encryption
     */

    override fun encrypt(
        inputStream: InputStream,
        outputStream: OutputStream,
        totalSize: Long,
        onProgress: (Int) -> Unit
    ) {
        val cipher = getInitializedCipher(Cipher.ENCRYPT_MODE)
        outputStream.write(cipher.iv)

        processStreamWithProgress(
            input = inputStream,
            output = CipherOutputStream(outputStream, cipher),
            cipher = null,
            totalSize = totalSize,
            onProgress = onProgress,
            isDecryption = false
        )
    }


    /**
     * Decrypts data from an input stream to an output stream.
     *
     * @param inputStream The source stream to decrypt
     * @param outputStream The destination stream for decrypted data
     * @param totalSize Total size of input data for progress calculation
     * @param onProgress Callback for progress updates (0-100)
     *
     * @throws SecurityException if there are key access issues
     * @throws IOException if there are I/O errors during decryption
     */

    override fun decrypt(
        inputStream: InputStream,
        outputStream: OutputStream,
        totalSize: Long,
        onProgress: (Int) -> Unit
    ) {
        val iv = ByteArray(BYTE_SIZE).also { inputStream.read(it) }
        val cipher = getInitializedCipher(Cipher.DECRYPT_MODE, IvParameterSpec(iv))

        processStreamWithProgress(
            input = inputStream,
            output = outputStream,
            cipher = cipher,
            totalSize = totalSize,
            onProgress = onProgress,
            isDecryption = true
        )
    }

    /**
     * Initializes a cipher instance for encryption/decryption
     *
     * @param mode Cipher mode (ENCRYPT_MODE or DECRYPT_MODE)
     * @param ivSpec Optional IV parameter spec for decryption
     * @return Initialized Cipher instance
     *
     * @throws SecurityException if cipher initialization fails
     */
    private fun getInitializedCipher(mode: Int, ivSpec: IvParameterSpec? = null): Cipher {
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(mode, secretKey, ivSpec)
        }
    }

    /**
     * Processes data between streams using the provided Cipher (optional).
     * Used for both encryption and decryption with real-time progress reporting.
     *
     * ## Encryption
     * - Uses `CipherOutputStream` internally.
     * - Cipher should be null (already wrapped).
     *
     * ## Decryption
     * - Manually processes the cipher using `cipher.update()` and `cipher.doFinal()`.
     * - Avoids performance issues caused by `CipherInputStream`, which internally buffers
     *   large chunks and causes UI stalling and delayed progress callbacks.
     *
     * @param input Source input stream
     * @param output Destination output stream (wrapped with CipherOutputStream for encryption)
     * @param cipher Cipher instance (required for decryption, null for encryption)
     * @param totalSize Total size of the data, used for progress calculation
     * @param onProgress Callback to report progress (0-100)
     * @param isDecryption Flag to indicate decryption or encryption mode
     *
     * ### Performance Notes:
     * - `CipherInputStream` is avoided due to known performance problems, especially on large files.
     * - Manual cipher update/decryption allows for smooth progress tracking and no blocking.
     */
    private fun processStreamWithProgress(
        input: InputStream,
        output: OutputStream,
        cipher: Cipher?,
        totalSize: Long,
        onProgress: (Int) -> Unit,
        isDecryption: Boolean
    ) {
        val buffer = ByteArray(MB_FROM_KB)
        var totalRead = START
        var lastReportedProgress = 0

        try {
            var bytesRead: Int
            if (isDecryption && cipher != null) {
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    totalRead += bytesRead

                    val decrypted = cipher.update(buffer, 0, bytesRead)
                    decrypted?.let { output.write(it) }

                    val currentProgress = ((totalRead * TOTAL_PROGRESS) / totalSize).toInt()
                        .coerceAtMost(TOTAL_PROGRESS)

                    if (currentProgress > lastReportedProgress) {
                        onProgress(currentProgress)
                        lastReportedProgress = currentProgress
                    }
                }

                val finalBytes = cipher.doFinal()
                finalBytes?.let { output.write(it) }

                onProgress(TOTAL_PROGRESS)

            } else {
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, START, bytesRead)
                    totalRead += bytesRead

                    val currentProgress = ((totalRead * TOTAL_PROGRESS) / totalSize).toInt()
                        .coerceAtMost(TOTAL_PROGRESS)

                    onProgress(currentProgress)
                }
            }
        } finally {
            output.flush()
            output.close()
            input.close()
        }
    }

}