package de.hanno.tasky.task

import de.hanno.tasky.cache.Cacheable
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class TaskContainer {
    val _tasks: MutableList<Task> = mutableListOf()
    val tasks: List<Task> by ::_tasks
    val _requirements: MutableList<Requirement> = mutableListOf()
    val _introductions: MutableList<Introduction> = mutableListOf()
    val _propertyRequirements = mutableListOf<KProperty0<*>>()

    fun register(definition: Task) {
        if(_tasks.any { it.name == definition.name }) throw TaskAlreadyRegisteredException(definition.name)
        _tasks.add(definition)
    }
    fun getOrNull(name: String): Task? = tasks.firstOrNull { it.name == name }
    fun getOrThrow(name: String) = getOrNull(name) ?: throw NoSuchTaskException(name)

    infix fun Task.requires(other: Task) {
        if(this == other) throw IllegalStateException("Task can not require itself!")
        if(other.requirements.contains(this)) throw CycleDetectedException(listOf(this, other))

        _requirements.add(Requirement(this, other))
    }
    val Task.directRequirements get() = _requirements.filter {
        it.task == this
    }.map { it.requiredTask }

    val Task.requirements: List<Task> get() {
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

    val Task.requiredBy: List<Requirement> get() = _requirements.filter { it.requiredTask == this }

    infix fun Task.introduces(other: Task) {
        if(this == other) throw IllegalStateException("Task can not introduce itself!")
        if(other.introductions.contains(this)) throw CycleDetectedException(listOf(this, other))

        _introductions.add(Introduction(this, other))
    }

    fun <T> Task.referenceTo(target: Task, kProperty0: KProperty0<T>): KProperty0<T> {
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
        thisRef: Task,
        property: KProperty<*>
    ): KProperty0<*> {
        thisRef.cached[this] = property.name // TODO: Check whether that's safe
        return delegateTo
    }

    val Task.directIntroductions get() = _introductions.filter {
        it.task == this
    }.map { it.introducedTask }

    val Task.introductions: List<Task> get() {
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

    fun traverse(root: Task): List<Task> {
        val requirementsAndRoot = run {
            val visited: MutableSet<Task> = LinkedHashSet()
            val stack = mutableListOf(root)

            while (stack.isNotEmpty()) {
                val vertex: Task = stack.removeLast()
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
            val visited: MutableSet<Task> = LinkedHashSet()
            val stack = mutableListOf<Task>()
            stack.add(root)

            while (stack.isNotEmpty()) {
                val vertex: Task = stack.removeLast()
                if (!visited.contains(vertex)) {
                    visited.add(vertex)
                    val introductionsForTask = _introductions.filter { it.task == vertex }.map { it.introducedTask }
                    for (v in introductionsForTask) {
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

    enum class EdgeType { Dependency, Finalization }
    data class Edge(val type: EdgeType, val target: Node)
    data class Node(val task: Task, val edges: List<Edge>)

    fun newTraverse(root: Task): Node {
        fun unfold(task: Task): Node =if(task.requirements.isEmpty() && task.introductions.isEmpty()) {
            Node(task, emptyList())
        } else {
            val edges = mutableListOf<Edge>()

            edges.addAll(task.requirements.map {
                Edge(EdgeType.Dependency, unfold(it))
            })

            edges.addAll(task.introductions.map {
                Edge(EdgeType.Finalization, unfold(it))
            })

            Node(task, edges)
        }

        return unfold(root)
    }

}