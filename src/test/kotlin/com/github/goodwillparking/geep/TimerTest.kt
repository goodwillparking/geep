package com.github.goodwillparking.geep

import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.Duration
import java.util.concurrent.CancellationException

class TimerTest {

    @Test
    fun `a state should be able to set a single timer`() = AsyncTestHarness().run {
        stateMachine.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO, "e1")))
        async.fireTimer("t1")
        s1.assertEvents("e1")
    }

    @Test
    fun `a state should be able to set a periodic timer`() = AsyncTestHarness().run {
        stateMachine.handleMessage(Stay().async(SetPeriodicTimer("t1", Duration.ZERO, "e1")))
        repeat(10) { i ->
            async.fireTimer("t1")
            val events = generateSequence { "e1" }.take(i + 1).toList()
            s1.assertEvents(*events.toTypedArray())
        }
    }

    @Test
    fun `a single timer should not fire if it has been canceled`() = AsyncTestHarness().run {
        stateMachine.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO, "e1")))
        stateMachine.handleMessage(Stay().async(CancelTimer("t1")))
        expectException<CancellationException> { async.fireTimer("t1") }
        s1.assertEvents()
    }

    @Test
    fun `a periodic timer should not fire if it has been canceled`() = AsyncTestHarness().run {
        stateMachine.handleMessage(Stay().async(SetPeriodicTimer("t1", Duration.ZERO, "e1")))
        stateMachine.handleMessage(Stay().async(CancelTimer("t1")))
        expectException<CancellationException> { async.fireTimer("t1") }
        s1.assertEvents()
    }

    @Test
    fun `a timer should override an existing timer`() = AsyncTestHarness().run {
        stateMachine.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO, "e1")))
        stateMachine.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO, "e2")))
        async.fireTimer("t1")
        s1.assertEvents("e2")
    }

    @Test
    fun `a timer should not override an existing timer if it is passively set`() = AsyncTestHarness().run {
        stateMachine.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO, "e1")))
        stateMachine.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO, "e2", true)))
        async.fireTimer("t1")
        s1.assertEvents("e1")
    }

    @Test
    fun `the next state should be processed even if the AsyncContext fails to schedule a timer`() {
        val async = mock(AsyncContext::class.java)
        val s1 = TestPrimaryState("1")
        val stateMachine = StateMachine(s1, async)
        stateMachine.assertStack(s1)
        s1.assertCounts(1, 0, 1, 0)

        `when`(async.setSingleTimer(
            anyObject(),
            anyObject()
        )).thenThrow(RuntimeException("No timer for you."))
        val s2 = TestPrimaryState("2")
        stateMachine.handleMessage(Start(s2).async(SetSingleTimer("t1", Duration.ZERO, "e1")))
        stateMachine.assertStack(s1, s2)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 0, 1, 0)
    }

    @Test
    fun `a state's timers should not fire if it leaves the stack`() = AsyncTestHarness().run {
        stateMachine.handleMessage(Stay().async(
            SetSingleTimer("t1", Duration.ZERO, "e1"),
            SetPeriodicTimer("t2", Duration.ZERO, "e2")))
        s1.assertEvents()

        val s2 = TestPrimaryState("2")
        stateMachine.handleMessage(AbsoluteClear(s2))
        s1.assertCounts(1, 1, 1, 1)
        s2.assertCounts(1, 0, 1, 0)

        async.fireTimer("t1")
        s1.assertEvents()
        async.fireTimer("t2")
        s1.assertEvents()
    }
}

