package GameModel

import GUI.HEIGHT
import GUI.WIDTH
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

const val SHIP_POINTS = 3
const val BULLET_RADIUS = 5.0
private const val DRAG_COEFFICIENT = 0.01
private const val ROTATIONAL_DRAG_COEFFICIENT = 0.1
private const val MAX_ROT_VEL = 15.0
private const val MAX_VEL = 0.1
private const val BULLET_VEL = 10.0
private const val RELOAD_TIME = 300L
private const val BULLET_TIMEOUT = 1000L

private val mesh = arrayOf(Pair(-7.0, -7.0),
        Pair(0.0, 14.0),
        Pair(7.0, -7.0))

class Ship : Bullet.Callbacks {
    var xPos = 0.0
    var yPos = 0.0
    private var vX = 0.0
    private var vY = 0.0
    private var vRot = 0.0
    private var rot = 0.0
    private var canShoot = true
    private val shootTimer = Timer()
    val bullets = CopyOnWriteArrayList<Bullet>()

    init {
        shootTimer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                canShoot = true
            }
        }, 0, RELOAD_TIME)
    }

    override fun timedOut(bullet: Bullet) {
        this.bullets.remove(bullet)
    }

    fun setPosition(x: Double, y: Double) {
        xPos = x
        yPos = y
    }

    fun update(torque: Double, thrust: Double, shooting: Boolean) {
        vRot = Math.max(-MAX_ROT_VEL, Math.min(MAX_ROT_VEL, vRot + torque)) - ROTATIONAL_DRAG_COEFFICIENT * vRot
        rot += vRot
        vX += Math.max(-MAX_VEL, Math.min(MAX_VEL, thrust * -Math.sin(Math.toRadians(rot)))) - DRAG_COEFFICIENT * vX
        vY += Math.max(-MAX_VEL, Math.min(MAX_VEL, thrust * Math.cos(Math.toRadians(rot)))) - DRAG_COEFFICIENT * vY
        xPos += vX
        yPos += vY
        if (xPos > WIDTH) xPos = 0.0
        else if (xPos < 0) xPos = WIDTH
        if (yPos > HEIGHT) yPos = 0.0
        else if (yPos < 0) yPos = HEIGHT

        if (shooting && canShoot) {
            val bullet = Bullet(this, vX, vY)
            bullet.setPosition(xPos, yPos)
            bullet.setRotation(rot)
            bullets.add(bullet)
            canShoot = false
        }
    }

    fun getXPoints(scale: Int, xOrigin: Double) : DoubleArray {
        val xPoints = DoubleArray(SHIP_POINTS, { index -> 0.0 })
        for (i in 0 until SHIP_POINTS) {
            val x = mesh[i].first / scale
            val y = mesh[i].second / scale
            xPoints[i] = (xPos / scale + (x * Math.cos(Math.toRadians(rot)) - y * Math.sin(Math.toRadians(rot)))) + xOrigin
        }
        return xPoints
    }

    fun getYPoints(scale: Int, yOrigin: Double) : DoubleArray {
        val yPoints = DoubleArray(SHIP_POINTS, { index -> 0.0 })
        for (i in 0 until SHIP_POINTS) {
            val x = mesh[i].first / scale
            val y = mesh[i].second / scale
            yPoints[i] = (yPos / scale + (x * Math.sin(Math.toRadians(rot)) + y * Math.cos(Math.toRadians(rot)))) + yOrigin
        }
        return yPoints
    }
}

class Bullet(callbacks: Bullet.Callbacks, private val initialVelX: Double, private val initialVelY: Double) {
    var xPos = 0.0
    var yPos = 0.0
    private var rot = 0.0
    private val timer = Timer()

    init {
        timer.schedule(object: TimerTask() {
            override fun run() {
                callbacks.timedOut(this@Bullet)
            }
        }, BULLET_TIMEOUT)
    }

    fun setPosition(x: Double, y: Double) {
        xPos = x
        yPos = y
    }

    fun setRotation(rot: Double) {
        this.rot = rot
    }

    fun getPoints(scale: Int, xOrigin: Double, yOrigin: Double) : DoubleArray {
        xPos += initialVelX + BULLET_VEL * -Math.sin(Math.toRadians(rot))
        yPos += initialVelY + BULLET_VEL * Math.cos(Math.toRadians(rot))

        if (xPos > WIDTH) xPos = 0.0
        else if (xPos < 0) xPos = WIDTH
        if (yPos > HEIGHT) yPos = 0.0
        else if (yPos < 0) yPos = HEIGHT

        return doubleArrayOf((xPos / scale) + xOrigin, (yPos / scale) + yOrigin)
    }

    interface Callbacks {
        fun timedOut(bullet: Bullet)
    }
}