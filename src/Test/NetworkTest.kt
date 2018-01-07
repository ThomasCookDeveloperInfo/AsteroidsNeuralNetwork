package Test

import Learning.Network
import org.junit.Assert
import org.junit.Test

internal class NetworkTest {
    @Test
    fun testNetwork() {
        // ARRANGE
        val network = Network()
        val inputs = arrayOf(1.0, 0.5)

        // ACT
        val output = network.update(inputs)

        // ASSERT
        Assert.assertTrue(output.size == 2)
    }

    @Test
    fun testTorqueNormalization() {
        val networkOutput = 1.0
        val normalized = ((1.0 - -1.0) / (1.0 - 0.0)) * (networkOutput - 1.0) + 1.0
        Assert.assertTrue(normalized == 0.0)
    }

    @Test
    fun testGenetics() {
        // ARRANGE
//        val genetics = Genetics(1)
//
//        // ACT
//        genetics.epoch()
//
//        // ASSERT
//        Assert.assertTrue(genetics !== null)
    }
}