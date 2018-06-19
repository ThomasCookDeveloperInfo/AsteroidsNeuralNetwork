package GUI

import java.util.*

// Define the injector provider for the game loop
private val injector = SimulationInjection

// Define the maximum allowed frame time as 250 milliseconds
// Any frames which take longer than this are assumed to have taken 250 milliseconds
// Which can lead to discrepancies in what is rendered and what was simulated
// However, this is unlikely to occur on anything other than huge simulations on low end hardware
private const val MAX_ALLOWED_FRAME_TIME_MILLIS = 250L

// The fixed time step of the physics in milliseconds
private const val dt = 100L

// Track the total time (t) elapsed since start of simulation in milliseconds
private var t = 0L

// Track the current time since epoch in milliseconds
// We use this to get the delta between frames
private var currentTimeMillis = Date().time

// We use this to track the accumulated time between frames
// It's decremented in the update loop by steps of size (dt)
// Remainder is used to get blending factor for interpolation of render
private var accumulator = 0.0

// Track the previous and current simulation states for rendering and integration.
// This could be a stack instead if we wanted time scrubbing of the simulation
private var previousState: SimulationState = injector.provideBaseSimState()
private var currentState: SimulationState = previousState

// The loop function that integrates physics against the dt of render time
// Decouples physics from the rendering. See: https://gafferongames.com/post/fix_your_timestep/
fun loop() {
    // Get the new time in milliseconds since epoch
    // Then we can calculate the time the last frame took to render
    val newTimeMillis = Date().time

    // Calculate the milliseconds the last frame took to render
    var frameTimeMillis = newTimeMillis - currentTimeMillis

    // If the last frame took longer than the max allowed frame time
    // We just assume a frame time of max allowed frame time
    // This should never happen, but, if it does we basically just skip some of the physics
    if (frameTimeMillis > MAX_ALLOWED_FRAME_TIME_MILLIS)
        frameTimeMillis = MAX_ALLOWED_FRAME_TIME_MILLIS

    // Update current time millis
    currentTimeMillis = newTimeMillis

    // Update the accumulator
    accumulator += frameTimeMillis

    // Now, decumulate the accumulator by steps of size (dt) until it's value is less than dt
    // Integrating the physics engine by dt each time. The remainder will be used to work out the blend
    // Factor for interpolation of the render at the end
    while (accumulator >= dt) {
        // Update previous state
        previousState = currentState

        // Integrate the current state against dt
        currentState = currentState.integrate(t, dt)

        //Update time elapsed
        t += dt

        // Decumulate the accumulator
        // When the loop is broken there may be a remainder on this variable
        // The remaining value will be used outside the loop to calculate
        // the alpha value to interpolate the render frame with
        accumulator -= dt
    }

    // Now, work out the blend factor for render interpolation
    // As a factor of the remaining accumulator value with respect to dt
    val alpha = accumulator / dt

    // Interpolate the current state with respect to the alpha value
    // Note - we don't actually update current state as this would make physics non-deterministic
    // Instead, we just create a temporary interpolated state for the sake of rendering
    val interpolatedCurrentState = currentState.interpolate(alpha, previousState)

    // Finally, render the interpolated state
    render(interpolatedCurrentState)
}

private fun render(state: SimulationState) {
    // Render the state here
}

// Interface for any simulation that is integrated against dt
interface SimulationState {
    fun integrate(totalTime: Long, deltaTime: Long) : SimulationState
    fun interpolate(alpha: Double, previousState: SimulationState) : SimulationState
}