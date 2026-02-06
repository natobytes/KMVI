@file:Suppress("UNCHECKED_CAST")

package io.github.natobytes.kmvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.natobytes.kmvi.contract.Action
import io.github.natobytes.kmvi.contract.Effect
import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.Processor
import io.github.natobytes.kmvi.contract.Reducer
import io.github.natobytes.kmvi.contract.Result
import io.github.natobytes.kmvi.contract.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel implementation for the MVI (Model-View-Intent) architecture pattern.
 *
 * This class orchestrates the unidirectional data flow of the MVI pattern:
 * 1. Receives [Intent]s from the View
 * 2. Processes them through a [Processor] to generate [Result]s
 * 3. Reduces [Action]s through a [Reducer] to update the [State]
 * 4. Emits [Effect]s for side effects outside the state cycle
 *
 * @param I The type of [Intent] this ViewModel handles
 * @param R The type of [Result] (specifically [Action]s) processed by the reducer
 * @param E The type of [Effect] emitted for side effects
 * @param S The type of [State] managed by this ViewModel
 * @param initialState The initial state of the ViewModel
 * @param processor The processor that transforms intents into results
 * @param reducer The reducer that transforms actions into state updates
 * @param computationDispatcher Dispatcher for heavy computation (defaults to [Dispatchers.Default])
 * @param mainDispatcher Dispatcher for UI updates (defaults to [Dispatchers.Main])
 * @param onError Optional error handler for uncaught exceptions during intent processing
 *
 * @see Intent
 * @see State
 * @see Action
 * @see Effect
 * @see Result
 * @see Processor
 * @see Reducer
 */
abstract class ViewModel<I : Intent, R : Result, E : Effect, S : State>(
    initialState: S,
    private val processor: Processor<I, S>,
    private val reducer: Reducer<R, S>,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val onError: ((Throwable) -> Unit)? = null
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)

    /**
     * The current state as a [StateFlow].
     * Observers can collect this flow to receive state updates.
     */
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<E>()

    /**
     * A [SharedFlow] of one-time effects.
     * Effects represent side effects such as navigation, showing dialogs, or other UI events
     * that don't modify the state directly.
     */
    val effects: SharedFlow<E> = _effects.asSharedFlow()

    /**
     * Processes an [Intent] through the MVI cycle.
     *
     * The intent is passed to the processor, which generates a flow of results.
     * Each result is either:
     * - An [Action]: Passed to the reducer to update the state
     * - An [Effect]: Emitted to the effects flow for side effect handling
     *
     * Any errors during processing are caught and emitted to the [errors] flow.
     *
     * @param intent The intent to process
     */
    fun process(intent: I) {
        viewModelScope.launch(coroutineExceptionHandler) {
            processor.process(intent, _state.value)
                .flowOn(computationDispatcher)
                .onEach { result ->
                    when (result) {
                        is Effect -> _effects.emit(result as E)
                        is Action -> _state.update { state -> reducer.reduce(result as R, state) }
                    }
                }
                .flowOn(mainDispatcher)
                .launchIn(viewModelScope)
        }
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("Uncaught exception: $throwable")
    }
}
