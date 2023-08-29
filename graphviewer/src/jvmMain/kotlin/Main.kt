import de.hanno.tasky.Executor
import de.hanno.tasky.NoTasksMatching
import de.hanno.tasky.TasksToBeExecuted
import de.hanno.tasky.task.*
import org.graphstream.graph.Graph
import org.graphstream.graph.implementations.SingleGraph
import org.graphstream.ui.view.Viewer
import kotlin.random.Random
import kotlin.random.nextUInt


fun TaskContainer.createTaskWithSomeDependencies(dependencyCount: Int = 3, introductionCount: Int = 1): Task {
    return object : Task(Random.nextUInt().toString()) {
        val property = "myProperty"
        override fun execute() {
            Thread.sleep(2000)
            println("Executing $name")
        }
    }.apply {
        register(this)

        repeat(dependencyCount) {
            this requires createTaskWithSomeDependencies(dependencyCount - 1, introductionCount - 1)
        }
        repeat(introductionCount) {
            this introduces createTaskWithSomeDependencies(dependencyCount - 1, 0)
        }
    }
}

fun main(args: Array<String>) {
    Executor().apply {
        val taskContainer = TaskContainer().apply {
            val taskOfInterest = object : Task("taskOfInterest") {
                val property = "myProperty"
                override fun execute() {
                    Thread.sleep(2000)
                    println("Executing $name")
                }
            }.apply {
                register(this)
            }

            repeat(2) {
                taskOfInterest requires createTaskWithSomeDependencies()
                taskOfInterest introduces createTaskWithSomeDependencies()
            }
            System.setProperty("org.graphstream.ui", "swing")
            val graph = SingleGraph("I can see dead pixels").apply {
                setAttribute(
                    "ui.stylesheet",
                    styleSheet
                )
            }
            when (val planResult = plan(args.firstOrNull() ?: taskOfInterest.name, this)) {
                is NoTasksMatching -> println("No task matches ${planResult.name}, available: ${tasks.joinToString { it.name }}")
                is TasksToBeExecuted -> graph.apply {
                    tasks.forEach {
                        val node = addNode(it.name)
                        node.setAttribute("ui.label", it.name)
                    }
                    planResult.tasks.forEach { task ->
                        task.directRequirements.forEach {
                            addEdge(task.name + "_" + it.name, it.name, task.name, true).apply {
                                setAttribute("type", TaskContainer.EdgeType.Dependency)
                            }
                        }
                        task.directIntroductions.forEach {
                            addEdge(task.name + "_" + it.name, task.name, it.name, true).apply {
                                setAttribute("type", TaskContainer.EdgeType.Finalization)
                            }
                        }
                    }
                    getNode(taskOfInterest.name).setAttribute("ui.class", "start")
                }
            }

            graph.display()

            executeAsyncParallel(
                args.firstOrNull() ?: taskOfInterest.name, this, listeners = listOf(
                    PrintingListener(),
                    ExecutionNodeStateListener(graph, taskOfInterest)
                )
            )
        }
    }
}

class PrintingListener : Executor.Listener {
    override fun taskStarted(task: Task) = println("Started ${task.name}")
    override fun taskFinished(task: Task) = println("Finished ${task.name}")
}

private class ExecutionNodeStateListener(
    private val graph: SingleGraph,
    private val startTask: Task
) : Executor.Listener {
    override fun taskStarted(task: Task) {
        graph.getNode(task.name).apply {
            val uiClass = if(task == startTask) "startexecuting" else "executing"
            setAttribute("ui.class", uiClass)
            edges().forEach { edge ->
                when(edge.getAttribute("type", TaskContainer.EdgeType::class.java)) {
                    TaskContainer.EdgeType.Dependency -> edge.setAttribute("ui.class", "dependency")
                    TaskContainer.EdgeType.Finalization -> edge.setAttribute("ui.class", "finalization")
                }
            }
        }
    }

    override fun taskFinished(task: Task) {
        try {
            val s = if(task == startTask) "startexecuted" else "executed"
            graph.getNode(task.name).setAttribute("ui.class", s)

        } catch (e: NoSuchElementException) {
            e.printStackTrace()
        }
    }

}

val styleSheet = """
node { 
    shape: box;
    text-alignment: at-right;
    text-padding: 3px, 2px;
    text-background-mode: rounded-box;
    text-background-color: #EB2;
    text-color: #222;
      
    fill-color: #DEE;
    stroke-mode: plain;
    stroke-color: #555;
   }
   node.start {
    text-size: 16;
    text-background-color: #A7CC;
   }
   node.startexecuting {
    text-size: 16;
    text-background-color: red;
   }
   node.startexecuted {
    text-size: 16;
    text-background-color: green;
   }
   node.executing{ 
    fill-color: red; 
    text-background-color: red;
   }
   node.executed{ 
    fill-color: green; 
    text-background-color: grey;
   }
   
   edge {
    fill-color: #444;
    arrow-size: 8px;
    shape: line;  
   }
   edge.dependency{ 
    fill-color: red;
    shape: cubic-curve;  
    arrow-shape: arrow;
   }
   edge.finalization{
    arrow-shape: arrow;
    fill-color: green; 
   }
""".trimIndent()