package io.github.natobytes.kmvi.sample

import io.github.natobytes.kmvi.KMVIViewModel
import io.github.natobytes.kmvi.sample.contract.TodoAction
import io.github.natobytes.kmvi.sample.contract.TodoEffect
import io.github.natobytes.kmvi.sample.contract.TodoIntent
import io.github.natobytes.kmvi.sample.contract.TodoState

class TodoViewModel(
    initialState: TodoState = TodoState(),
    processor: TodoProcessor = TodoProcessor(),
    reducer: TodoReducer = TodoReducer()
) : KMVIViewModel<TodoIntent, TodoAction, TodoEffect, TodoState>(
    initialState = initialState,
    processor = processor,
    reducer = reducer,
)
