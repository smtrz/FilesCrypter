/**
 * @Authors: Tahir Raza
 * @Date: 31/12/2023
 */

package com.tahir.fileencrypter

import java.io.InputStream
import java.io.OutputStream

interface ICryptoManager {
    fun encrypt(
        inputStream: InputStream,
        outputStream: OutputStream,
        totalSize: Long,
        onProgress: (Int) -> Unit
    )

    fun decrypt(
        inputStream: InputStream,
        outputStream: OutputStream,
        totalSize: Long,
        onProgress: (Int) -> Unit
    )
}