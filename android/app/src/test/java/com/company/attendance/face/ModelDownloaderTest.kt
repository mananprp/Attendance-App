package com.company.attendance.face

import android.content.Context
import com.company.attendance.face.download.ModelDownloader
import com.company.attendance.face.model.ModelType
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var modelsDir: File
    private lateinit var downloader: ModelDownloader

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        modelsDir = tempFolder.newFolder("models")

        downloader = ModelDownloader(
            context = mockContext,
            modelsDir = modelsDir
        )
    }

    @Test
    fun testSha256ChecksumCalculation() {
        val testFile = File(modelsDir, "test.txt")
        testFile.writeText("hello world")
        // sha256("hello world") = b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9
        val sha = downloader.calculateSha256(testFile)
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", sha)
    }

    @Test
    fun testAreAllModelsReadyWhenFilesExist() {
        assertFalse(downloader.areAllModelsReady())

        // Create all required model files
        for (type in ModelType.entries) {
            val f = File(modelsDir, type.fileName)
            f.writeBytes(ByteArray(1024))
        }

        assertTrue(downloader.areAllModelsReady())
    }

    @Test
    fun testGetMissingOrCorruptModels() = runTest {
        // Only create det_10g
        val detFile = File(modelsDir, ModelType.DETECTION.fileName)
        detFile.writeBytes(ByteArray(100))

        val missing = downloader.getMissingOrCorruptModels()
        assertTrue(missing.contains(ModelType.LANDMARK_3D_68.modelName))
        assertTrue(missing.contains(ModelType.RECOGNITION.modelName))
        assertFalse(missing.contains(ModelType.DETECTION.modelName))
    }
}
