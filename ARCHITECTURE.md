# KMVI Architecture Documentation

## Overview

KMVI (Kotlin Multiplatform MVI) is a library that implements the Model-View-Intent (MVI) architecture pattern for Kotlin Multiplatform projects. It provides a structured approach to managing state and handling user interactions in a unidirectional data flow.

## Architecture Pattern

### MVI (Model-View-Intent)

MVI is a reactive architecture pattern that enforces unidirectional data flow:

```
User Action -> Intent -> Processor -> Result -> ViewModel -> Action -> Reducer -> State
                                                           -> Effect -> Flow<Effect>
```

### Key Principles

1. **Single Source of Truth**: The state is the single source of truth for the UI
2. **Unidirectional Data Flow**: Data flows in one direction through the system
3. **Immutability**: State objects are immutable and never modified in place
4. **Separation of Concerns**: Business logic, state management, and UI are clearly separated
5. **Predictability**: Same inputs always produce the same outputs
6. **Testability**: Each component can be tested in isolation

## Core Components

### 1. State

**Purpose**: Represents the current state of the UI

**Characteristics**:
- Immutable data class
- Contains all data needed to render the UI
- No business logic

**Example**:
```kotlin
data class LoginState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : State
```

**Best Practices**:
- Keep state flat and simple
- Use data classes for automatic equals/hashCode
- Default values for initialization
- Avoid nullable fields when possible

### 2. Intent

**Purpose**: Represents user actions or events that should trigger state changes

**Characteristics**:
- Sealed interface hierarchy for exhaustive when statements
- Contains only data necessary for the action
- No business logic
- Immutable

**Example**:
```kotlin
sealed interface LoginIntent : Intent {
    data class UsernameChanged(val username: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data object LoginClicked : LoginIntent
    data object ForgotPasswordClicked : LoginIntent
}
```

**Best Practices**:
- Use sealed interfaces for type safety
- Keep intents simple and focused
- One intent per user action
- Use data objects for intents without parameters

### 3. Result (Action & Effect)

**Purpose**: Represents the outcome of processing an Intent

**Types**:
- **Action**: Modifies state (processed by Reducer)
- **Effect**: One-shot side effect without state modification

The library provides `Result` as a sealed interface with two subtypes: `Action` and `Effect`. The recommended pattern is to define separate sealed interfaces for your Actions and Effects:

**Example**:
```kotlin
sealed interface LoginAction : Action {
    data class UpdateUsername(val username: String) : LoginAction
    data class UpdatePassword(val password: String) : LoginAction
    data class SetLoading(val isLoading: Boolean) : LoginAction
    data class SetError(val message: String?) : LoginAction
}

sealed interface LoginEffect : Effect {
    data class NavigateToHome(val userId: String) : LoginEffect
    data class ShowToast(val message: String) : LoginEffect
}
```

**Best Practices**:
- Use separate sealed interfaces for Actions and Effects
- Actions should be self-contained
- Effects represent one-shot events (navigation, toasts, etc.)

### 4. Processor

**Purpose**: Processes Intents and transforms them into a stream of Results

**Characteristics**:
- Contains business logic
- Handles async operations
- Emits `Flow<Result>`
- Can emit multiple Results (both Actions and Effects) for one Intent
- Has access to current state
- Defined as a `fun interface` (supports SAM conversion)

**Example**:
```kotlin
class LoginProcessor(
    private val authService: AuthService
) : Processor<LoginIntent, LoginState> {

    override fun process(input: LoginIntent, state: LoginState): Flow<Result> = flow {
        when (input) {
            is LoginIntent.UsernameChanged -> {
                emit(LoginAction.UpdateUsername(input.username))
            }

            is LoginIntent.PasswordChanged -> {
                emit(LoginAction.UpdatePassword(input.password))
            }

            is LoginIntent.LoginClicked -> {
                emit(LoginAction.SetLoading(true))
                emit(LoginAction.SetError(null))

                try {
                    val response = authService.login(state.username, state.password)
                    emit(LoginAction.SetLoading(false))
                    emit(LoginEffect.NavigateToHome(response.userId))
                    emit(LoginEffect.ShowToast("Login successful"))
                } catch (e: Exception) {
                    emit(LoginAction.SetLoading(false))
                    emit(LoginAction.SetError(e.message))
                    emit(LoginEffect.ShowToast("Login failed"))
                }
            }

            is LoginIntent.ForgotPasswordClicked -> {
                emit(LoginEffect.ShowToast("Password reset email sent"))
            }
        }
    }
}
```

