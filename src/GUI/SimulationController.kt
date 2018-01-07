package GUI

import Learning.Genetics
import Simulation.Simulation
import javafx.animation.AnimationTimer
import javafx.fxml.FXML
import javafx.scene.paint.Color
import javafx.scene.canvas.Canvas
import javafx.scene.layout.AnchorPane

private const val SIMULATIONS_TO_RUN = 100
private const val COLUMNS = 3

// The controller for all the simulations
class SimulationController(@FXML private var mainPane: AnchorPane? = null,
                           @FXML private var canvas: Canvas? = null,
                           private val simulations: MutableCollection<Simulation> = mutableListOf(),
                           private val genetics: Genetics = Genetics()) {

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

    // Reset everything
    @FXML
    fun initialize() {
        // Reset all the simulation
        for (simulation in 0 until SIMULATIONS_TO_RUN) {
            val simulation = Simulation()
            simulation.start()
            simulations.add(simulation)
        }

        simulations.forEach {
            genetics.addPopulationMember(it.getWeights())
        }

        // Setup the canvas to resize itself based on the parent frame
        mainPane?.heightProperty()?.addListener({ ov, oldValue, newValue ->
            canvas?.let {
                it.height = newValue.toDouble()
            }
        })

        mainPane?.widthProperty()?.addListener({ ov, oldValue, newValue ->
            canvas?.let {
                it.width = newValue.toDouble()
            }
        })

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

            // Work out row height
            val simRenderHeight = canvasHeight / COLUMNS

            // Loop over columns and rows
            for (row in 0..COLUMNS) {
                for (column in 0..COLUMNS) {
                    // Convert 2d coords to 1d array position
                    val simIndex = row * COLUMNS + column

                    // Get the sim at this position if it exists
                    val sim = simulations.elementAtOrNull(simIndex)

                    // If the sim at this position exists, render it
                    sim?.let {
                        // Work out the render dimensions
                        val simRenderWidth = canvasWidth / COLUMNS

                        // Work out the x and y origin
                        val xOrigin = column * simRenderWidth
                        val yOrigin = row * simRenderHeight

                        // Get the set of shapes to draw
                        val shapes = it.getShapes(simRenderWidth, xOrigin, simRenderHeight, yOrigin)

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
                        }
                    }
                }
            }
        }
    }
}