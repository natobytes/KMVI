package io.github.natobytes.kmvi.sample.contract

import io.github.natobytes.kmvi.contract.Intent

sealed interface TodoIntent : Intent {
    data class AddTodo(val text: String) : TodoIntent
    data class ToggleTodo(val id: Int) : TodoIntent
    data class RemoveTodo(val id: Int) : TodoIntent
}
