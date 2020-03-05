package com.github.goodwillparking.geep

import org.junit.jupiter.api.Assertions.assertEquals
import java.lang.IllegalArgumentException

data class TargetedNext(val id: String, val next: Next)

sealed class BaseTestState(val id: String, val interceptedType: Class<*>) :
    State {

    var events: List<Any> = emptyList()
        protected set
    var startCount = 0
        private set
    var endCount = 0
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
        .match<TargetedNext>({ it.id == id }) { it.next }
        .match<Any>({ interceptedType.isAssignableFrom(it::class.java) && it !is TargetedNext }) {
            events = events + it
            Stay()
        }

    override fun onStart(): Next = Stay()
        .also { startCount++ }

    override fun onEnd() {
        endCount++
    }

    fun assertCounts(start: Int, end: Int) {
        assertEquals(start, startCount, "Start count.")
        assertEquals(end, endCount, "End count.")
    }

    fun assertEvents(vararg events: Any) {
        assertIterableContents(this.events, *events)
    }
}

class TestPrimaryState(
    id: String,
    val onStart: Next = Stay(),
    val onFocusGained: Next = Stay(),
    interceptedType: Class<*> = Any::class.java,
    override val auxiliaryState: TestAuxiliaryState? = null
) : BaseTestState(id, interceptedType),
    PrimaryState {

    var focusGainedCount = 0
        private set
    var focusLostCount = 0
        private set

    override fun toString() =
        "TestPrimaryState(id='$id', " +
            "auxId='${auxiliaryState?.id},' " +
            "startCount='$startCount', " +
            "endCount='$endCount', " +
            "focusGainedCount=$focusGainedCount, " +
            "focusLostCount=$focusLostCount, " +
            "events=$events)"

    override fun onStart(): Next {
        super<BaseTestState>.onStart()
        return onStart
    }

    override fun onFocusGained(): Next {
        focusGainedCount++
        return onFocusGained
    }

    override fun onFocusLost() {
        focusLostCount++
    }

    fun assertCounts(start: Int, end: Int, focusGained: Int, focusLost: Int) {
        assertEquals(start, startCount, "Start count.")
        assertEquals(end, endCount, "End count.")
        assertEquals(focusGained, focusGainedCount, "Focus gained count.")
        assertEquals(focusLost, focusLostCount, "Focus lost count.")
    }
}

open class TestAuxiliaryState(
    id: String,
    val onStart: AbsoluteNext = Stay(),
    interceptedType: Class<*> = Any::class.java,
    override val auxiliaryState: TestAuxiliaryState? = null
) : BaseTestState(id, interceptedType),
    AuxiliaryState {

    override val receive: AuxiliaryReceive = AuxiliaryReceiveBuilder()
        .match<AbsoluteNext>{ it }
        .match<TargetedNext>({ it.id == id }) {
            if (it.next is AbsoluteNext) it.next
            else throw IllegalArgumentException("TargetedNext for aux state has has invalid Next")
        }
        .match<Any>({ a -> interceptedType.isAssignableFrom(a::class.java) && a !is TargetedNext }) {
            events = events + it
            Stay()
        }

    override fun toString() =
        "TestAuxiliaryState(id='$id', " +
            "auxId='${auxiliaryState?.id}', " +
            "startCount='$startCount', " +
            "endCount='$endCount', " +
            "events=$events)"

    override fun onStart(): AbsoluteNext {
        super<BaseTestState>.onStart()
        return onStart
    }
}
