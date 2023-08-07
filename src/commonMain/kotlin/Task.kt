import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

data class Requirement(val task: TaskDefinition, val requiredTask: TaskDefinition)
data class Introduction(val task: TaskDefinition, val introducedTask: TaskDefinition)

class TaskContainer {
    val _tasks: MutableList<TaskDefinition> = mutableListOf()
    val tasks: List<TaskDefinition> by ::_tasks
    val _requirements: MutableList<Requirement> = mutableListOf()
    val _introductions: MutableList<Introduction> = mutableListOf()
    val _propertyRequirements = mutableListOf<KProperty0<*>>()

    fun register(definition: TaskDefinition) {
        if(_tasks.any { it.name == definition.name }) throw TaskAlreadyRegisteredException(definition.name)
        _tasks.add(definition)
    }
    fun getOrNull(name: String): TaskDefinition? = tasks.firstOrNull { it.name == name }

    infix fun TaskDefinition.requires(other: TaskDefinition) {
        _requirements.add(Requirement(this, other))
    }
    val TaskDefinition.directRequirements get() = _requirements.filter {
        it.task == this
    }.map { it.requiredTask }

    val TaskDefinition.requirements: List<TaskDefinition> get() {
        var current = directRequirements

        return buildList {
            while(current.isNotEmpty()) {
                val directRequirements = current.flatMap { it.directRequirements }
                addAll(directRequirements)
                current = directRequirements
            }
            addAll(directRequirements)
        }
    }


    infix fun TaskDefinition.introduces(other: TaskDefinition) {
        _introductions.add(Introduction(this, other))
    }

    fun TaskDefinition.referenceTo(target: TaskDefinition, kProperty0: KProperty0<*>): KProperty0<*> {
        _propertyRequirements.add(kProperty0)
        _requirements.add(Requirement(this, target))
        return kProperty0
    }

    operator fun <T> Cachable<T>.provideDelegate(
        thisRef: TaskDefinition,
        property: KProperty<*>
    ): KProperty0<T> {
        thisRef.cached.add((property as KProperty1<TaskDefinition, *>)) // TODO: Check whether that's safe
        return delegateTo
    }

    val TaskDefinition.directIntroductions get() = _introductions.filter {
        it.task == this
    }.map { it.introducedTask }

    val TaskDefinition.introductions: List<TaskDefinition> get() {
        var current = directIntroductions

        return buildList {
            addAll(directIntroductions)

            while(current.isNotEmpty()) {
                val directIntroductions = current.flatMap { it.directIntroductions }
                addAll(directIntroductions)
                current = directIntroductions
            }
        }
    }

}

data class TaskAlreadyRegisteredException(val name: String): RuntimeException(
    "Task with name $name already registered!"
)

fun TaskContainer.register(name: String): TaskDefinition = TaskDefinition(name).apply { register(this) }

fun tasks(function: TaskContainer.() -> Unit) = TaskContainer().apply {
    function()
}

open class TaskDefinition(val name: String) {
    internal val cached = mutableListOf<KProperty1<TaskDefinition, *>>()

    open fun execute() { }

    override fun toString() = "Task[$name][cached:${cached.joinToString { it.name }}]"
    override fun equals(other: Any?): Boolean = other is TaskDefinition && other.name == name
    override fun hashCode(): Int = name.hashCode()
}

data class File(val path: String)