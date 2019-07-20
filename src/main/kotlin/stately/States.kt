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

sealed class Next {
    protected abstract val asyncUpdate: AsyncUpdate?

    protected abstract fun withAsync(asyncUpdate: AsyncUpdate): Next

    abstract fun async(vararg timerUpdate: TimerUpdate): Next

    protected inline fun <reified N : Next> asyncTypeSafe(vararg timerUpdate: TimerUpdate): N {
        return N::class.java.cast(timerUpdate.fold(this) { n, u -> n.timerUpdate(u) })
    }

    protected fun timerUpdate(timerUpdate: TimerUpdate): Next {
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

    override fun async(vararg timerUpdate: TimerUpdate): Stay {
        return asyncTypeSafe(*timerUpdate)
    }
}

data class Goto(val state: State, override val asyncUpdate: AsyncUpdate?) : Next() {

    constructor(state: State) : this(state, null)

    override fun withAsync(asyncUpdate: AsyncUpdate) = copy(asyncUpdate = asyncUpdate)

    override fun async(vararg timerUpdate: TimerUpdate): Goto {
        return asyncTypeSafe(*timerUpdate)
    }
}

data class Start(val state: State, override val asyncUpdate: AsyncUpdate?) : Next() {

    constructor(state: State) : this(state, null)

    override fun withAsync(asyncUpdate: AsyncUpdate) = copy(asyncUpdate = asyncUpdate)

    override fun async(vararg timerUpdate: TimerUpdate): Start {
        return asyncTypeSafe(*timerUpdate)
    }
}

data class AbsoluteStart(val state: State, val position: AbsolutePosition = AbsolutePosition.Top) : AbsoluteNext() {
    override fun async(vararg timerUpdate: TimerUpdate): Next {
        TODO("not implemented")
    }

    override val asyncUpdate: AsyncUpdate?
        get() = TODO("not implemented")

    override fun withAsync(asyncUpdate: AsyncUpdate): Next {
        TODO("not implemented")
    }
}

class Done(override val asyncUpdate: AsyncUpdate?) : Next() {

    constructor() : this(null)

    override fun withAsync(asyncUpdate: AsyncUpdate) = Done(asyncUpdate)

    override fun async(vararg timerUpdate: TimerUpdate): Done {
        return asyncTypeSafe(*timerUpdate)
    }
}

// TODO: nullable state for clear?
data class Clear(val state: State, override val asyncUpdate: AsyncUpdate?) : Next() {

    constructor(state: State) : this(state, null)

    override fun withAsync(asyncUpdate: AsyncUpdate) = copy(asyncUpdate = asyncUpdate)

    override fun async(vararg timerUpdate: TimerUpdate): Clear {
        return asyncTypeSafe(*timerUpdate)
    }
}

data class AbsoluteClear(val state: State) : AbsoluteNext() {
    override fun async(vararg timerUpdate: TimerUpdate): Next {
        TODO("not implemented")
    }

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

sealed class TimerUpdate {
    abstract val key: Any
}

data class CancelTimer(override val key: Any) : TimerUpdate()

interface SetTimer {
    val duration: Duration
    val message: Any
    val passiveSet: Boolean
}

data class SetSingleTimer(
    override val key: Any,
    override val duration: Duration,
    override val message: Any,
    override val passiveSet: Boolean = false
) : TimerUpdate(), SetTimer

data class SetPeriodicTimer(
    override val key: Any,
    override val duration: Duration,
    override val message: Any,
    override val passiveSet: Boolean = false,
    val initialDelay: Duration = Duration.ZERO
) : TimerUpdate(), SetTimer
