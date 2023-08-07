import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.creat
import platform.posix.sleep
import kotlin.native.concurrent.AtomicInt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExecutorCachingTest {

    @Test
    fun `tasks are cached`() {
        val executor = Executor()
        val counter = AtomicInt(0)

        tasks {
            val task = object : TaskDefinition("someTask") {
                var cacheableProperty = "foo"
                val property by Cachable(::cacheableProperty)

                override fun execute() {
                    counter.addAndGet(1)
                }
            }
            register(task)

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(1, executor.cache.size, "Expected the executor to cache results of single task")

            val planResult = assertIs<TasksToBeExecuted>(executor.plan("someTask", this))
            assertContentEquals(
                listOf(CachedTask(task, listOf(task::property.name))),
                planResult.cachedTasks,
                "Expected exactly one task to be cached after ran once"
            )

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(1, counter.value, "Expected counter to be incremented exactly one time")

        }
    }

    @Test
    fun `tasks are cached for file property that did not change`() {
        val executor = Executor()
        val counter = AtomicInt(0)

        tasks {
            val filePath = TestDirectory.root / "${Random.nextInt()}.txt"
            val fileCreationResult = creat(filePath.toString(), 666.toUInt())
            assertEquals(3, fileCreationResult)

            val task = object : TaskDefinition("someTask") {
                var cacheableProperty = File(filePath.toString())
                val property by Cachable(::cacheableProperty)

                override fun execute() {
                    counter.addAndGet(1)
                }
            }
            register(task)

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(1, executor.cache.size, "Expected the executor to cache results of single task")

            val planResult = assertIs<TasksToBeExecuted>(executor.plan("someTask", this))
            assertContentEquals(
                listOf(CachedTask(task, listOf(task::property.name))),
                planResult.cachedTasks,
                "Expected exactly one task to be cached after ran once"
            )

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(1, counter.value, "Expected counter to be incremented exactly one time")
        }
    }

    @Test
    fun `tasks are not cached if file property changed`() {
        val executor = Executor()
        val counter = AtomicInt(0)

        tasks {
            val filePath = TestDirectory.root / "${Random.nextInt()}.txt"
            FileSystem.SYSTEM.openReadWrite(filePath, mustCreate = true).close()

            val task = object : TaskDefinition("someTask") {
                var cacheableProperty = File(filePath.toString())
                val property by Cachable(::cacheableProperty)

                override fun execute() {
                    counter.addAndGet(1)
                }
            }
            register(task)

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(1, executor.cache.size, "Expected the executor to cache results of single task")

            sleep(1.toUInt())
            writeSthToFile(filePath.toString())

            val planResult = assertIs<TasksToBeExecuted>(executor.plan("someTask", this))
            assertContentEquals(
                emptyList(),
                planResult.cachedTasks,
                "Expected exactly one task to be cached after ran once"
            )

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(2, counter.value, "Expected counter to be incremented exactly one time")
        }
    }
}