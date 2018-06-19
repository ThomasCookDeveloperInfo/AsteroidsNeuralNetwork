package GUI

object SimulationInjection : Injection {
    override fun provideBaseSimState() : SimulationState {
        // TODO: Return a concrete implementation of a base sim state
    }
}

interface Injection {
    fun provideBaseSimState() : SimulationState
}