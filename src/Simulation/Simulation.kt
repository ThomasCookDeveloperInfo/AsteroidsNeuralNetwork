package Simulation

import GUI.HEIGHT
import GUI.WIDTH
import Learning.Network
import Utilities.NonDeterminism
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.scene.shape.Polygon
import javafx.scene.shape.Shape
import javafx.util.Duration

data class Configuration(val simTimeoutSeconds: Double = 90.0,
                         val activationResponse: Double = 1.0,
                         val asteroids: Int = 5,
                         val maxChunkFactor: Int = 3,
                         val chunkCount: Int = 3,
                         val maxAsteroids: Int = asteroids + ( asteroids * chunkCount) + (asteroids * chunkCount * maxChunkFactor),
                         val reloadTimeMilliseconds: Double = 333.0,
                         val dragCoefficient: Double = 0.025,
                         val rotationalDragCoefficient: Double = 0.1,
                         val maxVel: Double = 0.1,
                         val maxRotVel: Double = 15.0,
                         val asteroidFitnessWeight: Double = 20.0,
                         val asteroidsToConsider: Int = 5,
                         val bulletVel: Double = 5.0,
                         val bulletTimeoutMilliseconds: Double = 2000.0)

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

    fun getWeights() : DoubleArray {
        return ship.getCopyOfNetworkWeights().toDoubleArray()
    }

    fun getFitness() : Double {
        return ship.fitness()
    }

    fun applyWeights(newWeights: DoubleArray) {
        ship.setNetworkWeights(newWeights.toTypedArray())
    }

    override fun onShipUpdated(bullets: Collection<Bullet>) {
        val asteroidsToRemove = mutableListOf<Int>()
        val bulletsToRemove = mutableListOf<Int>()

        bullets.forEachIndexed { bulletIndex, bullet ->
            asteroids.forEachIndexed { asteroidIndex, asteroid ->
                if (!asteroidsToRemove.contains(asteroidIndex)) {
                    if (asteroid.collidesWith(bullet)) {
                        asteroidsToRemove.add(asteroidIndex)
                        bulletsToRemove.add(bulletIndex)
                    }
                }
            }
        }

        bulletsToRemove.forEach { bullets.elementAtOrNull(it)?.explode() }
        asteroidsToRemove.forEach {
            asteroids.elementAtOrNull(it)?.let {
                val newSize = it.size + 1
                if (newSize <= configuration.maxChunkFactor) {
                    for (newRoid in 0 until configuration.chunkCount) {
                        asteroids.add(Asteroid(newSize, it))
                    }
                }
                asteroids.remove(it)
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

    private fun stop() {
        ship.stop()
        running = false
    }
}

const val SIM_WIDTH = 1000.0
const val SIM_HEIGHT = 600.0

class Asteroid(var size: Int = 1) {

    constructor(size: Int, parent: Asteroid) : this(size) {
        x = parent.x
        y = parent.y
    }

    /**
     * Shape
     */
    private val mesh = arrayOf(Pair(-30.0 / size, -30.0 / size),
            Pair(-30.0 / size, 30.0 / size),
            Pair(30.0 / size, 30.0 / size),
            Pair(30.0 / size,  -30.0 / size))

    /**
     * State variables
     */
    var x = NonDeterminism.randomAsteroidX(SIM_WIDTH)
    var y = NonDeterminism.randomAsteroidY(SIM_HEIGHT)

    private var vX = (((1.0 - -1.0) / (1.0 - 0.0)) * (NonDeterminism.randomDouble() - 1.0) + 1.0) * size
    private var vY = (((1.0 - -1.0) / (1.0 - 0.0)) * (NonDeterminism.randomDouble() - 1.0) + 1.0) * size
    private var vRot = NonDeterminism.randomDouble()
    private var rot = 0.0

    fun collidesWith(bullet: Bullet) : Boolean {
        return bullet.isWithin(this.getPolygon())
    }

    fun collidesWith(ship: Ship) : Boolean {
        val shape = getPolygon()
        return !Shape.intersect(shape, ship.getPolygon()).boundsInLocal.isEmpty
    }

    fun update() {
        rot += vRot
        x += vX
        y += vY
        if (x > SIM_WIDTH) x = 0.0
        else if (x < 0) x = SIM_WIDTH
        if (y > SIM_HEIGHT) y = 0.0
        else if (y < 0) y = SIM_HEIGHT
    }

    private fun getPolygon(): Polygon {
        // Work out asteroid x points
        val asteroidXPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecX = vecX * Math.cos(Math.toRadians(rot)) - vecY * Math.sin(Math.toRadians(rot))
            rotatedVecX += x
            asteroidXPoints[i] = rotatedVecX
        }

        // Work out asteroid y points
        val asteroidYPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecY = vecX * Math.sin(Math.toRadians(rot)) + vecY * Math.cos(Math.toRadians(rot))
            rotatedVecY += y
            asteroidYPoints[i] = rotatedVecY
        }

        // Convert the x and y coordinates into a continuous array
        val points = mutableListOf<Double>()
        for (index in 0 until asteroidXPoints.size) {
            points.add(asteroidXPoints[index])
            points.add(asteroidYPoints[index])
        }

        // Create a polygon from the array
        return Polygon(*points.toDoubleArray())
    }

    fun getShape(renderWidth: Double, xPos: Double, renderHeight: Double, yPos: Double): Pair<DoubleArray, DoubleArray> {
        val xScale = SIM_WIDTH / renderWidth
        val yScale = SIM_HEIGHT / renderHeight

        // Work out asteroid x points
        val asteroidXPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecX = vecX * Math.cos(Math.toRadians(rot)) - vecY * Math.sin(Math.toRadians(rot))
            rotatedVecX += x
            rotatedVecX /= xScale
            rotatedVecX += xPos
            asteroidXPoints[i] = rotatedVecX
        }

        // Work out asteroid y points
        val asteroidYPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecY = vecX * Math.sin(Math.toRadians(rot)) + vecY * Math.cos(Math.toRadians(rot))
            rotatedVecY += y
            rotatedVecY /= yScale
            rotatedVecY += yPos
            asteroidYPoints[i] = rotatedVecY
        }

        // Create the asteroid pair
        return Pair(asteroidXPoints, asteroidYPoints)
    }
}

