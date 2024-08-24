package io.github.natobytes.kmvi.contract

/**
 * Represents the current state of a view or a specific part of the application within the MVI architecture.
 *
 * A [State] object encapsulates all the data necessary to render the UI at any given time. It is an immutable
 * snapshot of the application's relevant data and UI properties, ensuring predictable rendering and simplifying
 * state management.
 */
interface State
