package io.github.natobytes.kmvi.sample

import io.github.natobytes.kmvi.Processor
import io.github.natobytes.kmvi.contract.Result
import io.github.natobytes.kmvi.sample.contract.TodoAction
import io.github.natobytes.kmvi.sample.contract.TodoEffect
import io.github.natobytes.kmvi.sample.contract.TodoIntent
import io.github.natobytes.kmvi.sample.contract.TodoState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TodoProcessor : Processor<TodoIntent, TodoState> {
    override fun process(
        input: TodoIntent,
        state: TodoState
    ): Flow<Result> {
        return when (input) {
            is TodoIntent.AddTodo -> flow {
                val item = TodoItem(
                    id = state.nextId,
                    text = input.text,
                    completed = false,
                )
                emit(TodoAction.TodoAdded(item))
            }

            is TodoIntent.ToggleTodo -> flow {
                emit(TodoAction.TodoToggled(input.id))
            }

            is TodoIntent.RemoveTodo -> flow {
                val item = state.todos.find { it.id == input.id } ?: return@flow
                emit(TodoAction.TodoRemoved(input.id))
                emit(TodoEffect.ShowUndo(item))
            }
        }
    }
}
