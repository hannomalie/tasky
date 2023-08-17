import okio.FileSystem
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
            val task = object : Task("someTask") {
                var cacheableProperty = "foo"
                val property by Cacheable(::cacheableProperty)

                override fun execute() {
                    counter.addAndGet(1)
                }
            }
            register(task)

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(1, executor.cache.size, "Expected the executor to cache results of single task")

            val planResult = assertIs<TasksToBeExecuted>(executor.plan("someTask", this))
            assertContentEquals(
                listOf(
                    CachedTask(task, listOf(task::property.name))
                ),
                planResult.cachedTasks,
                "Expected exactly one task to be cached after ran once"
            )

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(1, counter.value, "Expected counter to be incremented exactly one time")
        }
    }

    @Test
    fun `tasks are cached with file cache`() {

        val file = TestDirectory.root / (Random.nextInt().toString() + ".cache.json")
        FileSystem.SYSTEM.openReadWrite(file, true).close()

        val cache = FileBasedCache(File(file.toString()))

        val executor = Executor(cache)
        val counter = AtomicInt(0)

        tasks {
            val task = object : Task("someTask") {
                var cacheableProperty = "foo"
                val property by Cacheable(::cacheableProperty)

                override fun execute() {
                    counter.addAndGet(1)
                }
            }
            register(task)

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(1, executor.cache.size, "Expected the executor to cache results of single task: $cache")

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
    fun `tasks are cached when property reference is used`() {
        val executor = Executor()
        val counter = AtomicInt(0)

        tasks {
            val dependency = object : Task("dependency") {
                val cacheableProperty = "someString"
                val property by Cacheable(::cacheableProperty)

                override fun execute() {
                    counter.addAndGet(1)
                }
            }
            val task = object : Task("someTask") {
                val property by Cacheable(referenceTo(dependency, dependency::cacheableProperty))

                override fun execute() {
                    counter.addAndGet(1)
                }
            }
            register(task)

            val executionResult = assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(2, counter.value, "Expected counter to be incremented exactly one time per task")
            assertContentEquals(
                listOf(dependency, task),
                executionResult.tasks,
                "Expected both tasks to be run"
            )
            assertEquals(2, executor.cache.size, "Expected the executor to cache results of the two tasks")

            val planResult = assertIs<TasksToBeExecuted>(executor.plan("someTask", this))
            assertContentEquals(
                listOf(CachedTask(dependency, listOf(dependency::property.name)), CachedTask(task, listOf(task::property.name))),
                planResult.cachedTasks,
                "Expected both tasks to be cached after ran once"
            )

            assertIs<TasksToBeExecuted>(executor.execute("someTask", this))
            assertEquals(2, counter.value, "Expected counter to be incremented exactly one time per task")
        }
    }

    @Test
    fun `tasks are cached for file property that did not change`() {
        val executor = Executor()
        val counter = AtomicInt(0)

        tasks {
            val filePath = TestDirectory.root / "${Random.nextInt()}.txt"
            FileSystem.SYSTEM.openReadWrite(filePath, true).close()

            val task = object : Task("someTask") {
                var cacheableProperty = File(filePath.toString())
                val property by Cacheable(::cacheableProperty)

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

            val task = object : Task("someTask") {
                var cacheableProperty = File(filePath.toString())
                val property by Cacheable(::cacheableProperty)

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