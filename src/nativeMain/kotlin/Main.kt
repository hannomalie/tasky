fun main(args: Array<String>) {
    Executor().apply {
        TaskContainer().apply {
            val taskOfInterest = object: Task("taskOfInterest") {
                val property = "myProperty"
                override fun execute() { println("Executing $name") }
            }.apply {
                register(this)
            }

            repeat(5) {
                val dependency = object: Task("dependency$it") {
                    override fun execute() { println("Executing $name") }
                }.apply {
                    register(this)
                }

                taskOfInterest requires dependency
            }

            repeat(5) {
                val followUp = object: Task("followUp$it") {
                    override fun execute() { println("Executing $name") }
                }.apply {
                    register(this)
                }

                taskOfInterest introduces followUp
            }
            val followUp3 = tasks.first { it.name == "followUp3" }
            val followUp3Dependency = object: Task("followUp3Dependency") {
                override fun execute() { println("Executing $name") }
            }.apply {
                register(this)
            }
            followUp3 requires followUp3Dependency

            val result = execute(args.firstOrNull() ?: taskOfInterest.name, this)
            println()
            println("######")
            println()

            when(result) {
                is NoTasksMatching -> println("No task matches ${result.name}, available: ${tasks.joinToString { it.name }}")
                is TasksToBeExecuted -> println(
                    "### Execution graph \n\n" +
                        "${result.tasks.joinToString("\n") { "-> ${it.name}" }}"
                )
            }

            println()
            println("### Task graph")
            println()

            println(traverse(taskOfInterest))
        }
    }
}
