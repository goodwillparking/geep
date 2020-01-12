package com.github.goodwillparking.geep

import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import java.util.Locale

class StateMachineTest {

    @Test
    fun `states should be able to modify the stack when they receive messages`() {
        val s1 = TestPrimaryState("1")
        val stateMachine = StateMachine(s1)
        stateMachine.assertStack(s1)
        s1.assertCounts(1, 0, 1, 0)

        val s2 = TestPrimaryState("2")
        stateMachine.handleMessage(Goto(s2))
        stateMachine.assertStack(s2)
        s1.assertCounts(1, 1,1, 1)
        s2.assertCounts(1, 0,1, 0)

        stateMachine.handleMessage(Start(s1))
        stateMachine.assertStack(s2, s1)
        s1.assertCounts(2, 1, 2, 1)
        s2.assertCounts(1, 0,1, 1)

        val s3 = TestPrimaryState("3")
        stateMachine.handleMessage(Start(s3))
        stateMachine.assertStack(s2, s1, s3)
        s1.assertCounts(2, 1, 2, 2)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        stateMachine.handleMessage(Stay())
        stateMachine.assertStack(s2, s1, s3)
        s1.assertCounts(2, 1, 2, 2)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        stateMachine.handleMessage(Done)
        stateMachine.assertStack(s2, s1)
        s1.assertCounts(2, 1, 3, 2)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 1, 1, 1)

        stateMachine.handleMessage(Clear(s3))
        stateMachine.assertStack(s3)
        s1.assertCounts(2, 2, 3, 3)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(2, 1, 2, 1)

