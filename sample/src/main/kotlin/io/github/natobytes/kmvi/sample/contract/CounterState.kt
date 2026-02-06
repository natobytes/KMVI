package io.github.natobytes.kmvi.sample

data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val history: List<String> = emptyList()
) : State
