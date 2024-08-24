package io.github.natobytes.kmvi.contract

import kotlinx.coroutines.flow.Flow

interface Processor<I : Intent, R : Request, S : State> {

    fun process(input: I, state: S): Flow<R>
}
