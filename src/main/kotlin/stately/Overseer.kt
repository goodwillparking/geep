package stately

import io.vavr.collection.List
import io.vavr.collection.Queue
import io.vavr.collection.Seq
import org.slf4j.LoggerFactory

private typealias Stack = Seq<StackElement>

// TODO: Generic type? Probably not. How would type be enforced on state change (Goto, Start).
// TODO: Have both on start/end and on focus gained/lost for async support.
class Overseer {

    companion object {
        private val log = LoggerFactory.getLogger(Overseer::class.java)
    }

    constructor(initialState: State) {
        stack = start(Start(initialState))
    }

    var stack: Stack = newStack()
        private set

    fun handleMessage(message: Any) {
        stack = stack.headOption()
            .flatMap { element ->
                element.chain
                    .find { state -> state.receive.isDefinedAt(message) }
                    .map {
                        log.debug("Applying message to state. message: {}, state: {}", message, element.state)
                        processNext(stack, it.receive.apply(message), IndexedElement(element, 0))
                    }
            }
            .getOrElse { stack }
    }

    fun start(start: Start) = processNext(stack, start, stack.headOption().map { IndexedElement(it, 0) }.orNull)

    // TODO: support processing state other than the head?
    private tailrec fun processNext(stack: Stack, next: Next, element: IndexedElement? = null): Stack {

        tailrec fun processNextAndApplyStartResult(stack: Stack, next: Next, element: IndexedElement?): Stack {
            val result = when (next) {
                is Goto -> element?.let { goto(stack, next, it) }
                is Stay -> null
                is Start -> start(stack, next, element)
                is Done -> element?.let { done(stack, it) }
                is Clear -> clear(stack, next)
            } ?: Result(stack)

            return if (result.newElement != null) {
                processNextAndApplyStartResult(
                    result.stack,
                    result.newElement.element.state.onStart(),
                    result.newElement
                )
            } else {
                result.stack
            }
        }

        log.debug("Processing Next for State. next: {}, state {}", next, element?.element?.state)

        val oldFocused: StackElement? = stack.headOption().orNull
        if (oldFocused != null && next !is Stay) {
            log.debug("Focus lost for state {}", oldFocused.state)
            oldFocused.state.onFocusLost()
        }

        val newStack = processNextAndApplyStartResult(stack, next, element)
        val newFocused: StackElement? = newStack.headOption().orNull

        return if (oldFocused !== newFocused) {
            if (newFocused != null) {
                processNext(newStack, newFocused.state.onFocusGained(), IndexedElement(newFocused, 0))
            } else {
                newStack
            }
        } else {
            newStack
        }
    }

    private fun goto(stack: Stack, goto: Goto, element: IndexedElement): Result {
        val new = StackElement(goto.state)
        val newStack = stack.update(element.index, new)
        element.element.state.onEnd()
        return Result(newStack, new, element.index)
    }

    // TODO: consider allowing the state to start at the top/bottom of the stack
    private fun start(stack: Stack, start: Start, element: IndexedElement?): Result {
        val new = StackElement(start.state)
        val newStack = if (element != null) {
            stack.insert(element.index, new)
        } else {
            stack.prepend(new)
        }
        return Result(newStack, new, if (element == null) 0 else element.index)
    }

    private fun done(stack: Stack, element: IndexedElement): Result {
        val newStack = stack.removeAt(element.index)
        element.element.state.onEnd()
        return Result(newStack)
    }

    // TODO: Consider allowing to clear to nothing or to another stack of states.
    private fun clear(stack: Stack, clear: Clear): Result {
        stack.forEach { it.state.onEnd() }
        val new = StackElement(clear.state)
        val newStack = newStack().prepend(new)
        return Result(newStack, new, 0)
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


private data class Result(val stack: Stack, val newElement: IndexedElement? = null) {
    constructor(stack: Stack, element: StackElement, index: Int) : this(stack, IndexedElement(element, index))
}

private data class IndexedElement(val element: StackElement, val index: Int)