package GameModel

import GUI.HEIGHT
import GUI.WIDTH
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
        rot = Math.max(0.0, Math.min(360.0, Math.random() * 360.0))
        vRot = Math.max(0.0, Math.min(2.5, Math.random() * 2.5))
        vel = Math.max(0.0, Math.min(2.5, Math.random() * 2.5))
    }

    constructor(xPos: Double, yPos: Double) : this() {
        this.xPos = xPos
        this.yPos = yPos
    }

    fun collidesWith(x: Double, y: Double) : Boolean {
        val roidRect = Polygon(getXPoints().map { it.toInt() }.toIntArray(),
                getYPoints().map { it.toInt() }.toIntArray(), ASTEROID_POINTS)

        return roidRect.contains(x, y)
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

    fun getXPoints() : DoubleArray {
        val xPoints = DoubleArray(ASTEROID_POINTS, { index -> 0.0 })
        for (i in 0 until ASTEROID_POINTS) {
            val x = mesh[i].first / size
            val y = mesh[i].second / size
            xPoints[i] = xPos + (x * Math.cos(Math.toRadians(rot)) - y * Math.sin(Math.toRadians(rot))) + vel
        }
        return xPoints
    }

    fun getYPoints() : DoubleArray {
        val yPoints = DoubleArray(ASTEROID_POINTS, { index -> 0.0 })
        for (i in 0 until ASTEROID_POINTS) {
            val x = mesh[i].first / size
            val y = mesh[i].second / size
            yPoints[i] = yPos + (x * Math.sin(Math.toRadians(rot)) + y * Math.cos(Math.toRadians(rot))) + vel
        }
        return yPoints
    }
}