const val MILLISECONDS_IN_SECOND = 1000.0

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

    private val survivalTimer: Timeline
    init {
        survivalTimer = Timeline(KeyFrame(Duration.millis(MILLISECONDS_IN_SECOND), EventHandler<javafx.event.ActionEvent> {
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
    fun getCopyOfNetworkWeights(): Array<Double> {
        return network.getCopyOfWeights()
    }
    fun setNetworkWeights(weights: Array<Double>) {
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
                val dx = it.x + i * SIM_WIDTH - this.x
                val dy = it.y + j * SIM_HEIGHT - this.y
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
        val flattenedVectors = mutableListOf<Double>()
        for (index in 0 until vectorsToClosestAsteroids.size) {
            flattenedVectors.add(vectorsToClosestAsteroids.elementAt(index).first)
            flattenedVectors.add(vectorsToClosestAsteroids.elementAt(index).second)
        }

        val dirX = -Math.sin(Math.toRadians(rot))
        val dirY = Math.cos(Math.toRadians(rot))

        // Get the ships current direction vector
        directionVector = Pair(dirX, dirY)

        flattenedVectors.addAll(listOf(directionVector.first, directionVector.second))

        // Get the network outputs
        val networkOutputs = network.update(flattenedVectors.toTypedArray())

        val torque = networkOutputs[0]
        val thrust = ((1.0 - 0.0) / (1.0 - -1.0)) * (networkOutputs[1] - 1.0) + 1.0
        val wantsToShoot = networkOutputs[2] > 0

//        val torque = if (networkOutputs[0] > 0.33) 1.0 else if (networkOutputs[0] < -0.33) -1.0 else 0.0
//        val thrust = if (networkOutputs[1] > 0.0) 1.0 else 0.0
//        val wantsToShoot = networkOutputs[2] > 0.0

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
        val shapes = mutableListOf<Pair<DoubleArray, DoubleArray>>()
        shapes.addAll(asteroidVectors)
        shapes.add(shipDirection)

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

class Bullet(private val configuration: Configuration,
             private val shipX: Double, private val shipY: Double,
             private var x: Double = shipX, private var y: Double = shipY,
             private val shipVx: Double, private val shipVy: Double,
             private val shipRot: Double) {

    private val timeoutTimer: Timeline

    /**
     * Shape
     */
    private val mesh = arrayOf(Pair(-2.0, -2.0),
            Pair(-2.0, 2.0),
            Pair(2.0, 2.0),
            Pair(2.0, -2.0))

    init {
        timeoutTimer = Timeline(KeyFrame(Duration.millis(configuration.bulletTimeoutMilliseconds), EventHandler<javafx.event.ActionEvent> {
            callbacks?.onBulletTimedOut(this@Bullet)
        }, null))
        timeoutTimer.play()
    }

    interface Callbacks {
        fun onBulletExploded(bullet: Bullet)
        fun onBulletTimedOut(bullet: Bullet)
    }

    private var callbacks: Callbacks? = null
    fun setCallbacks(callbacks: Callbacks) {
        this.callbacks = callbacks
    }

    fun explode() {
        callbacks?.onBulletExploded(this)
    }

    fun update() {
        x += shipVx + configuration.bulletVel * -Math.sin(Math.toRadians(shipRot))
        y += shipVy + configuration.bulletVel * Math.cos(Math.toRadians(shipRot))

        if (x > WIDTH) x = 0.0
        else if (x < 0) x = WIDTH
        if (y > HEIGHT) y = 0.0
        else if (y < 0) y = HEIGHT
    }

    fun getShape(renderWidth: Double, xPos: Double, renderHeight: Double, yPos: Double): Pair<DoubleArray, DoubleArray> {
        val xScale = SIM_WIDTH / renderWidth
        val yScale = SIM_HEIGHT / renderHeight

        // Work out bullet x points
        val bulletXPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecX = vecX * Math.cos(Math.toRadians(shipRot)) - vecY * Math.sin(Math.toRadians(shipRot))
            rotatedVecX += x
            rotatedVecX /= xScale
            rotatedVecX += xPos
            bulletXPoints[i] = rotatedVecX
        }

        // Work out bullet y points
        val bulletYPoints = DoubleArray(mesh.size, { index -> 0.0 })
        for (i in 0 until mesh.size) {
            val vecX = mesh[i].first
            val vecY = mesh[i].second
            var rotatedVecY = vecX * Math.sin(Math.toRadians(shipRot)) + vecY * Math.cos(Math.toRadians(shipRot))
            rotatedVecY += y
            rotatedVecY /= yScale
            rotatedVecY += yPos
            bulletYPoints[i] = rotatedVecY
        }

        // Create the asteroid pair
        return Pair(bulletXPoints, bulletYPoints)
    }

    fun isWithin(polygon: Polygon) : Boolean {
        return polygon.contains(x, y)
    }
}