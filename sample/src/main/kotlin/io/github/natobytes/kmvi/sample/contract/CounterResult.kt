package io.github.natobytes.kmvi.sample

sealed class CounterResult : Result {
    // Actions - modify state
    data class UpdateCount(val newCount: Int) : CounterResult(), Action
    data class SetLoading(val isLoading: Boolean) : CounterResult(), Action
    data class AddToHistory(val message: String) : CounterResult(), Action

    // Effects - side effects
    data class ShowToast(val message: String) : CounterResult(), Effect
    data class Navigate(val destination: String) : CounterResult(), Effect
}
