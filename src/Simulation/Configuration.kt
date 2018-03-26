package Simulation

data class Configuration(val simTimeoutSeconds: Double = 60.0,
                         val activationResponse: Double = 1.0,
                         val asteroids: Int = 5,
                         val maxChunkFactor: Int = 3,
                         val chunkCount: Int = 3,
                         val maxAsteroids: Int = asteroids + ( asteroids * chunkCount) + (asteroids * chunkCount * maxChunkFactor),
                         val reloadTimeMilliseconds: Double = 333.0,
                         val dragCoefficient: Double = 0.04,
                         val rotationalDragCoefficient: Double = 0.85,
                         val maxVel: Double = 0.1,
                         val maxRotVel: Double = 15.0,
                         val asteroidFitnessWeight: Double = 20.0,
                         val asteroidsToConsider: Int = 5,
                         val bulletVel: Double = 5.0,
                         val bulletTimeoutMilliseconds: Double = 2000.0)