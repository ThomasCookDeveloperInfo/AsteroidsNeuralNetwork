package GUI

import GameModel.*
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import java.util.*

private const val FRAME_TIME_MILLISECONDS = 25L

class GameController(@FXML private var canvas: Canvas? = null,
                     private val timer: Timer = Timer(),
                     private var game: Game = Game()) {

    fun initialize() {
        game.ship.setPosition(WIDTH / 2, HEIGHT / 2)

        canvas?.setOnKeyPressed { keyPress ->
            when {
                keyPress.code == KeyCode.LEFT -> {
                    game.xAxis = -2.0
                }
                keyPress.code == KeyCode.RIGHT -> {
                    game.xAxis = 2.0
                }
                keyPress.code == KeyCode.UP -> {
                    game.yAxis = 1.0
                }
                keyPress.code == KeyCode.SPACE -> {
                    game.space = true
                }
            }
        }

        canvas?.setOnKeyReleased { keyUp ->
            when {
                keyUp.code == KeyCode.LEFT -> {
                    game.xAxis = 0.0
                }
                keyUp.code == KeyCode.RIGHT -> {
                    game.xAxis = 0.0
                }
                keyUp.code == KeyCode.UP -> {
                    game.yAxis = 0.0
                }
                keyUp.code == KeyCode.SPACE -> {
                    game.space = false
                }
            }
        }

        // Setup the renderer timer
        timer.schedule(object: TimerTask() {
            override fun run() {
                game.update()
                invalidate()
            }
        }, 0, FRAME_TIME_MILLISECONDS)
    }

    fun invalidate() {
        canvas?.apply {
            val context = graphicsContext2D
            context.apply {
                clearRect(0.0, 0.0, WIDTH, HEIGHT)

                fill = Color.BLACK

                fillRect(0.0, 0.0, WIDTH, HEIGHT)

                stroke = Color.WHITE
                fill = Color.WHITE

                // Get reference to the ship and draw it
                val ship = game.ship
                strokePolygon(ship.getXPoints(), ship.getYPoints(), SHIP_POINTS)

                ship.bullets.forEach { bullet ->
                    val point = bullet.getPoints()
                    val x = point[0]
                    val y = point[1]
                    fillOval(x, y, BULLET_RADIUS, BULLET_RADIUS)
                }

                val destroyedRoids = mutableListOf<Int>()
                // For each asteroid draw it
                game.asteroids.forEachIndexed { index, asteroid ->
                    asteroid.update()
                    strokePolygon(asteroid.getXPoints(), asteroid.getYPoints(), ASTEROID_POINTS)
                    val destroyedBullets = mutableListOf<Int>()
                    game.ship.bullets.forEachIndexed { bulletIndex, bullet ->
                        if (asteroid.collidesWith(bullet.xPos, bullet.yPos)) {
                            destroyedRoids.add(index)
                            destroyedBullets.add(bulletIndex)
                        }
                    }
                    destroyedBullets.forEach {
                        game.ship.bullets.removeAt(it)
                    }
                }

                destroyedRoids.forEach {
                    val destroyedRoid = game.asteroids.elementAt(it)
                    if (destroyedRoid.size < 3) {
                        val newRoid1 = Asteroid(destroyedRoid.xPos, destroyedRoid.yPos)
                        val newRoid2 = Asteroid(destroyedRoid.xPos, destroyedRoid.yPos)
                        newRoid1.size = destroyedRoid.size + 1
                        newRoid2.size = destroyedRoid.size + 1
                        game.asteroids.addAll(listOf(newRoid1, newRoid2))
                    }
                    game.asteroids.remove(destroyedRoid)
                }

                if (game.asteroids.size == 0) {
                    game = Game()
                    initialize()
                }

//                game.asteroids.forEach {
//                    if (it.collidesWith(game.ship.xPos, game.ship.yPos)) {
//                        game = Game()
//                        initialize()
//                    }
//                }
            }
        }
    }
}