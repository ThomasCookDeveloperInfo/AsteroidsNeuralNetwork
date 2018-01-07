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

private const val SIM_TIMEOUT_SECONDS = 30.0
private const val ASTEROIDS = 4
private const val MAX_CHUNK_FACTOR = 3
private const val CHUNK_COUNT = 3
private const val MAX_ASTEROIDS = ASTEROIDS + (ASTEROIDS * CHUNK_COUNT) + (ASTEROIDS * CHUNK_COUNT * MAX_CHUNK_FACTOR)
const val SIM_WIDTH = 1000.0
const val SIM_HEIGHT = 600.0

class Simulation : Ship.Callbacks {
    private val asteroids = mutableListOf<Asteroid>()
    private val ship = Ship()
    private var running = false

    private val simTimeoutTimer: Timeline
    init {
        simTimeoutTimer = Timeline(KeyFrame(Duration.seconds(SIM_TIMEOUT_SECONDS), EventHandler<javafx.event.ActionEvent> {
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
        for (roid in 0 until ASTEROIDS) {
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
                if (newSize <= MAX_CHUNK_FACTOR) {
                    for (newRoid in 0 until CHUNK_COUNT) {
                        asteroids.add(Asteroid(newSize, it))
                    }
                }
                asteroids.remove(it)
            }
        }

        if (asteroids.size == 0) {
            running = false
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

    private fun stop() {
        ship.stop()
        running = false
    }
}

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
    var x = NonDeterminism.randomDouble(SIM_WIDTH)
    var y = NonDeterminism.randomDouble(SIM_HEIGHT)

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

private const val MILLISECONDS_IN_SECOND = 20000.0
private const val RELOAD_TIME_MILLISECONDS = 200.0
private const val DRAG_COEFFICIENT = 0.025
private const val ROTATIONAL_DRAG_COEFFICIENT = 0.1
private const val MAX_ROT_VEL = 15.0
private const val MAX_VEL = 0.1
const val ASTEROIDS_TO_CONSIDER = 10

class Ship : Bullet.Callbacks {

    private var canShoot = true
    private var reloadTimer: Timeline? = null
    private var secondsSurvived = 0
    private var asteroidsDestroyed = 0
    private val vectorsToClosestAsteroids = mutableListOf<Pair<Double, Double>>()

    private val survivalTimer: Timeline
    init {
        survivalTimer = Timeline(KeyFrame(Duration.millis(MILLISECONDS_IN_SECOND), EventHandler<javafx.event.ActionEvent> {
            updateSecondsSurvived()
        }, null))
        survivalTimer.play()
    }

    private fun updateSecondsSurvived() {
        secondsSurvived++
        survivalTimer.play()
    }

    fun stop() {
        survivalTimer.stop()
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
    private val network = Network()
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
        val asteroidDestroyedDelta = MAX_ASTEROIDS - asteroidsDestroyed
        return (((asteroidsDestroyed / if (asteroidDestroyedDelta <= 0) 1 else asteroidDestroyedDelta)) * 10
                * Math.abs(SIM_TIMEOUT_SECONDS - secondsSurvived)) / SIM_TIMEOUT_SECONDS
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
        bullets.clear()
        x = SIM_WIDTH / 2
        y = SIM_HEIGHT / 2
        vX = 0.0
        vY = 0.0
        vRot = 0.0
        rot = 0.0
        asteroidsDestroyed = 0
        secondsSurvived = 0
        survivalTimer.play()
    }

    private fun vectorsToClosestAsteroids(asteroids: Collection<Asteroid>) : Collection<Pair<Double, Double>> {
        val vectors = mutableListOf<Pair<Double, Double>>()
        val selectedIndexes = mutableListOf<Int>()

        for (selected in 0 until ASTEROIDS_TO_CONSIDER) {
            var closestIndex = 0
            var closestDistance = Double.MAX_VALUE
            asteroids.forEachIndexed { index, asteroid ->
                if (!selectedIndexes.contains(index)) {
                    val dx = Math.abs(this.x - asteroid.x)
                    val dy = Math.abs(this.y - asteroid.y)
                    val distance = Math.sqrt(Math.pow(dx, 2.0) + Math.pow(dy, 2.0))
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestIndex = index
                    }
                }
            }
            val closestAsteroid = asteroids.elementAt(closestIndex)
            vectors.add(Pair(this.x - (this.x - closestAsteroid.x), this.y - (this.y - closestAsteroid.y)))
            selectedIndexes.add(closestIndex)
        }

        while (vectors.size != ASTEROIDS_TO_CONSIDER) {
            vectors.add(Pair(Double.MAX_VALUE, Double.MAX_VALUE))
        }

        return vectors
    }

    fun update(asteroids: Collection<Asteroid>) {
        // Get the the vectors to the 3 nearest asteroids
        vectorsToClosestAsteroids.clear()
        vectorsToClosestAsteroids.addAll(vectorsToClosestAsteroids(asteroids))

        // Convert the vectors into a continuous array
        val flattenedVectors = mutableListOf<Double>()
        for (index in 0 until vectorsToClosestAsteroids.size) {
            flattenedVectors.add(vectorsToClosestAsteroids.elementAt(index).first)
            flattenedVectors.add(vectorsToClosestAsteroids.elementAt(index).second)
        }

        // Get the network outputs
        val networkOutputs = network.update(flattenedVectors.toTypedArray())
        val torque =  ((1.0 - -1.0) / (1.0 - 0.0)) * (networkOutputs[0] - 1.0) + 1.0
        val thrust = networkOutputs[1]

        // Apply the outputs
        vRot = Math.max(-MAX_ROT_VEL, Math.min(MAX_ROT_VEL, vRot + torque)) - ROTATIONAL_DRAG_COEFFICIENT * vRot
        rot += vRot
        vX += Math.max(-MAX_VEL, Math.min(MAX_VEL, thrust * -Math.sin(Math.toRadians(rot)))) - DRAG_COEFFICIENT * vX
        vY += Math.max(-MAX_VEL, Math.min(MAX_VEL, thrust * Math.cos(Math.toRadians(rot)))) - DRAG_COEFFICIENT * vY
        x += vX
        y += vY
        if (x > SIM_WIDTH) x = 0.0
        else if (x < 0) x = SIM_WIDTH
        if (y > SIM_HEIGHT) y = 0.0
        else if (y < 0) y = SIM_HEIGHT

        // Shoot a bullet
        if (canShoot) {
            canShoot = false
            val bullet = Bullet(x, y, x, y, vX, vY, rot)
            bullet.setCallbacks(this)
            bullets.add(bullet)
            reloadTimer = Timeline(KeyFrame(Duration.millis(RELOAD_TIME_MILLISECONDS), EventHandler<javafx.event.ActionEvent> {
                canShoot = true
            }, null))
            reloadTimer?.play()
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

//        vectorsToClosestAsteroids.forEach {
//            val shipXOrigin = (x / xScale) + xPos
//            val shipYOrigin = (y / yScale) + yPos
//            val asteroidXOrigin = (it.first / xScale) + xPos
//            val asteroidYOrigin = (it.second / yScale) + yPos
//            val xArray = doubleArrayOf(shipXOrigin, asteroidXOrigin)
//            val yArray = doubleArrayOf(shipYOrigin, asteroidYOrigin)
//            shapes.add(Pair(xArray, yArray))
//        }

        // Return the shapes
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

private const val BULLET_VEL = 5.0
private const val BULLET_TIMEOUT_MILLISECONDS = 2000.0

class Bullet(private val shipX: Double, private val shipY: Double,
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
        timeoutTimer = Timeline(KeyFrame(Duration.millis(BULLET_TIMEOUT_MILLISECONDS), EventHandler<javafx.event.ActionEvent> {
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
        x += shipVx + BULLET_VEL * -Math.sin(Math.toRadians(shipRot))
        y += shipVy + BULLET_VEL * Math.cos(Math.toRadians(shipRot))

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