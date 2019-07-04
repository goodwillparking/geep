package stately

import io.vavr.collection.List
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.emptyIterable
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

class OverseerTest {

    @Test
    fun basicTransitions() {
        val s1 = ChildState("1")
        val overseer = Overseer(s1)
        overseer.assertStack(s1)
        s1.assertCounts(1, 0)

        val s2 = ChildState("2")
        overseer.handleMessage(Goto(s2))
        overseer.assertStack(s2)
        s1.assertCounts(1, 1)
        s2.assertCounts(1, 0)

        overseer.handleMessage(Start(s1))
        overseer.assertStack(s2, s1)
        s1.assertCounts(2, 1)
        s2.assertCounts(1, 1)

        val s3 = ChildState("3")
        overseer.handleMessage(Start(s3))
        overseer.assertStack(s2, s1, s3)
        s1.assertCounts(2, 2)
        s2.assertCounts(1, 1)
        s3.assertCounts(1, 0)

        overseer.handleMessage(Done)
        overseer.assertStack(s2, s1)
        s1.assertCounts(3, 2)
        s2.assertCounts(1, 1)
        s3.assertCounts(1, 1)

        overseer.handleMessage(Clear(s3))
        overseer.assertStack(s3)
        s1.assertCounts(3, 3)
        s2.assertCounts(1, 1)
        s3.assertCounts(2, 1)

        overseer.handleMessage(Done)
        overseer.assertStack()
        s1.assertCounts(3, 3)
        s2.assertCounts(1, 1)
        s3.assertCounts(2, 2)
    }

    @Test
    fun basicTransitionsOnStart() {
        val s1 = ChildState("1")
        val s2 = ChildState("2", Goto(s1))
        val s3 = ChildState("3", Start(s2))

        val overseer = Overseer(s3)
        overseer.assertStack(s3, s1)
        s1.assertCounts(1, 0)
        s2.assertCounts(1, 1)
        s3.assertCounts(1, 1)

        overseer.handleMessage(Goto(s2))
        overseer.assertStack(s3, s1)
        s1.assertCounts(2, 1)
        s2.assertCounts(2, 2)
        s3.assertCounts(1, 1)

        val s4 = ChildState("4", Done)

        overseer.handleMessage(Start(s4))
        overseer.assertStack(s3, s1)
        s1.assertCounts(3, 2)
        s2.assertCounts(2, 2)
        s3.assertCounts(1, 1)
        s4.assertCounts(1, 1)

        val s5 = ChildState("5", Start(s3))
        val s6 = ChildState("6", Clear(s5))

        overseer.handleMessage(Goto(s6))
        overseer.assertStack(s5, s3, s1)
        s1.assertCounts(4, 3)
        s2.assertCounts(3, 3)
        s3.assertCounts(2, 2)
        s4.assertCounts(1, 1)
        s5.assertCounts(1, 1)
        s6.assertCounts(1, 1)
    }

    private fun Overseer.assertStack(vararg states: State) {
        if (states.isEmpty()) {
            assertThat(stack, emptyIterable())
        } else {
            assertThat(stack.reverse().map { it.state }, contains(*states))
        }
    }
}

private sealed class TestState(val id: String, val interceptedType: Class<*>) : State {

    var events: List<Any> = List.empty()
        private set
    var startCount = 0
        private set
    var endCount = 0
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestState

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override val receive: Receive = empty<Any, Next>()
        .match { n: Next -> n }
        .match({a -> interceptedType.isAssignableFrom(a::class.java) }) { a: Any -> events = events.append(a); Stay }

    override fun onStart(): Next = Stay.also { startCount++ }

    override fun onEnd() {
        endCount++
    }

    fun assertCounts(start: Int, end: Int) {
        assertEquals(start, startCount)
        assertEquals(end, endCount)
    }
}

private class ParentState(id: String, interceptedType: Class<*>, val child: TestState) : TestState(id, interceptedType) {
    constructor(id: String, child: TestState) : this(id, Any::class.java, child)

    override fun toString() =
        "ParentState(id='$id', childId='${child.id}' startCount=$startCount, endCount=$endCount, events=$events)"

}
private class ChildState(
    id: String,
    val onStart: Next = Stay,
    interceptedType: Class<*> = Any::class.java
) : TestState(id, interceptedType) {

    override fun toString() = "ChildState(id='$id', startCount=$startCount, endCount=$endCount, events=$events)"

    override fun onStart(): Next {
        super.onStart()
        return onStart
    }
}
