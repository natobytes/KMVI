package io.github.natobytes.kmvi.contract

/**
 * Defines a function that transforms the application state in response to a [Request] within the MVI architecture.
 *
 * A Reducer is a pure function that takes the current [State] of the application and a [Request]
 * representing an intention to change the state, and returns a new, immutable [State] reflecting
 * the result of processing that Request.
 */
interface Reducer<R : Request, S : State> {

    /**
     * Processes a [Request] and updates the application [State] accordingly.
     *
     * @param result The [Request] to be processed, representing an intention to change the state.
     * @param state The current [State] of the application.
     * @return A new, immutable [State] reflecting the changes resulting from processing the [Request].
     */
    fun reduce(result: R, state: S): S
}
