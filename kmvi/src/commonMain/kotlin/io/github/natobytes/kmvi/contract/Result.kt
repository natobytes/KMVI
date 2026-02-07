package io.github.natobytes.kmvi.contract

/**
 * Represents the outcome of processing an [Intent] within the MVI architecture, serving as input to the [io.github.natobytes.kmvi.Reducer]
 * to determine how the application's [State] should be updated.
 *
 * A [Result] encapsulates the consequences of handling a user intention or event, and can be categorized into
 * two main types:
 *
 * - [Action]: Represents a change in the application's state that should be immediately reflected in the UI.
 * - [Effect]: Represents a side effect, such as navigation or displaying a dialog, that should be handled
 *   outside the core Model-View-Intent cycle.
 */
public sealed interface Result

/**
 * Represents a change in the application's state that should be immediately reflected in the UI.
 *
 * [Action]s are processed by the [io.github.natobytes.kmvi.Reducer] to update the current [State], leading to a re-rendering of the View.
 */
public interface Action : Result

/**
 * Represents a side effect that should be handled outside the core Model-View-Intent cycle.
 *
 * [Effect]s are typically one-time actions that interact with external systems or trigger UI events
 * that don't directly modify the application's state. Examples include navigation, displaying dialogs,
 * or logging events.
 */
public interface Effect : Result
