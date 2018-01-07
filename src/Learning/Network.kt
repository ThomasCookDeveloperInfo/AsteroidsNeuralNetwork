package Learning

import Utilities.NonDeterminism

private const val LAYER_COUNT = 3
private const val HIDDEN_NEURON_COUNT = 6
private const val INPUT_COUNT = 4
private const val OUTPUT_COUNT = 2
private const val BIAS = 0
private const val ACTIVATION_RESPONSE = 1

// A simple ANN
// No back prop as we'll train it with a GA
class Network {

    // The networks layers
    private val layers = mutableListOf<Layer>()

    // Default constructor initializes all layers with random weights
    constructor() {
        layers.clear()
        for (layer in 0 until LAYER_COUNT) {
            when (layer) {
                0 -> { layers.add(Layer(INPUT_COUNT, HIDDEN_NEURON_COUNT)) }
                LAYER_COUNT - 1 -> { layers.add(Layer(if(LAYER_COUNT == 2) { INPUT_COUNT } else { HIDDEN_NEURON_COUNT }, OUTPUT_COUNT)) }
                else -> { layers.add(Layer(HIDDEN_NEURON_COUNT, HIDDEN_NEURON_COUNT)) }
            }
        }
    }

    fun setWeights(weights: Array<Double>) {
        layers.clear()

        for (layer in 0 until LAYER_COUNT) {
            val startIndex = if (LAYER_COUNT == 2) layer * INPUT_COUNT + layer * OUTPUT_COUNT else layer * INPUT_COUNT + layer * HIDDEN_NEURON_COUNT
            val endIndex = if (LAYER_COUNT == 2) startIndex + layer * OUTPUT_COUNT else startIndex + layer * HIDDEN_NEURON_COUNT

            when (layer) {
                0 -> { layers.add(Layer(INPUT_COUNT, HIDDEN_NEURON_COUNT, weights.sliceArray(IntRange(startIndex, endIndex)))) }
                LAYER_COUNT - 1 -> { layers.add(Layer(if(LAYER_COUNT == 2) { INPUT_COUNT } else { HIDDEN_NEURON_COUNT }, OUTPUT_COUNT, weights.sliceArray(IntRange(startIndex, endIndex)))) }
                else -> { layers.add(Layer(HIDDEN_NEURON_COUNT, HIDDEN_NEURON_COUNT, weights.sliceArray(IntRange(startIndex, endIndex)))) }
            }

            when (layer) {
                0 -> {
                    layers.add(Layer(INPUT_COUNT, HIDDEN_NEURON_COUNT, weights.sliceArray(IntRange(layer, INPUT_COUNT * HIDDEN_NEURON_COUNT + 1))))
                }
                LAYER_COUNT - 1 -> {
                    val startIndex = INPUT_COUNT * HIDDEN_NEURON_COUNT + 1 + layer * HIDDEN_NEURON_COUNT
                    layers.add(Layer(HIDDEN_NEURON_COUNT, OUTPUT_COUNT, weights.sliceArray(IntRange(startIndex, weights.size - 1))))
                }
                else -> {
                    val startIndex = INPUT_COUNT * HIDDEN_NEURON_COUNT + 1 + layer * HIDDEN_NEURON_COUNT
                    val endIdex = startIndex + (HIDDEN_NEURON_COUNT * HIDDEN_NEURON_COUNT) + 1
                    layers.add(Layer(HIDDEN_NEURON_COUNT, HIDDEN_NEURON_COUNT, weights.sliceArray(IntRange(startIndex, endIdex))))
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
    private class Layer(val inputSize: Int,
                        private val outputSize: Int,
                        var weights: Array<Double> = Array((inputSize * outputSize) + 1, { _ ->
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

                // Add the bias
                netInput += weights.last() * BIAS

                // Set the output
                outputs[output] = sigmoid(netInput)
            }

            return outputs
        }

        // Calculate the sigmoid derivative of the passed input
        private fun sigmoid(input: Double) : Double = 1 / (1 + Math.exp(-input / ACTIVATION_RESPONSE))
    }
}