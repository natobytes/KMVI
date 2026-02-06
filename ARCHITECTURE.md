# KMVI Architecture Documentation

## Overview

KMVI (Kotlin Multiplatform MVI) is a library that implements the Model-View-Intent (MVI) architecture pattern for Kotlin Multiplatform projects. It provides a structured approach to managing state and handling user interactions in a unidirectional data flow.

## Architecture Pattern

### MVI (Model-View-Intent)

MVI is a reactive architecture pattern that enforces unidirectional data flow:

```
User Action → Intent → Processor → Result → Reducer → State → View
                                      ↓
                                   Effect → Side Effects Handler
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
- Should be serializable for state persistence
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
- Sealed class hierarchy for exhaustive when statements
- Contains only data necessary for the action
- No business logic
- Immutable

**Example**:
```kotlin
sealed class LoginIntent : Intent {
    data class UsernameChanged(val username: String) : LoginIntent()
    data class PasswordChanged(val password: String) : LoginIntent()
    data object LoginClicked : LoginIntent()
    data object ForgotPasswordClicked : LoginIntent()
}
```

**Best Practices**:
- Use sealed classes for type safety
- Keep intents simple and focused
- One intent per user action
- Use data objects for intents without parameters

### 3. Result

**Purpose**: Represents the outcome of processing an Intent

**Types**:
- **Action**: Modifies state (processed by Reducer)
- **Effect**: Side effect without state modification

**Example**:
```kotlin
sealed class LoginResult : Result {
    // Actions
    data class UpdateUsername(val username: String) : LoginResult(), Action
    data class UpdatePassword(val password: String) : LoginResult(), Action
    data class SetLoading(val isLoading: Boolean) : LoginResult(), Action
    data class SetError(val message: String?) : LoginResult(), Action
    
