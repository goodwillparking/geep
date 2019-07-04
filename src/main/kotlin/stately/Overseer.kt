package stately

import io.vavr.collection.List
import io.vavr.collection.Seq

// TODO: Generic type? Probably not. How would type be enforced on state change (Goto, Start).
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
        stack.headOption().peek { it.state.onStart() }
    }

    // TODO: support processing state other than the head?
    private fun process(element: StackElement, stackIndex: Int, next: Next) {
        when (next) {
            is Goto -> goto(element, stackIndex, next)
            is Stay -> {}
            is Start -> start(next)
            is Done -> done(element, stackIndex)
            is Clear -> clear(next)
        }
    }

    private fun goto(processed: StackElement, stackIndex: Int, goto: Goto) {
        stack = stack.update(stackIndex, StackElement(goto.state))
        if (stackIndex == 0) {
            processed.state.onEnd()
            goto.state.onStart()
        }
    }

    private fun start(start: Start) {
        stack.headOption().peek { it.state.onEnd() }
        stack = stack.prepend(StackElement(start.state))
        start.state.onStart()
    }

    private fun done(processed: StackElement, stackIndex: Int) {
        stack = stack.removeAt(stackIndex)
        if (stackIndex == 0) {
            processed.state.onEnd()
            stack.headOption().peek { it.state.onStart() }
        }
    }

    private fun clear(clear: Clear) {
        stack.headOption().peek { it.state.onEnd() }
        stack = newStack().prepend(StackElement(clear.state))
        clear.state.onStart()
    }

    private fun newStack() = List.empty<StackElement>()
}

data class StackElement(val state: State) {
    // TODO: need to be lazy?
    val chain: Seq<State> by lazy {
        var acc = List.of(state)
        var s = state
        while (s is ParentState) {
            s = s.childState
            acc = acc.append(s)
        }
        acc
    }
}