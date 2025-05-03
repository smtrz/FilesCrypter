package com.tahir.fileencrypter

import com.tahir.fileencrypter.filemanagement.Files
import com.tahir.fileencrypter.filemanagement.Files.OperationResult
import com.tahir.fileencrypter.utils.KeyStoreHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.mock.MockProviderRule
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.SecretKey

@RunWith(RobolectricTestRunner::class)
class FilesTest : KoinTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        // Your KoinApplication instance here
        printLogger() // Optional: helps with debugging

        modules(
            module {
                single<ICryptoManager> { CryptoManagerImpl() } // Or use mockk() if preferred
            }
        )
    }
    val files = mutableListOf<File>()
    val contentToWrite = "Hello, Tahir!"

    @Before
    fun setupKoin() {
        createFiles()
    }

    @After
    fun teardown() {
        stopKoin()
        files.forEach { it.delete() }
        files.clear()
    }

    private fun createFiles() {
        val tempFile = File.createTempFile("testFile", ".txt")
        val tempFile2 = File.createTempFile("testFile2", ".txt")

        tempFile.writeText(contentToWrite)
        tempFile2.writeText(contentToWrite)
        files.addAll(mutableListOf(tempFile, tempFile2))
    }

    @get:Rule
    val mockProvider = MockProviderRule.create { clazz ->
        // Your way to build a Mock here
        mockkClass(clazz)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun encryptAndDecryptFiles() = runTest {
        val secretKey = mockk<SecretKey>(relaxed = true)
        every { secretKey.encoded } returns ByteArray(32) { 1 } // AES-256 key (32 bytes)
        every { secretKey.algorithm } returns "AES"
        every { secretKey.format } returns "RAW"

        mockkConstructor(KeyStoreHelper::class)
        every { anyConstructed<KeyStoreHelper>().getKey() } returns secretKey

        val encryptionCompleted = AtomicBoolean(false)
        Files(files).encrypt(
            this, mainDispatcherRule.dispatcher, onEachFileResult = { file, result ->
                when (result) {
                    is OperationResult.Progress -> {
                        // NOOP
                    }

                    OperationResult.Completed -> {
                        // NOOP

                    }

                    is OperationResult.Error -> {
                        // NOOP
                    }
                }
            },
            onAllCompleted = { success: List<File>, failed: Map<File, OperationResult.Error> ->
                encryptionCompleted.set(true)
                Assert.assertEquals(success.size, files.size)

            }
        )
        advanceUntilIdle()
        Assert.assertTrue(encryptionCompleted.get())

        // lets decrypt now
        val decryptionCompleted = AtomicBoolean(false)

        Files(files).decrypt(
            this, mainDispatcherRule.dispatcher, onEachFileResult = { file, result ->
                when (result) {
                    is OperationResult.Progress -> {
                        // NOOP

                    }

                    OperationResult.Completed -> {
                        // NOOP
                    }

                    is OperationResult.Error -> {
                        //NOOP

                    }
                }
            },
            onAllCompleted = { success: List<File>, failed: Map<File, OperationResult.Error> ->
                decryptionCompleted.set(true)
                Assert.assertEquals(success.size, files.size)
            }
        )
        advanceUntilIdle()
        Assert.assertTrue(decryptionCompleted.get())
        files.forEach {
            Assert.assertArrayEquals(it.readBytes(), contentToWrite.toByteArray())
        }

    }

}