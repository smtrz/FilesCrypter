package com.tahir.samplecrypter

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tahir.fileencrypter.filemanagement.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class FileCrypterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FileEncryptionUiState())
    val uiState: StateFlow<FileEncryptionUiState> = _uiState.asStateFlow()

    private var filesToProcess: Files? = null
    private val fileList = mutableListOf<File>()

    fun prepareFilesFromAssets(context: Context) {
        // Reset state
        _uiState.value = FileEncryptionUiState()

        // Clear previous files
        fileList.clear()

        // Copy sample files from assets to app's files directory
        val filesDir = context.filesDir
        val assetManager = context.assets

        // List of sample files to use (add your own files to assets folder)
        val assetFiles = listOf(
            "file_1.csv", "file_2.csv", "file_3.csv", "file_4.csv", "file_5.csv", "file_6.csv"
        )

        assetFiles.forEach { assetFile ->
            try {
                val outputFile = File(filesDir, assetFile)
                assetManager.open(assetFile).use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                fileList.add(outputFile)
            } catch (e: Exception) {
                // Handle error copying files
                _uiState.value = _uiState.value.copy(
                    lastError = "Failed to prepare file: ${e.message}"
                )
            }
        }

        filesToProcess = Files(fileList)
        _uiState.value = _uiState.value.copy(
            totalFiles = fileList.size, currentOperation = "Ready to process ${fileList.size} files"
        )
    }

    fun encryptFiles() {
        filesToProcess?.let { files ->
            _uiState.value = _uiState.value.copy(
                isOperationInProgress = true,
                currentOperation = "Encrypting files...",
                showResults = false
            )

            files.encrypt(
                scope = viewModelScope,
                dispatcher = Dispatchers.IO,
                onEachFileResult = { file, result ->
                    when (result) {
                        is Files.OperationResult.Progress -> {
                            _uiState.value = _uiState.value.copy(
                                currentFileProgress = result.percent, currentFileName = file.name
                            )
                        }

                        Files.OperationResult.Completed -> {
                            _uiState.value = _uiState.value.copy(
                                processedFiles = _uiState.value.processedFiles + 1,
                                currentFileProgress = 0,
                                currentFileName = ""
                            )
                        }

                        is Files.OperationResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                failedCount = _uiState.value.failedCount + 1,
                                lastError = result.cause.message
                            )
                        }
                    }
                },
                onAllCompleted = { success, failed ->
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        currentOperation = "Encryption completed",
                        successCount = success.size,
                        failedCount = failed.size,
                        showResults = true
                    )
                })
        }
    }

    fun decryptFiles() {
        filesToProcess?.let { files ->
            _uiState.value = _uiState.value.copy(
                isOperationInProgress = true,
                currentOperation = "Decrypting files...",
                showResults = false,
                processedFiles = 0
            )

            files.decrypt(
                scope = viewModelScope,
                dispatcher = Dispatchers.IO,
                onEachFileResult = { file, result ->
                    when (result) {
                        is Files.OperationResult.Progress -> {
                            _uiState.value = _uiState.value.copy(
                                currentFileProgress = result.percent, currentFileName = file.name
                            )
                        }

                        Files.OperationResult.Completed -> {
                            _uiState.value = _uiState.value.copy(
                                processedFiles = _uiState.value.processedFiles + 1,
                                currentFileProgress = 0,
                                currentFileName = ""
                            )
                        }

                        is Files.OperationResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                failedCount = _uiState.value.failedCount + 1,
                                lastError = result.cause.message
                            )
                        }
                    }
                },
                onAllCompleted = { success, failed ->
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        currentOperation = "Decryption completed",
                        successCount = success.size,
                        failedCount = failed.size,
                        showResults = true
                    )
                })
        }
    }
}

data class FileEncryptionUiState(
    val isOperationInProgress: Boolean = false,
    val currentOperation: String? = null,
    val currentFileProgress: Int = 0,
    val currentFileName: String = "",
    val totalFiles: Int = 0,
    val processedFiles: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val lastError: String? = null,
    val showResults: Boolean = false
)