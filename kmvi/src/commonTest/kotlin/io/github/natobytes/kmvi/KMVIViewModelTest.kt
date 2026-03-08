package io.github.natobytes.kmvi

import io.github.natobytes.kmvi.contract.Action
import io.github.natobytes.kmvi.contract.Effect
import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.Result
import io.github.natobytes.kmvi.contract.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KMVIViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- Test fixtures --

    sealed interface TestIntent : Intent {
        data class UpdateName(val name: String) : TestIntent
        data object IncrementCount : TestIntent
        data object TriggerEffect : TestIntent
        data object TriggerError : TestIntent
        data object EmitMultiple : TestIntent
        data object StartLongRunning : TestIntent
    }

    data class TestState(
        val name: String = "",
        val count: Int = 0,
    ) : State

    sealed interface TestAction : Action {
        data class NameUpdated(val name: String) : TestAction
        data object CountIncremented : TestAction
    }

    sealed interface TestEffect : Effect {
        data class ShowToast(val message: String) :
            TestEffect
    }

    class TestProcessor(
        val longRunningFlow: MutableSharedFlow<Result> = MutableSharedFlow(),
    ) : Processor<TestIntent, TestState> {
        override fun process(
            input: TestIntent,
            state: TestState,
        ): Flow<Result> = when (input) {
            is TestIntent.UpdateName -> flowOf(TestAction.NameUpdated(input.name))
            is TestIntent.IncrementCount -> flowOf(TestAction.CountIncremented)
            is TestIntent.TriggerEffect -> flowOf(TestEffect.ShowToast("Hello"))
            is TestIntent.TriggerError -> flow { throw IllegalStateException("test error") }
            is TestIntent.EmitMultiple -> flow {
                emit(TestAction.NameUpdated("multi"))
                emit(TestAction.CountIncremented)
                emit(TestEffect.ShowToast("done"))
            }

            is TestIntent.StartLongRunning -> longRunningFlow
        }
    }

    class TestReducer : Reducer<TestAction, TestState> {
        override fun reduce(
            action: TestAction,
            state: TestState,
        ): TestState = when (action) {
            is TestAction.NameUpdated -> state.copy(name = action.name)
            is TestAction.CountIncremented -> state.copy(count = state.count + 1)
        }
    }

    class TestKMVIViewModel(
        initialState: TestState = TestState(),
        processor: TestProcessor = TestProcessor(),
        val errors: MutableList<Throwable> = mutableListOf(),
    ) : KMVIViewModel<TestIntent, TestAction, TestEffect, TestState>(
        initialState = initialState,
        processor = processor,
        reducer = TestReducer(),
        computationDispatcher = Dispatchers.Unconfined,
    ) {
        override fun onError(throwable: Throwable) {
            errors.add(throwable)
        }
    }

    // -- Tests --

    @Test
    fun initialStateIsCorrect() = runTest {
        val vm = TestKMVIViewModel(initialState = TestState(name = "initial", count = 42))
        assertEquals(TestState(name = "initial", count = 42), vm.state.value)
    }

    @Test
    fun stateUpdatesOnAction() = runTest {
        val vm = TestKMVIViewModel()

        vm.process(TestIntent.UpdateName("Alice"))
        advanceUntilIdle()

        assertEquals("Alice", vm.state.value.name)
    }

    @Test
    fun effectIsEmitted() = runTest {
        val vm = TestKMVIViewModel()
        val collectedEffects = mutableListOf<TestEffect>()

        val job = launch(testDispatcher) {
            vm.effects.toList(collectedEffects)
        }

        vm.process(TestIntent.TriggerEffect)
        advanceUntilIdle()

        assertEquals(1, collectedEffects.size)
        assertEquals(TestEffect.ShowToast("Hello"), collectedEffects.first())
        job.cancel()
    }

    @Test
    fun sequentialIntentsSeeUpdatedState() = runTest {
        val vm = TestKMVIViewModel()

        vm.process(TestIntent.IncrementCount)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.count)

        vm.process(TestIntent.IncrementCount)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.count)

        vm.process(TestIntent.IncrementCount)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.count)
    }

    @Test
    fun multipleResultsFromSingleIntent() = runTest {
        val vm = TestKMVIViewModel()
        val collectedEffects = mutableListOf<TestEffect>()

        val job = launch(testDispatcher) {
            vm.effects.toList(collectedEffects)
        }

        vm.process(TestIntent.EmitMultiple)
        advanceUntilIdle()

        assertEquals("multi", vm.state.value.name)
        assertEquals(1, vm.state.value.count)
        assertEquals(1, collectedEffects.size)
        assertEquals(TestEffect.ShowToast("done"), collectedEffects.first())
        job.cancel()
    }

    @Test
    fun longRunningFlowDoesNotBlockSubsequentIntents() = runTest {
        val processor = TestProcessor()
        val vm = TestKMVIViewModel(processor = processor)

        // Start a long-running (never-completing) flow
        vm.process(TestIntent.StartLongRunning)
        advanceUntilIdle()

        // Emit an action through the long-running flow to prove it's active
        processor.longRunningFlow.emit(TestAction.NameUpdated("from-long-running"))
        advanceUntilIdle()
        assertEquals("from-long-running", vm.state.value.name)

        // Process a new intent — must NOT be blocked by the long-running flow
        vm.process(TestIntent.IncrementCount)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.count)

        // Long-running flow should still be active
        processor.longRunningFlow.emit(TestAction.NameUpdated("still-active"))
        advanceUntilIdle()
        assertEquals("still-active", vm.state.value.name)
    }

    @Test
    fun processorErrorCallsOnErrorAndDoesNotKillViewModel() = runTest {
        val vm = TestKMVIViewModel()

        vm.process(TestIntent.TriggerError)
        advanceUntilIdle()

        assertEquals(1, vm.errors.size)
        assertTrue(vm.errors.first() is IllegalStateException)

        // ViewModel is still functional after error
        vm.process(TestIntent.UpdateName("recovered"))
        advanceUntilIdle()

        assertEquals("recovered", vm.state.value.name)
    }
}
