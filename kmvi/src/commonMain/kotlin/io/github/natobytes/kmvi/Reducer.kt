package io.github.natobytes.kmvi

import io.github.natobytes.kmvi.contract.Action
import io.github.natobytes.kmvi.contract.State

/**
 * Defines a function that transforms the application state in response to an [io.github.natobytes.kmvi.contract.Action] within the MVI architecture.
 *
 * A Reducer is a pure function that takes the current [io.github.natobytes.kmvi.contract.State] of the application and an [io.github.natobytes.kmvi.contract.Action]
 * representing an intention to change the state, and returns a new, immutable [io.github.natobytes.kmvi.contract.State] reflecting
 * the result of processing that Action.
 */
public fun interface Reducer<A : Action, S : State> {

    /**
     * Processes an [Action] and updates the application [State] accordingly.
     *
     * @param action The [Action] to be processed, representing an intention to change the state.
     * @param state The current [State] of the application.
     * @return A new, immutable [State] reflecting the changes resulting from processing the [Action].
     */
    public fun reduce(
        action: A,
        state: S,
    ): S
}
