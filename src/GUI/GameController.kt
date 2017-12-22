package GUI

import GameModel.*
import Learning.Genetics
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import java.util.*

private const val FRAME_TIME_MILLISECONDS = 25L
private const val SIMULATIONS_TO_RUN = 100

// The controller for all the simulations
class SimulationController(@FXML private var canvas: Canvas? = null,
                           private val frameTimer: Timer = Timer(),
                           private val genetics: Genetics = Genetics(SIMULATIONS_TO_RUN),
                           private var simulations: Array<Simulation> = Array(SIMULATIONS_TO_RUN, { index -> Simulation(genetics.population[index]) })) {

    // Reset everything
    fun reset() {
        // Reset all the simulation
        simulations = Array(SIMULATIONS_TO_RUN, { index -> Simulation(genetics.population[index]) })

        // Setup the frame timer
        frameTimer.schedule(object: TimerTask() {
            override fun run() {
                simulations.forEach { it.update() }
                invalidate()
            }
        }, 0, FRAME_TIME_MILLISECONDS)
    }

    // Invalidate the canvas and redraw everything
    fun invalidate() {
        val canvas = this.canvas ?: return

        // Start drawing
    }
}