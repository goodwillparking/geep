package com.github.goodwillparking.geep

import org.junit.Test
import java.lang.RuntimeException
import java.time.Duration

class AsyncExecutionTest {

    @Test
    fun `a state can run some async execution`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(ExecuteAsync { "e1" }))
        s1.assertEvents()
        async.fireAsync()
        s1.assertEvents("e1")
    }

    @Test
    fun `a state can handle messages while waiting for async execution`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(ExecuteAsync { "e1" }))
        s1.assertEvents()

        overseer.handleMessage("e2")
        s1.assertEvents("e2")

        async.fireAsync()
        s1.assertEvents("e2", "e1")
    }

    @Test
    fun `the state that started the async execution will receive the result`() = AsyncTestHarness().run {
        val s2 = TestState("s2")
        overseer.handleMessage(Start(s2))
        overseer.assertStack(s1, s2)

        val s3 = TestState("s3")
        overseer.handleMessage(Start(s3))
        overseer.assertStack(s1, s2, s3)

        overseer.handleMessage(Stay().async(ExecuteAsync { "e1" }), index = 1)
        s1.assertEvents()
        s2.assertEvents()
        s3.assertEvents()

        overseer.handleMessage("e2")
        s1.assertEvents()
        s2.assertEvents()
        s3.assertEvents("e2")

        async.fireAsync()
        s1.assertEvents()
        s2.assertEvents("e1")
        s3.assertEvents("e2")
    }

    @Test
    fun `a failed async execution should send a Failure message back to the caller`() = AsyncTestHarness().run {
        val exception = RuntimeException("boom")
        overseer.handleMessage(Stay().async(ExecuteAsync { throw exception }))
        s1.assertEvents()
        async.fireAsync()
        s1.assertEvents(Failure(exception))
    }

    @Test
    fun `a custom failure mapper can be used when running async tasks`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(ExecuteAsync { throw RuntimeException("boom") }.onFailure { "fallback" }))
        s1.assertEvents()
        async.fireAsync()
        s1.assertEvents("fallback")
    }

    @Test
    fun `when a custom failure mapper fails, send a Failure back to the caller`() = AsyncTestHarness().run {
        val exception = RuntimeException("boom")
        overseer.handleMessage(Stay().async(
            ExecuteAsync { throw exception }.onFailure { throw RuntimeException("second boom") }))
        s1.assertEvents()
        async.fireAsync()
        s1.assertEvents(Failure(exception))
    }

    @Test
    fun `a state can schedule multiple async executions`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(ExecuteAsync { "e1" }, ExecuteAsync { "e2" }))
        overseer.handleMessage(Stay().async(ExecuteAsync { "e3" }))
        s1.assertEvents()

        async.fireAsync(1)
        s1.assertEvents("e2")

        async.fireAsync(1)
        s1.assertEvents("e2", "e3")

        async.fireAsync(0)
        s1.assertEvents("e2", "e3", "e1")
    }

    @Test
    fun `a state can schedule an async execution and timer at the same time`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(
            ExecuteAsync { "e1" },
            SetSingleTimer("t1", Duration.ZERO, "e2")))
        s1.assertEvents()

        async.fireTimer("t1")
        s1.assertEvents("e2")

        async.fireAsync()
        s1.assertEvents("e2", "e1")
    }
}

