import kotlin.collections.List
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.minus
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.toSet
import kotlin.reflect.KProperty1

class Executor {
    internal val cache = mutableMapOf<TaskProperty, Any?>()

    fun plan(taskName: String, taskContainer: TaskContainer): Result {
        val taskToExecute = taskContainer.tasks.firstOrNull {
            it.name == taskName
        } ?: return NoTasksMatching(taskName)

        return taskContainer.run {
            val tasksToBeExecuted = taskToExecute.requirements + listOf(taskToExecute) + taskToExecute.introductions

            val cachedTasks = mutableListOf<CachedTask>()
            tasksToBeExecuted.forEach { taskDefinition ->

                taskDefinition.cached.takeIf { it.isNotEmpty() }?.let { cacheCandidates ->
                    val changedCachedProperties = mutableListOf<KProperty1<TaskDefinition, *>>()

                    cacheCandidates.forEach { property ->

                        // TODO: More clever comparison for different data types here
                        val taskProperty = TaskProperty(taskDefinition, property)
                        val currentPropertyValue = property.get(taskDefinition)
                        val alreadyCached = cache.containsKey(taskProperty)
                        val notYetCached = !alreadyCached

                        val propertyHasChanged = if(alreadyCached) {
                            if(currentPropertyValue is File) {
                                val lastModifiedTimeOrNull = getLastModifiedTime(currentPropertyValue.path)
                                if(lastModifiedTimeOrNull != null) {
                                    lastModifiedTimeOrNull > (cache[taskProperty] as Long) // TODO: This might fail
                                } else false
                            } else {
                                (currentPropertyValue != cache[taskProperty])
                            }
                        } else false

                        if(notYetCached || propertyHasChanged) {
                            changedCachedProperties.add(property)
                        }
                    }

                    val cachedPropertiesChanged = changedCachedProperties.isNotEmpty()
                    if(!cachedPropertiesChanged) {
                        cachedTasks.add(CachedTask(taskDefinition, cacheCandidates.map { it.name }))
                    }
                }
            }

            TasksToBeExecuted((tasksToBeExecuted - cachedTasks.map { it.taskDefinition }.toSet()), cachedTasks)
        }
    }

    fun execute(taskName: String, taskContainer: TaskContainer): Result = plan(taskName, taskContainer).apply {
        when(this) {
            is NoTasksMatching -> { }
            is TasksToBeExecuted -> tasks.forEach { taskDefinition ->
                taskDefinition.execute()
                taskDefinition.cached.forEach { property ->
                    val newPropertyValue = property.get(taskDefinition)

                    if (newPropertyValue is File) {
                        getLastModifiedTime(newPropertyValue.path)?.let {
                            cache[TaskProperty(taskDefinition, property)] = it
                        }
                    } else {
                        cache[TaskProperty(taskDefinition, property)] = newPropertyValue
                    }
                }
            }
        }
    }
}

sealed interface Result
data class NoTasksMatching(val name: String): Result
data class TasksToBeExecuted(
    val tasks: List<TaskDefinition>,
    val cachedTasks: List<CachedTask>,
): Result

data class CachedTask(val taskDefinition: TaskDefinition, val reasons: List<String>) {
    init {
        require(reasons.isNotEmpty()) {
            "Cannot cache task if there is no reason to, don't pass an empty reasons list"
        }
    }
}
data class TaskProperty(val taskDefinition: TaskDefinition, val property: KProperty1<TaskDefinition, *>)
