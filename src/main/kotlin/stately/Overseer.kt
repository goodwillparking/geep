package stately

import org.slf4j.LoggerFactory
import java.util.LinkedList


// TODO: Generic type? Probably not. How would type be enforced on state change (Goto, Start).
// TODO: Have both on start/end and on focus gained/lost for async support.
class Overseer() {

    companion object {
        private val log = LoggerFactory.getLogger(Overseer::class.java)
    }

    constructor(initialState: State) : this() {
        start(initialState)
    }

    private val stack: MutableList<StackElement> = LinkedList()

    fun stack() = stack.map { it.state }

    fun handleMessage(message: Any, index: Int = 0) {
        if (index < 0 || index >= stack.size) {
            log.debug("Index ({}) out of bounds. Stack size: {}", index, stack.size)
            return
        }

        val element = stack[index]
        element.chain
            .find { state -> state.receive.isDefinedAt(message) }
            ?.also {
                log.debug("Applying message to state. message: {}, state: {}", message, element.state)
                processNext(it.receive.apply(message), IndexedElement(element, index))
            }
    }

    fun start(state: State) {
        processNext(Start(state), stack.firstOrNull()?.let { IndexedElement(it, 0) })
    }

    // TODO: support processing state other than the head?
    private tailrec fun processNext(next: Next, element: IndexedElement? = null) {

        tailrec fun processNextAndApplyStartResult(next: Next, element: IndexedElement?) {
            val newElement: IndexedElement? = when (next) {
                is Goto -> element?.let { goto(next, it) }
                is Stay -> null
                is Start -> start(next, element)
                is Done -> { element?.also { done(it) }; null}
                is Clear -> clear(next)
            }

            if (newElement != null) {
                processNextAndApplyStartResult(newElement.element.state.onStart(), newElement)
            }
        }

        log.debug("Processing Next for State. next: {}, state {}", next, element?.element?.state)

        val oldFocused: StackElement? = stack.firstOrNull()
        // TODO: Should a state lose and gain focus
        //  if it is at the top of the stack before or after the transition is processed?
        if ((element?.index == 0 || next is Clear) && oldFocused != null && next !is Stay) {
            log.debug("Focus lost for state {}", oldFocused.state)
            oldFocused.state.onFocusLost()
        }

        processNextAndApplyStartResult(next, element)
        val newFocused: StackElement? = stack.firstOrNull()

        if (((element?.index == 0 || element == null || next is Clear)
                && (next !is Stay || oldFocused?.state !== newFocused?.state))
            && newFocused != null
        ) {
            processNext(newFocused.state.onFocusGained(), IndexedElement(newFocused, 0))
        }
    }

    private fun goto(goto: Goto, element: IndexedElement): IndexedElement? {
        val new = StackElement(goto.state)
        stack[element.index] = new
        element.element.state.onEnd()
        return IndexedElement(new, element.index)
    }

    // TODO: consider allowing the state to start at the top/bottom of the stack
    private fun start(start: Start, element: IndexedElement?): IndexedElement? {
        val new = StackElement(start.state)
        val index = element?.index ?: 0
        stack.add(index, new)
        return IndexedElement(new, index)
    }

    private fun done(element: IndexedElement) {
        stack.removeAt(element.index)
        element.element.state.onEnd()
    }

    // TODO: Consider allowing to clear to nothing or to another stack of states.
    private fun clear(clear: Clear): IndexedElement? {
        stack.forEach { it.state.onEnd() }
        val new = StackElement(clear.state)
        stack.clear()
        stack.add(new)
        return IndexedElement(new, 0)
    }

}

private data class StackElement(val state: State) {
    // TODO: need to be lazy?
    val chain: List<State> by lazy {
        var s = state
        val acc = LinkedList<State>()
        acc.add(s)
        while (s is ParentState) {
            s = s.childState
            acc.add(s)
        }
        acc
    }
}

private data class IndexedElement(val element: StackElement, val index: Int)