**Best Practices**:
- One Processor per feature/screen
- Inject dependencies via constructor
- Use try-catch for error handling
- Emit loading states before async operations
- Can emit multiple Results in sequence
- Keep pure logic in separate functions for testability

### 5. Reducer

**Purpose**: Pure function that updates state based on Actions

**Characteristics**:
- Pure function (no side effects)
- Synchronous
- Creates new state instances
- Only receives Actions (Effects are routed separately by the ViewModel)
- Deterministic
- Defined as a `fun interface` (supports SAM conversion)

**Example**:
```kotlin
class LoginReducer : Reducer<LoginAction, LoginState> {

    override fun reduce(action: LoginAction, state: LoginState): LoginState {
        return when (action) {
            is LoginAction.UpdateUsername ->
                state.copy(username = action.username)

            is LoginAction.UpdatePassword ->
                state.copy(password = action.password)

            is LoginAction.SetLoading ->
                state.copy(isLoading = action.isLoading)

            is LoginAction.SetError ->
                state.copy(errorMessage = action.message)
        }
    }
}
```

**Best Practices**:
- Always return a new state instance
- Never modify the input state
- Keep reducers simple and fast
- No async operations
- No side effects
- Easy to unit test

### 6. KMVIViewModel

**Purpose**: Orchestrates the MVI cycle

**Responsibilities**:
- Manages state lifecycle
- Processes intents through Processor (on computation dispatcher)
- Routes Actions through Reducer to update state
- Routes Effects to a buffered channel for UI consumption
- Handles errors via overridable `onError`

**Example**:
```kotlin
class LoginViewModel(
    authService: AuthService
) : KMVIViewModel<LoginIntent, LoginAction, LoginEffect, LoginState>(
    initialState = LoginState(),
    processor = LoginProcessor(authService),
    reducer = LoginReducer(),
)
```

**Exposed Members**:
- `state: StateFlow<S>` — Current state, observable
- `effects: Flow<E>` — One-shot effects delivered via a buffered channel, each event reaches exactly one collector
- `process(intent: I)` — Entry point for submitting intents

**Error Handling**:
Override `onError` to handle exceptions thrown during intent processing. By default, errors are printed and the ViewModel continues processing subsequent intents.

```kotlin
class LoginViewModel(
    authService: AuthService
) : KMVIViewModel<LoginIntent, LoginAction, LoginEffect, LoginState>(
    initialState = LoginState(),
    processor = LoginProcessor(authService),
    reducer = LoginReducer(),
) {
    override fun onError(throwable: Throwable) {
        // Log to analytics or crash reporting
    }
}
```

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       KMVIViewModel                         │
│                                                             │
│  ┌────────┐     ┌───────────┐     ┌─────────┐             │
│  │        │     │           │     │         │             │
│  │ State  │◄────┤  Reducer  │◄────┤ Action  │             │
│  │        │     │           │     │         │             │
│  └───┬────┘     └───────────┘     └────▲────┘             │
│      │                                  │                   │
│      │          ┌───────────┐     ┌────┴────┐              │
│      │          │           │     │         │              │
│      │          │ Processor │────▶│ Result  │              │
│      │          │           │     │         │              │
│      │          └─────▲─────┘     └────┬────┘              │
│      │                │                 │                   │
│      │          ┌─────┴─────┐     ┌────▼────┐              │
│      │          │           │     │         │              │
│      │          │  Intent   │     │ Effect  │              │
│      │          │           │     │         │              │
│      │          └─────▲─────┘     └────┬────┘              │
│      │                │                 │                   │
└──────┼────────────────┼─────────────────┼───────────────────┘
       │                │                 │
       ▼                │                 ▼
