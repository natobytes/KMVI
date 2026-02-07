# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

KMVI is a Kotlin Multiplatform MVI (Model-View-Intent) architecture library published to Maven Central under `io.github.natobytes:kmvi`. It targets JVM, Android (API 36), and iOS (x64, ARM64, Simulator ARM64 via XCFramework).

## Build & Test Commands

```bash
# Build and run all tests (requires macOS for iOS targets)
./gradlew allTests

# Run JVM tests only (works on Linux/macOS)
./gradlew jvmTest

# Run iOS simulator tests (macOS only)
./gradlew iosSimulatorArm64Test

# Run all checks (includes linting)
./gradlew check

# Build without tests
./gradlew build
```

JDK 21 (Temurin) is used in CI. JDK 21 (Zulu) is used for release publishing.

## Architecture

The entire library lives in `kmvi/src/commonMain/kotlin/io/github/natobytes/kmvi/`. It's a small, focused codebase (~155 lines of core code) with five contract interfaces and one abstract ViewModel class.

**Data flow:** `Intent → Processor → Flow<Result> → ViewModel → (Action → Reducer → State) | (Effect → Flow)`

Core contract types in `contract/`:
- **Intent** — marker interface for user actions
- **State** — marker interface for immutable UI state
- **Result** — sealed interface with subtypes `Action` (state mutations) and `Effect` (side effects like navigation)
- **Action** — represents a state mutation to be applied by the reducer
- **Effect** — represents a one-off side effect (e.g., navigation, toasts)

Processing types in `io.github.natobytes.kmvi`:
- **Processor** — transforms `Intent` + current `State` into `Flow<Result>`
- **Reducer** — pure function: `(Action, State) → State`

**KMVIViewModel** (`KMVIViewModel.kt`) — abstract class generic over `<I: Intent, A: Action, E: Effect, S: State>`. Extends AndroidX `ViewModel`, uses `viewModelScope` for coroutine management. Exposes `state: StateFlow<S>` and `effects: Flow<E>` (backed by buffered Channel). Entry point is `process(intent)`. Processor runs on computation dispatcher (Default), results collected on main dispatcher.

## Code Style

- Kotlin official code style (`ktlint_official`) enforced via `.editorconfig`
- Max line length: 140 characters
- 4-space indentation
- Function signatures force multiline at 2+ parameters
- `@Composable` functions exempt from naming conventions
- Package name validation disabled

## Commit & Release Conventions

- Semantic commit messages required on PR titles (e.g., `feat:`, `fix:`, `chore:`)
- Releases are automated via semantic-release; version is set from `RELEASE_NAME` env var
- Publishing to Maven Central uses `com.vanniktech.maven.publish` plugin

## Module Structure

- `kmvi/` — the library (multiplatform)
- `sample/` — Todo List example (JVM-only KMP module demonstrating the library)
