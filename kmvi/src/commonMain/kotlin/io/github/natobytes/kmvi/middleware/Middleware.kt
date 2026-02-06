package io.github.natobytes.kmvi.middleware

import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.Result
import io.github.natobytes.kmvi.contract.State

/**
 * Middleware interface for intercepting and transforming intents and results in the MVI flow.
 *
 * Middleware can be used to add cross-cutting concerns like:
 * - Logging
 * - Analytics
 * - Debugging
 * - Performance monitoring
 * - Error tracking
 * - State persistence
 *
 * Middleware is executed in the order it's registered.
 *
 * @param I The type of [Intent]
 * @param S The type of [State]
 */
interface Middleware<I : Intent, S : State> {
    
    /**
     * Called before an intent is processed.
     * 
     * @param intent The intent about to be processed
     * @param state The current state
     * @return The (potentially modified) intent to process, or null to skip processing
     */
    fun beforeIntent(intent: I, state: S): I? = intent
    
    /**
     * Called after a result is produced but before it's applied to state.
     * 
     * @param result The result produced by the processor
     * @param state The current state
     * @return The (potentially modified) result to apply, or null to skip
     */
    fun afterResult(result: Result, state: S): Result? = result
    
    /**
     * Called after an error occurs during intent processing.
     * 
     * @param error The error that occurred
     * @param intent The intent that was being processed
     * @param state The current state
     */
    fun onError(error: Throwable, intent: I, state: S) {
        // Default implementation does nothing
    }
}

/**
 * A logging middleware that prints intent and result information.
 * Useful for debugging and development.
 */
class LoggingMiddleware<I : Intent, S : State>(
    private val tag: String = "KMVI",
    private val enabled: Boolean = true
) : Middleware<I, S> {
    
    override fun beforeIntent(intent: I, state: S): I {
        if (enabled) {
            println("[$tag] Intent: ${intent::class.simpleName}")
        }
        return intent
    }
    
    override fun afterResult(result: Result, state: S): Result {
        if (enabled) {
            println("[$tag] Result: ${result::class.simpleName}")
        }
        return result
    }
    
    override fun onError(error: Throwable, intent: I, state: S) {
        if (enabled) {
            println("[$tag] Error processing ${intent::class.simpleName}: ${error.message}")
        }
    }
}

/**
 * A middleware that tracks analytics events.
 * Implement the [tracker] lambda to send events to your analytics service.
 */
class AnalyticsMiddleware<I : Intent, S : State>(
    private val tracker: (eventName: String, properties: Map<String, Any>) -> Unit
) : Middleware<I, S> {
    
    override fun beforeIntent(intent: I, state: S): I {
        tracker("intent_processed", mapOf(
            "intent" to (intent::class.simpleName ?: "Unknown"),
            "timestamp" to System.currentTimeMillis()
        ))
        return intent
    }
}

/**
 * A middleware that provides timing information for intent processing.
 * Useful for performance monitoring.
 */
class TimingMiddleware<I : Intent, S : State>(
    private val onTiming: (intent: String, durationMs: Long) -> Unit
) : Middleware<I, S> {
    
    private val startTimes = mutableMapOf<I, Long>()
    
    override fun beforeIntent(intent: I, state: S): I {
        startTimes[intent] = System.currentTimeMillis()
        return intent
    }
    
    override fun afterResult(result: Result, state: S): Result {
        // Note: This is simplified - in a real implementation, you'd need to correlate
        // results with their originating intents
        return result
    }
}
