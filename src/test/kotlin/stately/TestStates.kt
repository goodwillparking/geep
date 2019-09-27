package stately

import org.junit.Assert


sealed class BaseTestState(val id: String, val interceptedType: Class<*>) : State {

    var events: List<Any> = emptyList()
        protected set
    var startCount = 0
        private set
    var endCount = 0
        private set
    var focusGainedCount = 0
        private set
    var focusLostCount = 0
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseTestState

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override val receive: Receive = ReceiveBuilder()
        .match { n: Next -> n }
        .match({a -> interceptedType.isAssignableFrom(a::class.java) }) { a: Any -> events = events + a; Stay() }

    override fun onStart(): Next = Stay().also { startCount++ }

    override fun onEnd() {
        endCount++
    }

    override fun onFocusGained(): Next = Stay().also { focusGainedCount++ }

    override fun onFocusLost() {
        focusLostCount++
    }

    fun assertCounts(start: Int, end: Int, focusGained: Int, focusLost: Int) {
        Assert.assertEquals("Start count.", start, startCount)
        Assert.assertEquals("End count.", end, endCount)
        Assert.assertEquals("Focus gained count.", focusGained, focusGainedCount)
        Assert.assertEquals("Focus lost count.", focusLost, focusLostCount)
    }

    fun assertEvents(vararg events: Any) {
        assertIterableContents(this.events, *events)
    }
}

class TestParentState(
    id: String,
    interceptedType: Class<*>,
    override val childState: TestChildState
) : BaseTestState(id, interceptedType), ParentState {
    constructor(id: String, child: TestChildState) : this(id, Any::class.java, child)

    override fun toString() =
        "TestParentState(id='$id', childId='${childState.id}' focusGainedCount=$focusGainedCount, focusLostCount=$focusLostCount, events=$events)"

}

class TestState(
    id: String,
    val onStart: Next = Stay(),
    val onFocusGained: Next = Stay(),
    interceptedType: Class<*> = Any::class.java
) : BaseTestState(id, interceptedType) {

    override fun toString() = "TestState(id='$id', focusGainedCount=$focusGainedCount, focusLostCount=$focusLostCount, events=$events)"

    override fun onStart(): Next {
        super.onStart()
        return onStart
    }

    override fun onFocusGained(): Next {
        super.onFocusGained()
        return onFocusGained
    }
}

open class TestChildState(
    id: String,
    val onStart: AbsoluteNext = Stay(),
    val onFocusGained: AbsoluteNext = Stay(),
    interceptedType: Class<*> = Any::class.java
) : BaseTestState(id, interceptedType), ChildState {

    override val receive: ChildReceive = ChildReceiveBuilder()
        .match{ n: AbsoluteNext -> n }
        .match({a -> interceptedType.isAssignableFrom(a::class.java) }) { a: Any -> events = events + a; Stay() }

    override fun toString() = "TestChildState(id='$id', focusGainedCount=$focusGainedCount, focusLostCount=$focusLostCount, events=$events)"

    override fun onStart(): AbsoluteNext {
        super<BaseTestState>.onStart()
        return onStart
    }

    override fun onFocusGained(): AbsoluteNext {
        super<BaseTestState>.onFocusGained()
        return onFocusGained
    }
}

class TestMiddleState(
    id: String,
    override val childState: TestChildState,
    onStart: AbsoluteNext = Stay(),
    onFocusGained: AbsoluteNext = Stay(),
    interceptedType: Class<*> = Any::class.java
) : TestChildState(id, onStart, onFocusGained, interceptedType), ParentState {

    override val receive: ChildReceive = ChildReceiveBuilder()
        .match{ n: AbsoluteNext -> n }
        .match({a -> interceptedType.isAssignableFrom(a::class.java) }) { a: Any -> events = events + a; Stay() }

    override fun toString() = "TestMiddleState(id='$id', childId='${childState.id}', focusGainedCount=$focusGainedCount, focusLostCount=$focusLostCount, events=$events)"
}
