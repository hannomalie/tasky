import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExecutorTest {
    @Test
    fun `task requires and introduces tasks transitively`() {
        tasks {
            val grandParent0Task = register(name = "grandParent0")
            val grandParent1Task = register(name = "grandParent1")
            val parentTask = register(name = "parent")
            val childTask = register(name = "child")
            val grandChildTask = register(name = "grandChild")

            parentTask requires childTask
            parentTask introduces grandParent0Task
            grandParent0Task introduces grandParent1Task
            childTask requires grandChildTask

            val tasksToBeExecuted = assertIs<TasksToBeExecuted>(
                Executor().plan("parent", this)
            )
            assertContentEquals(
                listOf(grandChildTask, childTask, parentTask, grandParent0Task, grandParent1Task),
                tasksToBeExecuted.tasks,
                tasksToBeExecuted.tasks.toString()
            )
        }
    }

    @Test
    fun `custom task gets executed`() {
        tasks {
            val childTaskDefinition = object : TaskDefinition("child") {
                var property = "someProperty"
                override fun execute() {
                    property = "somePropertyChanged"
                }
            }
            register(childTaskDefinition)

            val parentTaskDefinition = object : TaskDefinition("parent") {
                val property by referenceTo(childTaskDefinition, childTaskDefinition::property)

                override fun execute() {
                    assertEquals("somePropertyChanged", property)
                }
            }
            register(parentTaskDefinition)

            Executor().execute("parent", this)
        }
    }
}

