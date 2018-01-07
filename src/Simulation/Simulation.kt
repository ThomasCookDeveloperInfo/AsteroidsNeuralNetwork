package Simulation

import GUI.HEIGHT
import GUI.WIDTH
import Learning.Network
import Utilities.NonDeterminism
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.scene.shape.Polygon
import javafx.util.Duration

private const val ASTEROIDS = 4
const val SIM_WIDTH = 1000.0
const val SIM_HEIGHT = 600.0

class Simulation : Ship.Callbacks {
    private val asteroids = mutableListOf<Asteroid>()
    private val ship = Ship()
    private var running = false

    fun start() {
        running = true
        ship.reset()
        ship.setCallbacks(null)
        ship.setCallbacks(this)
        asteroids.clear()
        for (roid in 0 until ASTEROIDS) {
            asteroids.add(Asteroid())
        }
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

    fun applyWeights(newWeights: DoubleArray) {
        ship.setNetworkWeights(newWeights.toTypedArray())
    }

    override fun onShipUpdated(bullets: Collection<Bullet>) {
        val asteroidsToRemove = mutableListOf<Int>()
        val bulletsToRemove = mutableListOf<Int>()

        asteroids.forEachIndexed { asteroidIndex, asteroid ->
            bullets.forEachIndexed { bulletIndex, bullet ->
                if (asteroid.collidesWith(bullet)) {
                    asteroidsToRemove.add(asteroidIndex)
                    bulletsToRemove.add(bulletIndex)
                }
            }
        }

        bulletsToRemove.forEach { bullets.elementAtOrNull(it)?.explode() }
        asteroidsToRemove.forEach {
            asteroids.elementAtOrNull(it)?.let {
                val newSize = it.size + 1
                if (newSize <= 3) {
                    asteroids.addAll(listOf(Asteroid(newSize, it), Asteroid(newSize, it), Asteroid(newSize, it)))
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
    private val mesh = arrayOf(Pair(0.0, 0.0),
            Pair(0.0, 60.0 / size),
            Pair(60.0 / size, 60.0 / size),
            Pair(60.0 / size,  0.0))

    /**
     * State variables
     */
    private var x = NonDeterminism.randomDouble(SIM_WIDTH)
    private var y = NonDeterminism.randomDouble(SIM_HEIGHT)
    private var vX = NonDeterminism.randomDouble()
    private var vY = NonDeterminism.randomDouble()
    private var vRot = NonDeterminism.randomDouble()
    private var rot = 0.0

    fun collidesWith(bullet: Bullet) : Boolean {
        val shape = getPolygon()
        return bullet.isWithin(shape)
    }

    fun collidesWith(ship: Ship) : Boolean {
        val shape = getPolygon()
        return ship.isWithin(shape)
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

    fun calculateDanger(polygon: Polygon, withRespectX: Double, withRespectY: Double): Double {
        if (polygon.contains(x, y)) {
            // Work out the distance - lower means higher danger
            val dX = Math.abs(x - withRespectX)
            val dY = Math.abs(y - withRespectY)
            val danger = 200.0 / Math.sqrt(Math.pow(dX, 2.0) + Math.pow(dY, 2.0))
            return danger
        } else {
            return 0.0
        }
    }
}

private const val RELOAD_TIME_MILLISECONDS = 50.0
private const val DRAG_COEFFICIENT = 0.025
private const val ROTATIONAL_DRAG_COEFFICIENT = 0.1
private const val MAX_ROT_VEL = 15.0
private const val MAX_VEL = 0.1

class Ship : Bullet.Callbacks {

    private var canShoot = true
    private var reloadTimer: Timeline? = null

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
    private val mesh = arrayOf(Pair(0.0, 0.0),
            Pair(10.0, 30.0),
            Pair(20.0, 0.0))

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
    private var fitness = 0.0

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
        bullets.clear()
        x = SIM_WIDTH / 2
        y = SIM_HEIGHT / 2
        vX = 0.0
        vY = 0.0
        vRot = 0.0
        rot = 0.0
        fitness = 0.0
    }

    fun update(asteroids: Collection<Asteroid>) {
        // Calculate the ships quadrants
        val upQuadrant = Polygon(x, y, 0.0, 0.0, SIM_WIDTH, 0.0)
        val rightQuadrant = Polygon(x, y, SIM_WIDTH, 0.0, SIM_WIDTH, SIM_HEIGHT)
        val downQuadrant = Polygon(x, y, SIM_WIDTH, SIM_HEIGHT, 0.0, SIM_HEIGHT)
        val leftQuadrant = Polygon(x, y, 0.0, SIM_HEIGHT, 0.0, 0.0)

        // Setup the network inputs
        val upDanger = asteroids.sumByDouble { it.calculateDanger(upQuadrant, x, y) }
        val rightDanger = asteroids.sumByDouble { it.calculateDanger(rightQuadrant, x, y) }
        val downDanger = asteroids.sumByDouble { it.calculateDanger(downQuadrant, x, y) }
        val leftDanger = asteroids.sumByDouble { it.calculateDanger(leftQuadrant, x, y) }

        // Get the network outputs
        val networkOutputs = network.update(arrayOf(upDanger, rightDanger, downDanger, leftDanger))
        val torque = networkOutputs[0]
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
        fitness++
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

        // Add the debug info
        val upQuadrant = Polygon(x, y, 0.0, 0.0, SIM_WIDTH, 0.0)
        val rightQuadrant = Polygon(x, y, SIM_WIDTH, 0.0, SIM_WIDTH, SIM_HEIGHT)
        val downQuadrant = Polygon(x, y, SIM_WIDTH, SIM_HEIGHT, 0.0, SIM_HEIGHT)
        val leftQuadrant = Polygon(x, y, 0.0, SIM_HEIGHT, 0.0, 0.0)
        shapes.add(Pair(upQuadrant.points.mapIndexed { index, d -> if (index == 0 || index == 2 || index == 4) (d / xScale) + xPos else null }.filterNotNull().toDoubleArray(),
                upQuadrant.points.mapIndexed { index, d -> if (index == 1 || index == 3 || index == 5) (d / yScale) + yPos else null }.filterNotNull().toDoubleArray()))
        shapes.add(Pair(rightQuadrant.points.mapIndexed { index, d -> if (index == 0 || index == 2 || index == 4) (d / xScale) + xPos else null }.filterNotNull().toDoubleArray(),
                rightQuadrant.points.mapIndexed { index, d -> if (index == 1 || index == 3 || index == 5) (d / yScale) + yPos else null }.filterNotNull().toDoubleArray()))
        shapes.add(Pair(downQuadrant.points.mapIndexed { index, d -> if (index == 0 || index == 2 || index == 4) (d / xScale) + xPos else null }.filterNotNull().toDoubleArray(),
                downQuadrant.points.mapIndexed { index, d -> if (index == 1 || index == 3 || index == 5) (d / yScale) + yPos else null }.filterNotNull().toDoubleArray()))
        shapes.add(Pair(leftQuadrant.points.mapIndexed { index, d -> if (index == 0 || index == 2 || index == 4) (d / xScale) + xPos else null }.filterNotNull().toDoubleArray(),
                leftQuadrant.points.mapIndexed { index, d -> if (index == 1 || index == 3 || index == 5) (d / yScale) + yPos else null }.filterNotNull().toDoubleArray()))

        // Return the shapes
        return shapes
    }

    fun isWithin(shape: Polygon): Boolean {
        val contains = shape.contains(x, y)
        return contains
    }
}

private const val BULLET_VEL = 5.0
private const val BULLET_TIMEOUT_MILLISECONDS = 2500.0

class Bullet(private val shipX: Double, private val shipY: Double,
             private var x: Double = shipX, private var y: Double = shipY,
             private val shipVx: Double, private val shipVy: Double,
             private val shipRot: Double) {

    private val timeoutTimer: Timeline

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

    fun getShape(renderWidth: Double, xPos: Double, renderHeight: Double, yPos: Double) : Pair<DoubleArray, DoubleArray> {
        // Work out the scale
        val xScale = SIM_WIDTH / renderWidth
        val yScale = SIM_HEIGHT / renderHeight

        return Pair(doubleArrayOf(x / xScale + xPos, x / xScale + xPos, (x + 5.0) / xScale + xPos, (x + 5.0) / xScale + xPos),
                doubleArrayOf(y / yScale + yPos, (y + 5.0) / yScale + yPos, (y + 5.0) / yScale + yPos, y / yScale + yPos))
    }

    fun isWithin(shape: Polygon): Boolean {
        val contains = shape.contains(x, y)
        return contains
    }
}