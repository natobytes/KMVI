package io.github.natobytes.kmvi.sample

class CounterProcessor : Processor<CounterIntent, CounterState> {

    override fun process(input: CounterIntent, state: CounterState): Flow<Result> = flow {
        when (input) {
            is CounterIntent.Increment -> {
                val newCount = state.count + 1
                emit(CounterResult.UpdateCount(newCount))
                emit(CounterResult.AddToHistory("Incremented to $newCount"))

                // Show celebration for milestones
                if (newCount % 10 == 0) {
                    emit(CounterResult.ShowToast("Milestone reached: $newCount! 🎉"))
                }
            }

            is CounterIntent.Decrement -> {
                val newCount = state.count - 1
                emit(CounterResult.UpdateCount(newCount))
                emit(CounterResult.AddToHistory("Decremented to $newCount"))

                // Warn about negative numbers
                if (newCount < 0) {
                    emit(CounterResult.ShowToast("Counter is now negative!"))
                }
            }

            is CounterIntent.Reset -> {
                emit(CounterResult.UpdateCount(0))
                emit(CounterResult.AddToHistory("Reset to 0"))
                emit(CounterResult.ShowToast("Counter reset"))
            }

            is CounterIntent.IncrementBy -> {
                val newCount = state.count + input.amount
                emit(CounterResult.UpdateCount(newCount))
                emit(CounterResult.AddToHistory("Incremented by ${input.amount} to $newCount"))
            }

            is CounterIntent.LoadAsync -> {
                // Demonstrate async operation
                emit(CounterResult.SetLoading(true))
                emit(CounterResult.ShowToast("Loading..."))

                // Simulate network delay
                delay(2000)

                val randomValue = (1..100).random()
                emit(CounterResult.UpdateCount(randomValue))
                emit(CounterResult.SetLoading(false))
                emit(CounterResult.AddToHistory("Loaded random value: $randomValue"))
                emit(CounterResult.ShowToast("Loaded: $randomValue"))
            }
        }
    }
}
