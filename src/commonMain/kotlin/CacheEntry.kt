import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty0

@Serializable
sealed interface CacheEntry

@Serializable
data class FileCacheEntry(val path: String, val lastModification: Long?): CacheEntry

@Serializable
data class StringCacheEntry(val value: String): CacheEntry

fun KProperty0<*>.getCacheEntry(): CacheEntry = when(val value = get()) {
    is File -> FileCacheEntry(value.path, getLastModifiedTime(value.path))
    is String -> StringCacheEntry(value)
    else -> throw IllegalStateException(
        "No support for cached properties of $value's type!"
    )
}