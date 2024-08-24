package io.github.natobytes.kmvi.contract


interface Reducer<R : Request, S : State> {
    
    fun reduce(result: R, state: S): S
}
