![Maven Central Version](https://img.shields.io/maven-central/v/io.github.natobytes/kmvi)


# KMVI - Kotlin Multiplatform MVI Architecture Library

**A robust and flexible framework for building modern, maintainable, and testable applications across multiple platforms using the Model-View-Intent (MVI) pattern.**

## Introduction

This library provides a set of core components and utilities to streamline the implementation of MVI architecture in your Kotlin Multiplatform projects. It leverages Kotlin's powerful features and coroutines to create a reactive and efficient development experience.

## Key Features

* **Platform Agnostic:** Build applications for Android, iOS, Desktop, and Web with a shared codebase.
* **Type-Safe State Management:** Leverage Kotlin's type system to ensure predictable and reliable state updates.
* **Unidirectional Data Flow:** Enforce a clear separation of concerns and predictable state changes with the MVI pattern.
* **Coroutine-Based Asynchronicity:** Handle asynchronous operations seamlessly using Kotlin coroutines.
* **Comprehensive Error Handling:** Built-in error flow for robust error management.
* **Testability:** Write comprehensive unit tests for your ViewModels, Reducers, and other components with included test helpers.
* **Extensibility:** Middleware support for logging, analytics, and custom behaviors.
* **Well Documented:** Comprehensive KDoc documentation for all public APIs.

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
implementation("io.github.natobytes:kmvi:{version}")
```

For testing utilities:
```kotlin
testImplementation("io.github.natobytes:kmvi:{version}")
```

## Core Concepts

* **Intent:** Represents a user intention or event that triggers a state change.
* **State:** An immutable snapshot of the UI data at any given time.
* **Action:** Represents a change in the application's state that should be immediately reflected in the UI.
* **Result:** Encapsulates the outcome of processing an Intent, which can be either an Action or an Effect.
* **Effect:** Represents a side effect, such as navigation or displaying a dialog, that should be handled outside the core MVI cycle.
* **Reducer:** A pure function that takes the current state and an Action and returns a new, immutable state.
* **Processor:** Processes Intents and transforms them into a stream of Results.
* **ViewModel:** Manages the application's state, handles Intents, and emits new states.

## Quick Start

### 1. Define Your State

```kotlin
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
) : State
```

### 2. Define Your Intents

```kotlin
sealed class CounterIntent : Intent {
    data object Increment : CounterIntent()
    data object Decrement : CounterIntent()
    data object Reset : CounterIntent()
}
```

### 3. Define Your Results

```kotlin
sealed class CounterResult : Result {
    data class UpdateCount(val newCount: Int) : CounterResult(), Action
    data class ShowMessage(val message: String) : CounterResult(), Effect
}
```

### 4. Implement the Processor

```kotlin
class CounterProcessor : Processor<CounterIntent, CounterState> {
    override fun process(input: CounterIntent, state: CounterState): Flow<Result> = flow {
        when (input) {
            is CounterIntent.Increment -> {
                emit(CounterResult.UpdateCount(state.count + 1))
                if ((state.count + 1) % 10 == 0) {
                    emit(CounterResult.ShowMessage("You reached ${state.count + 1}!"))
                }
            }
            is CounterIntent.Decrement -> {
                emit(CounterResult.UpdateCount(state.count - 1))
            }
            is CounterIntent.Reset -> {
                emit(CounterResult.UpdateCount(0))
                emit(CounterResult.ShowMessage("Counter reset"))
            }
        }
    }
}
```

### 5. Implement the Reducer

```kotlin
class CounterReducer : Reducer<CounterResult, CounterState> {
    override fun reduce(result: CounterResult, state: CounterState): CounterState {
        return when (result) {
            is CounterResult.UpdateCount -> state.copy(count = result.newCount)
            is CounterResult.ShowMessage -> state // Effects don't modify state
        }
    }
}
```

### 6. Create Your ViewModel

```kotlin
class CounterViewModel : ViewModel<CounterIntent, CounterResult, CounterResult, CounterState>(
    initialState = CounterState(),
    processor = CounterProcessor(),
    reducer = CounterReducer(),
    onError = { error ->
        // Handle errors (e.g., log to analytics)
        println("Error: ${error.message}")
    }
)
```

### 7. Use in Your UI (Compose Example)

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val state by viewModel.state.collectAsState()
    
    // Collect effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CounterResult.ShowMessage -> {
                    // Show snackbar or toast
                }
            }
        }
    }
    
    // Collect errors
    LaunchedEffect(Unit) {
        viewModel.errors.collect { error ->
            // Show error dialog or snackbar
        }
    }
    
    Column {
        Text("Count: ${state.count}")
        Button(onClick = { viewModel.process(CounterIntent.Increment) }) {
            Text("Increment")
        }
        Button(onClick = { viewModel.process(CounterIntent.Decrement) }) {
            Text("Decrement")
        }
        Button(onClick = { viewModel.process(CounterIntent.Reset) }) {
            Text("Reset")
        }
    }
}
```

