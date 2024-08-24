package io.github.natobytes.kmvi.contract

/**
 * Represents a one-time action or operation that needs to be executed outside the core Model-View-Intent cycle within the MVIarchitecture.
 *
 * A [SideEffect] is a specialized type of [Request] that typically involves interacting with external systems or
 * triggering actions that don't directly update the application's [State], such as navigation, displaying dialogs,
 * or logging events.
 *
 * Unlike regular [Request]s that are processed by the [Reducer] to update the [State], [SideEffect]s are
 * immediately presented to the View for handling.
 */
interface SideEffect : Request
