package com.tahir.fileencrypter.utils

import android.security.keystore.KeyProperties

/**
 * @Authors: Tahir Raza
 * @Date: 31/12/2023
 */
const val BYTE_SIZE = 16
const val KEY_ALIAS = "FILE_ENCRYPTED"
const val ALGO = KeyProperties.KEY_ALGORITHM_AES
const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
const val TRANSFORMATION = "$ALGO/$BLOCK_MODE/$PADDING"
const val TOTAL_PROGRESS = 100
const val START = 0
const val SEGMENT = 5
const val CHUNK_SIZE = 5
const val MB_FROM_KB = 1024 * 1024

