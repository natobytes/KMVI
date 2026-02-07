package io.github.natobytes.kmvi.sample.contract

import io.github.natobytes.kmvi.contract.Effect
import io.github.natobytes.kmvi.sample.TodoItem

sealed interface TodoEffect : Effect {
    data class ShowUndo(val removedItem: TodoItem) : TodoEffect
}
