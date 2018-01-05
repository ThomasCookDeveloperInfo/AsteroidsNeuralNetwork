package Simulation

import Learning.Network
import Utilities.NonDeterminism
import javafx.scene.shape.Polygon

private const val ASTEROIDS = 3
const val SIM_WIDTH = 1000.0
const val SIM_HEIGHT = 600.0

class Simulation : Ship.Callbacks {
    private val asteroids = mutableListOf<Asteroid>()
    private val ship = Ship()

    fun start() {
        ship.reset()
        ship.setCallbacks(null)
        ship.setCallbacks(this)
        asteroids.clear()
        for (roid in 0 until ASTEROIDS) {
            asteroids.add(Asteroid())
        }
    }

    fun update() {
        asteroids.forEach {
            it.update()
            if (it.collidesWith(ship)) {
                stop()
            }
        }

        ship.update(asteroids)
    }

    override fun onShipUpdated(bullets: Collection<Bullet>) {
        asteroids.forEach {
            bullets.forEach { bullet ->
                if (it.collidesWith(bullet)) {
                    it.explode()
                    bullet.explode()
                }
            }
        }
    }

    fun getShapes(xScale: Double, xPos: Double, yScale: Double, yPos: Double) : Collection<Pair<DoubleArray, DoubleArray>> {
        // Initialize shapes
        val shapes = mutableListOf<Pair<DoubleArray, DoubleArray>>()

        // Add ship
        shapes.addAll(ship.getShapes(xScale, xPos, yScale, yPos))

        // Add asteroids
        asteroids.forEach { asteroid ->
            shapes.addAll(asteroid.getShapes(xScale, xPos, yScale, yPos))
        }

        // Return
        return shapes
    }

    private fun stop() {

    }
}

class Asteroid {
    /**
     * Shape
     */
    private val mesh = arrayOf(Pair(0.0, 0.0),
            Pair(0.0, 60.0),
            Pair(60.0, 60.0),
            Pair(60.0, 0.0))

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
        return false
    }

    fun collidesWith(ship: Ship) : Boolean {
        return false
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

    fun explode() {

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

        // Return the shapes
        return shapes
    }

    fun isWithin(polygon: Polygon): Boolean {
        return polygon.contains(x, y)
    }
}

private const val DRAG_COEFFICIENT = 0.01
private const val ROTATIONAL_DRAG_COEFFICIENT = 0.1
private const val MAX_ROT_VEL = 15.0
private const val MAX_VEL = 0.1

class Ship : Bullet.Callbacks {

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
    fun setNetworkWeights(weights: Array<Double>) {
        network.setWeights(weights)
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
        bullets.clear()
        x = SIM_WIDTH / 2
        y = SIM_HEIGHT / 2
        vX = 0.0
        vY = 0.0
        vRot = 0.0
        rot = 0.0
    }

    fun update(asteroids: Collection<Asteroid>) {
        // Calculate the ships quadrants
        val upQuadrant = Polygon(x, y, 0.0, 0.0, SIM_WIDTH, 0.0)
        val rightQuadrant = Polygon(x, y, SIM_WIDTH, 0.0, SIM_WIDTH, SIM_HEIGHT)
        val downQuadrant = Polygon(x, y, SIM_WIDTH, SIM_HEIGHT, 0.0, SIM_HEIGHT)
        val leftQuadrant = Polygon(x, y, 0.0, SIM_HEIGHT, 0.0, 0.0)

        // Setup the network inputs
        val upDanger = asteroids.sumByDouble { if (it.isWithin(upQuadrant)) 1.0 else 0.0 }
        val rightDanger = asteroids.sumByDouble { if (it.isWithin(rightQuadrant)) 1.0 else 0.0 }
        val downDanger = asteroids.sumByDouble { if (it.isWithin(downQuadrant)) 1.0 else 0.0 }
        val leftDanger = asteroids.sumByDouble { if (it.isWithin(leftQuadrant)) 1.0 else 0.0 }

        // Get the network outputs
        val networkOutputs = network.update(arrayOf(leftDanger, upDanger, rightDanger, downDanger))
        val torque = if (networkOutputs[0] > 0.5) 1 else -1
        val thrust = if (networkOutputs[1] > 0.5) 1 else 0

        // Apply the outputs
        vRot = Math.max(-MAX_ROT_VEL, Math.min(MAX_ROT_VEL, vRot + torque)) - ROTATIONAL_DRAG_COEFFICIENT * vRot
        rot += vRot
        vX += Math.max(0.0, Math.min(MAX_VEL, thrust * -Math.sin(Math.toRadians(rot)))) - DRAG_COEFFICIENT * vX
        vY += Math.max(0.0, Math.min(MAX_VEL, thrust * Math.cos(Math.toRadians(rot)))) - DRAG_COEFFICIENT * vY
        x += vX
        y += vY
        if (x > SIM_WIDTH) x = 0.0
        else if (x < 0) x = SIM_WIDTH
        if (y > SIM_HEIGHT) y = 0.0
        else if (y < 0) y = SIM_HEIGHT
    }

    override fun onBulletExploded(bullet: Bullet) {
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

        // Return the shapes
        return shapes
    }
}

class Bullet {

    interface Callbacks {
        fun onBulletExploded(bullet: Bullet)
    }

    private var callbacks: Callbacks? = null
    fun setCallbacks(callbacks: Callbacks) {
        this.callbacks = callbacks
    }

    fun explode() {
        callbacks?.onBulletExploded(this)
    }
}