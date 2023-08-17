import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

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
    fun getOrThrow(name: String) = getOrNull(name) ?: throw NoSuchTaskException(name)

    infix fun TaskDefinition.requires(other: TaskDefinition) {
        if(this == other) throw IllegalStateException("Task can not require itself!")
        if(other.requirements.contains(this)) throw CycleDetectedException(listOf(this, other))

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
        if(this == other) throw IllegalStateException("Task can not introduce itself!")
        if(other.introductions.contains(this)) throw CycleDetectedException(listOf(this, other))

        _introductions.add(Introduction(this, other))
    }

    fun <T> TaskDefinition.referenceTo(target: TaskDefinition, kProperty0: KProperty0<T>): KProperty0<T> {
        if(this == target) {
            throw IllegalStateException(
                "Task ${this.name} tries to reference it's own property - use a simple property reference for that and referenceTo only for foreign properties"
            )
        }

        _propertyRequirements.add(kProperty0)
        _requirements.add(Requirement(this, target))
        return kProperty0
    }

    operator fun Cacheable.provideDelegate(
        thisRef: TaskDefinition,
        property: KProperty<*>
    ): KProperty0<*> {
        thisRef.cached[this] = property.name // TODO: Check whether that's safe
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
    fun traverse(root: String) = traverse(tasks.first { it.name == root })

    fun traverse(root: TaskDefinition): List<TaskDefinition> {
        val requirementsAndRoot = run {
            val visited: MutableSet<TaskDefinition> = LinkedHashSet()
            val stack = mutableListOf(root)

            while (stack.isNotEmpty()) {
                val vertex: TaskDefinition = stack.removeLast()
                if (visited.contains(vertex)) {
                    visited.remove(vertex)
                }

                for ((_, requiredTask) in _requirements.filter { it.task == vertex }) {
                    stack.add(requiredTask)
                    stack.addAll(requiredTask.introductions)
                }
                visited.add(vertex)
            }
            visited.reversed()
        }

        val introductions = run {
            val visited: MutableSet<TaskDefinition> = LinkedHashSet()
            val stack = mutableListOf<TaskDefinition>()
            stack.add(root)

            while (stack.isNotEmpty()) {
                val vertex: TaskDefinition = stack.removeLast()
                if (!visited.contains(vertex)) {
                    visited.add(vertex)
                    for (v in _introductions.filter { it.task == vertex }.map { it.introducedTask }) {
                        val dependenciesOfIntroducedTask = traverse(v)
                        stack.addAll(dependenciesOfIntroducedTask)
                        visited.addAll(dependenciesOfIntroducedTask)
                        stack.add(v)
                    }
                }
            }
            visited.remove(root)
            visited
        }

        return requirementsAndRoot + introductions
    }

}