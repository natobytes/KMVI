package io.github.natobytes.kmvi.sample

import io.github.natobytes.kmvi.ViewModel
import io.github.natobytes.kmvi.contract.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


/**
 * Sample Counter application demonstrating KMVI library usage.
 *
 * This example shows:
 * - How to define State, Intent, and Results
 * - How to implement Processor and Reducer
 * - How to create a ViewModel
 * - How to handle both Actions and Effects
 * - Error handling
 */


class CounterViewModel : ViewModel<CounterIntent, CounterResult, CounterResult, CounterState>(
    initialState = CounterState(),
    processor = CounterProcessor(),
    reducer = CounterReducer(),
    onError = { error ->
        // In a real app, you might log to Crashlytics or similar
        println("Error in CounterViewModel: ${error.message}")
    }
)

/**
 * Example usage in a UI (pseudo-code):
 *
 * ```kotlin
 * @Composable
 * fun CounterScreen(viewModel: CounterViewModel = remember { CounterViewModel() }) {
 *     val state by viewModel.state.collectAsState()
 *
 *     // Collect effects
 *     LaunchedEffect(Unit) {
 *         viewModel.effects.collect { effect ->
 *             when (effect) {
 *                 is CounterResult.ShowToast -> showToast(effect.message)
 *                 is CounterResult.Navigate -> navigate(effect.destination)
 *             }
 *         }
 *     }
 *
 *     // Collect errors
 *     LaunchedEffect(Unit) {
 *         viewModel.errors.collect { error ->
 *             showErrorDialog(error.message)
 *         }
 *     }
 *
 *     Column(modifier = Modifier.padding(16.dp)) {
 *         Text("Count: ${state.count}", style = MaterialTheme.typography.h3)
 *
 *         if (state.isLoading) {
 *             CircularProgressIndicator()
 *         }
 *
 *         Row {
 *             Button(onClick = { viewModel.process(CounterIntent.Decrement) }) {
 *                 Text("-")
 *             }
 *             Button(onClick = { viewModel.process(CounterIntent.Increment) }) {
 *                 Text("+")
 *             }
 *         }
 *
 *         Button(onClick = { viewModel.process(CounterIntent.IncrementBy(5)) }) {
 *             Text("+5")
 *         }
 *
 *         Button(onClick = { viewModel.process(CounterIntent.LoadAsync) }) {
 *             Text("Load Random")
 *         }
 *
 *         Button(onClick = { viewModel.process(CounterIntent.Reset) }) {
 *             Text("Reset")
 *         }
 *
 *         // Show history
 *         LazyColumn {
 *             items(state.history) { item ->
 *                 Text(item)
 *             }
 *         }
 *     }
 * }
 * ```
 */