┌─────────┐      ┌──────┴──────┐   ┌───────────┐
│         │      │             │   │           │
│   View  │─────▶│  User       │   │   Side    │
│         │      │  Action     │   │   Effects │
│         │      │             │   │           │
└─────────┘      └─────────────┘   └───────────┘
```

## Error Handling

KMVI provides two layers of error handling:

1. **Try-Catch in Processor**: Catch exceptions in your processor and convert them to Actions or Effects
2. **`onError` in ViewModel**: Override the `protected open fun onError(throwable: Throwable)` to handle uncaught exceptions from processors. By default, errors are logged and the intent subscription continues.

**Example**:
```kotlin
// In Processor — convert expected errors to Results
override fun process(input: MyIntent, state: MyState): Flow<Result> = flow {
    try {
        val data = repository.fetchData()
        emit(MyAction.Success(data))
    } catch (e: NetworkException) {
        emit(MyAction.NetworkError(e.message))
    } catch (e: Exception) {
        throw e // Let ViewModel onError handle unexpected errors
    }
}
```

## Testing Strategy

### Unit Testing Reducers

```kotlin
@Test
fun `reducer should update state correctly`() {
    val reducer = MyReducer()
    val state = MyState(count = 0)
    val action = MyAction.Increment

    val newState = reducer.reduce(action, state)

    assertEquals(1, newState.count)
    assertEquals(0, state.count) // Original unchanged
}
```

### Unit Testing Processors

```kotlin
@Test
fun `processor should emit correct results`() = runTest {
    val processor = MyProcessor(mockRepository)
    val intent = MyIntent.Load
    val state = MyState()

    val results = processor.process(intent, state).toList()

    assertTrue(results[0] is MyAction.Loading)
    assertTrue(results[1] is MyAction.Success)
}
```

### Integration Testing ViewModels

```kotlin
@Test
fun `viewModel should update state on intent`() = runTest {
    val viewModel = MyViewModel()

    viewModel.process(MyIntent.Increment)
    advanceUntilIdle()

    assertEquals(1, viewModel.state.value.count)
}
```

## Best Practices Summary

1. **Keep State Simple**: Flat structure, immutable
2. **Intents Are Actions**: One intent = one user action
3. **Processors Have Logic**: All business logic goes here
4. **Reducers Are Pure**: No side effects, fast, deterministic
5. **Effects for Side Effects**: Navigation, dialogs, etc.
6. **Test Everything**: Processors, Reducers, ViewModels
7. **Handle Errors Gracefully**: Try-catch in processors, `onError` in ViewModel
8. **State Immutability**: Always create new instances
9. **Dispatcher Management**: Processor runs on Default, results collected on Main

## Performance Considerations

1. **State Updates**: Keep state objects small and focused
2. **Flow Operators**: Use appropriate operators (flowOn, buffer)
3. **Dispatcher Selection**: Processor runs on Default dispatcher, UI collection on Main
4. **Reducer Performance**: Keep reducers fast and simple
5. **State Comparison**: Use data classes for efficient comparison
6. **Flow Collection**: Collect in lifecycle-aware scope
7. **Memory Leaks**: Use viewModelScope for lifecycle management

## Migration Guide

### From Traditional MVVM

1. Replace LiveData with StateFlow
2. Convert commands to Effects
3. Move business logic to Processor
4. Create Reducer for state updates
5. Define Intent hierarchy

### From Other MVI Libraries

1. Map existing states to KMVI State
2. Convert intents to KMVI Intent
3. Merge ActionProcessor -> Processor
4. Convert reducer -> KMVI Reducer
5. Update ViewModels to extend KMVIViewModel

## Common Patterns

### Loading States

```kotlin
data class MyState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) : State
```

### Pagination

```kotlin
data class MyState(
    val items: List<Item> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
) : State
```

### Form Validation

```kotlin
data class FormState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isValid: Boolean = false
) : State
```

## Resources

- [README.md](README.md) - Getting started guide
- [Sample Code](sample/) - Todo List example
- [Tests](kmvi/src/commonTest/) - Test examples
