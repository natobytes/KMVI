# KMVI Testing Guide

This guide covers best practices and patterns for testing applications built with KMVI.

## Table of Contents

- [Testing Philosophy](#testing-philosophy)
- [Test Setup](#test-setup)
- [Unit Testing](#unit-testing)
  - [Testing Reducers](#testing-reducers)
  - [Testing Processors](#testing-processors)
  - [Testing ViewModels](#testing-viewmodels)
- [Integration Testing](#integration-testing)
- [Test Helpers](#test-helpers)
- [Common Patterns](#common-patterns)
- [Best Practices](#best-practices)

## Testing Philosophy

KMVI makes testing easy by enforcing:
- **Pure functions** (Reducers)
- **Testable logic** (Processors)
- **Observable state** (StateFlow)
- **Predictable behavior** (Unidirectional flow)

Each component can be tested in isolation:
- **Reducers**: Pure functions, easiest to test
- **Processors**: Business logic, mock dependencies
- **ViewModels**: Integration of all components

## Test Setup

### Dependencies

Add to your `build.gradle.kts`:

```kotlin
commonTest {
    dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
    }
}
```

### Import Test Utilities

```kotlin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import io.github.natobytes.kmvi.test.*
```

## Unit Testing

### Testing Reducers

Reducers are pure functions and the easiest to test.

#### Basic Reducer Test

```kotlin
class CounterReducerTest {
    
    @Test
    fun `reducer should increment count`() {
        val reducer = CounterReducer()
        val state = CounterState(count = 5)
        val action = CounterResult.UpdateCount(10)
        
        val newState = reducer.reduce(action, state)
        
        assertEquals(10, newState.count)
    }
}
```

#### Test Immutability

```kotlin
@Test
fun `reducer should not modify original state`() {
    val reducer = CounterReducer()
    val originalState = CounterState(count = 5)
    
    val newState = reducer.reduce(CounterResult.UpdateCount(10), originalState)
    
    // Original state should be unchanged
    assertEquals(5, originalState.count)
    assertEquals(10, newState.count)
}
```

#### Test Determinism

```kotlin
@Test
fun `reducer should be deterministic`() {
    val reducer = CounterReducer()
    val state = CounterState(count = 3)
    val action = CounterResult.UpdateCount(7)
    
    val result1 = reducer.reduce(action, state)
    val result2 = reducer.reduce(action, state)
    
    assertEquals(result1, result2)
}
```

#### Test Multiple Transformations

```kotlin
@Test
fun `reducer should handle multiple transformations`() {
    val reducer = CounterReducer()
    var state = CounterState(count = 0)
    
    state = reducer.reduce(CounterResult.UpdateCount(5), state)
    assertEquals(5, state.count)
    
    state = reducer.reduce(CounterResult.UpdateCount(10), state)
    assertEquals(10, state.count)
    
    state = reducer.reduce(CounterResult.SetLoading(true), state)
    assertEquals(10, state.count)
    assertTrue(state.isLoading)
}
```

### Testing Processors

Processors contain business logic and may have dependencies.

#### Basic Processor Test

```kotlin
class CounterProcessorTest {
    
    @Test
    fun `processor should emit increment result`() = runTest {
        val processor = CounterProcessor()
        val state = CounterState(count = 5)
        val intent = CounterIntent.Increment
        
        val results = processor.process(intent, state).collectResults()
        
        assertEquals(1, results.size)
        assertTrue(results[0] is CounterResult.UpdateCount)
        assertEquals(6, (results[0] as CounterResult.UpdateCount).newCount)
    }
}
```

#### Test Async Operations

```kotlin
@Test
fun `processor should handle async operations`() = runTest {
    val mockRepository = MockRepository()
    val processor = DataProcessor(mockRepository)
    val state = DataState()
    val intent = DataIntent.Load
    
    val results = processor.process(intent, state).collectResults()
    
    // Should emit loading, then success
    assertEquals(2, results.size)
    assertTrue(results[0] is DataResult.SetLoading)
    assertTrue(results[1] is DataResult.Success)
}
```

#### Test Error Handling

```kotlin
@Test
fun `processor should handle errors gracefully`() = runTest {
    val mockRepository = MockRepository(shouldFail = true)
    val processor = DataProcessor(mockRepository)
    val state = DataState()
    val intent = DataIntent.Load
    
    val results = processor.process(intent, state).collectResults()
    
    assertTrue(results.any { it is DataResult.Error })
}
```

#### Test Multiple Results

```kotlin
@Test
fun `processor should emit multiple results for single intent`() = runTest {
    val processor = CounterProcessor()
    val state = CounterState(count = 9)
    val intent = CounterIntent.Increment // Reaches milestone at 10
    
    val results = processor.process(intent, state).collectResults()
    
    assertEquals(3, results.size)
    assertTrue(results[0] is CounterResult.UpdateCount)
    assertTrue(results[1] is CounterResult.AddToHistory)
    assertTrue(results[2] is CounterResult.ShowToast) // Milestone effect
}
```

#### Test with Mocks

```kotlin
@Test
fun `processor should use repository correctly`() = runTest {
    val mockRepository = mockk<UserRepository>()
    every { mockRepository.getUser(any()) } returns User("John")
    
    val processor = UserProcessor(mockRepository)
    val intent = UserIntent.LoadUser("123")
    val state = UserState()
    
    val results = processor.process(intent, state).collectResults()
    
    verify { mockRepository.getUser("123") }
    assertTrue(results.any { it is UserResult.UserLoaded })
}
```

### Testing ViewModels

ViewModels integrate all components and manage the MVI lifecycle.

#### Basic ViewModel Test

```kotlin
class CounterViewModelTest {
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewModel should update state on intent`() = runTest {
        val viewModel = CounterViewModel()
        
        viewModel.process(CounterIntent.Increment)
        delay(100) // Allow processing time
        
        assertEquals(1, viewModel.state.value.count)
    }
}
```

#### Test State Flow

```kotlin
@Test
fun `viewModel should emit state updates`() = runTest {
    val viewModel = CounterViewModel()
    val states = mutableListOf<CounterState>()
    
    val job = launch {
        viewModel.state.take(3).toList(states)
    }
    
    viewModel.process(CounterIntent.Increment)
    viewModel.process(CounterIntent.Increment)
    delay(100)
    job.cancel()
    
    assertEquals(3, states.size) // Initial + 2 updates
    assertEquals(0, states[0].count)
    assertEquals(1, states[1].count)
    assertEquals(2, states[2].count)
}
```

#### Test Effects

```kotlin
@Test
fun `viewModel should emit effects`() = runTest {
    val viewModel = CounterViewModel()
    val effects = mutableListOf<CounterResult>()
    
    val job = launch {
        viewModel.effects.take(1).toList(effects)
    }
    
    viewModel.process(CounterIntent.Reset)
    delay(100)
    job.cancel()
    
    assertEquals(1, effects.size)
    assertTrue(effects[0] is CounterResult.ShowToast)
}
```

#### Test Error Handling

```kotlin
@Test
fun `viewModel should emit errors to error flow`() = runTest {
    val viewModel = CounterViewModel()
    val errors = mutableListOf<Throwable>()
    
    val job = launch {
        viewModel.errors.take(1).toList(errors)
    }
    
    viewModel.process(CounterIntent.ThrowError)
    delay(100)
    job.cancel()
    
    assertEquals(1, errors.size)
    assertTrue(errors[0] is IllegalStateException)
}
```

#### Test Custom Error Handler

```kotlin
@Test
fun `viewModel should call custom error handler`() = runTest {
    var capturedError: Throwable? = null
    val viewModel = CounterViewModel(
        onError = { capturedError = it }
    )
    
    viewModel.process(CounterIntent.ThrowError)
    delay(100)
    
    assertNotNull(capturedError)
    assertTrue(capturedError is IllegalStateException)
}
```

## Integration Testing

Test the full MVI flow from Intent to State update.

```kotlin
@Test
fun `full MVI flow integration test`() = runTest {
    val viewModel = LoginViewModel(FakeAuthService())
    
    // Initial state
    assertEquals("", viewModel.state.value.username)
    assertFalse(viewModel.state.value.isLoading)
    
    // Update username
    viewModel.process(LoginIntent.UsernameChanged("john@example.com"))
    delay(50)
    assertEquals("john@example.com", viewModel.state.value.username)
    
    // Update password
    viewModel.process(LoginIntent.PasswordChanged("password123"))
    delay(50)
    assertEquals("password123", viewModel.state.value.password)
    
    // Attempt login
    val effects = mutableListOf<LoginResult>()
    val job = launch {
        viewModel.effects.take(1).toList(effects)
    }
    
    viewModel.process(LoginIntent.LoginClicked)
    delay(100)
    
    // State should show loading then success
    assertFalse(viewModel.state.value.isLoading)
    assertTrue(effects.any { it is LoginResult.NavigateToHome })
    
    job.cancel()
}
```

## Test Helpers

KMVI provides test helpers in `io.github.natobytes.kmvi.test` package.

### StateRecorder

Track state changes and effects:

```kotlin
@Test
fun `test with state recorder`() = runTest {
    val viewModel = CounterViewModel()
    val recorder = StateRecorder<CounterState>()
    
    val job = launch {
        viewModel.state.collect(recorder::recordState)
    }
    
    viewModel.process(CounterIntent.Increment)
    viewModel.process(CounterIntent.Increment)
    delay(100)
    job.cancel()
    
    assertEquals(3, recorder.stateCount()) // Initial + 2 updates
    assertEquals(2, recorder.getLastState()?.count)
}
```

### Collection Helpers

```kotlin
@Test
fun `test with collection helpers`() = runTest {
    val processor = CounterProcessor()
    val state = CounterState(count = 0)
    
    // Collect only actions
    val actions = processor.process(CounterIntent.Increment, state)
        .collectActions()
    
    assertTrue(actions.all { it is Action })
    
    // Collect only effects
    val effects = processor.process(CounterIntent.Reset, state)
        .collectEffects()
    
    assertTrue(effects.all { it is Effect })
}
```

### Assert Helpers

```kotlin
@Test
fun `test with assert helpers`() = runTest {
    val viewModel = CounterViewModel()
    val recorder = StateRecorder<CounterState>()
    
    val job = launch {
        viewModel.state.collect(recorder::recordState)
    }
    
    viewModel.process(CounterIntent.Increment)
    delay(100)
    job.cancel()
    
    // Assert state sequence
    recorder.assertStates(
        CounterState(count = 0),
        CounterState(count = 1)
    )
}
```

## Common Patterns

### Test with Fake Dependencies

```kotlin
class FakeRepository : Repository {
    var users = listOf(User("1", "John"), User("2", "Jane"))
    
    override suspend fun getUsers(): List<User> = users
}

@Test
fun `test with fake repository`() = runTest {
    val fakeRepo = FakeRepository()
    val processor = UserProcessor(fakeRepo)
    
    val results = processor.process(UserIntent.LoadUsers, UserState())
        .collectResults()
    
    assertTrue(results.any { 
        it is UserResult.UsersLoaded && it.users.size == 2 
    })
}
```

### Test Timeout Scenarios

```kotlin
@Test(timeout = 5000)
fun `processor should not hang`() = runTest {
    val processor = CounterProcessor()
    val state = CounterState()
    
    // Should complete quickly
    processor.process(CounterIntent.Increment, state).collect()
}
```

### Parameterized Tests

```kotlin
@Test
fun `test multiple increments`() = runTest {
    val testCases = listOf(
        Triple(0, CounterIntent.Increment, 1),
        Triple(5, CounterIntent.Increment, 6),
        Triple(10, CounterIntent.Increment, 11)
    )
    
    val reducer = CounterReducer()
    
    testCases.forEach { (initial, intent, expected) ->
        val state = CounterState(count = initial)
        val result = CounterResult.UpdateCount(expected)
        val newState = reducer.reduce(result, state)
        assertEquals(expected, newState.count)
    }
}
```

## Best Practices

1. **Test in Isolation**: Test each component separately
2. **Use runTest**: Always wrap coroutine tests with `runTest`
3. **Test Immutability**: Verify original state is not modified
4. **Test Error Cases**: Don't just test happy paths
5. **Use Fakes Over Mocks**: Prefer simple fake implementations
6. **Test Async Operations**: Include delays for async tests
7. **Test Effects Separately**: Collect effects in separate flows
8. **Use Test Helpers**: Leverage provided test utilities
9. **Keep Tests Fast**: Use UnconfinedTestDispatcher
10. **Test Edge Cases**: Boundary conditions, empty states, etc.

### Test Structure

Follow Arrange-Act-Assert pattern:

```kotlin
@Test
fun `test description`() = runTest {
    // Arrange
    val viewModel = CounterViewModel()
    val expectedCount = 5
    
    // Act
    viewModel.process(CounterIntent.IncrementBy(5))
    delay(100)
    
    // Assert
    assertEquals(expectedCount, viewModel.state.value.count)
}
```

### Test Naming

Use descriptive test names:

```kotlin
// Good
@Test
fun `increment should increase count by one`()

@Test
fun `reducer should not modify original state`()

@Test
fun `processor should emit error on network failure`()

// Bad
@Test
fun testIncrement()

@Test
fun test1()

@Test
fun reducerTest()
```

## Example Test Suite

Here's a complete test suite example:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CounterFeatureTest {
    
    @Test
    fun `reducer handles increment`() {
        val reducer = CounterReducer()
        val state = CounterState(count = 0)
        
        val newState = reducer.reduce(
            CounterResult.UpdateCount(1), 
            state
        )
        
        assertEquals(1, newState.count)
    }
    
    @Test
    fun `processor emits correct results`() = runTest {
        val processor = CounterProcessor()
        val state = CounterState(count = 0)
        
        val results = processor
            .process(CounterIntent.Increment, state)
            .collectResults()
        
        assertTrue(results[0] is CounterResult.UpdateCount)
    }
    
    @Test
    fun `viewModel integrates all components`() = runTest {
        val viewModel = CounterViewModel()
        
        viewModel.process(CounterIntent.Increment)
        delay(100)
        
        assertEquals(1, viewModel.state.value.count)
    }
    
    @Test
    fun `effects are emitted correctly`() = runTest {
        val viewModel = CounterViewModel()
        val effects = mutableListOf<CounterResult>()
        
        val job = launch {
            viewModel.effects.take(1).toList(effects)
        }
        
        viewModel.process(CounterIntent.Reset)
        delay(100)
        job.cancel()
        
        assertTrue(effects[0] is CounterResult.ShowToast)
    }
}
```

## Troubleshooting

### Tests Hang

**Problem**: Tests don't complete
**Solution**: Use `UnconfinedTestDispatcher` and add timeout

```kotlin
@Test(timeout = 5000)
fun `test name`() = runTest {
    // test code
}
```

### State Not Updated

**Problem**: State updates not reflected
**Solution**: Add delay after processing intent

```kotlin
viewModel.process(intent)
delay(100) // Give time for processing
```

### Effects Not Collected

**Problem**: Effects not received
**Solution**: Start collection before processing intent

```kotlin
val job = launch {
    viewModel.effects.collect { /* handle */ }
}

viewModel.process(intent)
delay(100)
job.cancel()
```

## Resources

- [Architecture Documentation](ARCHITECTURE.md)
- [API Documentation](https://javadoc.io/doc/io.github.natobytes/kmvi)
- [Sample Tests](kmvi/src/commonTest/)
- [Sample App](sample/)

## Contributing

When adding tests:
1. Follow existing patterns
2. Test both success and failure cases
3. Use descriptive names
4. Include comments for complex tests
5. Keep tests focused and simple
