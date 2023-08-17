import kotlinx.serialization.Serializable

data class Requirement(val task: TaskDefinition, val requiredTask: TaskDefinition)
data class Introduction(val task: TaskDefinition, val introducedTask: TaskDefinition)

data class CycleDetectedException(val tasks: List<TaskDefinition>): RuntimeException(
    "Cycle detected! Tasks: $tasks"
)
data class TaskAlreadyRegisteredException(val name: String): RuntimeException(
    "Task with name $name already registered!"
)
data class NoSuchTaskException(val name: String): RuntimeException(
    "Task with name $name not found!"
)

fun TaskContainer.register(name: String): TaskDefinition = TaskDefinition(name).apply { register(this) }

fun tasks(function: TaskContainer.() -> Unit) = TaskContainer().apply {
    function()
}

open class TaskDefinition(val name: String) {
    internal val cached = mutableMapOf<Cacheable, String>()

    open fun execute() { }

    override fun toString() = "Task[$name][cached:${cached.values.joinToString()}]"
    override fun equals(other: Any?): Boolean = other is TaskDefinition && other.name == name
    override fun hashCode(): Int = name.hashCode()
}

data class File(val path: String)
@Serializable
data class SerializedFile(val path: String, val lastModification: Long?)
