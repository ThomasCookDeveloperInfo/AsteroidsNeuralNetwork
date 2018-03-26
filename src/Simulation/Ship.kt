package Simulation

import Learning.Network
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.shape.Polygon
import javafx.util.Duration

class Ship(private val configuration: Configuration) : Bullet.Callbacks {

    private var canShoot = false
    private val reloadTimer = Timeline(KeyFrame(Duration.millis(configuration.reloadTimeMilliseconds), EventHandler<javafx.event.ActionEvent> {
        canShoot = true
    }, null))

    private var secondsSurvived = 0
    private var secondsElapsedWhenWon: Int? = null
    private var asteroidsDestroyed = 0
    private val vectorsToClosestAsteroids = mutableListOf<Pair<Double, Double>>()
    private var directionVector = Pair(0.0, 0.0)
    private var velocityVector = Pair(0.0, 0.0)

    private val survivalTimer: Timeline
    init {
        survivalTimer = Timeline(KeyFrame(Duration.millis(MILLISECONDS_IN_SECOND), EventHandler<ActionEvent> {
            updateSecondsSurvived()
        }, null))
        survivalTimer.play()
    }

    private fun updateSecondsSurvived() {
        survivalTimer.stop()
        secondsSurvived++
        survivalTimer.play()
    }

    fun stop() {
        survivalTimer.stop()
        reloadTimer.stop()
        if (asteroidsDestroyed == configuration.asteroids) { secondsElapsedWhenWon = secondsSurvived }
    }

    /**
     * System variables
     */
    interface Callbacks {
        fun onShipUpdated(bullets: Collection<Bullet>)
    }

    private var callbacks: Callbacks? = null
    fun setCallbacks(callbacks: Callbacks?) {
        this.callbacks = callbacks
    }

    /**
     * Shape
     */
    private val mesh = arrayOf(Pair(-10.0, -10.0),
            Pair(0.0, 20.0),
            Pair(10.0, -10.0))

    /**
     * Network
     */
    private val network = Network(configuration)
    fun getCopyOfNetworkWeights(): Array<Float> {
        return network.getCopyOfWeights()
    }
    fun setNetworkWeights(weights: Array<Float>) {
        network.setWeights(weights)
    }

    /**
     * Fitness
     */
    fun fitness() : Double {
        return asteroidsDestroyed * configuration.asteroidFitnessWeight
    }

    /**
     * State variables
     */
    private val bullets = mutableListOf<Bullet>()
    private var x = SIM_WIDTH / 2
    private var y = SIM_HEIGHT / 2
    private var vX = 0.0
    private var vY = 0.0
    private var vRot = 0.0
    private var rot = 0.0

    fun reset() {
        survivalTimer.stop()
        reloadTimer.stop()
        x = SIM_WIDTH / 2
        y = SIM_HEIGHT / 2
        vX = 0.0
        vY = 0.0
        vRot = 0.0
        rot = 0.0
        canShoot = true
        bullets.clear()
        vectorsToClosestAsteroids.clear()
        directionVector = Pair(0.0, 0.0)
        velocityVector = Pair(0.0, 0.0)
        secondsElapsedWhenWon = null
        asteroidsDestroyed = 0
        secondsSurvived = 0
        reloadTimer.play()
        survivalTimer.play()
    }

    private fun vectorsToClosestAsteroids(asteroids: Collection<Asteroid>) : Collection<Pair<Double, Double>> {
        val vectors = mutableListOf<Pair<Double, Double>>()
        val selectedIndexes = mutableListOf<Int>()

        for (selected in 0 until configuration.asteroidsToConsider) {
            var closestIndex = -1
            var closestDistance = Double.MAX_VALUE
            asteroids.forEachIndexed { index, asteroid ->
                if (!selectedIndexes.contains(index)) {
                    val i = if (this.x - asteroid.x > SIM_WIDTH / 2) 1 else if (this.x - asteroid.x < -SIM_WIDTH / 2) -1 else 0
                    val j = if (this.y - asteroid.y > SIM_HEIGHT / 2) 1 else if (this.y - asteroid.y < -SIM_HEIGHT / 2) -1 else 0
                    val dx = asteroid.x + i * SIM_WIDTH - this.x
                    val dy = asteroid.y + j * SIM_HEIGHT - this.y
                    val distance = Math.sqrt(Math.pow(dx, 2.0) + Math.pow(dy, 2.0))
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestIndex = index
                    }
                }
            }
            val closestAsteroid = asteroids.elementAtOrNull(closestIndex)
            closestAsteroid?.let {
                val i = if (this.x - it.x > SIM_WIDTH / 2) 1 else if (this.x - it.x < -SIM_WIDTH / 2) -1 else 0
                val j = if (this.y - it.y > SIM_HEIGHT / 2) 1 else if (this.y - it.y < -SIM_HEIGHT / 2) -1 else 0
                val dx = it.x + it.vX + i * SIM_WIDTH - this.x
                val dy = it.y + + it.vY + j * SIM_HEIGHT - this.y
                val normalizedDx = ((1.0 - -1.0) / (SIM_WIDTH - -SIM_WIDTH)) * (dx - SIM_WIDTH) + 1.0
                val normalizedDy = ((1.0 - -1.0) / (SIM_HEIGHT - -SIM_HEIGHT)) * (dy - SIM_HEIGHT) + 1.0

                vectors.add(Pair(normalizedDx, normalizedDy))

                selectedIndexes.add(closestIndex)
            }
            closestIndex = -1
        }

