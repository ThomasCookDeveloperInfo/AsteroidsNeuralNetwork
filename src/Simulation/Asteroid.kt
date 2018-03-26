package Simulation

import Utilities.NonDeterminism
import javafx.scene.shape.Polygon
import javafx.scene.shape.Shape

class Asteroid(var size: Int = 1) {

    /**
     * State variables
     */
    var x = NonDeterminism.randomAsteroidX(SIM_WIDTH)
    var y = NonDeterminism.randomAsteroidY(SIM_HEIGHT)

    var vX = (((1.0 - -1.0) / (1.0 - 0.0)) * (NonDeterminism.randomDouble() - 1.0) + 1.0) * size
    var vY = (((1.0 - -1.0) / (1.0 - 0.0)) * (NonDeterminism.randomDouble() - 1.0) + 1.0) * size
    private var vRot = NonDeterminism.randomDouble()
    private var rot = 0.0

    constructor(size: Int, parent: Asteroid, bullet: Bullet) : this(size) {
        x = parent.x
        y = parent.y

        vX = bullet.vX / 3 + NonDeterminism.randomDouble(2.0) * if (NonDeterminism.randomBoolean()) 1 else -1
        vY = bullet.vY / 3 + NonDeterminism.randomDouble(2.0) * if (NonDeterminism.randomBoolean()) 1 else -1
    }

    /**
     * Shape
     */
    private val mesh = arrayOf(Pair(-30.0 / size, -30.0 / size),
            Pair(-30.0 / size, 30.0 / size),
            Pair(30.0 / size, 30.0 / size),
            Pair(30.0 / size,  -30.0 / size))

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