package GameModel

import GUI.HEIGHT
import GUI.WIDTH
import Learning.NetworkPopulationMember
import Utilities.NonDeterminism

// Handles simulating a game of asteroids being played by the ai
class Simulation(val networkPopulationMember: NetworkPopulationMember) {

    // The asteroids
    val asteroids = mutableListOf<Asteroid>()

    // The ship
    val ship = Ship()

    init {
        asteroids.addAll(listOf(Asteroid(Math.max(0.0, Math.min(WIDTH, NonDeterminism.randomDouble() * WIDTH)),
                Math.max(0.0, Math.min(HEIGHT, NonDeterminism.randomDouble() * HEIGHT))), Asteroid(Math.max(0.0, Math.min(WIDTH, NonDeterminism.randomDouble() * WIDTH)),
                Math.max(0.0, Math.min(HEIGHT, NonDeterminism.randomDouble() * HEIGHT))), Asteroid(Math.max(0.0, Math.min(WIDTH, NonDeterminism.randomDouble() * WIDTH)),
                Math.max(0.0, Math.min(HEIGHT, NonDeterminism.randomDouble() * HEIGHT)))))

        ship.setPosition(WIDTH / 2, HEIGHT / 2)
    }

    // Call to update the simulation
    // It will run the network and apply the network outputs to the ship
    fun update() {
        // Get the asteroid closest to the ship
        val nearestAsteroid = nearestAsteroidToShip()

        // Get the vector between that asteroid and the ship
        val vectorBetween = vectorBetweenShipAndAsteroid(nearestAsteroid)

        // Get the network outputs
        val networkOutputs = networkPopulationMember.network.update(vectorBetween)

        // Update the ship
        val turn = networkOutputs[0] - networkOutputs[1]
        ship.update(if(turn < 0) -1.0 else 1.0, networkOutputs[0] + networkOutputs[1], true)

        // Update the asteroids
        asteroids.forEach {
            it.update()
        }
    }

    // Gets the asteroid nearest to the ship
    private fun nearestAsteroidToShip() : Asteroid {
        var nearestIndex = 0
        var nearestDistance = Double.MAX_VALUE
        asteroids.forEachIndexed { index, asteroid ->
            val dx = Math.abs(ship.xPos - asteroid.xPos)
            val dy = Math.abs(ship.yPos - asteroid.yPos)
            val distance = Math.sqrt(dx * dx + dy * dy)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestIndex = index
            }
        }
        return asteroids[nearestIndex]
    }

    // Returns the vector between the ship and the passed asteroid
    private fun vectorBetweenShipAndAsteroid(asteroid: Asteroid) : Array<Double> {
        val dX = Math.abs(ship.xPos - asteroid.xPos)
        val dY = Math.abs(ship.yPos - asteroid.yPos)
        return arrayOf(dX, dY)
    }
}