# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This repository contains two things:

1. **KMVI** — a Kotlin Multiplatform MVI (Model-View-Intent) library published to Maven Central as `io.github.natobytes:kmvi`. Targets JVM, Android (API 36), and iOS (x64, ARM64, Simulator ARM64 via XCFramework).

2. **Sample apps** — demo applications that use the library:
   - `sample/` — JVM-only Kotlin Multiplatform module (Todo List)
   - `teslaDrive/` — Android app (USB/OTG export tool using KMVI)

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

# TeslaDrive Android app
./gradlew :teslaDrive:assembleDebug
./gradlew :teslaDrive:check

# Auto-fix formatting
./gradlew ktlintFormat
```

JDK 21 (Temurin) is used in CI and for release publishing.

> **Never run `./gradlew` commands in parallel** — Gradle uses file locks and shared daemons, so concurrent Gradle invocations will fail or block each other. Always run them sequentially.

## KMVI Library Architecture

The entire library lives in `kmvi/src/commonMain/kotlin/io/github/natobytes/kmvi/`. Small, focused codebase (~155 lines) with five contract interfaces and one abstract ViewModel class.

**Data flow:** `Intent → Processor → Flow<Result> → ViewModel → (Action → Reducer → State) | (Effect → Flow)`

Core contract types in `contract/`:
- **Intent** — marker interface for user actions
- **State** — marker interface for immutable UI state
- **Result** — sealed interface with subtypes `Action` (state mutations) and `Effect` (side effects)
- **Action** — represents a state mutation applied by the reducer
- **Effect** — represents a one-off side effect (e.g., navigation, toasts)

Processing types in `io.github.natobytes.kmvi`:
- **Processor** — transforms `Intent` + current `State` into `Flow<Result>`
- **Reducer** — pure function: `(Action, State) → State`

**KMVIViewModel** (`KMVIViewModel.kt`) — abstract class generic over `<I: Intent, A: Action, E: Effect, S: State>`. Extends AndroidX `ViewModel`, uses `viewModelScope`. Exposes `state: StateFlow<S>` and `effects: Flow<E>` (backed by buffered Channel). Entry point is `process(intent)`. Processor runs on Default dispatcher, results collected on Main dispatcher.

## KMVI Screen Pattern (for app modules)

Each screen using KMVI consists of 8 files across 3 subpackages. This pattern is used in `teslaDrive/` and is the standard for any app built on KMVI (as used in the barbixas reference app):

```
features/<feature>/
├── contract/
│   ├── <Name>State.kt        — data class implementing State
│   ├── <Name>Intent.kt       — sealed interface implementing Intent
│   ├── <Name>Action.kt       — sealed interface implementing Action (state mutations)
│   └── <Name>Effect.kt       — sealed interface implementing Effect (one-shot side effects)
├── <Name>Processor.kt        — Processor<Intent, State>, returns Flow<Result>
├── <Name>Reducer.kt          — Reducer<Action, State>, pure function
├── <Name>ViewModel.kt        — extends KMVIViewModel<Intent, Action, Effect, State>
└── ui/
    └── <Name>Screen.kt       — @Composable screen + content + previews
