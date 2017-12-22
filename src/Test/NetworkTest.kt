package Test

import Learning.Network
import org.junit.Assert
import org.junit.Test

internal class NetworkTest {
    @Test
    fun test() {
        // ARRANGE
        val network = Network()
        val inputs = arrayOf(1.0, 0.5)

        // ACT
        val output = network.update(inputs)

        // ASSERT
        Assert.assertTrue(output.size == 2)
    }
}