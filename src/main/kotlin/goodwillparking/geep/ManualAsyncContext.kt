package goodwillparking.geep

import java.lang.Integer.max
import java.lang.Integer.min
import java.time.Duration
import java.time.Instant
import java.util.PriorityQueue
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class ManualAsyncContext(startTime: Instant = Instant.now()) : AsyncContext {

    private val timers: Queue<TimerInfo> = PriorityQueue { a, b -> a.fireTime.compareTo(b.fireTime) }

    private var asyncTasks = emptyList<AsyncInfo>()

    val asyncTaskCount: Int
        get() = asyncTasks.size

    var currentTime: Instant = startTime
        private set(new) {
            field = new
            fireTimers()
        }

    override fun setSingleTimer(timer: SetSingleTimer, messageHandler: (key: Any, message: Any) -> Unit) =
        trackTimer(timer, messageHandler)

    override fun setPeriodicTimer(timer: SetPeriodicTimer, messageHandler: (key: Any, message: Any) -> Unit) =
        trackTimer(timer, messageHandler)

    override fun runAsync(asyncTask: () -> Unit): Future<Unit> {
        val future = CompletableFuture<Unit>()
        asyncTasks = asyncTasks + AsyncInfo(future, asyncTask)
        return future
    }

    fun increment(interval: Duration) {
        if (!interval.isNegative) currentTime += interval
    }

    fun fireAllAsyncTasks() {
        while (asyncTasks.isNotEmpty()) fireAsyncTasks(1)
    }

    fun fireCurrentAsyncTasks() = fireAsyncTasks(asyncTaskCount)

    fun fireAsyncTasks(count: Int) {
        val adjusted = min(max(count, 0), asyncTaskCount)
        asyncTasks.take(adjusted).forEach { it.task() }
        asyncTasks = asyncTasks.drop(adjusted)
    }

    private fun trackTimer(timer: SetTimer, messageHandler: (key: Any, message: Any) -> Unit): Future<*> {
        val future = CompletableFuture<Any>()
        val delay = when (timer) {
            is SetPeriodicTimer -> timer.initialDelay
            else -> timer.duration
        }
        timers.add(TimerInfo(timer, future, currentTime + delay, messageHandler))
        return future
    }

    private fun fireTimers() {
        while (timers.peek()?.fireTime?.let { it.isBefore(currentTime) || it == currentTime } == true) {
            val info = timers.poll()
            if (!info.future.isCancelled) {
                fireTimer(info)
            }
        }
    }

    private fun fireTimer(info: TimerInfo) {
        info.handler(info.update.key, info.update.message)

        if (info.update is SetPeriodicTimer && !info.future.isCancelled) {
            val newPeriodic = info.copy(fireTime = info.fireTime + info.update.duration)
            timers.add(newPeriodic)
        }
    }

    private data class TimerInfo(
        val update: SetTimer,
        val future: Future<*>,
        val fireTime: Instant,
        val handler: (key: Any, message: Any) -> Unit
    )

    private data class AsyncInfo(
        val future: Future<*>,
        val task: () -> Unit
    )
}