```

Key rules:
- `Result` is a **sealed interface** in KMVI — cannot be subtyped from external modules. Use `Action` and `Effect` interfaces directly.
- Reducer generic `R` type = **Action** (not Result). Use exhaustive `when`, no `else` branch. Parameter named `action`.
- ViewModel extends `KMVIViewModel<I, R, E, S>`, generic `R` type = **Action**.
- Processor returns `Flow<Result>`, emits `Action` or `Effect` subtypes.
- Use explicit named methods on use cases: `useCase.get(args)` — never `operator fun invoke`.
- Single `LaunchedEffect(Unit)` for both initial intent dispatch AND effects collection.
- **Screen**: public composable with route params + viewModel + navigation lambdas.
- **Content**: private composable with state + callbacks (no ViewModel) — used by previews.
- Tri-state pattern: loading spinner → error with retry → content.

## Recommended Libraries for App Development

When building apps that use KMVI (like `teslaDrive`), use these libraries (taken from barbixas reference app):

| Purpose | Library |
|---------|---------|
| Dependency Injection | `me.tatarka.inject:kotlin-inject` v0.9.0 + KSP (`@KmpComponentCreate`) |
| Networking | `io.ktor:ktor-client-*` with JSON content negotiation |
| Serialization | `org.jetbrains.kotlinx:kotlinx-serialization-json` |
| UI | Compose Multiplatform (Jetpack Compose for Android) |
| Navigation | Jetpack Navigation Compose with `@Serializable` destinations |
| Image Loading | Coil (for async images) |
| Settings/Storage | Multiplatform Settings (for token/preferences storage) |
| Result Handling | `Either<Success, Error>` sealed class in a `utils` module |

## Module Structure (for multi-module apps)

When expanding the repository with new app modules, follow this layered structure (used in barbixas reference app):

```
app:android / app:desktop / iosApp  (platform entry points)
  └── shared:app  (features, navigation, DI)
      ├── shared:ui       (shared Compose components, theme)
      ├── shared:data     (DTOs, API, repo impls, mappers)
      ├── shared:domain   (models, repo interfaces, use case interfaces + impls)
      ├── shared:config   (AppConfig interface, Environment enum)
      └── shared:utils    (Either<S,E>, common utilities)
```

**Layer rules:**
- **domain** — pure Kotlin, no `@Inject`, no data layer deps. Defines repo interfaces and use case impls.
- **data** — implements repo interfaces, maps DTOs → domain models/errors. Uses `@Inject`.
- **shared:app** — features depend only on domain use cases. No DTOs or networking imports.
- Repository interfaces return domain models (`Either<Model, DomainError>`), never DTOs.

## Clean Architecture Patterns

```
App Features  →  Domain  ←  Data
```

- Domain defines contracts; Data implements them.
- App features call use cases only — no knowledge of DTOs or Ktor.
- `DataComponent` (kotlin-inject) manually constructs use case impls and wires repo interfaces.
- `AppComponent` extends `DataComponent`, adds app-specific bindings.
- `ComponentHolder.init(appConfig)` initializes DI per platform.

## String Resources

- **Never hard-code user-visible strings** — always define in `strings.xml` and reference via `stringResource(Res.string.xxx)`.
- Naming convention: `<feature>_<element>` (e.g. `export_title`, `drive_connect_button`).
- `contentDescription` values also go in strings.xml.

## Code Style

- Kotlin official code style (`ktlint_official`) enforced via `.editorconfig`
- Max line length: 140 characters
- 4-space indentation
- Function signatures force multiline at 2+ parameters
- `@Composable` functions exempt from naming conventions
- Package name validation disabled
- Imports must be lexicographically ordered, no empty lines between

## Commit & Release Conventions

- Semantic commit messages required on PR titles (e.g., `feat:`, `fix:`, `chore:`)
- Releases are automated via semantic-release; version is set from `RELEASE_NAME` env var
- Publishing to Maven Central uses `com.vanniktech.maven.publish` plugin

## Module Structure (current repo)

- `kmvi/` — the library (multiplatform)
- `sample/` — Todo List example (JVM-only KMP module demonstrating the library)
- `teslaDrive/` — Android app using KMVI (USB/OTG Tesla data export)

## Reference App

The [natobytes/barbixas](https://github.com/natobytes/barbixas) repository is the reference implementation showing KMVI in a production-grade Compose Multiplatform app. It uses:
- KMVI for all screen state management
- kotlin-inject for DI
- Ktor for networking
- Clean Architecture (domain / data / app layers)
- Two separate apps (client + business) sharing domain and data modules
- Firebase for analytics and crash reporting

Consult the barbixas `CLAUDE.md` for detailed patterns on navigation, DI wiring, screen conventions, and Firebase config.
