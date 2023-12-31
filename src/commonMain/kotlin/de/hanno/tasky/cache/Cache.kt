package de.hanno.tasky.cache

import de.hanno.tasky.task.Task
import de.hanno.tasky.task.TaskContainer
import kotlin.reflect.KProperty0

sealed interface Cache {
    val entries: Map<Pair<String, KProperty0<Any?>>, CacheEntry?>
    val size: Int

    fun containsKey(taskName: String, key: KProperty0<Any?>): Boolean
    operator fun <T> get(task: Task, key: KProperty0<T>): Any?
    fun <T> putPropertyValue(taskName: String, key: KProperty0<T>)

    fun propertyHasChanged(cacheable: Cacheable, task: Task): Boolean {
        val alreadyCached = containsKey(task.name, cacheable.delegateTo)
        val notYetCached = !alreadyCached

        val propertyHasChanged = if (alreadyCached) {
            cacheable.delegateTo.getCacheEntry() != this[task, cacheable.delegateTo]
        } else false

        return notYetCached || propertyHasChanged
    }

    fun getChangedCacheableProperties(
        cacheCandidates: List<Cacheable>,
        task: Task
    ): MutableList<KProperty0<*>> {
        val changedCachedProperties = mutableListOf<KProperty0<*>>()

        cacheCandidates.forEach { cacheable ->
            val propertyHasChanged = propertyHasChanged(cacheable, task)
            if (propertyHasChanged) {
                changedCachedProperties.add(cacheable.delegateTo)
            }
        }
        return changedCachedProperties
    }

    fun afterExecution() {}
    fun beforePlan(taskContainer: TaskContainer) {}
    fun clear()
}

class InMemoryCache: Cache {
    private val underlying: MutableMap<Pair<String, KProperty0<Any?>>, CacheEntry?> = mutableMapOf()
    override val entries: Map<Pair<String, KProperty0<Any?>>, CacheEntry?> get() = underlying

    override val size: Int get() = underlying.size

    override fun containsKey(taskName: String, key: KProperty0<Any?>) = underlying.any {
        it.key.first == taskName && it.key.second == key
    }

    override operator fun <T> get(task: Task, key: KProperty0<T>): Any? = underlying.entries.first {
        it.key.first == task.name && it.key.second == key
    }.value

    override fun <T> putPropertyValue(taskName: String, key: KProperty0<T>) {
        underlying[Pair(taskName, key)] = key.getCacheEntry()
    }

    override fun clear() {
        underlying.clear()
    }
}
