package Test

import org.junit.Assert
import org.junit.Test

internal class NetworkTest {

    @Test
    fun testTorqueNormalization() {
        val networkOutput = 1.0
        val normalized = ((1.0 - -1.0) / (1.0 - 0.0)) * (networkOutput - 1.0) + 1.0
        Assert.assertTrue(normalized == 0.0)
    }
}