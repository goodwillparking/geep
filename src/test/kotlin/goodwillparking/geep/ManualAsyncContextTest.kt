package goodwillparking.geep

import org.junit.Test

class ManualAsyncContextTest {

    @Test
    fun `it should fire single timers`() = manualHarness().run {
        stateMachine.handleMessage(Stay().async(SetSingleTimer("t1", 1.ms, "e1")))
        s1.assertEvents()

        async.increment(2.ms)
        s1.assertEvents("e1")

        async.increment(1000.ms)
        s1.assertEvents("e1")
    }

    @Test
    fun `timers with duration of zero should not immediately fire`() = manualHarness().run {
        stateMachine.handleMessage(Stay().async(SetSingleTimer("t1", 0.ms, "e1")))
        s1.assertEvents()

        // any increment, even if it is 0 should trigger the 0 duration timer
        async.increment(0.ms)
        s1.assertEvents("e1")
    }

    @Test
    fun `periodic timers should fire a number of times proportional to their period`() = manualHarness().run {
        val period = 5.ms
        stateMachine.handleMessage(Stay().async(SetPeriodicTimer("t1", period, "e1")))
        s1.assertEvents()

        // any increment, even if it is 0 should trigger the 0 duration initial delay
        async.increment(0.ms)
        s1.assertEvents("e1")

        async.increment(period)
        s1.assertEvents("e1", "e1")

        async.increment(period.multipliedBy(3))
        s1.assertEvents("e1", "e1", "e1", "e1", "e1")
    }

    @Test
    fun `multiple timers should fire together`() = manualHarness().run {
        stateMachine.handleMessage(
            Stay().async(
                SetPeriodicTimer("t1", 2.ms, "e1", initialDelay = 2.ms),
                SetSingleTimer("t2", 3.ms, "e2")
            )
        )

        s1.assertEvents()

        async.increment(6.ms)
        s1.assertEvents("e1", "e2", "e1", "e1")
    }

    @Test
    fun `the time cannot be incremented with a negative amount`() = manualHarness().run {
        stateMachine.handleMessage(Stay().async(SetSingleTimer("t1", 2.ms, "e1")))
        s1.assertEvents()

        async.increment((-2).ms)
        s1.assertEvents()

        async.increment(2.ms)
        s1.assertEvents("e1")
    }

    @Test
    fun `canceled timers should not fire`() = manualHarness().run {
        stateMachine.handleMessage(
            Stay().async(
                SetSingleTimer("t1", 2.ms, "e1"),
                SetPeriodicTimer("t2", 2.ms, "e2")
            )
        )
        s1.assertEvents()

        async.increment(1.ms)
        s1.assertEvents("e2")

        stateMachine.handleMessage(Stay().async(CancelTimer("t1"), CancelTimer("t2")))

        async.increment(2.ms)
        s1.assertEvents("e2")
    }

    @Test
    fun `a state can run some async execution`() = manualHarness().run {
        stateMachine.handleMessage(Stay().async(ExecuteAsync { "e1" }))
        s1.assertEvents()
        async.fireCurrentAsyncTasks()
        s1.assertEvents("e1")
    }

    @Test
    fun `a failed async execution should send a Failure message back to the caller`() = manualHarness().run {
        val exception = RuntimeException("boom")
        stateMachine.handleMessage(Stay().async(ExecuteAsync { throw exception }))
        s1.assertEvents()
        async.fireCurrentAsyncTasks()
        s1.assertEvents(Failure(exception))
    }

    @Test
    fun `a state can schedule multiple async executions`() = manualHarness().run {
        stateMachine.handleMessage(
            Stay().async(
                ExecuteAsync { "e1" },
                ExecuteAsync { "e2" })
        )
        stateMachine.handleMessage(Stay().async(ExecuteAsync { "e3" }))
        stateMachine.handleMessage(
            Stay().async(
                ExecuteAsync { "e4" },
                ExecuteAsync { "e5" })
        )
        s1.assertEvents()

        async.fireAsyncTasks(1)
        s1.assertEvents("e1")

        async.fireAsyncTasks(1)
        s1.assertEvents("e1", "e2")

        async.fireAsyncTasks(1)
        s1.assertEvents("e1", "e2", "e3")

        async.fireAsyncTasks(2)
        s1.assertEvents("e1", "e2", "e3", "e4", "e5")
    }

    @Test
    fun `a state can schedule an async execution and timer at the same time`() = manualHarness().run {
        stateMachine.handleMessage(
            Stay().async(
                ExecuteAsync { "e1" },
                SetSingleTimer("t1", 1.ms, "e2")
            ))
        s1.assertEvents()

        async.increment(1.ms)
        s1.assertEvents("e2")

        async.fireAllAsyncTasks()
        s1.assertEvents("e2", "e1")
    }

    @Test
    fun `either the current tasks can be fired, or current and future tasks`() = manualHarness().run {
        fun oneThenAnother(event: String) = Stay().async(ExecuteAsync { Stay().async(ExecuteAsync { event }) })
        stateMachine.handleMessage(oneThenAnother("e1"))
        s1.assertEvents()

        async.fireAllAsyncTasks()
        s1.assertEvents("e1")

        stateMachine.handleMessage(oneThenAnother("e2"))
        async.fireCurrentAsyncTasks() // the nested async task shouldn't fire
        s1.assertEvents("e1")

        async.fireCurrentAsyncTasks()
        s1.assertEvents("e1", "e2")

        stateMachine.handleMessage(oneThenAnother("e3"))
        // if more than the current number of tasks is specified, then all the current tasks should be fired
        async.fireAsyncTasks(1000)
        s1.assertEvents("e1", "e2")

        async.fireAsyncTasks(1000)
        s1.assertEvents("e1", "e2", "e3")
    }

    private fun manualHarness() = AsyncTestHarness(async = ManualAsyncContext())
}
