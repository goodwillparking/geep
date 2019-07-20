package stately

import java.time.Duration

typealias Receive = PartialFunction<Any, out Next>

typealias ChildReceive = PartialFunction<Any, out AbsoluteNext>

typealias ReceiveBuilder = PFBuilder<Any, Next>

typealias ChildReceiveBuilder = PFBuilder<Any, AbsoluteNext>

interface State {

    val receive: Receive

    fun onStart(): Next {
        return Stay()
    }

    fun onEnd() {}

    fun onFocusGained(): Next {
        val n = Stay().cancelTimer("")
        return Stay()
    }

    fun onFocusLost() {}
}

// TODO: generic type???
interface ParentState : State {
    val childState: ChildState
}

// TODO: This parent/child naming is a bit counter intuitive if viewed from the perspective of a tree.
//  FallbackState?
interface ChildState : State {
    override val receive: ChildReceive

    override fun onStart(): AbsoluteNext {
        return Stay()
    }

    override fun onFocusGained(): AbsoluteNext {
        return Stay().cancelTimer("")
    }
}

// TODO: Async modifiers
sealed class Next {
    protected abstract val asyncUpdate: AsyncUpdate?

    protected abstract fun withAsync(asyncUpdate: AsyncUpdate): Next

    fun async(vararg timerUpdate: TimerUpdate): Next {
        TODO()
    }

    private fun timerUpdate(timerUpdate: TimerUpdate): Next {
        val asyncUpdate = asyncUpdate
        val pair = timerUpdate.key to timerUpdate
        return withAsync(asyncUpdate?.copy(timerUpdates = asyncUpdate.timerUpdates + pair)
            ?: AsyncUpdate(mapOf(pair)))
    }

    open fun setSingleTimer(key: Any, duration: Duration, message: Any): Next {
        return timerUpdate(SetSingleTimer(key, duration, message))
    }

    open fun cancelTimer(key: Any): Next {
        return timerUpdate(CancelTimer(key))
    }
}

sealed class RelativeNext : Next()

sealed class AbsoluteNext : Next()

class Stay private constructor(override val asyncUpdate: AsyncUpdate?) : AbsoluteNext() {

    constructor() : this(null)

    override fun withAsync(asyncUpdate: AsyncUpdate) = Stay(asyncUpdate)

    override fun cancelTimer(key: Any): Stay {
        return super.cancelTimer(key) as Stay
    }
}

data class Goto(val state: State) : RelativeNext() {
    override val asyncUpdate: AsyncUpdate?
        get() = TODO("not implemented")

    override fun withAsync(asyncUpdate: AsyncUpdate): Next {
        TODO("not implemented")
    }
}

data class Start(val state: State, val position: RelativePosition = RelativePosition.Above) : RelativeNext() {
    override val asyncUpdate: AsyncUpdate?
        get() = TODO("not implemented")

    override fun withAsync(asyncUpdate: AsyncUpdate): Next {
        TODO("not implemented")
    }
}

data class AbsoluteStart(val state: State, val position: AbsolutePosition = AbsolutePosition.Top) : AbsoluteNext() {
    override val asyncUpdate: AsyncUpdate?
        get() = TODO("not implemented")

    override fun withAsync(asyncUpdate: AsyncUpdate): Next {
        TODO("not implemented")
    }
}

object Done : RelativeNext() {
    override val asyncUpdate: AsyncUpdate?
        get() = TODO("not implemented")

    override fun withAsync(asyncUpdate: AsyncUpdate): Next {
        TODO("not implemented")
    }
}

// TODO: nullable state for clear?
data class Clear(val state: State, val range: RelativeRange = RelativeRange.Below(true)) : RelativeNext() {
    override val asyncUpdate: AsyncUpdate?
        get() = TODO("not implemented")

    override fun withAsync(asyncUpdate: AsyncUpdate): Next {
        TODO("not implemented")
    }
}

data class AbsoluteClear(val state: State) : AbsoluteNext() {
    override val asyncUpdate: AsyncUpdate?
        get() = TODO("not implemented")

    override fun withAsync(asyncUpdate: AsyncUpdate): Next {
        TODO("not implemented")
    }
}

sealed class AbsolutePosition {
    object Top : AbsolutePosition()
    object Bottom : AbsolutePosition()
}

sealed class RelativePosition() {
    object Above : RelativePosition()
    object Below : RelativePosition()
}

sealed class RelativeRange(val inclusive: Boolean) {
    class Above(inclusive: Boolean) : RelativeRange(inclusive)
    class Below(inclusive: Boolean) : RelativeRange(inclusive)
}

data class AsyncUpdate(val timerUpdates: Map<Any, TimerUpdate> = emptyMap())

sealed class Foo

object Bar : Foo()

fun a(foo: Foo) {
    when(foo) {
        Bar -> TODO()
        is CancelTimer -> TODO()
        is SetSingleTimer -> TODO()
        is SetPeriodicTimer -> TODO()
    }
}

sealed class TimerUpdate : Foo() {
    abstract val key: Any
}
data class CancelTimer(override val key: Any) : TimerUpdate()
data class SetSingleTimer(override val key: Any, val duration: Duration, val message: Any) : TimerUpdate()
data class SetPeriodicTimer(
    override val key: Any,
    val period: Duration,
    val message: Any,
    val initialDelay: Duration = Duration.ZERO
) : TimerUpdate()
