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

## Design Decisions

### Library

**Extends AndroidX ViewModel**
KMVI builds on top of `androidx.lifecycle.ViewModel` rather than rolling its own lifecycle owner. This gives lifecycle-aware scoping for free, integrates seamlessly with Compose `viewModel()`, and avoids re-implementing cancellation semantics that AndroidX already handles correctly.

**`Result` is a sealed interface with two subtypes: `Action` and `Effect`**
Both state mutations and side effects flow through the same processor pipeline and get routed by the ViewModel. A single sealed interface lets the processor return either kind without juggling separate streams, while still keeping the two concerns distinct at the type level.

**Sequential intent processing**
Intents are queued and processed one at a time. Each processor call receives the latest committed state, so concurrent mutations cannot interleave. This makes state transitions predictable and eliminates the need for optimistic-locking or merge logic in processors.

**Processor on Default dispatcher, result collection on Main**
Processors run on `Dispatchers.Default` so CPU-bound and async work does not block the UI thread. Results (state updates and effects) are applied back on `Dispatchers.Main` so `StateFlow` emissions are always safe to collect from Compose.

**Effects delivered via a buffered `Channel`**
Unlike `SharedFlow`, a `Channel` guarantees each effect reaches exactly one collector. This prevents navigation events or toasts from triggering twice on recomposition or when multiple collectors briefly overlap.

**`Processor` and `Reducer` as `fun interface`s**
Both are single-abstract-method interfaces, enabling SAM conversion for simple lambdas while keeping a consistent named-class pattern available for complex cases with dependencies.

**`onError` continues processing by default**
Uncaught exceptions from a processor are surfaced via `onError` and then discarded — the ViewModel keeps processing subsequent intents. This prevents a single bad intent from permanently breaking the UI. Override `onError` to log or report the error; handle expected failures inside the processor via try-catch.

### TeslaDrive app

**Android-only module**
TeslaDrive is inherently tied to Android's USB OTG stack. A Kotlin Multiplatform module would add build complexity with no benefit; the app is a plain Android module that depends on `:kmvi` directly.

**minSdk 29 (Android 10)**
Android 10 introduced reliable scoped storage semantics and stable `DocumentFile` behaviour for USB OTG volumes. Going lower would require different permission handling and StorageVolume APIs that complicate the code without adding value.

**Storage Access Framework (`DocumentFile`) for USB access**
SAF via `ACTION_OPEN_DOCUMENT_TREE` gives the user a system-standard drive picker and grants the app a persistent URI permission — no root, no `MANAGE_EXTERNAL_STORAGE`, and no undocumented mount-point paths. `takePersistableUriPermission` means the app can re-access the drive URI across process restarts without re-prompting.

**File overwrite on name conflict**
Before writing a file, the app deletes any existing file with the same name in the target folder. This avoids accumulating stale copies on the drive and keeps the export behaviour deterministic.

**8 KB copy buffer**
A fixed 8 KB buffer balances per-iteration overhead against peak memory use for the large video files Tesla records. Progress is emitted after each chunk so the UI reflects bytes transferred in near-real-time.

**Post-export verification step**
After all files are written, `verifyFiles` walks the drive and counts how many files are actually present. The result is surfaced as a `verifiedFiles` count so partial-write failures are visible to the user even if no exception was thrown during the copy.

**Coroutine cancellation via `coroutineContext.isActive`**
The copy loop checks `isActive` before each chunk write. This makes the "Cancel" button responsive: cancellation propagates to the running flow within one buffer-write cycle rather than waiting for the entire file to finish.

**Firebase Analytics and Crashlytics**
Operational visibility without a custom backend. Analytics tracks usage patterns; Crashlytics surfaces uncaught exceptions with stack traces. Both are thin SDKs with no impact on the core export logic.

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
