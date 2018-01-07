package Learning

import Utilities.NonDeterminism

private const val CROSSOVER_RATE = 1.75
private const val MUTATION_CHANCE = 0.01
private const val MAX_PERTURBATION = 0.1

// Functions for performing selection, crossover and mutation of a networks weights
class Genetics {

    // The population of network members to perform genetic selection on
    private val population = mutableListOf<NetworkPopulationMember>()

    // Creates a population member with the passed weights
    fun addPopulationMember(weights: DoubleArray) {
        population.add(NetworkPopulationMember(weights.toTypedArray()))
    }

    // Sets the population fitnesses to the passed set of doubles
    fun setPopulationFitnesses(fitnesses: Collection<Double>) {
        fitnesses.forEachIndexed { index, fitness ->
            population.elementAt(index).fitness = fitness
        }
    }

    // Starts a new epoch
    fun epoch() : Collection<DoubleArray> {
        // Create a new population
        val newPopulation = mutableListOf<NetworkPopulationMember>()

        // While the new population still needs filling
        while (newPopulation.size < population.size) {
            // Select parents
            val dad = rouletteSelection()
            val mum = rouletteSelection()

            // Create child
            val child = dad.crossover(mum)

            // Add child to new population
            newPopulation.add(child)
        }

        // Clear the current pop and set to the new one
        population.clear()
        population.addAll(newPopulation)

        // Return a copy of the new population
        return newPopulation.map { it.network.getCopyOfWeights().toDoubleArray() }
    }

    // Selects a member from the population using roullette method
    // This means the chance of being selected is directly proportional
    // To the fitness of the member in respect to the rest of the population
    fun rouletteSelection() : NetworkPopulationMember {
        // Work out the total fitness of the population
        var totalPopulationFitness = 0.0
        population.forEach { member ->
            totalPopulationFitness += member.fitness
        }

        // Select a random slice point
        val slice = NonDeterminism.randomDouble() * totalPopulationFitness

        // Keep looping the population until the trackFitness exceeds the slice point
        var trackedFitness = 0.0
        population.forEach { member ->
            trackedFitness += member.fitness
            if (trackedFitness > slice) {
                // We found the first member after the slice point
                // Return it
                return member
            }
        }

        // For some reason the slice was greater than than total fitness
        // So just return the last member from the population
        return population[population.size - 1]
    }
}

// Represents a network as a member of an evolving population set
class NetworkPopulationMember(val network: Network = Network()) {

    // Constructor for creating a new member with specified set of weights
    constructor(weights: Array<Double>) : this(Network()) {
        network.setWeights(weights)
    }

    // Tracks the fitness of this member with respect to the rest of the population
    var fitness = 0.0

    // Performs crossover with the passed member and returns a new member
    fun crossover(with: NetworkPopulationMember) : NetworkPopulationMember {
        // If the crossover rate is not met, or the parents are the same just return a copy of one of them
        if (NonDeterminism.randomDouble(2.0) < CROSSOVER_RATE || this == with) {
            return NetworkPopulationMember(this.network.getCopyOfWeights())
        }

        // Get the weights of the parents
        val mumWeights = with.network.getCopyOfWeights()
        val dadWeights = this.network.getCopyOfWeights()

        // Determine the random crossover point
        val crossoverPoint = NonDeterminism.randomCrossoverPoint(mumWeights.size - 1)

        // Create the child weights array
        val childWeights = mutableListOf<Double>()
        childWeights.addAll(mumWeights.sliceArray(IntRange(0, crossoverPoint)))
        childWeights.addAll(dadWeights.sliceArray(IntRange(crossoverPoint, mumWeights.size - 1)))

        for (index in 0 until childWeights.size) {
            if (NonDeterminism.randomDouble() < MUTATION_CHANCE) {
                childWeights[index] += if(NonDeterminism.randomBoolean()) MAX_PERTURBATION else - MAX_PERTURBATION
            }
        }

        // Create and return the child network
        return NetworkPopulationMember(childWeights.toTypedArray())
    }
}