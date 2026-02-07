package io.github.natobytes.kmvi

import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.Result
import io.github.natobytes.kmvi.contract.State
import kotlinx.coroutines.flow.Flow

/**
 * Processes [io.github.natobytes.kmvi.contract.Intent]s and transforms them into a stream of [io.github.natobytes.kmvi.contract.Result]s within the MVI architecture.
 *
 * A [Processor] acts as a bridge between the View and the Model, handling the logic of interpreting user
 * intentions ([io.github.natobytes.kmvi.contract.Intent]s) and determining the appropriate actions or data changes ([io.github.natobytes.kmvi.contract.Result]s)
 * required to update the application's [io.github.natobytes.kmvi.contract.State].
 */
public fun interface Processor<I : Intent, S : State> {

    /**
     * Processes an [Intent] and emits a stream of [io.github.natobytes.kmvi.contract.Result]s based on the current [State].
     *
     * @param input The [Intent] representing a user action or event.
     * @param state The current [State] of the relevant part of the application.
     * @return A [kotlinx.coroutines.flow.Flow] of [io.github.natobytes.kmvi.contract.Result]s that represent the actions or data changes needed to update the state.
     */
    public fun process(
        input: I,
        state: S,
    ): Flow<Result>
}
