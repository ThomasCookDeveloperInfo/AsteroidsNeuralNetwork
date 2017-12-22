package GameModel

import GUI.HEIGHT
import GUI.WIDTH
import Learning.NetworkPopulationMember

// Handles simulating a game of asteroids being played by the ai
class Simulation(val networkPopulationMember: NetworkPopulationMember) {

    // The asteroids
    val asteroids = mutableListOf<Asteroid>()
    init {
        asteroids.addAll(listOf(Asteroid(Math.max(0.0, Math.min(WIDTH, Math.random() * WIDTH)),
                Math.max(0.0, Math.min(HEIGHT, Math.random() * HEIGHT))), Asteroid(Math.max(0.0, Math.min(WIDTH, Math.random() * WIDTH)),
                Math.max(0.0, Math.min(HEIGHT, Math.random() * HEIGHT))), Asteroid(Math.max(0.0, Math.min(WIDTH, Math.random() * WIDTH)),
                Math.max(0.0, Math.min(HEIGHT, Math.random() * HEIGHT)))))
    }

    // The ship
    val ship = Ship()

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
        ship.update(networkOutputs[0], networkOutputs[1], networkOutputs[2] > 0)

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
        val dX = ship.xPos - asteroid.xPos
        val dY = ship.yPos - asteroid.yPos
        return arrayOf(dX, dY)
    }
}