package Learning

// Functions for performing selection, crossover and mutation of a networks weights
class Genetics {

    // The population of network members to perform genetic selection on
    val population: Array<NetworkPopulationMember>

    // Starts a new epoch
    fun epoch() {
        // Create a new population
        val newPopulation = mutableListOf<NetworkPopulationMember<()

        // While the new population still needs filling
        while (newPopulation.size < population.size) {
            // Select parents
            val dad = roulletteSelection()
            val mum = roulletteSelection()

            // Create child
            val child = dad.crossover(mum)

            // Mutate child
            child.mutate()

            // Add child to new population
            newPopulation.add(child)
        }
    }

    // Selects a member from the population using roullette method
    // This means the chance of being selected is directly proportional
    // To the fitness of the member in respect to the rest of the population
    fun roulletteSelection() : NetworkPopulationMember {
        // Work out the total fitness of the population
        var totalPopulationFitness 0.0
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
class NetworkPopulationMember {

    // A reference to the network that this population member represents
    private val network = Network()

    // Tracks the fitness of this member with respect to the rest of the population
    var fitness = 0.0

    // Performs crossover with the passed member and returns a new member
    fun crossover(with: NetworkPopulationMember) : NetworkPopulationMember {

    }

    // Performs mutation on the networks weights
    fun mutate() {

    }
}