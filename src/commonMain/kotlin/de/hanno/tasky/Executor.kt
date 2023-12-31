package de.hanno.tasky

import de.hanno.tasky.cache.Cache
import de.hanno.tasky.cache.InMemoryCache
import de.hanno.tasky.task.Task
import de.hanno.tasky.task.TaskContainer
import kotlinx.coroutines.*

class Executor(internal val cache: Cache = InMemoryCache()) {
    fun plan(taskName: String, taskContainer: TaskContainer): Result {
        cache.beforePlan(taskContainer)

        val taskToExecute = taskContainer.tasks.firstOrNull {
            it.name == taskName
        } ?: return NoTasksMatching(taskName)

        return taskContainer.run {
            val tasksToBeExecuted = traverse(taskToExecute)

            val cachedTasks = determineCachedTasks(tasksToBeExecuted)

            val nonCachedTasksToBeExecuted = tasksToBeExecuted - cachedTasks.map { it.task }.toSet()

            TasksToBeExecuted(
                nonCachedTasksToBeExecuted,
                cachedTasks,
            )
        }
    }

    private fun determineCachedTasks(tasksToBeExecuted: List<Task>): List<CachedTask> = buildList {
        tasksToBeExecuted.forEach { task ->

            task.cached.takeIf { it.isNotEmpty() }?.let { cacheCandidates ->
                val changedCachedProperties =
                    cache.getChangedCacheableProperties(cacheCandidates.keys.toList(), task)

                val cachedPropertiesChanged = changedCachedProperties.isNotEmpty()
                if (!cachedPropertiesChanged) {
                    add(CachedTask(task, cacheCandidates.map { it.value }))
                }
            }
        }
    }

    fun execute(taskName: String, taskContainer: TaskContainer): Result = plan(taskName, taskContainer).apply {
        when (this) {
            is NoTasksMatching -> {}
            is TasksToBeExecuted -> tasks.forEach { task ->
                task.execute()
                task.cached.forEach { cacheable ->
                    cache.putPropertyValue(task.name, cacheable.key.delegateTo)
                }
            }
        }
        cache.afterExecution()
    }

    interface Listener {
        fun taskStarted(task: Task)
        fun taskFinished(task: Task)
    }
    fun executeAsyncParallel(taskName: String, taskContainer: TaskContainer, listeners: List<Listener> = emptyList()) = plan(taskName, taskContainer).apply {
        GlobalScope.launch(Dispatchers.IO) {
            when (this@apply) {
                is NoTasksMatching -> {}
                is TasksToBeExecuted -> {
                    taskContainer.apply {
                        suspend fun Task.executeAndSpread() {
                            coroutineScope {
                                taskContainer.run {
                                    this@executeAndSpread.directRequirements.map {
                                        launch {
                                            it.executeAndSpread()
                                        }
                                    }.joinAll()
                                }

                                listeners.forEach { it.taskStarted(this@executeAndSpread) }
                                execute()
                                cached.forEach { cacheable ->
                                    cache.putPropertyValue(name, cacheable.key.delegateTo)
                                }
                                listeners.forEach { it.taskFinished(this@executeAndSpread) }
                                directIntroductions.map {
                                    launch {
                                        it.executeAndSpread()
                                    }
                                }.joinAll()
                            }
                        }

                        this@apply.tasks.first { it.name == taskName }.executeAndSpread()
                    }
                }
            }
            cache.afterExecution()
        }
        println("returning state")
    }
}

sealed interface Result
data class NoTasksMatching(val name: String) : Result
data class TasksToBeExecuted(
    val tasks: List<Task>,
    val cachedTasks: List<CachedTask>,
) : Result

data class CachedTask(val task: Task, val reasons: List<String>) {
    init {
        require(reasons.isNotEmpty()) {
            "Cannot cache task if there is no reason to, don't pass an empty reasons list"
        }
    }
}
