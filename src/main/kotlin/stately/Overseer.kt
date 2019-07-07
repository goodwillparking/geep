package stately

import io.vavr.collection.List
import io.vavr.collection.Queue
import io.vavr.collection.Seq

// TODO: Generic type? Probably not. How would type be enforced on state change (Goto, Start).
// TODO: Have both on start/end and on focus gained/lost for async support.
class Overseer {

    constructor(initialState: State) {
        stack = newStack().prepend(StackElement(initialState))
        init()
    }

    constructor(initialStates: Seq<State>) {
        stack = initialStates.map { StackElement(it) }
        init()
    }

    var stack: Seq<StackElement>
        private set

    fun handleMessage(message: Any) {
        stack.headOption().peek { element ->
            element.chain
                .find { state -> state.receive.isDefinedAt(message) }
                .peek { process(element, 0, it.receive.apply(message)) }
        }
    }

    private fun init() {
        stack.headOption().peek { process(it, 0, it.state.onFocusGained()) }
    }

    // TODO: support processing state other than the head?
    private tailrec fun process(element: StackElement, stackIndex: Int, next: Next) {
        val result =  when (next) {
            is Goto -> goto(element, stackIndex, next)
            is Stay -> null
            is Start -> start(stackIndex, next)
            is Done -> done(element, stackIndex)
            is Clear -> clear(next)
        }

        if (result != null) {
            return process(result.nextElement, result.nextIndex, result.onStart)
        }
    }

    private fun goto(processed: StackElement, stackIndex: Int, goto: Goto): Result? {
        val new = StackElement(goto.state)
        stack = stack.update(stackIndex, new)
        return if (stackIndex == 0) {
            processed.state.onFocusLost()
            Result(new, stackIndex, new.state.onFocusGained())
        } else {
            null
        }
    }

    // TODO: consider allowing the state to start at the top/bottom of the stack
    private fun start(stackIndex: Int, start: Start): Result {
        stack.headOption().peek { it.state.onFocusLost() }
        val new = StackElement(start.state)
        stack = stack.insert(stackIndex, new)
        return Result(new, stackIndex, new.state.onFocusGained())
    }

    private fun done(processed: StackElement, stackIndex: Int): Result? {
        stack = stack.removeAt(stackIndex)
        return if (stackIndex == 0) {
            processed.state.onFocusLost()
            stack.headOption().map {
                Result(it, stackIndex, it.state.onFocusGained())
            }.orNull
        } else {
            null
        }
    }

    // TODO: Consider allowing to clear to nothing or to another stack of states.
    private fun clear(clear: Clear): Result {
        stack.headOption().peek { it.state.onFocusLost() }
        val new = StackElement(clear.state)
        stack = newStack().prepend(new)
        return Result(new, 0, clear.state.onFocusGained())
    }

    private fun newStack() = List.empty<StackElement>()
}

data class StackElement(val state: State) {
    // TODO: need to be lazy?
    val chain: Seq<State> by lazy {
        var s = state
        var acc = Queue.of(s)
        while (s is ParentState) {
            s = s.childState
            acc = acc.append(s)
        }
        acc
    }
}

private data class Result(val nextElement: StackElement, val nextIndex: Int, val onStart: Next)