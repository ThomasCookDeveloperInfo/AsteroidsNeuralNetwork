package Test

import Learning.Genetics
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
    fun testGenetics() {
        // ARRANGE
        val genetics = Genetics(1)

        // ACT
        genetics.epoch()

        // ASSERT
        Assert.assertTrue(genetics !== null)
    }
}