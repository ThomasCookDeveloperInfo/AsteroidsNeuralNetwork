package GUI

import Learning.Genetics
import Simulation.Simulation
import javafx.animation.AnimationTimer
import javafx.fxml.FXML
import javafx.scene.paint.Color
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.layout.VBox
import Simulation.Configuration

private const val SIMULATIONS_TO_RUN = 9
private const val COLUMNS = 3

// The controller for all the simulations
class SimulationController(@FXML private var mainPane: VBox? = null,
                           @FXML private var resetButton: Button? = null,
                           @FXML private var canvas: Canvas? = null,
                           private val simulations: MutableCollection<Simulation> = mutableListOf(),
                           private val genetics: Genetics = Genetics()) {

    // Track the selected cell index
    private var selectedCellIndex: Int? = null

    // Frame timer for updating the sim states and redrawing
    private val frameTimer: AnimationTimer = object: AnimationTimer() {
        override fun handle(now: Long) {
            // Update the simulations
            simulations.forEach { it.update() }

            // Check if all sims have finished
            if (simulations.sumBy { if (it.isFinished()) 1 else 0 } == simulations.size) {
                // Set the fitness of each population member
                genetics.setPopulationFitnesses(simulations.map { it.getFitness() })

                // Ok, carry out genetics on the sims weights
                val newWeights = genetics.epoch()

                // Then reset sims and apply weights to sim networks
                simulations.forEachIndexed { index, sim ->
                    sim.applyWeights(newWeights.elementAt(index))
                    sim.start()
                }
            }

            // Invalide
            invalidate()
        }
    }

    // Find out which cell was clicked by the mouse
    private fun mouseClickToCellIndex(mouseX: Double, mouseY: Double) : Int? {
        canvas?.let {
            // Work out the render dimensions
            val canvasWidth = it.width
            val canvasHeight = it.height
            val simRenderHeight = canvasHeight / COLUMNS
            val simRenderWidth = canvasWidth / COLUMNS

            val mouseColumn = (mouseX / simRenderWidth).toInt()
            val mouseRow = (mouseY / simRenderHeight).toInt()

            // Convert 2d coords to 1d array position
            val coordinate = mouseRow * COLUMNS + mouseColumn
            return if (simulations.size - 1 > coordinate) coordinate else null
        }
        return null
    }

    // Reset everything
    @FXML
    fun initialize() {
        resetButton?.setOnMouseClicked {
            reset(Configuration())
        }

        // Setup the canvas to resize itself based on the parent frame
        mainPane?.heightProperty()?.addListener({ ov, oldValue, newValue ->
            canvas?.let {
                val settingsHeight = resetButton?.let { it.prefHeight } ?: 0.0
                it.height = newValue.toDouble() - settingsHeight
            }
        })
        mainPane?.widthProperty()?.addListener({ ov, oldValue, newValue ->
            canvas?.let {
                it.width = newValue.toDouble()
            }
        })

        // Reset everything
        reset(Configuration())
    }

    private fun reset(configuration: Configuration) {
        // Stop the frame timer
        frameTimer.stop()

        // Reset selected cell index
        selectedCellIndex = null

        // Remove on click listener
        mainPane?.setOnMouseClicked { null }

        // When the mouse is clicked, update the selected cell index
        mainPane?.setOnMouseClicked {
            if (selectedCellIndex === null) {
                selectedCellIndex = mouseClickToCellIndex(it.x, it.y)
            } else {
                selectedCellIndex = null
            }
        }

        // Reset the genetics
        genetics.reset()

        // Clear the simulations
        simulations.clear()

        // Reset all the simulations
        for (simulation in 0 until SIMULATIONS_TO_RUN) {
            val simulation = Simulation(configuration)
            simulation.start()
            simulations.add(simulation)
        }

        // Setup the genetics
        simulations.forEach {
            genetics.addPopulationMember(it.getWeights())
        }

        // Start the frame time
        frameTimer.start()
    }

    // Invalidate the canvas and redraw everything
    private fun invalidate() {
        canvas?.let { canvas ->
            // Get the canvas dimensions
            val canvasWidth = canvas.width
            val canvasHeight = canvas.height

            // Get the context
            val context = canvas.graphicsContext2D ?: return

            // Clear the context
            context.clearRect(0.0, 0.0, canvasWidth, canvasHeight)

            // Work out if we are rendering all columns or one
            val currentRenderingColumns = if (selectedCellIndex === null) COLUMNS else 1

            // Work out row height
            val simRenderHeight = canvasHeight / currentRenderingColumns

            // Loop over columns and rows
            for (row in 0..currentRenderingColumns) {
                for (column in 0..currentRenderingColumns) {
                    // Convert 2d coords to 1d array position
                    val simIndex = row * COLUMNS + column

                    // Get the sim at this position if it exists
                    val sim = simulations.elementAtOrNull(selectedCellIndex ?: simIndex)

                    // If the sim at this position exists, render it
                    sim?.let {
                        // Work out the render dimensions
                        val simRenderWidth = canvasWidth / currentRenderingColumns

                        // Work out the x and y origin
                        val xOrigin = column * simRenderWidth
                        val yOrigin = row * simRenderHeight

                        // Get the set of shapes to draw
                        val shapes = it.getShapes(simRenderWidth, xOrigin, simRenderHeight, yOrigin)

                        // Get debug shapes
                        val debugShapes = it.getDebugShapes(simRenderWidth, xOrigin, simRenderHeight, yOrigin)

                        // Draw to the canvas
                        context.apply {
                            // Fill the sims rect with black
                            fill = Color.WHITE
                            fillRect(xOrigin, yOrigin, simRenderWidth, simRenderHeight)

                            // Fill the sims rect with black
                            fill = Color.BLACK
                            fillRect(xOrigin - 1, yOrigin - 1, simRenderWidth - 1, simRenderHeight - 1)

                            // Set the stroke and fill to white
                            stroke = Color.WHITE
                            fill = Color.WHITE

                            // Draw the shapes
                            shapes.forEach { shape ->
                                strokePolygon(shape.first, shape.second, shape.first.size)
                            }

                            // Set the color to grey for debug
                            stroke = Color.SLATEGRAY
                            fill = Color.SLATEGRAY

                            // Draw the debug shapes
                            debugShapes.forEach { shape ->
                                strokePolygon(shape.first, shape.second, shape.first.size)
                            }
                        }
                    }
                }
            }
        }
    }
}