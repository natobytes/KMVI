package io.github.natobytes.kmvi.sample.contract

import io.github.natobytes.kmvi.contract.Action
import io.github.natobytes.kmvi.sample.TodoItem

sealed interface TodoAction : Action {
    data class TodoAdded(val item: TodoItem) : TodoAction
    data class TodoToggled(val id: Int) : TodoAction
    data class TodoRemoved(val id: Int) : TodoAction
}
