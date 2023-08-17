import kotlinx.serialization.Serializable

data class Requirement(val task: Task, val requiredTask: Task)
data class Introduction(val task: Task, val introducedTask: Task)

data class CycleDetectedException(val tasks: List<Task>): RuntimeException(
    "Cycle detected! Tasks: $tasks"
)
data class TaskAlreadyRegisteredException(val name: String): RuntimeException(
    "Task with name $name already registered!"
)
data class NoSuchTaskException(val name: String): RuntimeException(
    "Task with name $name not found!"
)

fun TaskContainer.register(name: String): Task = Task(name).apply { register(this) }

fun tasks(function: TaskContainer.() -> Unit) = TaskContainer().apply {
    function()
}

open class Task(val name: String) {
    internal val cached = mutableMapOf<Cacheable, String>()

    open fun execute() { }

    override fun toString() = "Task[$name][cached:${cached.values.joinToString()}]"
    override fun equals(other: Any?): Boolean = other is Task && other.name == name
    override fun hashCode(): Int = name.hashCode()
}

data class File(val path: String)
@Serializable
data class SerializedFile(val path: String, val lastModification: Long?)