        stateMachine.handleMessage(Done)
        stateMachine.assertStack()
        s1.assertCounts(2, 2, 3, 3)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(2, 2, 2, 2)
    }

    @Test
    fun `states should be able to modify the stack when they gain focus`() {
        val s1 = TestPrimaryState("1")
        val s2 =
            TestPrimaryState("2", onFocusGained = Goto(s1))
        val s3 =
            TestPrimaryState("3", onFocusGained = Start(s2))

        val stateMachine = StateMachine(s3)
        stateMachine.assertStack(s3, s1)
        s1.assertCounts(1, 0, 1, 0)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 1)

        stateMachine.handleMessage(Goto(s2))
        stateMachine.assertStack(s3, s1)
        s1.assertCounts(2, 1, 2, 1)
        s2.assertCounts(2, 2, 2, 2)
        s3.assertCounts(1, 0, 1, 1)

        val s4 = TestPrimaryState("4", onFocusGained = Done)

        stateMachine.handleMessage(Start(s4))
        stateMachine.assertStack(s3, s1)
        s1.assertCounts(2, 1, 3, 2)
        s2.assertCounts(2, 2, 2, 2)
        s3.assertCounts(1, 0, 1, 1)
        s4.assertCounts(1, 1, 1, 1)

        val s5 =
            TestPrimaryState("5", onFocusGained = Start(s3))
        val s6 =
            TestPrimaryState("6", onFocusGained = Clear(s5))

        stateMachine.handleMessage(Goto(s6))
        stateMachine.assertStack(s5, s3, s1)
        s1.assertCounts(3, 2, 4, 3)
        s2.assertCounts(3, 3, 3, 3)
        s3.assertCounts(2, 1, 2, 2)
        s4.assertCounts(1, 1, 1, 1)
        s5.assertCounts(1, 0, 1, 1)
        s6.assertCounts(1, 1, 1, 1)
    }

    @Test
    fun `auxiliary states should be able to handle messages that weren't handled by their parents`() {
        val s1 = TestAuxiliaryState("1", interceptedType = String::class.java)
        val s2 = TestAuxiliaryState("2", interceptedType = Integer::class.java, auxiliaryState = s1)
        val s3 = TestPrimaryState("3", interceptedType = Double::class.javaObjectType, auxiliaryState = s2)

        val stateMachine = StateMachine(s3)
        s1.assertCounts(0, 0)
        s2.assertCounts(0, 0)
        s3.assertCounts(1, 0, 1, 0)

        stateMachine.handleMessage(1.0)
        s1.assertEvents()
        s2.assertEvents()
        s3.assertEvents(1.0)

        stateMachine.handleMessage(1)
        s1.assertEvents()
        s2.assertEvents(1)
        s3.assertEvents(1.0)

        stateMachine.handleMessage("1")
        s1.assertEvents("1")
        s2.assertEvents(1)
        s3.assertEvents(1.0)

        stateMachine.handleMessage(Locale.CANADA)
        s1.assertEvents("1")
        s2.assertEvents(1)
        s3.assertEvents(1.0)

        stateMachine.handleMessage("2")
        s1.assertEvents("1", "2")
        s2.assertEvents(1)
        s3.assertEvents(1.0)

        stateMachine.handleMessage(2)
        s1.assertEvents("1", "2")
        s2.assertEvents(1, 2)
        s3.assertEvents(1.0)

        stateMachine.handleMessage(2.0)
        s1.assertEvents("1", "2")
        s2.assertEvents(1, 2)
        s3.assertEvents(1.0, 2.0)

        s1.assertCounts(0, 0)
        s2.assertCounts(0, 0)
        s3.assertCounts(1, 0, 1, 0)
    }

    @Test
    fun `states should be able to modify the stack when they start`() {
        val s1 = TestPrimaryState("1")
        val s2 = TestPrimaryState("2", onStart = Goto(s1))
        val s3 = TestPrimaryState("3", onStart = Start(s2))

        val stateMachine = StateMachine(s3)
        stateMachine.assertStack(s3, s1)
        s1.assertCounts(1, 0, 1, 0)
        s2.assertCounts(1, 1, 0, 0)
        s3.assertCounts(1, 0, 0, 0)

        stateMachine.handleMessage(Goto(s2))
        stateMachine.assertStack(s3, s1)
        s1.assertCounts(2, 1, 2, 1)
        s2.assertCounts(2, 2, 0, 0)
        s3.assertCounts(1, 0, 0, 0)

        val s4 = TestPrimaryState("4", onStart = Done)

        stateMachine.handleMessage(Start(s4))
        stateMachine.assertStack(s3, s1)
        s1.assertCounts(2, 1, 3, 2)
        s2.assertCounts(2, 2, 0, 0)
        s3.assertCounts(1, 0, 0, 0)
        s4.assertCounts(1, 1, 0, 0)

        val s5 = TestPrimaryState("5", onStart = Start(s3))
        val s6 = TestPrimaryState("6", onStart = Clear(s5))

        stateMachine.handleMessage(Goto(s6))
        stateMachine.assertStack(s5, s3, s1)
        s1.assertCounts(3, 2, 4, 3)
        s2.assertCounts(3, 3, 0, 0)
        s3.assertCounts(2, 1, 0, 0)
        s4.assertCounts(1, 1, 0, 0)
        s5.assertCounts(1, 0, 0, 0)
        s6.assertCounts(1, 1, 0, 0)
    }

    @Test
    fun `a state should lose and gain focus if it does a goto to itself`() {
        val s1 = TestPrimaryState("1")

        val stateMachine = StateMachine(s1)
        stateMachine.assertStack(s1)
        s1.assertCounts(1, 0, 1, 0)

        stateMachine.handleMessage(Goto(s1))
        stateMachine.assertStack(s1)
        s1.assertCounts(2, 1, 2, 1)
    }

    @Test
    fun `transition handlers should be called in the order of onEnd, onStart, onFocusLost, onFocusGained`() {

        fun mockState() = mock(PrimaryState::class.java).also {
            `when`(it.receive).thenReturn(ReceiveBuilder().match { n: Next -> n })
            `when`(it.onStart()).thenReturn(Stay())
            `when`(it.onFocusGained()).thenReturn(Stay())
        }

        val s1 = mockState()
        val s2 = mockState()
        val inOrder = inOrder(s1, s2)

        val stateMachine = StateMachine(s1)
        stateMachine.assertStack(s1)

        inOrder.verify(s1).onStart()
        inOrder.verify(s1).onFocusGained()

        stateMachine.handleMessage(Goto(s2))
        stateMachine.assertStack(s2)

        inOrder.verify(s1).onEnd()
        inOrder.verify(s2).onStart()
        inOrder.verify(s1).onFocusLost()
        inOrder.verify(s2).onFocusGained()

        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `a state can be started by calling start on the stateMachine`() {

        val stateMachine = StateMachine()
        stateMachine.assertStack()

        val s1 = TestPrimaryState("1")
        stateMachine.start(s1)
        stateMachine.assertStack(s1)
        s1.assertCounts(1, 0, 1, 0)

        val s2 = TestPrimaryState("2")
        stateMachine.start(s2)
        stateMachine.assertStack(s1, s2)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 0, 1, 0)
    }

    @Test
    fun `states should be able to modify the stack in the middle of the stack`() {
        val s1 = TestPrimaryState("1")
        val s2 = TestPrimaryState("2")
        val s3 = TestPrimaryState("3")

        val stateMachine = StateMachine(s1)
        stateMachine.start(s2)
        stateMachine.start(s3)
        stateMachine.assertStack(s1, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        stateMachine.handleMessage(Stay(), 1)
        stateMachine.assertStack(s1, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        val s4 = TestPrimaryState("4")
        stateMachine.handleMessage(Goto(s4), 1)
        stateMachine.assertStack(s1, s4, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 0, 0, 0)

        stateMachine.handleMessage(Start(s2), 1)
        stateMachine.assertStack(s1, s4, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(2, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 0, 0, 0)

        stateMachine.handleMessage(Done, 2)
        stateMachine.assertStack(s1, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(2, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 1, 0, 0)

        stateMachine.handleMessage(AbsoluteClear(s4), 1)
        stateMachine.assertStack(s4)
        s1.assertCounts(1, 1, 1, 1)
        s2.assertCounts(2, 2, 1, 1)
        s3.assertCounts(1, 1, 1, 1)
        s4.assertCounts(2, 1, 1, 0)
    }

    @Test
    fun `the publicly accessible stack should be a copy of the stateMachine's stack`() {
        val s1 = TestPrimaryState("1")
        val s2 = TestPrimaryState("2")
        val s3 = TestPrimaryState("3")

        val stateMachine = StateMachine(s1)
        stateMachine.start(s2)
        stateMachine.start(s3)
        stateMachine.assertStack(s1, s2, s3)

        val stack = stateMachine.stack() as MutableList
        stack.add(s1)
        stateMachine.assertStack(s1, s2, s3)
    }

    @Test
    fun `test relative clears and absolute starts`() {
        val s1 = TestPrimaryState("1")
        val s2 = TestPrimaryState("2")
        val s3 = TestPrimaryState("3")

        val stateMachine = StateMachine(s1)
        stateMachine.start(s2)
        stateMachine.start(s3)
        stateMachine.assertStack(s1, s2, s3)
        s1.assertCounts(1, 0, 1, 1)
        s2.assertCounts(1, 0, 1, 1)
        s3.assertCounts(1, 0, 1, 0)

        val s4 = TestPrimaryState("4")
        stateMachine.handleMessage(
            Clear(
                s4,
                RelativeRange.Below(false)
            )
        )
        stateMachine.assertStack(s4, s3)
        s1.assertCounts(1, 1, 1, 1)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 0, 0, 0)

        stateMachine.handleMessage(
            AbsoluteStart(
                s1,
                AbsolutePosition.Bottom
            )
        )
        stateMachine.assertStack(s1, s4, s3)
        s1.assertCounts(2, 1, 1, 1)
        s2.assertCounts(1, 1, 1, 1)
        s3.assertCounts(1, 0, 1, 0)
        s4.assertCounts(1, 0, 0, 0)

        stateMachine.handleMessage(
            Clear(
                s2,
                RelativeRange.Above(false)
            ), 2)
        stateMachine.assertStack(s1, s2)
        s1.assertCounts(2, 1, 1, 1)
        s2.assertCounts(2, 1, 2, 1)
        s3.assertCounts(1, 1, 1, 1)
        s4.assertCounts(1, 1, 0, 0)

        stateMachine.handleMessage(
            AbsoluteStart(
                s4,
                AbsolutePosition.Top
            ), 1)
        stateMachine.assertStack(s1, s2, s4)
        s1.assertCounts(2, 1, 1, 1)
        s2.assertCounts(2, 1, 2, 2)
        s3.assertCounts(1, 1, 1, 1)
        s4.assertCounts(2, 1, 1, 0)

        stateMachine.handleMessage(
            Start(
                s3,
                RelativePosition.Below
            )
        )
        stateMachine.assertStack(s1, s2, s3, s4)
        s1.assertCounts(2, 1, 1, 1)
        s2.assertCounts(2, 1, 2, 2)
        s3.assertCounts(2, 1, 1, 1)
        s4.assertCounts(2, 1, 1, 0)

        val s5 = TestPrimaryState("5")
        stateMachine.handleMessage(
            Clear(
                s5,
                RelativeRange.Below(true)
            ), 1)
        stateMachine.assertStack(s5, s4)
        s1.assertCounts(2, 2, 1, 1)
        s2.assertCounts(2, 2, 2, 2)
        s3.assertCounts(2, 2, 1, 1)
        s4.assertCounts(2, 1, 1, 0)

        stateMachine.handleMessage(
            Clear(
                s1,
                RelativeRange.Above(true)
            )
        )
        stateMachine.assertStack(s5, s1)
        s1.assertCounts(3, 2, 2, 1)
        s2.assertCounts(2, 2, 2, 2)
        s3.assertCounts(2, 2, 1, 1)
        s4.assertCounts(2, 2, 1, 1)

        stateMachine.handleMessage(
            Clear(
                s2,
                RelativeRange.Above(false)
            )
        )
        stateMachine.assertStack(s5, s1, s2)
        s1.assertCounts(3, 2, 2, 2)
        s2.assertCounts(3, 2, 3, 2)
        s3.assertCounts(2, 2, 1, 1)
        s4.assertCounts(2, 2, 1, 1)

        stateMachine.handleMessage(
            Clear(
                s3,
                RelativeRange.Below(false)
            ), 2)
        stateMachine.assertStack(s3, s5, s1, s2)
        s1.assertCounts(3, 2, 2, 2)
        s2.assertCounts(3, 2, 3, 2)
        s3.assertCounts(3, 2, 1, 1)
        s4.assertCounts(2, 2, 1, 1)
    }

    @Test
    fun `when states call back into the stateMachine to handle messages, those messages should be handled after the current message`() {
        val stateMachine = StateMachine()
        val s1 = object : State {
            override val receive: Receive = ReceiveBuilder()
                .match<Next> { it }
                .match { list: MutableList<State> ->
                    val last = list.removeAt(list.size - 1)
                    list.forEach { stateMachine.handleMessage(Start(it)) }
                    Start(last)
                }
        }

        stateMachine.start(s1)
        stateMachine.assertStack(s1)

        val s2 = TestPrimaryState("2")
        val s3 = TestPrimaryState("3")
        val s4 = TestPrimaryState("4")
        val s5 = TestPrimaryState("5")

        stateMachine.handleMessage(mutableListOf(s2, s3, s4, s5))
        // s5 should start first because Start(s5) was the result of the message being handled
        stateMachine.assertStack(s1, s5, s2, s3, s4)
    }

    // TODO: test aux states put directly on stack
}
