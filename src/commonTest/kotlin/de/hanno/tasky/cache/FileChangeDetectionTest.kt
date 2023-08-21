package de.hanno.tasky.cache

import TestDirectory
import de.hanno.tasky.fileSystem
import kotlinx.datetime.Clock
import okio.Path.Companion.toPath
import okio.use
import sleep
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FileChangeDetectionTest {

    @Test
    fun `file change is detected for single file`() {
        val testStartTimeMs = Clock.System.now().toEpochMilliseconds()

        val filePath = TestDirectory.root / "$testStartTimeMs.txt"
        fileSystem.openReadWrite(filePath, mustCreate = true).close()

        val hashBefore = getHashForFile(filePath.toString())
        sleep(1000.toUInt())

        writeSthToFile(filePath.toString())
        assertTrue(fileSystem.metadata(filePath).lastModifiedAtMillis!! > testStartTimeMs)

        val hashAfter = getHashForFile(filePath.toString())

        assertNotEquals(hashBefore, hashAfter)
    }

    @Test
    fun `file change is detected for a file in folder`() {
        val testStartTimeMs = Clock.System.now().toEpochMilliseconds()
        fileSystem.createDirectory(TestDirectory.root / "$testStartTimeMs", mustCreate = true)

        val fileNames = (0 until 5).map { fileName ->
            Random.nextInt().apply {
                fileSystem.openReadWrite(TestDirectory.root / "$testStartTimeMs/$fileName.txt", mustCreate = true).close()
            }
        }

        val folderPath = TestDirectory.root / "$testStartTimeMs"
        val hashBefore = getHashForFile(folderPath.toString())

        val filePath = TestDirectory.root / "$testStartTimeMs/${fileNames[2]}.txt"
        writeSthToFile(filePath.toString())

        val hashAfter = getHashForFile(folderPath.toString())

        assertNotEquals(hashBefore, hashAfter)
    }

    @Test
    fun `file change is detected recursively`() {
        val testStartTimeMs = Clock.System.now().toEpochMilliseconds()
        fileSystem.createDirectory(TestDirectory.root / "$testStartTimeMs", mustCreate = true)
        fileSystem.createDirectory(TestDirectory.root / "$testStartTimeMs/$testStartTimeMs", mustCreate = true)

        val fileNames = (0 until 5).map { fileName ->
            Random.nextInt().apply {
                fileSystem.openReadWrite(TestDirectory.root / "$testStartTimeMs/$testStartTimeMs/$fileName.txt", mustCreate = true).close()
            }
        }

        val folderPath = TestDirectory.root / "$testStartTimeMs"
        val hashBefore = getHashForFile(folderPath.toString())

        val filePath = TestDirectory.root / "$testStartTimeMs/$testStartTimeMs/${fileNames[2]}.txt"
        writeSthToFile(filePath.toString())

        val hashAfter = getHashForFile(folderPath.toString())

        assertNotEquals(hashBefore, hashAfter)
    }
}

internal fun writeSthToFile(filePath: String) {
    fileSystem.openReadWrite(filePath.toPath()).use {
        val byteArray = "change".encodeToByteArray()
        it.write(0, byteArray, 0, byteArray.size)
    }
}