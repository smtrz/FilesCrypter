/**
 * A class for managing batch file encryption and decryption operations.
 *
 * @Authors: Tahir Raza
 * @Version: 1.0.0
 * @Since: 2025-04-18
 *
 * Important Notes:
 * 1. Always provide a CoroutineScope when calling encryptAllFiles/decryptAllFiles to properly manage
 *    the operation lifecycle. The operations will be cancelled if the scope is cancelled.
 * 2. The onProgress callback is called from a background thread (Dispatchers.IO). If you need to
 *    update UI in this callback, you must switch to the UI thread (e.g., Dispatchers.Main).
 * 3. The onComplete callback (OperationResult.Completed) is called from the same dispatcher as the
 *    calling thread, typically the UI thread if you launched from there.
 */
package com.tahir.fileencrypter.filemanagement

import com.tahir.fileencrypter.ICryptoManager
import com.tahir.fileencrypter.utils.CHUNK_SIZE
import com.tahir.fileencrypter.utils.SEGMENT
import com.tahir.fileencrypter.utils.TOTAL_PROGRESS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class Files(private val files: MutableList<File>) {

    private var encryptJob: Job? = null
    private var decryptJob: Job? = null
    private val cryptoManager: ICryptoManager by lazy {
        org.koin.java.KoinJavaComponent.get(ICryptoManager::class.java)
    }

    /**
     * Gets a temporary file for the current file during operations
     */
    private val File.tempFile: File
        get() = File(parent, ".tmp_$name")

    /**
     * Gets the size of the file through its channel
     */
    private fun FileInputStream.getFileSize() = channel?.size() ?: -1L

    // region Core Operations

    /**
     * Starts either encryption or decryption operation based on the encrypt flag
     *
     * @param file The file to process
     * @param encrypt True for encryption, false for decryption
     * @param onResult Callback for operation results (progress, completion, or error)
     */
    private suspend fun startOperation(
        file: File,
        encrypt: Boolean,
        onResult: (OperationResult) -> Unit
    ) {
        if (encrypt) {
            executeOperation(
                file,
                operation = { input, output, progressCallback ->
                    cryptoManager.encrypt(input, output, input.getFileSize(), progressCallback)
                },
                onResult = onResult
            )
        } else {
            executeOperation(
                file,
                operation = { input, output, progressCallback ->
                    cryptoManager.decrypt(input, output, input.getFileSize(), progressCallback)
                },
                onResult = onResult
            )
        }
    }

    /**
     * Handles errors during file operations
     */
    private fun handleError(
        e: Exception,
        lastProgress: Int,
        onResult: (OperationResult) -> Unit
    ) {
        when (e) {
            is CancellationException -> {
                onResult(
                    OperationResult.Error(
                        FileOperationException.IO("Operation cancelled"),
                        lastProgress.takeIf { it > 0 }
                    ))
            }

            else -> {
                onResult(
                    OperationResult.Error(
                        wrapException(e),
                        lastProgress.takeIf { it > 0 }
                    ))
            }
        }
    }

    /**
     * Executes the actual file operation (encryption/decryption)
     *
     * @param file The file to process
     * @param operation The crypto operation to perform
     * @param onResult Callback for operation results
     */
    private suspend fun executeOperation(
        file: File,
        operation: suspend (FileInputStream, FileOutputStream, (Int) -> Unit) -> Unit,
        onResult: (OperationResult) -> Unit
    ) {
        val tempFile = file.tempFile.apply { deleteOnExit() }
        var lastProgress = 0

        try {
            FileInputStream(file).use { input ->
                FileOutputStream(tempFile).use { output ->
                    operation(input, output) { progress ->
                        if (progress == TOTAL_PROGRESS || progress >= lastProgress + SEGMENT) {
                            // WARNING: This callback is called from background thread!
                            onResult(OperationResult.Progress(progress))
                            lastProgress = progress
                        }
                    }
                }
            }

            if (!file.delete() || !tempFile.renameTo(file)) {
                throw FileOperationException.IO("File replacement failed")
            }
            // Completion callback is called from the caller's dispatcher
            onResult(OperationResult.Completed)
        } catch (e: Exception) {
            handleError(e, lastProgress, onResult)
            tempFile.delete()
        }
    }

    /**
     * Wraps exceptions into domain-specific error types
     */
    private fun wrapException(e: Throwable): FileOperationException = when (e) {
        is IOException -> FileOperationException.IO("I/O error: ${e.message}")
        is SecurityException -> FileOperationException.Security("Security violation: ${e.message}")
        else -> FileOperationException.Unknown("Unexpected error: ${e.message ?: "Unknown cause"}")
    }

    private fun handleError(
        file: File,
        throwable: Throwable,
        onEachFileResult: (File, OperationResult) -> Unit,
        failureMap: MutableMap<File, OperationResult.Error>
    ) {
        Timber.e("Error while encrypting file $file: $throwable")
        val wrapped = wrapException(throwable)
        val err = OperationResult.Error(wrapped)
        onEachFileResult(file, err)
        failureMap[file] = err
    }
    // endregion

    // region Result Types
    /**
     * Represents all possible operation states.
     */
    sealed class OperationResult {
        /**
         * Current progress update (0-100)
         * @property percent The current progress percentage
         */
        data class Progress(val percent: Int) : OperationResult() {
            init {
                require(percent in 0..TOTAL_PROGRESS) { "Progress must be between 0 and 100" }
            }
        }

        /**
         * Final success state indicating operation completed successfully
         */
        object Completed : OperationResult()

        /**
         * Operation failure with context
         * @property cause The exception that caused the failure
         * @property progress The last progress before failure (if any)
         */
        data class Error(
            val cause: FileOperationException,
            val progress: Int? = null
        ) : OperationResult()
    }

    /**
     * Cryptographic operation failure cases.
     */
    sealed class FileOperationException(message: String) : Exception(message) {
        class IO(message: String) : FileOperationException(message)
        class Security(message: String) : FileOperationException(message)
        class Unknown(message: String) : FileOperationException(message)
    }
    // endregion

    // region Public API
    /**
     * Encrypts all files in the list using AES encryption.
     * Files are processed in chunks of 5 at a time. Once a chunk is fully processed,
     * the next chunk begins. This helps manage memory usage and ensures that coroutines
     * from completed chunks can be garbage collected.
     *
     * @param scope The CoroutineScope to launch the operation in (must not use Dispatchers.Main).
     *              The operation will be cancelled if the scope is cancelled(Its caller's
     *              responsibility to cancel the scope when it's no longer needed)
     * @param dispatcher The CoroutineDispatcher to use for the operation (defaults to Dispatchers.IO).
     *                   WARNING: Using Dispatchers.Main will throw IllegalArgumentException as it may cause ANRs.
     * @param onEachFileResult Callback for each file's progress/result. Called from the specified dispatcher.
     * @param onAllCompleted Callback when all files are processed with success/failure lists.
     *                       Called from the same dispatcher as the calling thread.
     *
     * @throws IllegalArgumentException if Dispatchers.Main is passed as the dispatcher.
     * @throws IllegalStateException if encryption is already in progress.
     *
     * Usage Example:
     * ```
     * val files = Files(fileList)
     * files.encrypt(
     *     scope = viewModelScope,
     *     dispatcher = Dispatchers.IO,
     *     onEachFileResult = { file, result ->
     *         when (result) {
     *             is OperationResult.Progress -> updateProgress(file, result.percent)
     *             OperationResult.Completed -> handleCompletion(file)
     *             is OperationResult.Error -> handleError(file, result.cause)
     *         }
     *     },
     *     onAllCompleted = { success, failed ->
     *         showSummary(success.size, failed.size)
     *     }
     * )
     * ```
     */
    fun encrypt(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = Dispatchers.IO, // Default to IO in prod
        onEachFileResult: (file: File, result: OperationResult) -> Unit,
        onAllCompleted: (success: List<File>, failed: Map<File, OperationResult.Error>) -> Unit
    ) {
        // Validate dispatcher
        if (dispatcher == Dispatchers.Main) {
            throw IllegalArgumentException(
                "Dispatchers.Main is not allowed for encryption operations. " +
                        "Use Dispatchers.IO or another background dispatcher to prevent ANRs."
            )
        }
        if (encryptJob?.isActive == true) {
            Timber.d("Encryption already in progress")
            return
        }

        val successList = mutableListOf<File>()
        val failureMap = mutableMapOf<File, OperationResult.Error>()

        encryptJob = scope.launch {
            files.chunked(CHUNK_SIZE).forEach { chunkedFiles ->
                chunkedFiles.map { file ->
                    async(dispatcher) {
                        var lastError: OperationResult.Error? = null
                        try {
                            startOperation(file, true) { result ->
                                onEachFileResult(file, result)
                                if (result is OperationResult.Error) {
                                    lastError = result
                                }
                            }
                            if (lastError == null) {
                                successList.add(file)
                            } else {
                                failureMap[file] = lastError
                            }
                        } catch (e: Exception) {
                            handleError(
                                file,
                                e,
                                onEachFileResult,
                                failureMap
                            )
                        }
                    }.also { deferred ->
                        deferred.invokeOnCompletion { throwable ->
                            {
                                if (throwable != null) {
                                    handleError(
                                        file,
                                        throwable,
                                        onEachFileResult,
                                        failureMap
                                    )
                                }

                            }


                        }
                    }
                }.awaitAll()
            }
            onAllCompleted(successList, failureMap)
        }
    }

    /**
     * Decrypts all files in the list using AES decryption.
     * Files are processed in chunks of 5 at a time. Once a chunk is fully processed,
     * the next chunk begins. This helps manage memory usage and ensures that coroutines
     * from completed chunks can be garbage collected.
     * @param scope The CoroutineScope to launch the operation in (must not use Dispatchers.Main).
     *              The operation will be cancelled if the scope is cancelled.
     * @param dispatcher The CoroutineDispatcher to use for the operation (defaults to Dispatchers.IO).
     *                   WARNING: Using Dispatchers.Main will throw IllegalArgumentException as it may cause ANRs.
     * @param onEachFileResult Callback for each file's progress/result. Called from the specified dispatcher.
     * @param onAllCompleted Callback when all files are processed with success/failure lists.
     *                       Called from the same dispatcher as the calling thread.
     *
     * @throws IllegalArgumentException if Dispatchers.Main is passed as the dispatcher.
     * @throws IllegalStateException if decryption is already in progress.
     */
    fun decrypt(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = Dispatchers.IO, // Default to IO in prod
        onEachFileResult: (file: File, result: OperationResult) -> Unit,
        onAllCompleted: (success: List<File>, failed: Map<File, OperationResult.Error>) -> Unit
    ) {
        // Validate dispatcher
        if (dispatcher == Dispatchers.Main) {
            throw IllegalArgumentException(
                "Dispatchers.Main is not allowed for decryption operations. " +
                        "Use Dispatchers.IO or another background dispatcher to prevent ANRs."
            )
        }

        if (decryptJob?.isActive == true) {
            Timber.d("Decryption already in progress")
            return
        }

        val successList = mutableListOf<File>()
        val failureMap = mutableMapOf<File, OperationResult.Error>()

        decryptJob = scope.launch {
            files.chunked(CHUNK_SIZE).forEach { chunkedFiles ->
                chunkedFiles.map { file ->
                    async(dispatcher) {
                        var lastError: OperationResult.Error? = null
                        try {
                            startOperation(file, false) { result ->
                                onEachFileResult(file, result)
                                if (result is OperationResult.Error) {
                                    lastError = result
                                }
                            }

                            if (lastError == null) {
                                successList.add(file)
                            } else {
                                failureMap[file] = lastError
                            }
                        } catch (e: Exception) {
                            handleError(
                                file,
                                e,
                                onEachFileResult,
                                failureMap
                            )
                        }
                    }.also { deferred ->
                        deferred.invokeOnCompletion { throwable ->
                            {
                                if (throwable != null) {
                                    handleError(
                                        file,
                                        throwable,
                                        onEachFileResult,
                                        failureMap
                                    )
                                }

                            }
                        }
                    }


                }.awaitAll()

            }
            onAllCompleted(successList, failureMap)

        }
        // endregion
    }
}