## Advanced Features

### Middleware

Add cross-cutting concerns like logging and analytics:

```kotlin
val loggingMiddleware = LoggingMiddleware<MyIntent, MyState>(
    tag = "MyApp",
    enabled = BuildConfig.DEBUG
)

val analyticsMiddleware = AnalyticsMiddleware<MyIntent, MyState> { event, properties ->
    // Send to your analytics service
    Analytics.track(event, properties)
}
```

### Error Handling

The library provides comprehensive error handling:

```kotlin
class MyViewModel : ViewModel<MyIntent, MyResult, MyEffect, MyState>(
    // ... other parameters
    onError = { error ->
        when (error) {
            is NetworkException -> // Handle network errors
            is ValidationException -> // Handle validation errors
            else -> // Handle other errors
        }
    }
)

// In your UI, collect errors
LaunchedEffect(Unit) {
    viewModel.errors.collect { error ->
        // Show error UI
    }
}
```

### Testing

The library includes test helpers for easy testing:

```kotlin
class CounterViewModelTest {
    @Test
    fun `test increment increases count`() = runTest {
        val viewModel = CounterViewModel()
        
        viewModel.process(CounterIntent.Increment)
        delay(100)
        
        assertEquals(1, viewModel.state.value.count)
    }
    
    @Test
    fun `test effects are emitted`() = runTest {
        val viewModel = CounterViewModel()
        val effects = mutableListOf<CounterResult>()
        
        val job = launch {
            viewModel.effects.take(1).toList(effects)
        }
        
        viewModel.process(CounterIntent.Reset)
        delay(100)
        job.cancel()
        
        assertTrue(effects.first() is CounterResult.ShowMessage)
    }
}
```

Using test helpers:

```kotlin
@Test
fun `test state transitions`() = runTest {
    val processor = CounterProcessor()
    val state = CounterState(count = 0)
    
    val results = processor.process(CounterIntent.Increment, state).collectResults()
    
    assertEquals(1, results.size)
    assertTrue(results.first() is CounterResult.UpdateCount)
}
```

## Best Practices

1. **Keep Reducers Pure:** Reducers should be pure functions without side effects.
2. **Handle Errors Gracefully:** Use the error flow to handle errors in the UI.
3. **Use Effects for Side Effects:** Navigation, dialogs, and other side effects should use Effects, not Actions.
4. **Test Thoroughly:** Use the provided test helpers to test your Processors and Reducers.
5. **Use Middleware for Cross-Cutting Concerns:** Logging, analytics, and monitoring should use middleware.
6. **Keep State Immutable:** Always create new state instances in your reducer.

## Architecture Diagram

```
┌─────────┐
│  View   │
└────┬────┘
     │ User Action
     ▼
┌─────────┐     ┌───────────┐     ┌─────────┐
│ Intent  │────▶│ Processor │────▶│ Result  │
└─────────┘     └───────────┘     └────┬────┘
                                       │
                          ┌────────────┼────────────┐
                          ▼                         ▼
                     ┌─────────┐              ┌─────────┐
                     │ Action  │              │ Effect  │
                     └────┬────┘              └────┬────┘
                          │                        │
                          ▼                        │
                     ┌─────────┐                   │
                     │ Reducer │                   │
                     └────┬────┘                   │
                          │                        │
                          ▼                        │
                     ┌─────────┐                   │
                     │  State  │                   │
                     └────┬────┘                   │
                          │                        │
                          └────────────┬───────────┘
                                       ▼
                                  ┌─────────┐
                                  │  View   │
                                  └─────────┘
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

[Apache License 2.0](LICENSE)
