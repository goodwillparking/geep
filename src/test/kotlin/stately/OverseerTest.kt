package stately

import org.junit.Test
import java.util.Locale

class OverseerTest {

    @Test
    fun `states should be able to modify the stack when they receive messages`() {
        val s1 = TestState("1")
        val overseer = Overseer(s1)
        overseer.assertStack(s1)
        s1.assertCounts(1, 0, 1, 0)

        val s2 = TestState("2")
        overseer.handleMessage(Goto(s2))
        overseer.assertStack(s2)
        s1.assertCounts(1, 1,1, 1)
        s2.assertCounts(1, 0,1, 0)

        overseer.handleMessage(Start(s1))
        overseer.assertStack(s2, s1)
        s1.assertCounts(2, 1, 2, 1)
        s2.assertCounts(1, 0,1, 1)

        val s3 = TestState("3")
        overseer.handleMessage(Start(s3))
        overseer.assertStack(s2, s1, s3)
        s1.assertCounts(2, 1, 2, 2)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        overseer.handleMessage(Stay)
        overseer.assertStack(s2, s1, s3)
        s1.assertCounts(2, 1, 2, 2)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        overseer.handleMessage(Done)
        overseer.assertStack(s2, s1)
        s1.assertCounts(2, 1, 3, 2)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 1, 1, 1)

        overseer.handleMessage(Clear(s3))
        overseer.assertStack(s3)
        s1.assertCounts(2, 2, 3, 3)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(2, 1, 2, 1)

        overseer.handleMessage(Done)
        overseer.assertStack()
        s1.assertCounts(2, 2, 3, 3)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(2, 2, 2, 2)
    }

    @Test
    fun `states should be able to modify the stack when they gain focus`() {
        val s1 = TestState("1")
        val s2 = TestState("2", onFocusGained = Goto(s1))
        val s3 = TestState("3", onFocusGained = Start(s2))

        val overseer = Overseer(s3)
        overseer.assertStack(s3, s1)
        s1.assertCounts(1, 0, 1, 0)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 1)

        overseer.handleMessage(Goto(s2))
        overseer.assertStack(s3, s1)
        s1.assertCounts(2, 1, 2, 1)
        s2.assertCounts(2, 2, 2, 2)
        s3.assertCounts(1, 0, 1, 1)

        val s4 = TestState("4", onFocusGained = Done)

        overseer.handleMessage(Start(s4))
        overseer.assertStack(s3, s1)
        s1.assertCounts(2, 1, 3, 2)
        s2.assertCounts(2, 2, 2, 2)
        s3.assertCounts(1, 0, 1, 1)
        s4.assertCounts(1, 1, 1, 1)

        val s5 = TestState("5", onFocusGained = Start(s3))
        val s6 = TestState("6", onFocusGained = Clear(s5))

        overseer.handleMessage(Goto(s6))
        overseer.assertStack(s5, s3, s1)
        s1.assertCounts(3, 2, 4, 3)
        s2.assertCounts(3, 3, 3, 3)
        s3.assertCounts(2, 1, 2, 2)
        s4.assertCounts(1, 1, 1, 1)
        s5.assertCounts(1, 0, 1, 1)
        s6.assertCounts(1, 1, 1, 1)
    }

    @Test
    fun `child states should be able to handle messages that weren't handled by their parents`() {
        val s1 = TestState("1", interceptedType = String::class.java)
        val s2 = TestParentState("2", Integer::class.java, s1)
        val s3 = TestParentState("3", java.lang.Double::class.java, s2)

        val overseer = Overseer(s3)
        s1.assertCounts(0, 0, 0, 0)
        s2.assertCounts(0, 0, 0, 0)
        s3.assertCounts(1, 0, 1, 0)

        overseer.handleMessage(1.0)
        s1.assertEvents()
        s2.assertEvents()
        s3.assertEvents(1.0)

        overseer.handleMessage(1)
        s1.assertEvents()
        s2.assertEvents(1)
        s3.assertEvents(1.0)

        overseer.handleMessage("1")
        s1.assertEvents("1")
        s2.assertEvents(1)
        s3.assertEvents(1.0)

        overseer.handleMessage(Locale.CANADA)
        s1.assertEvents("1")
        s2.assertEvents(1)
        s3.assertEvents(1.0)

        overseer.handleMessage("2")
        s1.assertEvents("1", "2")
        s2.assertEvents(1)
        s3.assertEvents(1.0)

        overseer.handleMessage(2)
        s1.assertEvents("1", "2")
        s2.assertEvents(1, 2)
        s3.assertEvents(1.0)

        overseer.handleMessage(2.0)
        s1.assertEvents("1", "2")
        s2.assertEvents(1, 2)
        s3.assertEvents(1.0, 2.0)

        s1.assertCounts(0, 0, 0, 0)
        s2.assertCounts(0, 0, 0, 0)
        s3.assertCounts(1, 0, 1, 0)
    }

    @Test
    fun `states should be able to modify the stack when they start`() {
        val s1 = TestState("1")
        val s2 = TestState("2", onStart = Goto(s1))
        val s3 = TestState("3", onStart = Start(s2))

        val overseer = Overseer(s3)
        overseer.assertStack(s3, s1)
        s1.assertCounts(1, 0, 1, 0)
        s2.assertCounts(1, 1, 0, 0)
        s3.assertCounts(1, 0, 0, 0)

        overseer.handleMessage(Goto(s2))
        overseer.assertStack(s3, s1)
        s1.assertCounts(2, 1, 2, 1)
        s2.assertCounts(2, 2, 0, 0)
        s3.assertCounts(1, 0, 0, 0)

        val s4 = TestState("4", onStart = Done)

        overseer.handleMessage(Start(s4))
        overseer.assertStack(s3, s1)
        s1.assertCounts(2, 1, 3, 2)
        s2.assertCounts(2, 2, 0, 0)
        s3.assertCounts(1, 0, 0, 0)
        s4.assertCounts(1, 1, 0, 0)

        val s5 = TestState("5", onStart = Start(s3))
        val s6 = TestState("6", onStart = Clear(s5))

        overseer.handleMessage(Goto(s6))
        overseer.assertStack(s5, s3, s1)
        s1.assertCounts(3, 2, 4, 3)
        s2.assertCounts(3, 3, 0, 0)
        s3.assertCounts(2, 1, 0, 0)
        s4.assertCounts(1, 1, 0, 0)
        s5.assertCounts(1, 0, 0, 0)
        s6.assertCounts(1, 1, 0, 0)
    }

    @Test
    fun `a state should lose and gain focus if it does a goto to itself`() {
        val s1 = TestState("1")

        val overseer = Overseer(s1)
        overseer.assertStack(s1)
        s1.assertCounts(1, 0, 1, 0)

        overseer.handleMessage(Goto(s1))
        overseer.assertStack(s1)
        s1.assertCounts(2, 1, 2, 1)
    }
}
