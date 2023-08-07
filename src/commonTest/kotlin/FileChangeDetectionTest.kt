import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.datetime.Clock
import okio.BufferedSink
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.use
import platform.posix.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FileChangeDetectionTest {

    @Test
    fun `file change is detected for single file`() {
        val testStartTimeMs = Clock.System.now().toEpochMilliseconds()

        val filePath = TestDirectory.root / "$testStartTimeMs.txt"
        FileSystem.SYSTEM.openReadWrite(filePath, mustCreate = true).close()

        val hashBefore = getHashForFile(filePath.toString())
        sleep(1.toUInt())

        writeSthToFile(filePath.toString())
        assertTrue(FileSystem.SYSTEM.metadata(filePath).lastModifiedAtMillis!! > testStartTimeMs)

        val hashAfter = getHashForFile(filePath.toString())

        assertNotEquals(hashBefore, hashAfter)
    }

    @Test
    fun `file change is detected for a file in folder`() {
        val testStartTimeMs = Clock.System.now().toEpochMilliseconds()
        FileSystem.SYSTEM.createDirectory(TestDirectory.root / "$testStartTimeMs", mustCreate = true)

        val fileNames = (0 until 5).map { fileName ->
            Random.nextInt().apply {
                FileSystem.SYSTEM.openReadWrite(TestDirectory.root / "$testStartTimeMs/$fileName.txt", mustCreate = true).close()
            }
        }

        val folderPath = TestDirectory.root / "$testStartTimeMs"
        val hashBefore = getHashForFile(folderPath.toString())

        sleep(1.toUInt())

        val filePath = TestDirectory.root / "$testStartTimeMs/${fileNames[2]}.txt"
        writeSthToFile(filePath.toString())

        val hashAfter = getHashForFile(folderPath.toString())

        assertNotEquals(hashBefore, hashAfter)
    }

    @Test
    fun `file change is detected recursively`() {
        val testStartTimeMs = Clock.System.now().toEpochMilliseconds()
        FileSystem.SYSTEM.createDirectory(TestDirectory.root / "$testStartTimeMs", mustCreate = true)
        FileSystem.SYSTEM.createDirectory(TestDirectory.root / "$testStartTimeMs/$testStartTimeMs", mustCreate = true)

        val fileNames = (0 until 5).map { fileName ->
            Random.nextInt().apply {
                FileSystem.SYSTEM.openReadWrite(TestDirectory.root / "$testStartTimeMs/$testStartTimeMs/$fileName.txt", mustCreate = true).close()
            }
        }

        val folderPath = TestDirectory.root / "$testStartTimeMs"
        val hashBefore = getHashForFile(folderPath.toString())

        sleep(1.toUInt())

        val filePath = TestDirectory.root / "$testStartTimeMs/$testStartTimeMs/${fileNames[2]}.txt"
        writeSthToFile(filePath.toString())

        val hashAfter = getHashForFile(folderPath.toString())

        assertNotEquals(hashBefore, hashAfter)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun writeSthToFile(filePath: String) {
    val file = fopen(filePath, "w")
    try {
        memScoped {
            assertNotEquals(EOF, fputs("change", file))
        }
    } finally {
        fclose(file)
    }
}