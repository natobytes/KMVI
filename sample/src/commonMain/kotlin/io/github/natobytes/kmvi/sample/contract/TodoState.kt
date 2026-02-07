package io.github.natobytes.kmvi.sample.contract

import io.github.natobytes.kmvi.contract.State
import io.github.natobytes.kmvi.sample.TodoItem

data class TodoState(
    val todos: List<TodoItem> = emptyList(),
    val nextId: Int = 1,
) : State
