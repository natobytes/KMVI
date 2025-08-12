![Maven Central Version](https://img.shields.io/maven-central/v/io.github.natobytes/kmvi)


# KMVI - Kotlin Multiplatform MVI - Architecture Library

**A robust and flexible framework for building modern, maintainable, and testable applications across multiple platforms using the Model-View-Intent (MVI) pattern.**

## Introduction

This library provides a set of core components and utilities to streamline the implementation of MVI architecture in your Kotlin Multiplatform projects. Itleverages Kotlin's powerful features and coroutines to create a reactive and efficient development experience.

## Key Features

* **Platform Agnostic:** Build applications for Android, iOS, Desktop, and Web with a shared codebase.
* **Type-Safe State Management:** Leverage Kotlin's type system to ensure predictable and reliable state updates.
* **Unidirectional Data Flow:** Enforce a clear separation of concerns and predictable state changes with the MVI pattern.
* **Coroutine-Based Asynchronicity:** Handle asynchronous operations seamlessly using Kotlin coroutines.
* **Testability:** Write comprehensive unit tests for your ViewModels, Reducers, and other components.
* **Extensibility:** Easily customize and extend the library to fit your specific project needs.

## Core Concepts

* **Intent:** Represents a user intention or event that triggers a state change.
* **Action:** Represents a change in the application's state that should be immediately reflected in the UI.
* **Result:** Encapsulates the outcome of processing an Intent, which can be either an Action or an Effect.
* **Effect:** Represents a side effect, such as navigation or displaying a dialog, that should be handled outside the core MVI cycle.
* **Reducer:** A pure function that takes the current state and an Action and returns a new, immutable state.
* **Processor:** Processes Intents and transforms them into a stream of Results.
* **Store:** Manages the application's state, handles Intents, and emits new states.

## Import 
```
implementation("io.github.natobytes:kmvi:{version}")
```
