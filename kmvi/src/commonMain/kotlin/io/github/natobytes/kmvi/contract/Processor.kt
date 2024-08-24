package io.github.natobytes.kmvi.contract

import kotlinx.coroutines.flow.Flow

/**
 * Processes [Intent]s and transforms them into a stream of [Request]s within the MVI architecture.
 *
 * A [Processor] acts as a bridge between the View and the Model, handling the logic of interpreting user
 * intentions ([Intent]s) and determining the appropriate actions or data changes ([Request]s)
 * required to update the application's [State].
 */
interface Processor<I : Intent, R : Request, S : State> {

    /**
     * Processes an [Intent] and emits a stream of [Request]s based on the current [State].
     *
     * @param input The [Intent] representing a user action or event.
     * @param state The current [State] of the relevant part of the application.
     * @return A [Flow] of [Request]s that represent the actions or data changes needed to update the state.
     */
    fun process(input: I, state: S): Flow<R>
}
