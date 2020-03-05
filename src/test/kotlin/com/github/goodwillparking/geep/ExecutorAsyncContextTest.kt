package com.github.goodwillparking.geep

import org.junit.jupiter.api.Test

class ExecutorAsyncContextTest {

    @Test
    fun `it should schedule single timers`() = executorHarness().run {
        stateMachine.handleMessage(
            Stay()
                .async(SetSingleTimer("t1", 1.ms, "e1")))
        awaitCondition { s1.events.isNotEmpty() }
        s1.assertEvents("e1")
    }

    @Test
    fun `it should schedule periodic timers`() = executorHarness().run {
        val period = 20.ms
        stateMachine.handleMessage(
            Stay()
                .async(SetPeriodicTimer("t1", period, "e1")))

        awaitCondition(delay = period.dividedBy(4)) { s1.events.isNotEmpty() }
        s1.assertEvents("e1")

        awaitCondition(delay = period.dividedBy(4)) { s1.events.size > 1 }
        s1.assertEvents("e1", "e1")

        awaitCondition(delay = period.dividedBy(4)) { s1.events.size > 2 }
        s1.assertEvents("e1", "e1", "e1")

        async.executor.shutdown() // shut it down to keep the periodic timer from spamming logs.
    }

    @Test
    fun `it should schedule async tasks`() = executorHarness().run {
        stateMachine.handleMessage(
            Stay()
                .async(ExecuteAsync { "e1" }))
        awaitCondition { s1.events.isNotEmpty() }
        s1.assertEvents("e1")
    }

    private fun executorHarness() =
        AsyncTestHarness(async = ExecutorAsyncContext())
}
