package Utilities

import java.util.concurrent.ThreadLocalRandom

// Thread safe singleton for getting non deterministic data
object NonDeterminism {
    fun randomNetworkWeight() : Double = ThreadLocalRandom.current().nextDouble(-1.0, 1.0)
    fun randomDouble() : Double = ThreadLocalRandom.current().nextDouble()
    fun randomDouble(max: Double) : Double = ThreadLocalRandom.current().nextDouble(max)
    fun randomCrossoverPoint(max: Int) : Int =  ThreadLocalRandom.current().nextInt(max)
    fun randomBoolean() : Boolean = ThreadLocalRandom.current().nextBoolean()
}