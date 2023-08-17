# Tasky

A small library that lets you define pieces of work as Tasks and wire them
together so that they can be executed in the expected order.
A task can have dependencies to other tasks or can introduce other
tasks that need to be run after itself.

### Simple example

```kotlin
TaskContainer().apply {
    // Define some tasks
    val taskOfInterest = register("taskOfInterest")
    val taskOfInterestDependency = register("taskOfInterestDependency")
    val taskOfInterestDependencyDependency = register("taskOfInterestDependencyDependency")
    val taskOfInterestIntroduction = register("taskOfInterestDependency")
    
    // Define their relationships
    taskOfInterest requires taskOfInterestDependency
    taskOfInterestDependency requires taskOfInterestDependencyDependency
    taskOfInterest requires taskOfInterestDependencyDependency
    taskOfInterest introduces taskOfInterestIntroduction

    // traverse the underlying task graph
    val tasksToBeExecuted = traverse(taskOfInterest)

    // this is what we expect the execution list to look like
    assertContentEquals(
        listOf(
            taskOfInterestDependencyDependency,
            taskOfInterestDependency,
            taskOfInterest,
            taskOfInterestIntroduction
        ),
        tasksToBeExecuted,
}
```

Those example tasks don't do anything, they are just empty tasks, each given a name.
If you want to actually implement something real, you would implement a Task class like so:

### Usual tasks

```kotlin
val childTask = object : Task("myTask") {
    override fun execute() {
        println("$name executing...")
    }
}
taskContainer.register(childTask)

Executor().execute("myTask", taskContainer)
```

Instead of manually wiring tasks together, you can directly express dependencies between input and output of tasks and
the task dependency could be derived by the library. ""Input and output" here really only means plain old properties of your
task class.

### Task inputs and outputs and dependencies on them

When a piece of work depends on another piece of work, the reason for that
is (almost?) always that one piece of work consumes some output of the other
piece of work. Whenever such a thing is given, the dependency between the tasks becomes clear.
Thanks to Kotlin's delegated properties, we have a first class citizen in the language to model that, take a look
at this example:

```kotlin
TaskContainer().apply {
    val childTask = object : Task("child") {
        var property = "someProperty" // we define some property
        override fun execute() {
            property = "somePropertyChanged" // which is changed when the task executes
        }
    }
    register(childTask)

    val parentTask = object : Task("parent") {
        // this is an implicit task dependency
        // this property (a task input) is derived from a foreign property (a task output) 
        // the referenceTo method is available on the task container, which is responsible
        // for the relationships between tasks, not the task definitions themselves
        val property by referenceTo(childTask, childTask::property)

        override fun execute() {
            // child task is a task dependency, so it's executed before parent, so the linked property will reflect the change
            assertEquals("somePropertyChanged", property)
        }
    }
    register(parentTask)
}
```

### Caching

When the input for your work didn't change, why would you execute it again?
The answer is: Only when it depends on some undeclared inputs, some external state, some untracked side effects.
For that case, it's okay to rely on explicit wiring of tasks, but other than that, I would recommend
to model inputs and outputs clearly and (automatically) enable caching of your task's work.

```kotlin
TaskContainer().apply {
    val task = object : Task("someTask") {
        var cacheableProperty = File(filePath.toString())
        val property by Cacheable(::cacheableProperty)

        override fun execute() {
            println("Executing $name")
        }
    }
    register(task)    
}
```
When a property is wrapped by the Cacheable delegate (which can only be done through a task container again),
the task becomes subject to caching. It will only be executed, when at least one of its inputs changes.
For file properties - files and directories are supported - that means when either the file or any file
anywhere in a directory changes, the task will be executed. For other properties, comparison by value is done.
No change? No execution.
By passing the result of `referenceTo` to a Cacheable delegate, you can also use foreign task's outputs to implicitly
depend on the task and additionally execute the task only when those outputs also changed:

```kotlin
TaskContainer().apply {
    val dependency = object : Task("dependency") {
        val cacheableProperty = "someString"
        val property by Cacheable(::cacheableProperty)

        override fun execute() {
            println("Executing $name")
        }
    }
    val task = object : Task("someTask") {
        val property by Cacheable(referenceTo(dependency, dependency::cacheableProperty))

        override fun execute() {
            println("Executing $name")
        }
    }   
}
```

### Rationale

The sophisticated reader might already realize that this project is inspired by Gradle and its task graph.
Well, Gradle is unfortunately disliked or hated by a lot of people for a number of reasons.
From my pov however, a task graph is a much better abstraction over whatever kind of work you need to automate
than other concepts, like Mavens phase model. It is one of Gradle's big benefits. So I tried to extract this good thing
as an independent library, without inheriting the subjectively or objectively bad things Gradle does on top of that.
For example Gradle is stuck with weird Groovy property delegation concepts, annotations for input and output and also
adds some unnecessarily confusing or unintuitive dsl elements on top, like the "by registering, by getting" stuff.
For now, I will keep such as magic perceived elements absent or at a minimum at the cost of convenience when writing the code.
