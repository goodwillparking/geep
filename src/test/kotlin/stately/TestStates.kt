package stately

import io.vavr.collection.Queue
import io.vavr.collection.Seq
import org.junit.Assert


sealed class BaseTestState(val id: String, val interceptedType: Class<*>) : State {

    var events: Seq<Any> = Queue.empty()
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

    override val receive: Receive = empty<Any, Next>()
        .match { n: Next -> n }
        .match({a -> interceptedType.isAssignableFrom(a::class.java) }) { a: Any -> events = events.append(a); Stay }

    override fun onFocusGained(): Next = Stay.also { focusGainedCount++ }

    override fun onFocusLost() {
        focusLostCount++
    }

    fun assertCounts(focusGained: Int, focusLost: Int) {
        Assert.assertEquals(focusGained, focusGainedCount)
        Assert.assertEquals(focusLost, focusLostCount)
    }

    fun assertEvents(vararg events: Any) {
        assertIterableContents(this.events, *events)
    }
}

class TestParentState(
    id: String,
    interceptedType: Class<*>,
    override val childState: BaseTestState
) : BaseTestState(id, interceptedType), ParentState {
    constructor(id: String, child: BaseTestState) : this(id, Any::class.java, child)

    override fun toString() =
        "TestParentState(id='$id', childId='${childState.id}' focusGainedCount=$focusGainedCount, focusLostCount=$focusLostCount, events=$events)"

}

class TestState(
    id: String,
    val onFocusGained: Next = Stay,
    interceptedType: Class<*> = Any::class.java
) : BaseTestState(id, interceptedType) {

    override fun toString() = "TestState(id='$id', focusGainedCount=$focusGainedCount, focusLostCount=$focusLostCount, events=$events)"

    override fun onFocusGained(): Next {
        super.onFocusGained()
        return onFocusGained
    }
}