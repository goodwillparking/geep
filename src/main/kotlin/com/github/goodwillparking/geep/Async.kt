package com.github.goodwillparking.geep

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

/**
 * Interface used by the [StateMachine] to schedule async tasks and timers.
 *
 * **Note for synchronous implementations:** A task may not be executed in the same call that scheduled that task.
 * Doing so could cause unexpected behavior in the [StateMachine].
 */
interface AsyncContext {

    fun setSingleTimer(timer: SetSingleTimer, messageHandler: (key: Any, message: Any) -> Unit): Future<*>

    fun setPeriodicTimer(timer: SetPeriodicTimer, messageHandler: (key: Any, message: Any) -> Unit): Future<*>

    fun runAsync(asyncTask: () -> Unit): Future<Unit>
}

class ExecutorAsyncContext(
    val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(32)
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
