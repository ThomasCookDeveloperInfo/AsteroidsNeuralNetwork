package Learning

import Simulation.Configuration
import Utilities.NonDeterminism

private const val LAYER_COUNT = 3
private const val OUTPUT_COUNT = 3

// A simple ANN
// No back prop as we'll train it with a GA
class Network(private val configuration: Configuration) {
    
    private var inputCount = 0
    private var hiddenCount = 0
    
    // The networks layers
    private val layers = mutableListOf<Layer>()

    init {
        inputCount = configuration.asteroidsToConsider * 2 + 4
        hiddenCount = (inputCount + OUTPUT_COUNT) / 2
        layers.clear()
        for (layer in 0 until LAYER_COUNT) {
            when (layer) {
                0 -> { layers.add(Layer(configuration, inputCount, hiddenCount)) }
                LAYER_COUNT - 1 -> { layers.add(Layer(configuration, if(LAYER_COUNT == 2) { inputCount } else { hiddenCount }, OUTPUT_COUNT)) }
                else -> { layers.add(Layer(configuration, hiddenCount, hiddenCount)) }
            }
        }
    }

    fun setWeights(weights: Array<Double>) {
        layers.clear()
        for (layer in 0 until LAYER_COUNT) {
            when (layer) {
                0 -> {
                    layers.add(Layer(configuration, inputCount, hiddenCount, weights.sliceArray(IntRange(0, inputCount * hiddenCount))))
                }
                LAYER_COUNT - 1 -> {
                    val startIndex = layers.sumBy { it.weights.size - 1 }
                    val endIndex = when (LAYER_COUNT) {
                        2 -> {
                            startIndex + inputCount * OUTPUT_COUNT
                        } else -> {
                            startIndex + hiddenCount * OUTPUT_COUNT
                        }
                    }
                    when (LAYER_COUNT) {
                        2 -> {
                            layers.add(Layer(configuration, inputCount, OUTPUT_COUNT, weights.sliceArray(IntRange(startIndex, endIndex))))
                        } else -> {
                            layers.add(Layer(configuration, hiddenCount, OUTPUT_COUNT, weights.sliceArray(IntRange(startIndex, endIndex))))
                        }
                    }
                }
                else -> {
                    val startIndex = layers.sumBy { it.weights.size - 1 }
                    val endIndex = startIndex + hiddenCount * hiddenCount
                    layers.add(Layer(configuration, hiddenCount, hiddenCount, weights.sliceArray(IntRange(startIndex, endIndex))))
                }
            }
        }
    }

    // Run the network against the inputs and return the outputs
    fun update(inputs: Array<Double>) : Array<Double> {
        var outputs = inputs
        layers.forEach { layer ->
            outputs = layer.activate(outputs)
        }
        return outputs
    }

    // Gets the weights of all the layers as a single array
    fun getCopyOfWeights() : Array<Double> {
        val weights = mutableListOf<Double>()
        layers.forEach { layer ->
            weights.addAll(layer.weights)
        }
        return weights.toTypedArray()
    }

    // Represents a layer in the artificial neural network
    private class Layer(private val configuration: Configuration,
                        private val inputSize: Int,
                        private val outputSize: Int,
                        var weights: Array<Double> = Array((inputSize * outputSize), { _ ->
                            NonDeterminism.randomNetworkWeight()
                        })) {

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
                var netInput = 0.0
                for (input in 0 until inputs.size) {
                    netInput += weights[input * output + 1] * inputs[input]
                }

                // Set the output
                outputs[output] = tanh(netInput)
            }

            return outputs
        }

        // Calculate the sigmoid derivative of the passed input
        private fun sigmoid(input: Double) : Double = 1 / (1 + Math.exp(-input / configuration.activationResponse))
        // Calculate the tanh derivative of the passed input
        private fun tanh(input: Double) : Double = 2 * sigmoid(input * 2) - 1
    }
}