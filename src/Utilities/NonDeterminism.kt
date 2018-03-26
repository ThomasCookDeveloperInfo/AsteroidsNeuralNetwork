package Utilities

import Simulation.SIM_HEIGHT
import Simulation.SIM_WIDTH
import java.util.concurrent.ThreadLocalRandom

private const val HALF_OF_SIM_WIDTH = SIM_WIDTH / 2
private const val HALF_OF_SIM_HEIGHT = SIM_HEIGHT / 2
private const val SHIP_SPAWN_LEFT = SIM_WIDTH / 2 - HALF_OF_SIM_WIDTH
private const val SHIP_SPAWN_RIGHT = SIM_WIDTH / 2 + HALF_OF_SIM_WIDTH
private const val SHIP_SPAWN_TOP = SIM_HEIGHT / 2 - HALF_OF_SIM_HEIGHT
private const val SHIP_SPAWN_BOTTOM = SIM_HEIGHT / 2 + HALF_OF_SIM_HEIGHT

// Thread safe singleton for getting non deterministic data
object NonDeterminism {
    fun randomNetworkWeight() = ((ThreadLocalRandom.current().nextFloat() - 0f) / (1f - 0f)) * (1f - -1f) + -1f
    fun randomDouble() = ThreadLocalRandom.current().nextDouble()
    fun randomAsteroidX(max: Double) : Double {
        var x = Double.NEGATIVE_INFINITY
        while(x < SHIP_SPAWN_LEFT || x > SHIP_SPAWN_RIGHT) {
            x = ThreadLocalRandom.current().nextDouble(max)
        }
        return x
    }
    fun randomAsteroidY(max: Double) : Double {
        var y = Double.NEGATIVE_INFINITY
        while(y < SHIP_SPAWN_TOP || y > SHIP_SPAWN_BOTTOM) {
            y = ThreadLocalRandom.current().nextDouble(max)
        }
        return y
    }
    fun randomDouble(max: Double) = ThreadLocalRandom.current().nextDouble(max)
    fun randomCrossoverPoint(max: Int) =  ThreadLocalRandom.current().nextInt(max)
    fun randomBoolean() = ThreadLocalRandom.current().nextBoolean()
}