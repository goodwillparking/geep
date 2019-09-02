package stately

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

interface AsyncContext {

    fun cancelTimer(cancelTimer: CancelTimer) // Need this?

    fun setSingleTimer(timer: SetSingleTimer, messageHandler: (key: Any, message: Any) -> Unit): Future<*>

    fun setPeriodicTimer(timer: SetPeriodicTimer, messageHandler: (key: Any, message: Any) -> Unit): Future<*>
}

class JavaAsyncContext(
    val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
) : AsyncContext {

    override fun cancelTimer(cancelTimer: CancelTimer) {
        TODO("not implemented")
    }

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

}
