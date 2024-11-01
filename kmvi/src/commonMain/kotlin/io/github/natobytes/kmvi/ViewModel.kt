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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class ViewModel<I : Intent, R : Result, S : State, E : Effect>(
    initialState: S,
    private val processor: Processor<I, R, S>,
    private val reducer: Reducer<R, S>,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow() //Expose as read-only StateFlow

    private val _effects = MutableSharedFlow<E>()
    val effects: SharedFlow<E> = _effects.asSharedFlow() // Expose as read-only SharedFlow

    fun process(intent: I) {
        viewModelScope.launch(coroutineExceptionHandler) {
            processor.process(intent, _state.value)
                .flowOn(computationDispatcher)
                .onEach { result ->
                    when (result) {
                        is Effect -> _effects.emit(result as E)
                        is Action -> _state.update { state -> reducer.reduce(result, state) }
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
