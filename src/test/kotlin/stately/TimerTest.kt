package stately

import org.junit.Test
import java.time.Duration
import java.util.concurrent.CancellationException

class TimerTest {

    @Test
    fun `a state should be able to set a single timer`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO,"e1")))
        async.fireTimer("t1")
        s1.assertEvents("e1")
    }

    @Test
    fun `a state should be able to set a periodic timer`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(SetPeriodicTimer("t1", Duration.ZERO,"e1")))
        repeat(10) { i ->
            async.fireTimer("t1")
            val events = generateSequence { "e1" }.take(i + 1).toList()
            s1.assertEvents(*events.toTypedArray())
        }
    }

    @Test
    fun `a single timer should not fire if it has been canceled`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO,"e1")))
        overseer.handleMessage(Stay().async(CancelTimer("t1")))
        expectException<CancellationException> { async.fireTimer("t1") }
        s1.assertEvents()
    }

    @Test
    fun `a periodic timer should not fire if it has been canceled`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(SetPeriodicTimer("t1", Duration.ZERO,"e1")))
        overseer.handleMessage(Stay().async(CancelTimer("t1")))
        expectException<CancellationException> { async.fireTimer("t1") }
        s1.assertEvents()
    }

    @Test
    fun `a timer should override an existing timer`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO,"e1")))
        overseer.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO,"e2")))
        async.fireTimer("t1")
        s1.assertEvents("e2")
    }

    @Test
    fun `a timer should not override an existing timer if it is passively set`() = AsyncTestHarness().run {
        overseer.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO,"e1")))
        overseer.handleMessage(Stay().async(SetSingleTimer("t1", Duration.ZERO,"e2", true)))
        async.fireTimer("t1")
        s1.assertEvents("e1")
    }

    private class AsyncTestHarness() {
        val async = TestAsyncContext()
        val s1 = TestState("1")
        val overseer = Overseer(s1, async)

        init {
            overseer.assertStack(s1)
            s1.assertCounts(1, 0, 1, 0)
        }

        fun run(test: AsyncTestHarness.() -> Unit) = test()
    }
}

