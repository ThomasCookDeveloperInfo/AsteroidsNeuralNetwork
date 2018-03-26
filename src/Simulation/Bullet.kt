package Simulation

import GUI.HEIGHT
import GUI.WIDTH
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.shape.Polygon
import javafx.util.Duration

class Bullet(private val configuration: Configuration,
             private val shipX: Double, private val shipY: Double,
             private var x: Double = shipX, private var y: Double = shipY,
             private val shipVx: Double, private val shipVy: Double,
             private val shipRot: Double,
             val vX: Double = shipVx + configuration.bulletVel * -Math.sin(Math.toRadians(shipRot)),
             val vY: Double = shipVy + configuration.bulletVel * Math.cos(Math.toRadians(shipRot))) {

    private val timeoutTimer: Timeline

    /**
     * Shape
     */
    private val mesh = arrayOf(Pair(-2.0, -2.0),
            Pair(-2.0, 2.0),
            Pair(2.0, 2.0),
            Pair(2.0, -2.0))

    init {
        timeoutTimer = Timeline(KeyFrame(Duration.millis(configuration.bulletTimeoutMilliseconds), EventHandler<ActionEvent> {
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
        x += vX
        y += vY

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