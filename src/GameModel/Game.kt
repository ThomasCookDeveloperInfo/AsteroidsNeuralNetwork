package GameModel

import GUI.HEIGHT
import GUI.WIDTH

class Game {
    val asteroids = mutableListOf<Asteroid>()
    init {
        asteroids.addAll(listOf(Asteroid(Math.max(0.0, Math.min(WIDTH, Math.random() * WIDTH)),
                Math.max(0.0, Math.min(HEIGHT, Math.random() * HEIGHT))), Asteroid(Math.max(0.0, Math.min(WIDTH, Math.random() * WIDTH)),
                Math.max(0.0, Math.min(HEIGHT, Math.random() * HEIGHT))), Asteroid(Math.max(0.0, Math.min(WIDTH, Math.random() * WIDTH)),
                Math.max(0.0, Math.min(HEIGHT, Math.random() * HEIGHT)))))
    }

    val ship = Ship()

    var xAxis = 0.0
    var yAxis = 0.0
    var space = false

    fun update() {
        ship.update(xAxis, yAxis, space)
        asteroids.forEach {
            it.update()
        }
    }
}