package stately

typealias Receive = PartialFunction<Any, out Next>

typealias ChildReceive = PartialFunction<Any, out AbsoluteNext>

typealias ReceiveBuilder = PFBuilder<Any, Next>

typealias ChildReceiveBuilder = PFBuilder<Any, AbsoluteNext>

interface State {

    val receive: Receive

    fun onStart(): Next {
        return Stay
    }

    fun onEnd() {}

    fun onFocusGained(): Next {
        return Stay
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
        return Stay
    }

    override fun onFocusGained(): AbsoluteNext {
        return Stay
    }
}

// TODO: Async modifiers
sealed class Next

sealed class RelativeNext : Next()

sealed class AbsoluteNext : Next()

object Stay : AbsoluteNext()

data class Goto(val state: State) : RelativeNext()

data class Start(val state: State, val position: RelativePosition = RelativePosition.Above) : RelativeNext()

data class AbsoluteStart(val state: State, val position: AbsolutePosition = AbsolutePosition.Top) : AbsoluteNext()

object Done : RelativeNext()

// TODO: nullable state for clear?
data class Clear(val state: State, val range: RelativeRange = RelativeRange.Below(true)) : RelativeNext()

data class AbsoluteClear(val state: State) : AbsoluteNext()

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
