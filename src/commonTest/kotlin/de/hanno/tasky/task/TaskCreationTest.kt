package de.hanno.tasky.task

import kotlin.test.*

class TaskCreationTest {

    @Test
    fun `task is registered`() {
        tasks {
            register(name = "foo")
            register(name = "bar")

            assertTaskExists("bar")
            assertTaskExists("foo")
            assertTaskDoesNotExists("baz")
        }
    }

    @Test
    fun `task can not be registered twice`() {
        tasks {
            register(name = "foo")
            val cause = assertFails { register(name = "foo") }
            assertEquals("Task with name foo already registered!", cause.message)
        }
    }

    @Test
    fun `task requires other task directly`() {
        tasks {
            val fooTask = register(name = "foo")
            val barTask = register(name = "bar")

            fooTask requires barTask

            assertContentEquals(listOf(barTask), fooTask.requirements, fooTask.requirements.toString())
            assertContentEquals(listOf(), barTask.requirements)
        }
    }

    @Test
    fun `task introduces another task`() {
        tasks {
            val fooTask = register(name = "foo")
            val barTask = register(name = "bar")

            fooTask introduces barTask

            assertContentEquals(listOf(barTask), fooTask.introductions)
            assertContentEquals(listOf(), barTask.introductions)
        }
    }

    private fun TaskContainer.assertTaskExists(name: String): Task {
        val task = assertNotNull(getOrNull(name))
        assertEquals(name, task.name)
        return task
    }
    private fun TaskContainer.assertTaskDoesNotExists(name: String) = assertNull(getOrNull(name))

}