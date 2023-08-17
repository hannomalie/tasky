import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import kotlin.random.Random
import kotlin.reflect.KMutableProperty0
import kotlin.test.Test
import kotlin.test.assertEquals

class FileBasedCacheTest {

    @Test
    fun `fileCache is written on afterExecution`() {
        val cacheFile = TestDirectory.root / (Random.nextInt().toString())
        FileSystem.SYSTEM.openReadWrite(cacheFile, true).close()
        val cache = FileBasedCache(File(cacheFile.toString()))

        TaskContainer().apply {
            val task = object: TaskDefinition("myTask") {
                var cacheableProperty = "foo"
                val property by Cacheable(::cacheableProperty)
            }
            val originalProperty = task.cached.keys.first { it.delegateTo.name == "cacheableProperty" }.delegateTo
            originalProperty as KMutableProperty0<String>
            originalProperty.set("12345")
            cache.putPropertyValue(
                task.name,
                originalProperty
            )
            cache.afterExecution()

            val fileContent = FileSystem.SYSTEM.read(cacheFile) {
                readByteString().utf8()
            }
            assertEquals(
                listOf(
                    Entry("myTask", "cacheableProperty", mapOf("type" to "StringCacheEntry", "value" to "12345"))
                ),
                Json.decodeFromString(fileContent)
            )
        }
    }
}

@Serializable
data class Entry(val taskName: String, val propertyName: String, val value: Map<String, String>)
