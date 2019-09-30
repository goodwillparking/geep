package com.github.goodwillparking.geep

import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
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

        overseer.handleMessage(Stay())
        overseer.assertStack(s2, s1, s3)
        s1.assertCounts(2, 1, 2, 2)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        overseer.handleMessage(Done())
        overseer.assertStack(s2, s1)
        s1.assertCounts(2, 1, 3, 2)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 1, 1, 1)

        overseer.handleMessage(Clear(s3))
        overseer.assertStack(s3)
        s1.assertCounts(2, 2, 3, 3)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(2, 1, 2, 1)

        overseer.handleMessage(Done())
        overseer.assertStack()
        s1.assertCounts(2, 2, 3, 3)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(2, 2, 2, 2)
    }

    @Test
    fun `states should be able to modify the stack when they gain focus`() {
        val s1 = TestState("1")
        val s2 =
            TestState("2", onFocusGained = Goto(s1))
        val s3 =
            TestState("3", onFocusGained = Start(s2))

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

        val s4 = TestState("4", onFocusGained = Done())

        overseer.handleMessage(Start(s4))
        overseer.assertStack(s3, s1)
        s1.assertCounts(2, 1, 3, 2)
        s2.assertCounts(2, 2, 2, 2)
        s3.assertCounts(1, 0, 1, 1)
        s4.assertCounts(1, 1, 1, 1)

        val s5 =
            TestState("5", onFocusGained = Start(s3))
        val s6 =
            TestState("6", onFocusGained = Clear(s5))

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
        val s1 = TestChildState("1", interceptedType = String::class.java)
        val s2 =
            TestMiddleState("2", interceptedType = Integer::class.java, childState = s1)
        val s3 = TestParentState("3", Double::class.javaObjectType, s2)

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

        val s4 = TestState("4", onStart = Done())

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

    @Test
    fun `transition handlers should be called in the order of onEnd, onStart, onFocusLost, onFocusGained`() {

        fun mockState() = mock(State::class.java).also {
            `when`(it.receive).thenReturn(ReceiveBuilder().match { n: Next -> n })
            `when`(it.onStart()).thenReturn(Stay())
            `when`(it.onFocusGained()).thenReturn(Stay())
        }

        val s1 = mockState()
        val s2 = mockState()
        val inOrder = inOrder(s1, s2)

        val overseer = Overseer(s1)
        overseer.assertStack(s1)

        inOrder.verify(s1).onStart()
        inOrder.verify(s1).onFocusGained()

        overseer.handleMessage(Goto(s2))
        overseer.assertStack(s2)

        inOrder.verify(s1).onEnd()
        inOrder.verify(s2).onStart()
        inOrder.verify(s1).onFocusLost()
        inOrder.verify(s2).onFocusGained()

        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `a state can be started by calling start on the overseer`() {

        val overseer = Overseer()
        overseer.assertStack()

        val s1 = TestState("1")
        overseer.start(s1)
        overseer.assertStack(s1)
        s1.assertCounts(1, 0, 1, 0)

        val s2 = TestState("2")
        overseer.start(s2)
        overseer.assertStack(s1, s2)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 0, 1, 0)
    }

    @Test
    fun `states should be able to modify the stack in the middle of the stack`() {
        val s1 = TestState("1")
        val s2 = TestState("2")
        val s3 = TestState("3")

        val overseer = Overseer(s1)
        overseer.start(s2)
        overseer.start(s3)
        overseer.assertStack(s1, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        overseer.handleMessage(Stay(), 1)
        overseer.assertStack(s1, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        val s4 = TestState("4")
        overseer.handleMessage(Goto(s4), 1)
        overseer.assertStack(s1, s4, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 0, 0, 0)

        overseer.handleMessage(Start(s2), 1)
        overseer.assertStack(s1, s4, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(2, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 0, 0, 0)

        overseer.handleMessage(Done(), 2)
        overseer.assertStack(s1, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(2, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 1, 0, 0)

        overseer.handleMessage(AbsoluteClear(s4), 1)
        overseer.assertStack(s4)
        s1.assertCounts(1, 1, 1, 1)
        s2.assertCounts(2, 2, 1, 1)
        s3.assertCounts(1, 1, 1, 1)
        s4.assertCounts(2, 1, 1, 0)
    }

    @Test
    fun `the publicly accessible stack should be a copy of the overseer's stack`() {
        val s1 = TestState("1")
        val s2 = TestState("2")
        val s3 = TestState("3")

        val overseer = Overseer(s1)
        overseer.start(s2)
        overseer.start(s3)
        overseer.assertStack(s1, s2, s3)

        val stack = overseer.stack() as MutableList
        stack.add(s1)
        overseer.assertStack(s1, s2, s3)
    }

    @Test
    fun `test relative clears and absolute starts`() {
        val s1 = TestState("1")
        val s2 = TestState("2")
        val s3 = TestState("3")

        val overseer = Overseer(s1)
        overseer.start(s2)
        overseer.start(s3)
        overseer.assertStack(s1, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        val s4 = TestState("4")
        overseer.handleMessage(
            Clear(
                s4,
                RelativeRange.Below(false)
            )
        )
        overseer.assertStack(s4, s3)
        s1.assertCounts(1, 1, 1, 1)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 0, 0, 0)

        overseer.handleMessage(
            AbsoluteStart(
                s1,
                AbsolutePosition.Bottom
            )
        )
        overseer.assertStack(s1, s4, s3)
        s1.assertCounts(2, 1, 1, 1)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 0, 0, 0)

        overseer.handleMessage(
            Clear(
                s2,
                RelativeRange.Above(false)
            ), 2)
        overseer.assertStack(s1, s2)
        s1.assertCounts(2, 1, 1, 1)
        s2.assertCounts(2, 1, 2, 1)
        s3.assertCounts(1, 1, 1, 1)
        s4.assertCounts(1, 1, 0, 0)

        overseer.handleMessage(
            AbsoluteStart(
                s4,
                AbsolutePosition.Top
            ), 1)
        overseer.assertStack(s1, s2, s4)
        s1.assertCounts(2, 1, 1, 1)
        s2.assertCounts(2, 1, 2, 2)
        s3.assertCounts(1, 1, 1, 1)
        s4.assertCounts(2, 1, 1, 0)

        overseer.handleMessage(
            Start(
                s3,
                RelativePosition.Below
            )
        )
        overseer.assertStack(s1, s2, s3, s4)
        s1.assertCounts(2, 1, 1, 1)
        s2.assertCounts(2, 1, 2, 2)
        s3.assertCounts(2, 1, 1, 1)
        s4.assertCounts(2, 1, 1, 0)

        val s5 = TestState("5")
        overseer.handleMessage(
            Clear(
                s5,
                RelativeRange.Below(true)
            ), 1)
        overseer.assertStack(s5, s4)
        s1.assertCounts(2, 2, 1, 1)
        s2.assertCounts(2, 2, 2, 2)
        s3.assertCounts(2, 2, 1, 1)
        s4.assertCounts(2, 1, 1, 0)

        overseer.handleMessage(
            Clear(
                s1,
                RelativeRange.Above(true)
            )
        )
        overseer.assertStack(s5, s1)
        s1.assertCounts(3, 2, 2, 1)
        s2.assertCounts(2, 2, 2, 2)
        s3.assertCounts(2, 2, 1, 1)
        s4.assertCounts(2, 2, 1, 1)

        overseer.handleMessage(
            Clear(
                s2,
                RelativeRange.Above(false)
            )
        )
        overseer.assertStack(s5, s1, s2)
        s1.assertCounts(3, 2, 2, 2)
        s2.assertCounts(3, 2, 3, 2)
        s3.assertCounts(2, 2, 1, 1)
        s4.assertCounts(2, 2, 1, 1)

        overseer.handleMessage(
            Clear(
                s3,
                RelativeRange.Below(false)
            ), 2)
        overseer.assertStack(s3, s5, s1, s2)
        s1.assertCounts(3, 2, 2, 2)
        s2.assertCounts(3, 2, 3, 2)
        s3.assertCounts(3, 2, 1, 1)
        s4.assertCounts(2, 2, 1, 1)
    }

}
