
# FileCrypter - Batch File Encryption & Decryption
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-blue.svg)
![License](https://img.shields.io/github/license/smtrz/FilesCrypter)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/Heapy/awesome-kotlin)

A Kotlin-based utility for encrypting and decrypting multiple files asynchronously using coroutines. Designed to work efficiently in Android or backend projects that require secure batch processing of files with proper lifecycle handling and progress updates.

## Features

- AES encryption and decryption of multiple files.
- Supports chunked processing (default chunk size is 5).
- Coroutine-based architecture for async operations.
- Real-time progress updates per file.
- Graceful error handling and status reporting.
- Works seamlessly with `ICryptoManager` via Koin DI.

## Some Libraries used

- Kotlin
- Coroutines
- Koin (Dependency Injection)
- Timber (Logging)

## Usage
### Adding the Library to the Project

Step 1. Add it in your settings.gradle.kts at the end of repositories:

```kotlin
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
		}
	}

```
Step 2. Add the library to the dependencies:

```kotlin
	dependencies {
		implementation("com.github.smtrz:FilesCrypter:v1.0.0")
	}
```
**Note:** Always use the latest release of the library.

### 1. Initialize with a list of files:

```kotlin
val filesToEncrypt = Files(listOf(file1, file2, file3))
```

### 2. Call encrypt or decrypt:

```kotlin
filesToEncrypt.encrypt(
    scope = viewModelScope,
    dispatcher = Dispatchers.IO,
    onEachFileResult = { file, result ->
        when (result) {
            is Files.OperationResult.Progress -> updateProgressUI(file, result.percent)
            is Files.OperationResult.Completed -> onFileEncrypted(file)
            is Files.OperationResult.Error -> logError(file, result.cause)
        }
    },
    onAllCompleted = { success, failed ->
        showSummary(success.size, failed.size)
    }
)
```

> ⚠️ **Note:** Do **not** use `Dispatchers.Main` as the dispatcher, as it may cause ANRs.

### 3. Decrypt is similar:

```kotlin
filesToEncrypt.decrypt(
    scope = viewModelScope,
    dispatcher = Dispatchers.IO,
    onEachFileResult = { file, result -> /* handle result */ },
    onAllCompleted = { success, failed -> /* summary */ }
)
```

## Threading Notes

- `onEachFileResult` is triggered from a background thread (`Dispatchers.IO`). If you need to update the UI, switch to `Dispatchers.Main`.
- `onAllCompleted` is called from the dispatcher of the calling thread (typically the main/UI thread if invoked from there).

## Exception Handling

Exceptions are wrapped into specific types:
- `FileOperationException.IO`
- `FileOperationException.Security`
- `FileOperationException.Unknown`

These are exposed in `OperationResult.Error`.

## Development Notes

- Temporary files are used during encryption/decryption and are safely renamed after successful processing.
- Chunked processing ensures efficient memory usage and cancellation support.
  
## Upcoming Features

- Enhanced Security with extended meta-data

- Password strength estimator.

- AI-based password suggestions using NLP models.

- Sensitivity classification of files (e.g., personal, financial, legal).

- Encryption strength analysis with recommendations.

- Anomaly detection in app usage patterns.

- Tamper detection via hash validation.

- Encrypted file fingerprinting.

- Decryption activity logs (stored locally & optionally encrypted).


## Contributing

Feel free to fork this repository, raise issues, or submit pull requests. Feedback is always appreciated!

## License

MIT
