package io.github.natobytes.kmvi

import io.github.natobytes.kmvi.contract.Action
import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.Processor
import io.github.natobytes.kmvi.contract.Reducer
import io.github.natobytes.kmvi.contract.Result
import io.github.natobytes.kmvi.contract.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.test.Test
import kotlin.test.assertEquals

// Test implementations
data class CounterState(val count: Int = 0) : State

sealed class CounterIntent : Intent {
    data object Increment : CounterIntent()
}

sealed class CounterResult : Result {
    data class UpdateCount(val newCount: Int) : CounterResult(), Action
}

class CounterProcessor : Processor<CounterIntent, CounterState> {
    override fun process(input: CounterIntent, state: CounterState): Flow<Result> = flow {
        when (input) {
            is CounterIntent.Increment -> emit(CounterResult.UpdateCount(state.count + 1))
        }
    }
}

class CounterReducer : Reducer<CounterResult, CounterState> {
    override fun reduce(result: CounterResult, state: CounterState): CounterState {
        return when (result) {
            is CounterResult.UpdateCount -> state.copy(count = result.newCount)
        }
    }
}

class ReducerTest {

    @Test
    fun `reducer returns new state without modifying original`() {
        val reducer = CounterReducer()
        val originalState = CounterState(count = 5)
        
        val newState = reducer.reduce(CounterResult.UpdateCount(10), originalState)
        
        // Original state should not be modified
        assertEquals(5, originalState.count)
        // New state should have updated value
        assertEquals(10, newState.count)
    }

    @Test
    fun `reducer is deterministic`() {
        val reducer = CounterReducer()
        val state = CounterState(count = 3)
        val result = CounterResult.UpdateCount(7)
        
        val newState1 = reducer.reduce(result, state)
        val newState2 = reducer.reduce(result, state)
        
        // Same inputs should produce same outputs
        assertEquals(newState1, newState2)
    }

    @Test
    fun `reducer handles multiple transformations`() {
        val reducer = CounterReducer()
        var state = CounterState(count = 0)
        
        state = reducer.reduce(CounterResult.UpdateCount(5), state)
        assertEquals(5, state.count)
        
        state = reducer.reduce(CounterResult.UpdateCount(10), state)
        assertEquals(10, state.count)
        
        state = reducer.reduce(CounterResult.UpdateCount(2), state)
        assertEquals(2, state.count)
    }
}
