![Maven Central Version](https://img.shields.io/maven-central/v/io.github.natobytes/kmvi)

# KMVI

A lightweight Kotlin Multiplatform MVI (Model-View-Intent) library built on top of AndroidX ViewModel and Coroutines.

## Setup

Add the dependency to your `build.gradle.kts`:

```toml
# Version catalog (gradle/libs.versions.toml)
[versions]
kmvi = "<version>"

[libraries]
kmvi = { module = "io.github.natobytes:kmvi", version.ref = "kmvi" }
```

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kmvi)
        }
    }
}
```

Or directly:

```kotlin
implementation("io.github.natobytes:kmvi:<version>")
```

### Platform targets

KMVI supports **JVM**, **Android**, and **iOS** (x64, ARM64, Simulator ARM64 via XCFramework).

### Sample

See the [`sample/`](sample/) module for a complete Todo List example demonstrating collections, multiple intents, and effects (undo on remove).

## Architecture

KMVI implements a unidirectional data flow:

```
Intent -> Processor -> Flow<Result> -> ViewModel -> Action -> Reducer -> State
                                                 -> Effect -> Flow<Effect>
```

- **Intents** are processed sequentially, so each processor sees the latest state
- **Actions** update state through a pure reducer function
- **Effects** are one-shot events (navigation, toasts, etc.) delivered via a buffered channel

## Usage

### 1. Define your contracts

```kotlin
// What the user can do
sealed interface CounterIntent : Intent {
    data object Increment : CounterIntent
    data object Decrement : CounterIntent
    data object Reset : CounterIntent
}

// Your screen state
data class CounterState(
    val count: Int = 0,
) : State

// State mutations
sealed interface CounterAction : Action {
    data class UpdateCount(val count: Int) : CounterAction
}

// One-shot side effects
sealed interface CounterEffect : Effect {
    data object CounterReset : CounterEffect
}
```

### 2. Implement the Processor

The `Processor` maps intents to a stream of results (actions and/or effects). It receives the current state so you can make decisions based on it.

```kotlin
class CounterProcessor : Processor<CounterIntent, CounterState> {
    override fun process(
        input: CounterIntent,
        state: CounterState,
    ): Flow<Result> = when (input) {
        is CounterIntent.Increment -> flowOf(CounterAction.UpdateCount(state.count + 1))
        is CounterIntent.Decrement -> flowOf(CounterAction.UpdateCount(state.count - 1))
        is CounterIntent.Reset -> flow {
            emit(CounterAction.UpdateCount(0))
            emit(CounterEffect.CounterReset)
        }
    }
}
```

Both `Processor` and `Reducer` are `fun interface`s, so you can use SAM conversion for simple cases:

```kotlin
val processor = Processor<CounterIntent, CounterState> { input, state ->
    when (input) {
        is CounterIntent.Increment -> flowOf(CounterAction.UpdateCount(state.count + 1))
        is CounterIntent.Decrement -> flowOf(CounterAction.UpdateCount(state.count - 1))
        is CounterIntent.Reset -> flow {
            emit(CounterAction.UpdateCount(0))
            emit(CounterEffect.CounterReset)
        }
    }
}
```

### 3. Implement the Reducer

The `Reducer` is a pure function that applies an action to the current state and returns a new state.

```kotlin
class CounterReducer : Reducer<CounterAction, CounterState> {
    override fun reduce(
        action: CounterAction,
        state: CounterState,
    ): CounterState = when (action) {
        is CounterAction.UpdateCount -> state.copy(count = action.count)
    }
}
```

### 4. Create your ViewModel

Extend `KMVIViewModel` with your contract types and wire everything together.

```kotlin
class CounterViewModel : KMVIViewModel<CounterIntent, CounterAction, CounterEffect, CounterState>(
    initialState = CounterState(),
    processor = CounterProcessor(),
    reducer = CounterReducer(),
)
```

### 5. Connect to the UI

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CounterEffect.CounterReset -> { /* show snackbar, etc. */ }
            }
        }
    }

    Column {
        Text("Count: ${state.count}")
        Button(onClick = { viewModel.process(CounterIntent.Increment) }) {
            Text("+")
        }
        Button(onClick = { viewModel.process(CounterIntent.Decrement) }) {
            Text("-")
        }
        Button(onClick = { viewModel.process(CounterIntent.Reset) }) {
            Text("Reset")
        }
    }
}
```

## Error handling

Override `onError` in your ViewModel to handle exceptions thrown by processors. By default, errors are logged and the ViewModel continues processing subsequent intents.

```kotlin
class CounterViewModel : KMVIViewModel<CounterIntent, CounterAction, CounterEffect, CounterState>(
    initialState = CounterState(),
    processor = CounterProcessor(),
    reducer = CounterReducer(),
) {
    override fun onError(throwable: Throwable) {
        // Log, report to crash analytics, etc.
    }
}
```

## API reference

| Type | Description |
|---|---|
| `Intent` | Marker interface for user actions / events |
| `State` | Marker interface for immutable UI state |
| `Result` | Sealed interface — parent of `Action` and `Effect` |
| `Action` | A state mutation, processed by the `Reducer` |
| `Effect` | A one-shot side effect (navigation, toasts, etc.) |
| `Processor<I, S>` | Transforms an `Intent` + current `State` into `Flow<Result>` |
| `Reducer<A, S>` | Pure function: `(Action, State) -> State` |
| `KMVIViewModel<I, A, E, S>` | Abstract ViewModel wiring everything together |

### KMVIViewModel

| Member | Type | Description |
|---|---|---|
| `state` | `StateFlow<S>` | Current state, observable |
| `effects` | `Flow<E>` | One-shot effects, each delivered to exactly one collector |
| `process(intent)` | `fun` | Entry point — submit an intent for processing |
| `onError(throwable)` | `protected open fun` | Override to handle processor errors |

## License

```
Copyright 2026 natobytes

Licensed under the Apache License, Version 2.0
```
