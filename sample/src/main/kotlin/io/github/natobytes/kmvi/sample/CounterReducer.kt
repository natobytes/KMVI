package io.github.natobytes.kmvi.sample

class CounterReducer : Reducer<CounterResult, CounterState> {

    override fun reduce(result: CounterResult, state: CounterState): CounterState {
        return when (result) {
            is CounterResult.UpdateCount ->
                state.copy(count = result.newCount)

            is CounterResult.SetLoading ->
                state.copy(isLoading = result.isLoading)

            is CounterResult.AddToHistory ->
                state.copy(history = state.history + result.message)

            // Effects don't modify state
            is CounterResult.ShowToast -> state
            is CounterResult.Navigate -> state
        }
    }
}
