package io.github.natobytes.kmvi.contract

/**
 * Represents an action or data change triggered by the ViewModel in response to an [Intent] within the MVI architecture.
 ** A [Request] encapsulates the outcome of processing an [Intent] and serves as input to the [Reducer]
 * to determine how the application's [State] should be updated.
 * It can represent successful data updates, errors, or other events that require a state change.
 */

interface Request
