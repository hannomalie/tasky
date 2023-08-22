import de.hanno.tasky.Executor
import de.hanno.tasky.NoTasksMatching
import de.hanno.tasky.TasksToBeExecuted
import de.hanno.tasky.task.Task
import de.hanno.tasky.task.TaskContainer
import org.jetbrains.skia.*
import org.jetbrains.skiko.GenericSkikoView
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoView
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants


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

            repeat(5) {
                val dependency = object : Task("dependency$it") {
                    override fun execute() {
                        Thread.sleep(2000)
                        println("Executing $name")
                    }
                }.apply {
                    register(this)
                }

                taskOfInterest requires dependency
            }

            repeat(5) {
                val followUp = object : Task("followUp$it") {
                    override fun execute() {
                        Thread.sleep(2000)
                        println("Executing $name")
                    }
                }.apply {
                    register(this)
                }

                taskOfInterest introduces followUp
            }
            val followUp3 = tasks.first { it.name == "followUp3" }
            val followUp3Dependency = object : Task("followUp3Dependency") {
                override fun execute() {
                    Thread.sleep(2000)
                    println("Executing $name")
                }
            }.apply {
                register(this)
            }
            followUp3 requires followUp3Dependency

            val result = executeAsync(args.firstOrNull() ?: taskOfInterest.name, this)
            println()
            println("######")
            println()

            when (val planResult = result.planResult) {
                is NoTasksMatching -> println("No task matches ${planResult.name}, available: ${tasks.joinToString { it.name }}")
                is TasksToBeExecuted -> {
                    println(
                        "### Execution graph \n\n" +
                                "${planResult.tasks.joinToString("\n") { "-> ${it.name}" }}"
                    )

                    val typeface = FontMgr.default.matchFamilyStyle("Arial", FontStyle.NORMAL)!!
                    val font = Font(typeface, 13f)
                    val windowDimension = Dimension(800, 600)
                    val skiaLayer = SkiaLayer()
                    val taskHeight = 35f
                    val arrowHeadWidth = 10

                    data class Position(val x: Float, val y: Float)
                    data class TaskBox(val task: Task, val leftTop: Position, val height: Float = taskHeight) {
                        val textOffset = Position(5f, 0.75f*height)
                        val width = task.name.length * 8.toFloat()

                        val leftBottom: Position get() = Position(0f, taskHeight)
                        val leftMiddle: Position get() = Position(0f, taskHeight/2)

                        val rightBottom: Position get() = Position(width, taskHeight)
                        val rightMiddle: Position get() = Position(width, taskHeight/2)
                    }

                    skiaLayer.skikoView = GenericSkikoView(skiaLayer, object : SkikoView {
                        val paint = Paint().apply {
                            color = Color.RED
                        }

                        override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                            canvas.clear(Color.WHITE)
//                            val ts = nanoTime / 5_000_000
//                            canvas.drawCircle((ts % width).toFloat(), (ts % height).toFloat(), 20f, paint)

                            val centerX = windowDimension.width / 2f
                            val centerY = windowDimension.height / 2f

                            val leftTasks = planResult.tasks.takeWhile { it != taskOfInterest }
                            val rightTasks = planResult.tasks - leftTasks.toSet() - taskOfInterest

                            val leftBoxes = run {
                                val leftBoxesPositionX = centerX - 200
                                leftTasks.mapIndexed { index, it ->
                                    val positionY =
                                        (centerY - (leftTasks.size.toFloat() / 2f) * taskHeight) + (1.5f * taskHeight * index)
                                    TaskBox(it, Position(leftBoxesPositionX, positionY))
                                }
                            }
                            val rightBoxes = run {
                                val leftBoxesPositionX = centerX + 200
                                rightTasks.mapIndexed { index, it ->
                                    val positionY =
                                        (centerY - (rightTasks.size.toFloat() / 2f) * taskHeight) + (1.5f * taskHeight * index)
                                    TaskBox(it, Position(leftBoxesPositionX, positionY))
                                }
                            }

                            val taskOfInterestBox = TaskBox(
                                taskOfInterest,
                                Position(centerX, centerY - (taskHeight / 2f))
                            )


                            leftBoxes.forEach { start ->
                                val color = if (result.currentTask == start.task) Color.MAGENTA else Color.BLACK
                                canvas.renderTask(start, color)
                                val targetIsLeftFromStart = taskOfInterestBox.leftTop.x < start.leftTop.x

                                canvas.drawConnectionLine(
                                    start,
                                    taskOfInterestBox,
                                    targetIsLeftFromStart,
                                    arrowHeadWidth,
                                    Color.RED
                                )
                                canvas.drawArrowHead(start, targetIsLeftFromStart, arrowHeadWidth, Color.RED)
                            }

                            rightBoxes.forEach { start ->
                                val color = if (result.currentTask == start.task) Color.MAGENTA else Color.BLACK
                                canvas.renderTask(start, color)
                                val targetIsLeftFromStart = taskOfInterestBox.leftTop.x < start.leftTop.x

                                canvas.drawConnectionLine(
                                    start,
                                    taskOfInterestBox,
                                    targetIsLeftFromStart,
                                    arrowHeadWidth,
                                    Color.GREEN
                                )
                                canvas.drawArrowHead(start, targetIsLeftFromStart, arrowHeadWidth, Color.GREEN)
                            }

                            val color = if (result.currentTask == taskOfInterest) Color.MAGENTA else Color.BLACK
                            canvas.renderTask(taskOfInterestBox, color)
                        }


                        private fun Canvas.drawConnectionLine(
                            start: TaskBox,
                            target: TaskBox,
                            targetIsLeftFromStart: Boolean,
                            arrowHeadWidth: Int,
                            color: Int
                        ) {
                            resetMatrix()
                            val startX =
                                start.leftTop.x + if (targetIsLeftFromStart) start.leftMiddle.x - arrowHeadWidth else start.rightMiddle.x + arrowHeadWidth
                            val startY =
                                start.leftTop.y + if (targetIsLeftFromStart) start.leftMiddle.y else start.rightMiddle.y

                            val targetX =
                                target.leftTop.x + if (targetIsLeftFromStart) target.rightMiddle.x else target.leftMiddle.x
                            val targetY =
                                target.leftTop.y + if (targetIsLeftFromStart) target.rightMiddle.y else target.leftMiddle.y

                            val path = Path().moveTo(
                                startX,
                                startY,
                            ).lineTo(
                                targetX,
                                targetY,
                            ).closePath()

                            drawPath(path, Paint().apply {
                                this.color = color
                                setStroke(true)
                            })
                        }

                        private fun Canvas.drawArrowHead(
                            start: TaskBox,
                            pointToLeft: Boolean,
                            arrowHeadWidth: Int,
                            color: Int
                        ) {
                            resetMatrix()
                            val startX =
                                start.leftTop.x + if (pointToLeft) start.leftMiddle.x - arrowHeadWidth else start.rightMiddle.x + arrowHeadWidth
                            val startY = start.leftTop.y + if (pointToLeft) start.leftMiddle.y else start.rightMiddle.y

                            val arrowHead = if (pointToLeft) {
                                val arrowHeadStartX = startX + arrowHeadWidth
                                val arrowHeadStartY = startY
                                Path().moveTo(arrowHeadStartX, arrowHeadStartY)
                                    .lineTo(arrowHeadStartX - arrowHeadWidth, arrowHeadStartY - arrowHeadWidth)
                                    .lineTo(arrowHeadStartX - arrowHeadWidth, arrowHeadStartY + arrowHeadWidth)
                                    .moveTo(arrowHeadStartX - arrowHeadWidth, arrowHeadStartY + arrowHeadWidth)
                                    .lineTo(arrowHeadStartX, arrowHeadStartY)
                                    .closePath()
                            } else {
                                val arrowHeadStartX = startX - arrowHeadWidth
                                val arrowHeadStartY = startY
                                Path().moveTo(arrowHeadStartX, arrowHeadStartY)
                                    .lineTo(arrowHeadStartX + 10f, arrowHeadStartY - 10f)
                                    .lineTo(arrowHeadStartX + 10f, arrowHeadStartY + 10f)
                                    .moveTo(arrowHeadStartX + 10f, arrowHeadStartY + 10f)
                                    .lineTo(arrowHeadStartX, arrowHeadStartY)
                                    .closePath()
                            }

                            drawPath(arrowHead, Paint().apply {
                                this.color = color
                                mode = PaintMode.FILL
                            })
                        }

                        private fun Canvas.renderTask(box: TaskBox, color: Int) {
                            resetMatrix()
                            translate(box.leftTop.x, box.leftTop.y)

                            val rectangle = Rect(0f, 0f, box.width, box.height)
                            drawRectShadow(rectangle, 1.5f, 1.5f, 1.5f, 1.5f, color)
                            drawRRect(RRect.makeLTRB(0f, 0f, box.width, box.height, 5f), Paint().apply {
                                this.color = color
                                mode = PaintMode.STROKE
                            })
                            drawTextLine(
                                TextLine.make(box.task.name, font),
                                box.textOffset.x,
                                box.textOffset.y,
                                Paint().apply { this.color = color })
                        }
                    })
                    SwingUtilities.invokeLater {
                        val window = JFrame("Skiko example").apply {
                            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                            preferredSize = windowDimension
                        }
                        skiaLayer.attachTo(window.contentPane)
                        skiaLayer.needRedraw()
                        window.pack()
                        window.isVisible = true
                    }
                }
            }
        }
    }
}
