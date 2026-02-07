package io.github.natobytes.kmvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.natobytes.kmvi.contract.Action
import io.github.natobytes.kmvi.contract.Effect
import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.State
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

public abstract class KMVIViewModel<I : Intent, A : Action, E : Effect, S : State>(
    initialState: S,
    private val processor: Processor<I, S>,
    private val reducer: Reducer<A, S>,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    public val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    public val effects: Flow<E> = _effects.receiveAsFlow()

    private val intents = MutableSharedFlow<I>(extraBufferCapacity = 64)

    init {
        intents
            .onEach { intent -> processIntent(intent) }
            .launchIn(viewModelScope)
    }

    public fun process(intent: I) {
        check(intents.tryEmit(intent)) { "Intent buffer overflow" }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun processIntent(intent: I) {
        try {
            processor.process(intent, _state.value)
                .flowOn(computationDispatcher)
                .collect { result ->
                    when (result) {
                        is Effect -> _effects.send(result as E)
                        is Action -> _state.update { state -> reducer.reduce(result as A, state) }
                    }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    protected open fun onError(throwable: Throwable) {
        // Default: swallow so one failing intent doesn't kill the subscription.
        // Subclasses can override to log, report, or rethrow.
        println("Uncaught exception: $throwable")
    }
}
