package io.github.natobytes.kmvi.contract

/**
 * Represents an intention to change the state of the application within the MVI architecture.
 *
 * Intents are simple, sealed classes that describe user actions, data loading requests,
 * or other events that can trigger a change in the application's state.
 *
 * They are emitted by the View and consumed by the ViewModel, which processes them and
 * updates the Model accordingly.
 */
interface Intent
