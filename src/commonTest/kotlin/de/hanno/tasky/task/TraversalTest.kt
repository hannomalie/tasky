package de.hanno.tasky.task

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFails
import kotlin.test.assertIs

class TraversalTest {

    @Test
    fun `traversal is correct when multiple tasks depend on a task`() {
        TaskContainer().apply {
            val taskOfInterest = register("taskOfInterest")
            val taskOfInterestDependency = register("taskOfInterestDependency")
            val taskOfInterestDependencyDependency = register("taskOfInterestDependencyDependency")

            taskOfInterest requires taskOfInterestDependency
            taskOfInterestDependency requires taskOfInterestDependencyDependency
            taskOfInterest requires taskOfInterestDependencyDependency

            val tasksToBeExecuted = traverse(taskOfInterest)

            assertContentEquals(
                listOf(
                    taskOfInterestDependencyDependency,
                    taskOfInterestDependency,
                    taskOfInterest
                ),
                tasksToBeExecuted,
                tasksToBeExecuted.toString()
            )
        }
    }

    @Test
    fun `cycle is detected for requirements`() {
        TaskContainer().apply {
            val taskOfInterest = register("taskOfInterest")
            val taskOfInterestDependency = register("taskOfInterestDependency")
            val taskOfInterestDependencyDependency = register("taskOfInterestDependencyDependency")

            taskOfInterest requires taskOfInterestDependency
            taskOfInterestDependency requires taskOfInterestDependencyDependency

            val throwable = assertFails {
                taskOfInterestDependencyDependency requires taskOfInterest
            }
            assertIs<CycleDetectedException>(throwable)
        }
    }

    @Test
    fun `cycle is detected for introductions`() {
        TaskContainer().apply {
            val taskOfInterest = register("taskOfInterest")
            val taskOfInterestIntroduction = register("taskOfInterestDependency")

            taskOfInterest introduces taskOfInterestIntroduction

            val throwable = assertFails {
                taskOfInterestIntroduction introduces taskOfInterest
            }
            assertIs<CycleDetectedException>(throwable)
        }
    }

    @Test
    fun `traversal is correct`() {

        TaskContainer().apply {
            val taskOfInterest = register("taskOfInterest")

            repeat(2) { directDependencyCounter ->
                val dependency = register("dependency$directDependencyCounter")

                repeat(2) { indirectDependencyCounter ->
                    val dependencyDependency = register("${dependency.name}_Dependency$indirectDependencyCounter")
                    dependency requires dependencyDependency

                    val dependencyDependencyFollowUp = register("${dependencyDependency.name}_FollowUp")
                    dependencyDependency introduces dependencyDependencyFollowUp

                    val dependencyDependencyFollowUpDependency = register("${dependencyDependencyFollowUp.name}_Dependency")
                    dependencyDependencyFollowUp requires dependencyDependencyFollowUpDependency
                }
                taskOfInterest requires dependency
            }

            repeat(2) { directFollowUpCounter ->
                val followUp = register("followUp$directFollowUpCounter")
                repeat(2) { followUpCounter ->
                    val followUpDependency = register("${followUp.name}_Dependency$followUpCounter")
                    followUp requires followUpDependency

                    val followUpDependencyDependency = register("${followUp.name}_Dependency${followUpCounter}_Dependency")
                    followUpDependency requires followUpDependencyDependency

                    val followUpFollowUp = register("${followUp.name}_FollowUp$followUpCounter")
                    followUp introduces followUpFollowUp
                }

                taskOfInterest introduces followUp
            }

            val tasksToBeExecuted = traverse(taskOfInterest)
            assertContentEquals(
                listOf(
                    getOrThrow("dependency0_Dependency0"),
                    getOrThrow("dependency0_Dependency0_FollowUp_Dependency"),
                    getOrThrow("dependency0_Dependency0_FollowUp"),
                    getOrThrow("dependency0_Dependency1"),
                    getOrThrow("dependency0_Dependency1_FollowUp_Dependency"),
                    getOrThrow("dependency0_Dependency1_FollowUp"),
                    getOrThrow("dependency0"),

                    getOrThrow("dependency1_Dependency0"),
                    getOrThrow("dependency1_Dependency0_FollowUp_Dependency"),
                    getOrThrow("dependency1_Dependency0_FollowUp"),
                    getOrThrow("dependency1_Dependency1"),
                    getOrThrow("dependency1_Dependency1_FollowUp_Dependency"),
                    getOrThrow("dependency1_Dependency1_FollowUp"),
                    getOrThrow("dependency1"),

                    taskOfInterest,

                    getOrThrow("followUp0_Dependency0_Dependency"),
                    getOrThrow("followUp0_Dependency0"),
                    getOrThrow("followUp0_Dependency1_Dependency"),
                    getOrThrow("followUp0_Dependency1"),
                    getOrThrow("followUp0"),
                    getOrThrow("followUp0_FollowUp0"),
                    getOrThrow("followUp0_FollowUp1"),

                    getOrThrow("followUp1_Dependency0_Dependency"),
                    getOrThrow("followUp1_Dependency0"),
                    getOrThrow("followUp1_Dependency1_Dependency"),
                    getOrThrow("followUp1_Dependency1"),
                    getOrThrow("followUp1"),
                    getOrThrow("followUp1_FollowUp0"),
                    getOrThrow("followUp1_FollowUp1"),
                ),
                tasksToBeExecuted,
                tasksToBeExecuted.toString()
            )
        }
    }
}