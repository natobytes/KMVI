package io.github.natobytes.kmvi.sample

import io.github.natobytes.kmvi.Reducer
import io.github.natobytes.kmvi.sample.contract.TodoAction
import io.github.natobytes.kmvi.sample.contract.TodoState

class TodoReducer : Reducer<TodoAction, TodoState> {
    override fun reduce(
        action: TodoAction,
        state: TodoState
    ): TodoState =
        when (action) {
            is TodoAction.TodoAdded -> state.copy(
                todos = state.todos + action.item,
                nextId = state.nextId + 1,
            )

            is TodoAction.TodoToggled -> state.copy(
                todos = state.todos.map { todo ->
                    if (todo.id == action.id) todo.copy(completed = !todo.completed) else todo
                },
            )

            is TodoAction.TodoRemoved -> state.copy(
                todos = state.todos.filter { it.id != action.id },
            )
        }
}
