package Simulation

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.util.Duration

const val SIM_WIDTH = 1000.0
const val SIM_HEIGHT = 600.0
const val MILLISECONDS_IN_SECOND = 1000.0

class Simulation(private val configuration: Configuration) : Ship.Callbacks {
    private val asteroids = mutableListOf<Asteroid>()
    private val ship = Ship(configuration)
    private var running = false

    private val simTimeoutTimer: Timeline
    init {
        simTimeoutTimer = Timeline(KeyFrame(Duration.seconds(configuration.simTimeoutSeconds), EventHandler<javafx.event.ActionEvent> {
            running = false
        }, null))
        simTimeoutTimer.play()
    }

    fun start() {
        simTimeoutTimer.stop()
        running = true
        ship.reset()
        ship.setCallbacks(null)
        ship.setCallbacks(this)
        asteroids.clear()
        for (roid in 0 until configuration.asteroids) {
            asteroids.add(Asteroid())
        }
        simTimeoutTimer.play()
    }

    fun update() {
        if (running) {
            asteroids.forEach {
                it.update()
                if (it.collidesWith(ship)) {
                    stop()
                }
            }

            ship.update(asteroids)
        }
    }

    fun isFinished() : Boolean {
        return !running
    }

    fun getWeights() : FloatArray {
        return ship.getCopyOfNetworkWeights().toFloatArray()
    }

    fun getFitness() : Double {
        return ship.fitness()
    }

    fun applyWeights(newWeights: FloatArray) {
        ship.setNetworkWeights(newWeights.toTypedArray())
    }

    override fun onShipUpdated(bullets: Collection<Bullet>) {
        val elementsToRemove = mutableListOf<Pair<Int, Int>>()

        bullets.forEachIndexed { bulletIndex, bullet ->
            asteroids.forEachIndexed { asteroidIndex, asteroid ->
                if (elementsToRemove.none { it.first == asteroidIndex || it.second == bulletIndex }) {
                    if (asteroid.collidesWith(bullet)) {
                        elementsToRemove.add(Pair(asteroidIndex, bulletIndex))
                    }
                }
            }
        }

        elementsToRemove.forEach {
            val asteroidOptional = asteroids.elementAtOrNull(it.first)
            val bulletOptional = bullets.elementAtOrNull(it.second)
            asteroidOptional?.let { asteroid ->
                bulletOptional?.let { bullet ->
                    val newSize = asteroid.size + 1
                    if (newSize <= configuration.maxChunkFactor) {
                        for (newRoid in 0 until configuration.chunkCount) {
                            asteroids.add(Asteroid(newSize, asteroid, bullet))
                        }
                    }
                    asteroids.remove(asteroid)
                    bullet.explode()
                }
            }
        }

        if (asteroids.size == 0) {
            stop()
        }
    }

    fun getShapes(xScale: Double, xPos: Double, yScale: Double, yPos: Double) : Collection<Pair<DoubleArray, DoubleArray>> {
        // Initialize shapes
        val shapes = mutableListOf<Pair<DoubleArray, DoubleArray>>()

        // Add ship
        shapes.addAll(ship.getShapes(xScale, xPos, yScale, yPos))

        // Add asteroids
        asteroids.forEach { asteroid ->
            shapes.add(asteroid.getShape(xScale, xPos, yScale, yPos))
        }

        // Return
        return shapes
    }

    fun getDebugShapes(xScale: Double, xPos: Double, yScale: Double, yPos: Double) : Collection<Pair<DoubleArray, DoubleArray>> {
        return ship.getDebugShapes(xScale, xPos, yScale, yPos)
    }

    fun getDebugText() : String {
        return ship.fitness().toString()
    }

    private fun stop() {
        ship.stop()
        running = false
    }
}