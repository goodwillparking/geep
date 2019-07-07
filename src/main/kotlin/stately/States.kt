package stately

import io.vavr.PartialFunction

typealias Receive = PartialFunction<Any, out Next>

interface State {

    val receive: Receive

    fun onFocusGained(): Next {
        return Stay
    }

    fun onFocusLost() {}
}

// TODO: generic type???
interface ParentState : State {
    val childState: State
}

sealed class Next
object Stay : Next()
data class Goto(val state: State) : Next()
data class Start(val state: State) : Next()
object Done : Next()
data class Clear(val state: State) : Next()
// TODO: Async modifiers