package Learning

import java.util.*

private const val LAYER_COUNT = 3
private const val HIDDEN_NEURON_COUNT = 6
private const val INPUT_COUNT = 2
private const val OUTPUT_COUNT = 2
private const val BIAS = 1
private const val ACTIVATION_RESPONSE = 1

object NonDeterminism {
    private val rand = Random()
    fun nextRandomDouble() : Double = rand.nextDouble() * if (rand.nextBoolean()) 1 else -1
}

// A simple ANN
// No back prop as we'll train it with a GA
class Network {

    // The networks layers
    private val layers = Array(LAYER_COUNT, { index ->
        when (index) {
            0 -> { Layer(INPUT_COUNT, HIDDEN_NEURON_COUNT) }
            LAYER_COUNT - 1 -> { Layer(HIDDEN_NEURON_COUNT, OUTPUT_COUNT) }
            else -> { Layer(HIDDEN_NEURON_COUNT, HIDDEN_NEURON_COUNT) }
        }
    })

    // Run the network against the inputs and return the outputs
    fun update(inputs: Array<Double>) : Array<Double> {
        var outputs = inputs
        layers.forEach { layer ->
            outputs = layer.activate(outputs)
        }
        return outputs
    }

    // Represents a layer in the artificial neural network
    private class Layer(inputSize: Int, private val outputSize: Int) {

        // Initialize the weights to random double values between -1.0 and 1.0
        private val weights: Array<Double> = Array(inputSize + 1, { _ -> NonDeterminism.nextRandomDouble() })

        // Activate the layer and return the set of outputs
        fun activate(inputs: Array<Double>) : Array<Double> {
            // Validate the inputs are the correct size
            if (inputs.size >= weights.size)
                throw IllegalStateException("There are an incorrect number of inputs")

            // Create the output array
            val outputs = Array(outputSize, { _ -> 0.0 })

            // Foreach output, work out the activation
            for (output in 0 until outputs.size) {
                // Foreach input, multiply by the corresponding weight
                for (input in 0 until inputs.size) {
                    // Track the net input
                    var netInput = (0 until weights.size).sumByDouble {
                        weights[it] * inputs[input]
                    }

                    // Add the bias
                    netInput += weights.last() * BIAS

                    // Set the output
                    outputs[output] = sigmoid(netInput)
                }
            }

            return outputs
        }

        // Calculate the sigmoid derivative of the passed input
        private fun sigmoid(input: Double) : Double = 1 / (1 + Math.exp(-input / ACTIVATION_RESPONSE))
    }
}