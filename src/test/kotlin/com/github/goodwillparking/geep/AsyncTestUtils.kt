package com.github.goodwillparking.geep

import org.junit.Assert
import java.util.NoSuchElementException
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TestAsyncContext : AsyncContext {

    private val timers = mutableMapOf<Any, ScheduledTimer>()

    private val asyncExecutions = mutableListOf<Future<Unit>>()

    override fun setSingleTimer(timer: SetSingleTimer, messageHandler: (key: Any, message: Any) -> Unit) =
        setTimer(timer, messageHandler, false)

    override fun setPeriodicTimer(timer: SetPeriodicTimer, messageHandler: (key: Any, message: Any) -> Unit) =
        setTimer(timer, messageHandler, true)

    override fun runAsync(asyncTask: () -> Unit): Future<Unit> {
        val future = TestFuture { asyncTask() }
        asyncExecutions.add(future)
        return future
    }

    fun fireTimer(key: Any) {
        timers.filterValues { !it.future.isDone }
        timers[key]
            ?.apply {
                if (periodic) {
                    future.fire()
                } else {
                    future.get()
                }
            }
            ?: Assert.fail("Timer $key is not scheduled. It may have already been completed or canceled.")
    }

    fun fireAsync(index: Int = 0) {
        asyncExecutions.getOrNull(index)?.get()
            ?: throw AssertionError("There are no pending async executions at index $index.")
        asyncExecutions.removeAt(index)
    }

    private fun setTimer(
        timer: SetTimer,
        messageHandler: (key: Any, message: Any) -> Unit,
        periodic: Boolean
    ): TestFuture<*> {
        val future = TestFuture { messageHandler(timer.key, timer.message) }
        timers[timer.key] = ScheduledTimer(future, periodic)
        return future
    }

    private data class ScheduledTimer(val future: TestFuture<*>, val periodic: Boolean)
}

class TestFuture<V> private constructor(
    private val supplier: () -> V,
    private var status: Status,
    private var value: Option<V>
) : Future<V> {

    private val lock = ReentrantLock()

    constructor( supplier: () -> V) : this(supplier,
        Status.PENDING,
        Option.None()
    )

    override fun isDone() = lock.withLock { status != Status.PENDING }

    override fun get(): V = lock.withLock {
        value = Option.Some(fire())
        status = Status.DONE
        value.get()
    }

    override fun get(timeout: Long, unit: TimeUnit): V = get()

    override fun cancel(mayInterruptIfRunning: Boolean) = lock.withLock {
        if (status == Status.DONE) {
            false
        } else {
            status = Status.CANCELED
            true
        }
    }

    override fun isCancelled() = lock.withLock { status == Status.CANCELED }

    override fun toString() = "TestFuture(supplier=$supplier, status=$status, value=$value)"

    /**
     * Runs the future without completing it.
     */
    fun fire() = when (status) {
        Status.PENDING -> supplier()
        Status.CANCELED -> throw CancellationException()
        Status.DONE -> value.get()
    }

    private enum class Status {
        PENDING,
        DONE,
        CANCELED
    }

    private sealed class Option<V> {

        abstract fun get(): V

        data class Some<V>(val value: V) : Option<V>() {
            override fun get(): V = value
        }

        class None<V>() : Option<V>() {
            override fun get(): V = throw NoSuchElementException("No value present")
        }
    }
}

class AsyncTestHarness(val s1: TestPrimaryState = TestPrimaryState("1")) {
    val async = TestAsyncContext()
    val stateMachine = StateMachine(s1, async)

    init {
        stateMachine.assertStack(s1)
        s1.assertCounts(1, 0, 1, 0)
    }

    fun run(test: AsyncTestHarness.() -> Unit) = test()
}
