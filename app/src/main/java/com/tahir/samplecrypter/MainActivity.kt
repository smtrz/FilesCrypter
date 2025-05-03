package com.tahir.samplecrypter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileEncryptionDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FileEncryptionDemoScreen()
                }
            }
        }
    }
}

@Composable
fun FileEncryptionDemoScreen() {


    val viewModel: FileCrypterViewModel = viewModel()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Current operation status
        Text(
            text = uiState.currentOperation ?: "Ready",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress indicator for current file
        if (uiState.currentFileProgress > 0) {
            LinearProgressIndicator(
                progress = uiState.currentFileProgress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${uiState.currentFileProgress}%",
                modifier = Modifier.align(Alignment.End)
            )
            Text(
                text = "Processing: ${uiState.currentFileName}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Overall progress
        if (uiState.totalFiles > 0) {
            Text(text = "Overall progress: ${uiState.processedFiles}/${uiState.totalFiles}")
            LinearProgressIndicator(
                progress = if (uiState.totalFiles > 0) {
                    uiState.processedFiles.toFloat() / uiState.totalFiles.toFloat()
                } else 0f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Button(
            onClick = {
                viewModel.prepareFilesFromAssets(context)
                viewModel.encryptFiles()

            },
            enabled = !uiState.isOperationInProgress
        ) {
            Text("Encrypt Files")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // IMPORTANT NOTE:  IN CASE OF DECRYPTION YOU DON'T NEED TO CALL THE PREPARE FILES FROM ASSETS METHOD
                // SINCE THE FILES ARE ALREADY COPIED AND ENCRYPTED TO THE APP'S FILES DIRECTORY
                viewModel.decryptFiles()


            },
            enabled = !uiState.isOperationInProgress
        ) {
            Text("Decrypt Files")
        }

        // Show results when operation completes
        if (uiState.showResults) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Operation Results",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(text = "Success: ${uiState.successCount} files")
                    Text(text = "Failed: ${uiState.failedCount} files")
                    if (uiState.lastError != null) {
                        Text(
                            text = "Last error: ${uiState.lastError}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}