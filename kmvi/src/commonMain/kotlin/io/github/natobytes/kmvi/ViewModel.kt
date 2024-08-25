@file:Suppress("UNCHECKED_CAST")

package io.github.natobytes.kmvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.Processor
import io.github.natobytes.kmvi.contract.Reducer
import io.github.natobytes.kmvi.contract.Request
import io.github.natobytes.kmvi.contract.SideEffect
import io.github.natobytes.kmvi.contract.State
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class MviViewModel<I : Intent, R : Request, S : State, E : SideEffect>(
    initialState: S,
    private val processor: Processor<I, R, S>,
    private val reducer: Reducer<R, S>,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow() //Expose as read-only StateFlow

    private val _effects = MutableSharedFlow<E>()
    val effects: SharedFlow<E> = _effects.asSharedFlow() // Expose as read-only SharedFlow

    fun processIntent(intent: I) {
        viewModelScope.launch(logUncaughtExceptions) {
            processor.process(intent, _state.value)
                .collect { request ->
                    when (request) {
                        is SideEffect -> _effects.emit(request as E)
                        else -> _state.update { state -> reducer.reduce(request, state) }
                    }
                }
        }
    }

    private val logUncaughtExceptions = CoroutineExceptionHandler { _, throwable ->
        println("Uncaught exception: $throwable")
    }
}
