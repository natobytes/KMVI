package io.github.natobytes.kmvi.test

import io.github.natobytes.kmvi.contract.Action
import io.github.natobytes.kmvi.contract.Effect
import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.Result
import io.github.natobytes.kmvi.contract.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * Test utilities for testing MVI components.
 * These helpers make it easier to test ViewModels, Processors, and Reducers.
 */

/**
 * Collects all results from a Flow for testing purposes.
 * 
 * @return A list of all emitted results
 */
suspend fun Flow<Result>.collectResults(): List<Result> = this.toList()

/**
 * Collects only actions from a Flow for testing purposes.
 * 
 * @return A list of all emitted actions
 */
suspend fun Flow<Result>.collectActions(): List<Action> = 
    this.toList().filterIsInstance<Action>()

/**
 * Collects only effects from a Flow for testing purposes.
 * 
 * @return A list of all emitted effects
 */
suspend fun Flow<Result>.collectEffects(): List<Effect> = 
    this.toList().filterIsInstance<Effect>()

/**
 * A test recorder for tracking state changes and effects in tests.
 * 
 * Usage:
 * ```kotlin
 * val recorder = StateRecorder<MyState>()
 * viewModel.state.collect(recorder::recordState)
 * viewModel.effects.collect(recorder::recordEffect)
 * ```
 */
class StateRecorder<S : State> {
    private val _states = mutableListOf<S>()
    private val _effects = mutableListOf<Effect>()
    
    val states: List<S> get() = _states
    val effects: List<Effect> get() = _effects
    
    fun recordState(state: S) {
        _states.add(state)
    }
    
    fun recordEffect(effect: Effect) {
        _effects.add(effect)
    }
    
    fun clear() {
        _states.clear()
        _effects.clear()
    }
    
    fun getLastState(): S? = _states.lastOrNull()
    fun getStateAt(index: Int): S? = _states.getOrNull(index)
    fun stateCount(): Int = _states.size
    fun effectCount(): Int = _effects.size
}

/**
 * Assert helper for verifying state transitions.
 * 
 * @param expected The expected state values in order
 * @throws AssertionError if states don't match
 */
fun <S : State> StateRecorder<S>.assertStates(vararg expected: S) {
    if (states.size != expected.size) {
        throw AssertionError("Expected ${expected.size} states but got ${states.size}")
    }
    expected.forEachIndexed { index, expectedState ->
        val actualState = states[index]
        if (actualState != expectedState) {
            throw AssertionError("State at index $index: expected $expectedState but got $actualState")
        }
    }
}

/**
 * Assert helper for verifying that a specific number of effects were emitted.
 * 
 * @param count The expected number of effects
 * @throws AssertionError if count doesn't match
 */
fun StateRecorder<*>.assertEffectCount(count: Int) {
    if (effectCount() != count) {
        throw AssertionError("Expected $count effects but got ${effectCount()}")
    }
}
