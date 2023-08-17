class Executor(internal val cache: Cache = InMemoryCache()) {
    fun plan(taskName: String, taskContainer: TaskContainer): Result {
        cache.beforePlan(taskContainer)

        val taskToExecute = taskContainer.tasks.firstOrNull {
            it.name == taskName
        } ?: return NoTasksMatching(taskName)

        return taskContainer.run {
            val tasksToBeExecuted = traverse(taskToExecute)

            val cachedTasks = determineCachedTasks(tasksToBeExecuted)

            val nonCachedTasksToBeExecuted = tasksToBeExecuted - cachedTasks.map { it.taskDefinition }.toSet()

            TasksToBeExecuted(
                nonCachedTasksToBeExecuted,
                cachedTasks,
            )
        }
    }

    private fun determineCachedTasks(tasksToBeExecuted: List<TaskDefinition>): List<CachedTask> = buildList {
        tasksToBeExecuted.forEach { taskDefinition ->

            taskDefinition.cached.takeIf { it.isNotEmpty() }?.let { cacheCandidates ->
                val changedCachedProperties =
                    cache.getChangedCacheableProperties(cacheCandidates.keys.toList(), taskDefinition)

                val cachedPropertiesChanged = changedCachedProperties.isNotEmpty()
                if (!cachedPropertiesChanged) {
                    add(CachedTask(taskDefinition, cacheCandidates.map { it.value }))
                }
            }
        }
    }

    fun execute(taskName: String, taskContainer: TaskContainer): Result = plan(taskName, taskContainer).apply {
        when(this) {
            is NoTasksMatching -> { }
            is TasksToBeExecuted -> tasks.forEach { taskDefinition ->
                taskDefinition.execute()
                taskDefinition.cached.forEach { cacheable ->
                    cache.putPropertyValue(taskDefinition.name, cacheable.key.delegateTo)
                }
            }
        }
        cache.afterExecution()
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
