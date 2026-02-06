package io.github.natobytes.kmvi.sample

sealed class CounterIntent : Intent {
    data object Increment : CounterIntent()
    data object Decrement : CounterIntent()
    data object Reset : CounterIntent()
    data class IncrementBy(val amount: Int) : CounterIntent()
    data object LoadAsync : CounterIntent()
}
