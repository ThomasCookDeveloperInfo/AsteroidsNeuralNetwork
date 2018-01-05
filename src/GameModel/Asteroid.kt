package GameModel

import GUI.HEIGHT
import GUI.SQUARE_GRID_SIZE
import GUI.WIDTH
import Utilities.NonDeterminism
import java.awt.Polygon

const val ASTEROID_POINTS = 4

private val mesh = arrayOf(Pair(-20.0, -20.0),
        Pair(-20.0, 20.0),
        Pair(20.0, 20.0),
        Pair(20.0, -20.0))

class Asteroid {
    var size = 1
    var xPos = 0.0
    var yPos = 0.0
    private var rot = 0.0
    private var vRot = 0.0
    private var vel = 0.0

    constructor() {
        rot = Math.max(-360.0, Math.min(360.0, NonDeterminism.randomNetworkWeight() * 360.0))
        vRot = Math.max(-2.5, Math.min(2.5, NonDeterminism.randomNetworkWeight() * 2.5))
        vel = Math.max(-2.5, Math.min(2.5, NonDeterminism.randomNetworkWeight() * 2.5))
    }

    constructor(xPos: Double, yPos: Double) : this() {
        this.xPos = xPos
        this.yPos = yPos
    }

    fun collidesWith(x: Double, y: Double, xOrigin: Double, yOrigin: Double) : Boolean {
        val roidRect = Polygon(getXPoints(SQUARE_GRID_SIZE, xOrigin).map { it.toInt() }.toIntArray(),
                getYPoints(SQUARE_GRID_SIZE, yOrigin).map { it.toInt() }.toIntArray(), ASTEROID_POINTS)

        return roidRect.contains(x / SQUARE_GRID_SIZE, y / SQUARE_GRID_SIZE)
    }

    fun update() {
        rot += vRot
        xPos += vel
        yPos += vel
        if (xPos > WIDTH) xPos = 0.0
        else if (xPos < 0) xPos = WIDTH
        if (yPos > HEIGHT) yPos = 0.0
        else if (yPos < 0) yPos = HEIGHT
    }

    fun getXPoints(scale: Int, xOrigin: Double) : DoubleArray {
        val xPoints = DoubleArray(ASTEROID_POINTS, { index -> 0.0 })
        for (i in 0 until ASTEROID_POINTS) {
            val x = (mesh[i].first / size) / scale
            val y = (mesh[i].second / size) / scale
            xPoints[i] = (xPos / scale + (x * Math.cos(Math.toRadians(rot)) - y * Math.sin(Math.toRadians(rot))) + vel) + xOrigin
        }
        return xPoints
    }

    fun getYPoints(scale: Int, yOrigin: Double): DoubleArray {
        val yPoints = DoubleArray(ASTEROID_POINTS, { index -> 0.0 })
        for (i in 0 until ASTEROID_POINTS) {
            val x = (mesh[i].first / size) / scale
            val y = (mesh[i].second / size) / scale
            yPoints[i] = (yPos / scale + (x * Math.sin(Math.toRadians(rot)) + y * Math.cos(Math.toRadians(rot))) + vel) + yOrigin
        }
        return yPoints
    }
}