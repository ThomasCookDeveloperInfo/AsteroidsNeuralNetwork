package Learning

private const val CROSSOVER_RATE = 1
private const val MUTATION_CHANCE = 0.01
private const val POPULATION_SIZE = 10
private const val MAX_PERTURBATION = 0.1

// Functions for performing selection, crossover and mutation of a networks weights
class Genetics {

    // The population of network members to perform genetic selection on
    val population = Array(POPULATION_SIZE, { _ -> NetworkPopulationMember() })

    // Starts a new epoch
    fun epoch() {
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
        val slice = NonDeterminism.nextRandomDouble() * totalPopulationFitness

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
    constructor(weights: Array<Double>) : this(Network(weights))

    // Tracks the fitness of this member with respect to the rest of the population
    var fitness = 0.0

    // Performs crossover with the passed member and returns a new member
    fun crossover(with: NetworkPopulationMember) : NetworkPopulationMember {
        // If the crossover rate is not met, or the parents are the same just return a copy of one of them
        if (NonDeterminism.nextRandomDouble() > CROSSOVER_RATE || this == with) {
            return NetworkPopulationMember(this.network.getAllWeights())
        }

        // Get the weights of the parents
        val mumWeights = with.network.getAllWeights()
        val dadWeights = this.network.getAllWeights()

        // Determine the random crossover point
        val crossoverPoint = NonDeterminism.nextRandomInt(mumWeights.size - 1)

        // Create the child weights array
        val childWeights = mutableListOf<Double>()
        childWeights.addAll(mumWeights.sliceArray(IntRange(0, crossoverPoint)))
        childWeights.addAll(dadWeights.sliceArray(IntRange(crossoverPoint, mumWeights.size - 1)))

        for (index in 0 until childWeights.size) {
            if (NonDeterminism.nextRandomDouble() < MUTATION_CHANCE) {
                childWeights[index] += (NonDeterminism.nextRandomDouble() - NonDeterminism.nextRandomDouble()) * MAX_PERTURBATION
            }
        }

        // Create and return the child network
        return NetworkPopulationMember(childWeights.toTypedArray())
    }
}