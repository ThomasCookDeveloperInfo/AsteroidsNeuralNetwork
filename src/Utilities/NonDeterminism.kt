package Utilities

import java.util.*

// Thread safe singleton for getting non deterministic data
object NonDeterminism {
    private val rand = Random()
    fun randomNetworkWeight() : Double = rand.nextDouble() * if (rand.nextBoolean()) 1 else -1
    fun randomDouble() : Double = rand.nextDouble()
    fun randomDouble(max: Double) : Double = rand.nextInt(max.toInt()) * rand.nextDouble()
    fun randomCrossoverPoint(max: Int) : Int = rand.nextInt(max)
}