package io.github.natobytes.kmvi.contract

/**
 * Defines a function that transforms the application state in response to a [Result] within the MVI architecture.
 *
 * A Reducer is a pure function that takes the current [State] of the application and a [Result]
 * representing an intention to change the state, and returns a new, immutable [State] reflecting
 * the result of processing that Result.
 */
interface Reducer<R : Result, S : State> {

    /**
     * Processes a [Result] and updates the application [State] accordingly.
     *
     * @param result The [Result] to be processed, representing an intention to change the state.
     * @param state The current [State] of the application.
     * @return A new, immutable [State] reflecting the changes resulting from processing the [Result].
     */
    fun reduce(result: R, state: S): S
}
