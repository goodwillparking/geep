package com.github.goodwillparking.geep

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

// TODO: Support a synchronous implementation of AsyncContext (would be useful for testing).
//  Things will get weird if the timer executes on the calling thread in the current implementation.
// TODO: Provide method for shutting down the async context?
interface AsyncContext {

    fun setSingleTimer(timer: SetSingleTimer, messageHandler: (key: Any, message: Any) -> Unit): Future<*>

    fun setPeriodicTimer(timer: SetPeriodicTimer, messageHandler: (key: Any, message: Any) -> Unit): Future<*>

    fun runAsync(asyncTask: () -> Unit): Future<Unit>
}

class JavaAsyncContext(
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(32)
) : AsyncContext {

    override fun setSingleTimer(timer: SetSingleTimer, messageHandler: (key: Any, message: Any) -> Unit): Future<*> {
        return executor.schedule(
            { messageHandler(timer.key, timer.message) },
            timer.duration.toMillis(),
            TimeUnit.MILLISECONDS
        )
    }

    override fun setPeriodicTimer(timer: SetPeriodicTimer, messageHandler: (key: Any, message: Any) -> Unit): Future<*> {
        return executor.scheduleWithFixedDelay(
            { messageHandler(timer.key, timer.message) },
            timer.initialDelay.toMillis(),
            timer.duration.toMillis(),
            TimeUnit.MILLISECONDS
        )
    }

    override fun runAsync(asyncTask: () -> Unit): Future<Unit> =
        CompletableFuture.supplyAsync(Supplier { asyncTask() }, executor)
}
