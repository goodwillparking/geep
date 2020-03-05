package com.github.goodwillparking.geep

import org.junit.jupiter.api.Test
import java.time.Duration

class AsyncExecutionTest {

    @Test
    fun `a state can run some async execution`() = AsyncTestHarness()
        .run {
        stateMachine.handleMessage(
            Stay()
                .async(ExecuteAsync { "e1" }))
        s1.assertEvents()
        async.fireAsync()
        s1.assertEvents("e1")
    }

    @Test
    fun `a state can handle messages while waiting for async execution`() = AsyncTestHarness()
        .run {
        stateMachine.handleMessage(
            Stay()
                .async(ExecuteAsync { "e1" }))
        s1.assertEvents()

        stateMachine.handleMessage("e2")
        s1.assertEvents("e2")

        async.fireAsync()
        s1.assertEvents("e2", "e1")
    }

    @Test
    fun `the state that started the async execution will receive the result`() = AsyncTestHarness()
        .run {
        val s2 = TestPrimaryState("s2")
        stateMachine.handleMessage(Start(s2))
        stateMachine.assertStack(s1, s2)

        val s3 = TestPrimaryState("s3")
        stateMachine.handleMessage(Start(s3))
        stateMachine.assertStack(s1, s2, s3)

        stateMachine.handleMessage(
            Stay()
                .async(ExecuteAsync { "e1" }), index = 1)
        s1.assertEvents()
        s2.assertEvents()
        s3.assertEvents()

        stateMachine.handleMessage("e2")
        s1.assertEvents()
        s2.assertEvents()
        s3.assertEvents("e2")

        async.fireAsync()
        s1.assertEvents()
        s2.assertEvents("e1")
        s3.assertEvents("e2")
    }

    @Test
    fun `a failed async execution should send a Failure message back to the caller`() = AsyncTestHarness()
        .run {
        val exception = RuntimeException("boom")
        stateMachine.handleMessage(
            Stay()
                .async(ExecuteAsync { throw exception }))
        s1.assertEvents()
        async.fireAsync()
        s1.assertEvents(Failure(exception))
    }

    @Test
    fun `a custom failure mapper can be used when running async tasks`() = AsyncTestHarness()
        .run {
        stateMachine.handleMessage(
            Stay()
                .async(ExecuteAsync {
                    throw RuntimeException("boom")
                }.onFailure { "fallback" }))
        s1.assertEvents()
        async.fireAsync()
        s1.assertEvents("fallback")
    }

    @Test
    fun `when a custom failure mapper fails, send a Failure back to the caller`() = AsyncTestHarness()
        .run {
        val exception = RuntimeException("boom")
        stateMachine.handleMessage(Stay().async(
            ExecuteAsync { throw exception }.onFailure { throw RuntimeException("second boom") }))
        s1.assertEvents()
        async.fireAsync()
        s1.assertEvents(Failure(exception))
    }

    @Test
    fun `a state can schedule multiple async executions`() = AsyncTestHarness()
        .run {
        stateMachine.handleMessage(Stay().async(
            ExecuteAsync { "e1" },
            ExecuteAsync { "e2" }))
        stateMachine.handleMessage(
            Stay()
                .async(ExecuteAsync { "e3" }))
        s1.assertEvents()

        async.fireAsync(1)
        s1.assertEvents("e2")

        async.fireAsync(1)
        s1.assertEvents("e2", "e3")

        async.fireAsync(0)
        s1.assertEvents("e2", "e3", "e1")
    }

    @Test
    fun `a state can schedule an async execution and timer at the same time`() = AsyncTestHarness()
        .run {
        stateMachine.handleMessage(
            Stay().async(
                ExecuteAsync { "e1" },
                SetSingleTimer("t1", Duration.ZERO, "e2")
            ))
        s1.assertEvents()

        async.fireTimer("t1")
        s1.assertEvents("e2")

        async.fireAsync()
        s1.assertEvents("e2", "e1")
    }


    @Test
    fun `a state's aux state can handle the async result`() {
        val aux = TestAuxiliaryState("a1")
        val primary = TestPrimaryState(
            "s1",
            interceptedType = Int::class.javaObjectType,
            auxiliaryState = aux
        )

        AsyncTestHarness(primary).run {
            stateMachine.handleMessage(
                Stay()
                    .async(ExecuteAsync { "e1" }))
            stateMachine.handleMessage(
                Stay()
                    .async(ExecuteAsync { 1 }))

            primary.assertEvents()
            aux.assertEvents()

            async.fireAsync()
            primary.assertEvents()
            aux.assertEvents("e1")

            async.fireAsync()
            primary.assertEvents(1)
            aux.assertEvents("e1")
        }
    }

    @Test
    fun `an aux state's parent states should not handle its async results`() {
        val aux1 = TestAuxiliaryState("a1")
        val aux2 = TestAuxiliaryState("a2", auxiliaryState = aux1)
        val primary = TestPrimaryState("s1", auxiliaryState = aux2)

        AsyncTestHarness(primary).run {
            stateMachine.handleMessage(
                TargetedNext(
                    "a1",
                    Stay()
                        .async(ExecuteAsync { "e1" })
                )
            )

            primary.assertEvents()
            aux2.assertEvents()
            aux1.assertEvents()

            async.fireAsync()
            primary.assertEvents()
            aux2.assertEvents()
            aux1.assertEvents("e1")
        }
    }
}