        while (vectors.size != configuration.asteroidsToConsider) {
            vectors.add(Pair(0.0, 0.0))
        }

        return vectors
    }

    fun update(asteroids: Collection<Asteroid>) {
        // Get the the vectors to the N nearest asteroids
        vectorsToClosestAsteroids.clear()
        vectorsToClosestAsteroids.addAll(vectorsToClosestAsteroids(asteroids))

        // Convert the vectors into a continuous array
        val flattenedVectors = mutableListOf<Float>()
        for (index in 0 until vectorsToClosestAsteroids.size) {
            flattenedVectors.add(vectorsToClosestAsteroids.elementAt(index).first.toFloat())
            flattenedVectors.add(vectorsToClosestAsteroids.elementAt(index).second.toFloat())
        }

        val dirX = -Math.sin(Math.toRadians(rot))
        val dirY = Math.cos(Math.toRadians(rot))

        // Get the ships current direction vector
        directionVector = Pair(dirX, dirY)
        velocityVector = Pair(vX, vY)

        flattenedVectors.addAll(listOf(directionVector.first.toFloat(), directionVector.second.toFloat(),
                velocityVector.first.toFloat(), velocityVector.second.toFloat()))

        // Get the network outputs
        val networkOutputs = network.update(flattenedVectors.toTypedArray())

//        val torque = networkOutputs[0]
//        val thrust = ((1.0 - 0.0) / (1.0 - -1.0)) * (networkOutputs[1] - 1.0) + 1.0
//        val wantsToShoot = networkOutputs[2] > 0

        val torque = if (networkOutputs[0] > 0.0) 1.0 else if (networkOutputs[0] < 0.0) -1.0 else 0.0
        val thrust = if (networkOutputs[1] > 0.0) 1.0 else 0.0
        val wantsToShoot = networkOutputs[2] > 0.0

        // Apply the outputs
        vRot = Math.max(-configuration.maxRotVel, Math.min(configuration.maxRotVel, vRot + torque)) - configuration.rotationalDragCoefficient * vRot
        rot += vRot
        vX += Math.max(-configuration.maxVel, Math.min(configuration.maxVel, thrust * -Math.sin(Math.toRadians(rot)))) - configuration.dragCoefficient * vX
        vY += Math.max(-configuration.maxVel, Math.min(configuration.maxVel, thrust * Math.cos(Math.toRadians(rot)))) - configuration.dragCoefficient * vY
        x += vX
        y += vY
        if (x > SIM_WIDTH) x = 0.0
        else if (x < 0) x = SIM_WIDTH
        if (y > SIM_HEIGHT) y = 0.0
        else if (y < 0) y = SIM_HEIGHT

        // Shoot a bullet
        if (canShoot && wantsToShoot) {
            reloadTimer.stop()
            canShoot = false
            val bullet = Bullet(configuration, x, y, x, y, vX, vY, rot)
            bullet.setCallbacks(this)
            bullets.add(bullet)
            reloadTimer.play()
        }

        // Update all the bullets
        bullets.forEach { it.update() }

        // Notify ship finished updating
        callbacks?.onShipUpdated(bullets)
    }

    override fun onBulletExploded(bullet: Bullet) {
        bullets.remove(bullet)
        asteroidsDestroyed++
    }

    override fun onBulletTimedOut(bullet: Bullet) {
        bullets.remove(bullet)
    }

    fun getShapes(renderWidth: Double, xPos: Double, renderHeight: Double, yPos: Double): Collection<Pair<DoubleArray, DoubleArray>> {
        val shapes = mutableListOf<Pair<DoubleArray, DoubleArray>>()

        val xScale = SIM_WIDTH / renderWidth
        val yScale = SIM_HEIGHT / renderHeight

        // Work out ship x points
        val shipXPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecX = vecX * Math.cos(Math.toRadians(rot)) - vecY * Math.sin(Math.toRadians(rot))
            rotatedVecX += x
            rotatedVecX /= xScale
            rotatedVecX += xPos
            shipXPoints[i] = rotatedVecX
        }

        // Work out ship y points
        val shipYPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecY = vecX * Math.sin(Math.toRadians(rot)) + vecY * Math.cos(Math.toRadians(rot))
            rotatedVecY += y
            rotatedVecY /= yScale
            rotatedVecY += yPos
            shipYPoints[i] = rotatedVecY
        }

        // Create the ship pair
        val shipPair = Pair(shipXPoints, shipYPoints)

        // Add the ship to the shapes
        shapes.add(shipPair)

        // Add the bullets
        shapes.addAll(bullets.map { it.getShape(renderWidth, xPos, renderHeight, yPos) })

        // Return the shapes
        return shapes
    }

    fun getDebugShapes(renderWidth: Double, xPos: Double, renderHeight: Double, yPos: Double): Collection<Pair<DoubleArray, DoubleArray>> {
        val xScale = SIM_WIDTH / renderWidth
        val yScale = SIM_HEIGHT / renderHeight

        val shipXOrigin = (x / xScale) + xPos
        val shipYOrigin = (y / yScale) + yPos

        val asteroidVectors = vectorsToClosestAsteroids.map {
            val dx = it.first
            val dy = it.second

            val unnormalizedDx = ((SIM_WIDTH - -SIM_WIDTH) / (1.0 - -1.0)) * (dx - 1.0) + SIM_WIDTH
            val unnormalizedDy = ((SIM_HEIGHT - -SIM_HEIGHT) / (1.0 - -1.0)) * (dy - 1.0) + SIM_HEIGHT

            var asteroidXOrigin = x + unnormalizedDx
            if (asteroidXOrigin < 0.0) {
                asteroidXOrigin = 0.0
            } else if (asteroidXOrigin > SIM_WIDTH) {
                asteroidXOrigin = SIM_WIDTH
            }
            asteroidXOrigin = asteroidXOrigin / xScale + xPos
            var asteroidYOrigin = y + unnormalizedDy
            if (asteroidYOrigin < 0.0) {
                asteroidYOrigin = 0.0
            } else if (asteroidYOrigin > SIM_HEIGHT) {
                asteroidYOrigin = SIM_HEIGHT
            }
            asteroidYOrigin = asteroidYOrigin / yScale + yPos
            val xArray = doubleArrayOf(shipXOrigin, asteroidXOrigin)
            val yArray = doubleArrayOf(shipYOrigin, asteroidYOrigin)
            Pair(xArray, yArray)
        }


        val shipDirection = Pair(doubleArrayOf(shipXOrigin, shipXOrigin + (directionVector.first * 50) / xScale), doubleArrayOf(shipYOrigin, shipYOrigin + (directionVector.second * 50) / yScale))
        val shipVelocity = Pair(doubleArrayOf(shipXOrigin, shipXOrigin + (velocityVector.first * 50) / xScale), doubleArrayOf(shipYOrigin, shipYOrigin + (velocityVector.second * 50) / yScale))
        val shapes = mutableListOf<Pair<DoubleArray, DoubleArray>>()
        shapes.addAll(asteroidVectors)
        shapes.add(shipDirection)
        shapes.add(shipVelocity)

        return shapes
    }

    // Gets the polygon of the ship
    fun getPolygon(): Polygon {
        // Work out ship x points
        val shipXPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecX = vecX * Math.cos(Math.toRadians(rot)) - vecY * Math.sin(Math.toRadians(rot))
            rotatedVecX += x
            shipXPoints[i] = rotatedVecX
        }

        // Work out ship y points
        val shipYPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecY = vecX * Math.sin(Math.toRadians(rot)) + vecY * Math.cos(Math.toRadians(rot))
            rotatedVecY += y
            shipYPoints[i] = rotatedVecY
        }

        // Convert the x and y coordinates into a continuous array
        val points = mutableListOf<Double>()
        for (index in 0 until shipXPoints.size) {
            points.add(shipXPoints[index])
            points.add(shipYPoints[index])
        }

        // Create a polygon from the array
        return Polygon(*points.toDoubleArray())
    }
}