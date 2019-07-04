package stately

import io.vavr.PartialFunction

interface State {

    fun receive(): PartialFunction<Any, out Next>

    fun receiveAll(): PartialFunction<Any, out Next> {
        return when {
            this is ParentState -> receive().orElse(childState.receiveAll())
            else -> receive()
        }
    }

    fun onStart(from: State): Next {
        return Stay
    }

    fun onEnd(to: State) {}
}

interface ParentState : State {
    val childState: State
}

interface FooState : State {
     override fun onStart(from: State): Next {
        return Stay
    }
}

sealed class Next
object Stay : Next()
data class Goto(val state: State) : Next()
data class Start(val state: State) : Next()
object Done : Next()
object Pass : Next()
data class Reset(val state: State) : Next()