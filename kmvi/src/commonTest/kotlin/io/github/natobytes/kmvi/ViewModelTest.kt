package io.github.natobytes.kmvi

import io.github.natobytes.kmvi.contract.Action
import io.github.natobytes.kmvi.contract.Effect
import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.Processor
import io.github.natobytes.kmvi.contract.Reducer
import io.github.natobytes.kmvi.contract.Result
import io.github.natobytes.kmvi.contract.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Test State
data class TestState(val count: Int = 0, val message: String = "") : State

// Test Intents
sealed class TestIntent : Intent {
    data object Increment : TestIntent()
    data object Decrement : TestIntent()
    data class SetMessage(val message: String) : TestIntent()
    data object TriggerEffect : TestIntent()
    data object ThrowError : TestIntent()
}

// Test Results
sealed class TestResult : Result {
    data class UpdateCount(val delta: Int) : TestResult(), Action
    data class UpdateMessage(val message: String) : TestResult(), Action
    data class ShowToast(val message: String) : TestResult(), Effect
}

// Test Processor
class TestProcessor : Processor<TestIntent, TestState> {
    override fun process(input: TestIntent, state: TestState): Flow<Result> = flow {
        when (input) {
            is TestIntent.Increment -> emit(TestResult.UpdateCount(1))
            is TestIntent.Decrement -> emit(TestResult.UpdateCount(-1))
            is TestIntent.SetMessage -> emit(TestResult.UpdateMessage(input.message))
            is TestIntent.TriggerEffect -> emit(TestResult.ShowToast("Effect triggered"))
            is TestIntent.ThrowError -> throw IllegalStateException("Test error")
        }
    }
}

// Test Reducer
class TestReducer : Reducer<TestResult, TestState> {
    override fun reduce(result: TestResult, state: TestState): TestState {
        return when (result) {
            is TestResult.UpdateCount -> state.copy(count = state.count + result.delta)
            is TestResult.UpdateMessage -> state.copy(message = result.message)
            is TestResult.ShowToast -> state // Effects don't modify state
        }
    }
}

// Test ViewModel implementation
@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModel(
    initialState: TestState = TestState(),
    onError: ((Throwable) -> Unit)? = null
) : ViewModel<TestIntent, TestResult, TestResult, TestState>(
    initialState = initialState,
    processor = TestProcessor(),
    reducer = TestReducer(),
    computationDispatcher = UnconfinedTestDispatcher(),
    mainDispatcher = UnconfinedTestDispatcher(),
    onError = onError
)

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

    @Test
    fun `initial state is set correctly`() = runTest {
        val initialState = TestState(count = 5, message = "initial")
        val viewModel = TestViewModel(initialState)
        
        assertEquals(initialState, viewModel.state.value)
    }

    @Test
    fun `process intent updates state through reducer`() = runTest {
        val viewModel = TestViewModel()
        
        viewModel.process(TestIntent.Increment)
        delay(100) // Give time for processing
        
        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun `multiple intents are processed sequentially`() = runTest {
        val viewModel = TestViewModel()
        
        viewModel.process(TestIntent.Increment)
        viewModel.process(TestIntent.Increment)
        viewModel.process(TestIntent.Decrement)
        delay(100)
        
        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun `state message is updated correctly`() = runTest {
        val viewModel = TestViewModel()
        
        viewModel.process(TestIntent.SetMessage("Hello"))
        delay(100)
        
        assertEquals("Hello", viewModel.state.value.message)
    }

    @Test
    fun `effects are emitted correctly`() = runTest {
        val viewModel = TestViewModel()
        val effects = mutableListOf<TestResult>()
        
        // Collect effects in background
        val job = kotlinx.coroutines.launch {
            viewModel.effects.take(1).toList(effects)
        }
        
        viewModel.process(TestIntent.TriggerEffect)
        delay(100)
        job.cancel()
        
        assertEquals(1, effects.size)
        assertTrue(effects.first() is TestResult.ShowToast)
        assertEquals("Effect triggered", (effects.first() as TestResult.ShowToast).message)
    }

    @Test
    fun `errors are emitted to error flow`() = runTest {
        val viewModel = TestViewModel()
        val errors = mutableListOf<Throwable>()
        
        val job = kotlinx.coroutines.launch {
            viewModel.errors.take(1).toList(errors)
        }
        
        viewModel.process(TestIntent.ThrowError)
        delay(100)
        job.cancel()
        
        assertEquals(1, errors.size)
        assertTrue(errors.first() is IllegalStateException)
        assertEquals("Test error", errors.first().message)
    }

    @Test
    fun `custom error handler is invoked`() = runTest {
        var capturedError: Throwable? = null
        val viewModel = TestViewModel(onError = { capturedError = it })
        
        viewModel.process(TestIntent.ThrowError)
        delay(100)
        
        assertNotNull(capturedError)
        assertTrue(capturedError is IllegalStateException)
    }

    @Test
    fun `state updates maintain immutability`() = runTest {
        val viewModel = TestViewModel(TestState(count = 10))
        val initialState = viewModel.state.value
        
        viewModel.process(TestIntent.Increment)
        delay(100)
        
        // Initial state should not be modified
        assertEquals(10, initialState.count)
        // New state should be updated
        assertEquals(11, viewModel.state.value.count)
    }

    @Test
    fun `reducer is pure function test`() = runTest {
        val reducer = TestReducer()
        val initialState = TestState(count = 5, message = "test")
        
        val result1 = reducer.reduce(TestResult.UpdateCount(2), initialState)
        val result2 = reducer.reduce(TestResult.UpdateCount(2), initialState)
        
        // Same inputs should produce same outputs
        assertEquals(result1, result2)
        assertEquals(7, result1.count)
        assertEquals(7, result2.count)
        
        // Original state should not be modified
        assertEquals(5, initialState.count)
    }
}
