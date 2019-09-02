package stately

import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// TODO: Generic type? Probably not. How would type be enforced on state change (Goto, Start).
// TODO: Have both on start/end and on focus gained/lost for async support.
class Overseer(val asyncContext: AsyncContext) {

    companion object {
        private val log = LoggerFactory.getLogger(Overseer::class.java)
    }

    constructor(initialState: State) : this() {
        start(initialState)
    }

    private fun emptyList(): MutableList<StackElement> = LinkedList()

    private var stack: MutableList<StackElement> = emptyList()

    private val lock = ReentrantLock()

    private val timers: MutableMap<State, MutableMap<Any, Future<*>>> = HashMap()

    fun stack() = lock.withLock { stack.map { it.state } }

    fun handleMessage(message: Any, index: Int = 0) = lock.withLock<Unit> {
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
        lock.withLock { processNext(AbsoluteStart(state), null) }
    }

    // TODO: support processing state other than the head?
    private tailrec fun processNext(next: Next, element: IndexedElement? = null) {

        val oldFocused: StackElement? = stack.firstOrNull()
        var focusedChanged = false

        tailrec fun processNextAndApplyStartResult(next: Next, element: IndexedElement?) {

            fun handleAbsolute(absoluteNext: AbsoluteNext): IndexedElement? = when (absoluteNext) {
                is Stay -> null
                is AbsoluteStart -> absoluteStart(absoluteNext)
                is AbsoluteClear -> absoluteClear(absoluteNext)
            }

            val newElement: IndexedElement? = if (element == null) {
                if (next is AbsoluteNext) {
                    handleAbsolute(next)
                } else {
                    // Should never happen.
                    log.error("StackElement is null but does not use AbsoluteNext, ignoring it. next: {}", next)
                    null
                }
            } else {
                when (next) {
                    is Goto -> goto(next, element)
                    is Start -> start(next, element)
                    is Done -> { done(element); null }
                    is Clear -> clear(next, element)
                    is AbsoluteNext -> handleAbsolute(next)
                }
            }

            if (stack.firstOrNull()?.state !== oldFocused?.state) {
                focusedChanged = true
            }

            if (newElement != null) {
                processNextAndApplyStartResult(newElement.element.state.onStart(), newElement)
            }
        }

        log.debug("Processing Next for State. next: {}, state {}", next, element?.element?.state)
        processNextAndApplyStartResult(next, element)

        val newFocused: StackElement? = stack.firstOrNull()

        if (focusedChanged || (oldFocused?.state === element?.element?.state && next is Goto)) {
            if (oldFocused != null) {
                log.debug("Focus lost for state {}", oldFocused.state)
                oldFocused.state.onFocusLost()
            }
            if (newFocused != null) {
                log.debug("Focus gained for state {}", newFocused.state)
                processNext(newFocused.state.onFocusGained(), IndexedElement(newFocused, 0))
            }
        }
    }

    private fun goto(goto: Goto, element: IndexedElement): IndexedElement? {
        val new = StackElement(goto.state)
        stack[element.index] = new
        element.element.state.onEnd()
        return IndexedElement(new, element.index)
    }

    private fun start(start: Start, element: IndexedElement): IndexedElement? {
        val new = StackElement(start.state)
        val index = when(start.position) {
            RelativePosition.Above -> element.index
            RelativePosition.Below -> element.index + 1
        }
        stack.add(index, new)
        return IndexedElement(new, index)
    }

    private fun done(element: IndexedElement) {
        stack.removeAt(element.index)
        element.element.state.onEnd()
    }

    // TODO: Consider allowing to clear to nothing or to another stack of states.
    private fun clear(clear: Clear, element: IndexedElement): IndexedElement? {
        var keep = emptyList()
        var cleared = emptyList()

        fun keepAbove() {
            val iterator = stack.iterator()
            while (iterator.hasNext()) {
                val e = iterator.next()
                if (e == element.element) {
                    if (clear.range.inclusive) {
                        cleared.add(e)
                        iterator.remove()
                    }
                    keep = stack
                    return
                }
                cleared.add(e)
                iterator.remove()
            }
        }

        fun keepBelow() {
            val iterator = stack.iterator()
            while (iterator.hasNext()) {
                val e = iterator.next()
                if (e == element.element) {
                    if (!clear.range.inclusive) {
                        keep.add(e)
                        iterator.remove()
                    }
                    cleared = stack
                    return
                }
                keep.add(e)
                iterator.remove()
            }
        }

        fun clearAndCreateNew(): StackElement {
            cleared.forEach { it.state.onEnd() }
            stack = keep
            return StackElement(clear.state)
        }

        return when (clear.range) {
            is RelativeRange.Below -> {
                keepBelow()
                val new = clearAndCreateNew()
                stack.add(stack.size, new)
                IndexedElement(new, stack.size - 1)
            }
            is RelativeRange.Above -> {
                keepAbove()
                val new = clearAndCreateNew()
                stack.add(0, new)
                IndexedElement(new, 0)
            }
        }
    }

    private fun absoluteStart(start: AbsoluteStart): IndexedElement {
        val new = StackElement(start.state)
        val index = when(start.position) {
            AbsolutePosition.Top -> 0
            AbsolutePosition.Bottom -> stack.size
        }
        stack.add(index, new)
        return IndexedElement(new, index)
    }

    private fun absoluteClear(clear: AbsoluteClear): IndexedElement {
        val new = StackElement(clear.state)
        stack.forEach { it.state.onEnd() }
        stack.clear()
        stack.add(new)
        return IndexedElement(new, 0)
    }

    fun foo(recipient: State, timerUpdate: TimerUpdate) {
        when (timerUpdate) {
            is SetSingleTimer -> {
                asyncContext.setSingleTimer(timerUpdate) { key, message ->
                    lock.withLock {
                        if (timers[recipient]?.containsKey(key) == true) {
                            if (recipient.receive.isDefinedAt(message)) {
                                processNext(recipient.receive.apply(message), IndexedElement(element, index))
                            }
                        }
                    }
                }
            }
        }
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