    // Effects
    data class NavigateToHome(val userId: String) : LoginResult(), Effect
    data class ShowToast(val message: String) : LoginResult(), Effect
}
```

**Best Practices**:
- Separate Actions from Effects clearly
- Actions should be self-contained
- Effects should not contain state data

### 4. Processor

**Purpose**: Processes Intents and transforms them into Results

**Characteristics**:
- Contains business logic
- Handles async operations
- Emits Flow<Result>
- Can emit multiple Results for one Intent
- Access to current state

**Example**:
```kotlin
class LoginProcessor(
    private val authService: AuthService
) : Processor<LoginIntent, LoginState> {
    
    override fun process(input: LoginIntent, state: LoginState): Flow<Result> = flow {
        when (input) {
            is LoginIntent.UsernameChanged -> {
                emit(LoginResult.UpdateUsername(input.username))
            }
            
            is LoginIntent.PasswordChanged -> {
                emit(LoginResult.UpdatePassword(input.password))
            }
            
            is LoginIntent.LoginClicked -> {
                emit(LoginResult.SetLoading(true))
                emit(LoginResult.SetError(null))
                
                try {
                    val response = authService.login(state.username, state.password)
                    emit(LoginResult.SetLoading(false))
                    emit(LoginResult.NavigateToHome(response.userId))
                    emit(LoginResult.ShowToast("Login successful"))
                } catch (e: Exception) {
                    emit(LoginResult.SetLoading(false))
                    emit(LoginResult.SetError(e.message))
                    emit(LoginResult.ShowToast("Login failed"))
                }
            }
            
            is LoginIntent.ForgotPasswordClicked -> {
                emit(LoginResult.ShowToast("Password reset email sent"))
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
- Only processes Actions (ignores Effects)
- Deterministic

**Example**:
```kotlin
class LoginReducer : Reducer<LoginResult, LoginState> {
    
    override fun reduce(result: LoginResult, state: LoginState): LoginState {
        return when (result) {
            is LoginResult.UpdateUsername -> 
                state.copy(username = result.username)
            
            is LoginResult.UpdatePassword -> 
                state.copy(password = result.password)
            
            is LoginResult.SetLoading -> 
                state.copy(isLoading = result.isLoading)
            
            is LoginResult.SetError -> 
                state.copy(errorMessage = result.message)
            
            // Effects don't modify state
            is LoginResult.NavigateToHome -> state
            is LoginResult.ShowToast -> state
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

### 6. ViewModel

**Purpose**: Orchestrates the MVI cycle

**Responsibilities**:
- Manages state lifecycle
- Processes intents through Processor
- Applies Actions through Reducer
- Emits Effects
- Handles errors

**Example**:
```kotlin
class LoginViewModel(
    authService: AuthService
) : ViewModel<LoginIntent, LoginResult, LoginResult, LoginState>(
    initialState = LoginState(),
    processor = LoginProcessor(authService),
    reducer = LoginReducer(),
    onError = { error ->
        // Log to analytics or crash reporting
        Analytics.logError(error)
    }
)
```

**Exposed Flows**:
- `state: StateFlow<S>` - Current state
- `effects: SharedFlow<E>` - One-time effects
- `errors: SharedFlow<Throwable>` - Errors during processing

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                          ViewModel                           │
│                                                               │
│  ┌────────┐     ┌───────────┐     ┌─────────┐              │
│  │        │     │           │     │         │              │
│  │ State  │◄────┤  Reducer  │◄────┤ Action  │              │
│  │        │     │           │     │         │              │
│  └───┬────┘     └───────────┘     └────▲────┘              │
│      │                                  │                    │
│      │          ┌───────────┐     ┌────┴────┐               │
│      │          │           │     │         │               │
│      │          │ Processor │────▶│ Result  │               │
│      │          │           │     │         │               │
│      │          └─────▲─────┘     └────┬────┘               │
│      │                │                 │                    │
│      │          ┌─────┴─────┐     ┌────▼────┐               │
│      │          │           │     │         │               │
│      │          │  Intent   │     │ Effect  │               │
│      │          │           │     │         │               │
│      │          └─────▲─────┘     └────┬────┘               │
│      │                │                 │                    │
└──────┼────────────────┼─────────────────┼────────────────────┘
       │                │                 │
       │                │                 │
       ▼                │                 ▼
┌─────────┐      ┌──────┴──────┐   ┌───────────┐
│         │      │             │   │           │
│   View  │─────▶│  User       │   │   Side    │
│         │      │  Action     │   │   Effects │
│         │      │             │   │           │
└─────────┘      └─────────────┘   └───────────┘
```

## Advanced Features

### Middleware

Middleware allows you to intercept and modify the MVI flow:

```kotlin
class LoggingMiddleware<I : Intent, S : State> : Middleware<I, S> {
    override fun beforeIntent(intent: I, state: S): I? {
        println("Processing intent: $intent")
        return intent
    }
    
    override fun afterResult(result: Result, state: S): Result? {
        println("Result produced: $result")
        return result
    }
    
    override fun onError(error: Throwable, intent: I, state: S) {
        println("Error: $error for intent: $intent")
    }
}
```

**Use Cases**:
- Logging
- Analytics
- Performance monitoring
- Debugging
- State persistence
- Error tracking

### Error Handling

KMVI provides multiple layers of error handling:

1. **Try-Catch in Processor**: Catch and convert to Results
2. **Flow.catch()**: Automatic error catching in flow
3. **CoroutineExceptionHandler**: Fallback for uncaught exceptions
4. **Error Flow**: Exposed for UI consumption
5. **Custom Error Handler**: Callback for custom handling

**Example**:
```kotlin
// In Processor
override fun process(input: MyIntent, state: MyState): Flow<Result> = flow {
    try {
        val data = repository.fetchData()
        emit(MyResult.Success(data))
    } catch (e: NetworkException) {
        emit(MyResult.NetworkError(e.message))
    } catch (e: Exception) {
        throw e // Let ViewModel error handling catch it
    }
}

// In UI
LaunchedEffect(Unit) {
    viewModel.errors.collect { error ->
        showErrorSnackbar(error.message)
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
    val result = MyResult.Increment()
    
    val newState = reducer.reduce(result, state)
    
    assertEquals(1, newState.count)
    assertEquals(0, state.count) // Original unchanged
}
```

### Unit Testing Processors

```kotlin
@Test
fun `processor should emit correct results`() = runTest {
    val processor = MyProcessor(mockRepository)
    val intent = MyIntent.Load()
    val state = MyState()
    
    val results = processor.process(intent, state).collectResults()
    
    assertTrue(results[0] is MyResult.Loading)
    assertTrue(results[1] is MyResult.Success)
}
```

### Integration Testing ViewModels

```kotlin
@Test
fun `viewModel should update state on intent`() = runTest {
    val viewModel = MyViewModel()
    
    viewModel.process(MyIntent.Increment())
    delay(100)
    
    assertEquals(1, viewModel.state.value.count)
}
```

## Best Practices Summary

1. **Keep State Simple**: Flat structure, immutable, serializable
2. **Intents Are Actions**: One intent = one user action
3. **Processors Have Logic**: All business logic goes here
4. **Reducers Are Pure**: No side effects, fast, deterministic
5. **Effects for Side Effects**: Navigation, dialogs, etc.
6. **Test Everything**: Processors, Reducers, ViewModels
7. **Handle Errors Gracefully**: Multiple layers of error handling
8. **Use Middleware**: Cross-cutting concerns
9. **State Immutability**: Always create new instances
10. **Dispatcher Management**: Use provided dispatchers

## Performance Considerations

1. **State Updates**: Keep state objects small and focused
2. **Flow Operators**: Use appropriate operators (flowOn, buffer)
3. **Dispatcher Selection**: Use Default for computation, Main for UI
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
3. Merge ActionProcessor → Processor
4. Convert reducer → KMVI Reducer
5. Update ViewModels to extend KMVI ViewModel

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
- [Sample Code](sample/) - Example implementations
- [Tests](kmvi/src/commonTest/) - Test examples
- [API Documentation](https://javadoc.io/doc/io.github.natobytes/kmvi)

## Contributing

When contributing to KMVI, please:

1. Follow the existing architecture patterns
2. Add tests for new features
3. Update documentation
4. Keep changes minimal and focused
5. Follow Kotlin coding conventions

## License

Apache License 2.0
