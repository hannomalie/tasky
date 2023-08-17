import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.reflect.KProperty0

class FileBasedCache(private val file: File, private val format: StringFormat = Json): Cache {
    private val underlying = InMemoryCache()
    override val entries: Map<Pair<String, KProperty0<Any?>>, CacheEntry?> get() = underlying.entries

    override val size: Int get() = underlying.size
    override fun containsKey(taskName: String, key: KProperty0<Any?>) = underlying.containsKey(taskName, key)

    override operator fun <T> get(task: TaskDefinition, key: KProperty0<T>) = underlying[task, key]

    override fun <T> putPropertyValue(taskName: String, key: KProperty0<T>) = underlying.putPropertyValue(taskName, key)

    private fun load(taskContainer: TaskContainer) {
        val string: String = FileSystem.SYSTEM.read(file.path.toPath()) {
            readByteArray().decodeToString()
        }
        if(string.isNotBlank()) {
            val deserialized = format.decodeFromString<List<Entry>>(string)
            underlying.clear()
            deserialized.forEach { entry ->
                val task = taskContainer.getOrThrow(entry.taskName)
                val cacheable = task.cached.keys.first { it.delegateTo.name == entry.propertyName }

                underlying.putPropertyValue(task.name, cacheable.delegateTo)
            }
        }
    }

    override fun afterExecution() {
        val serializable = underlying.entries.map {
            Entry(it.key.first, it.key.second.name, it.value)
        }
        val string = format.encodeToString(serializable)
        FileSystem.SYSTEM.write(file.path.toPath()) {
            write(string.encodeToByteArray())
        }
    }
    override fun beforePlan(taskContainer: TaskContainer) {
        load(taskContainer)
    }

    override fun clear() = underlying.clear()

    override fun toString(): String = "FileBasedCache(file=$file,underlying=$underlying)"
}

@Serializable
private data class Entry(val taskName: String, val propertyName: String, val value: CacheEntry?)
