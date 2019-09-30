package stately

import org.slf4j.LoggerFactory
import java.util.IdentityHashMap
import java.util.LinkedList
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// TODO: Generic type? Probably not. How would type be enforced on state change (Goto, Start).
// TODO: Have both on start/end and on focus gained/lost for async support.
class Overseer(val asyncContext: AsyncContext = JavaAsyncContext()) {

    companion object {
        private val log = LoggerFactory.getLogger(Overseer::class.java)
    }

    constructor(initialState: State, asyncContext: AsyncContext = JavaAsyncContext()) : this(asyncContext) {
        start(initialState)
    }

    private fun emptyList(): MutableList<StackElement> = LinkedList()

    private var stack: MutableList<StackElement> = emptyList()

    private val lock = ReentrantLock()

    // Client States may not use referential equality,
    // so use a IdentityHashMap to make sure we don't lose track of what state have async tasks.
    private val timers: MutableMap<State, MutableMap<Any, Future<*>>> = IdentityHashMap()

    fun stack() = lock.withLock { stack.map { it.state } }

    fun handleMessage(message: Any, index: Int = 0) = lock.withLock<Unit> {
        if (index < 0 || index >= stack.size) {
            log.debug("Index ({}) out of bounds. Stack size: {}", index, stack.size)
            return
        }

        val element = stack[index]
        checkAndHandle(message, IndexedElement(element, index))
    }

    private fun checkAndHandle(message: Any, indexed: IndexedElement) {
        indexed.element.chain
            .find { state -> state.receive.isDefinedAt(message) }
            ?.also { state -> applyMessage(message, Recipient(state, indexed)) }
            ?: kotlin.run { logUnhandled(message, indexed.element.state) }
    }

    private fun checkAndHandle(message: Any, recipient: Recipient) {
        if (recipient.state.receive.isDefinedAt(message)) {
            applyMessage(message, recipient)
        } else {
            logUnhandled(message, recipient.state)
        }
    }

    private fun applyMessage(message: Any, recipient: Recipient) {
        log.debug("Applying message to state. message: {}, state: {}", message, recipient.indexed.element.state)
        processNext(recipient.state.receive.apply(message), recipient)
    }

    private fun logUnhandled(message: Any, state: State) =
        log.debug("Message unhandled. message: {}, state: {}", message, state)

    fun start(state: State) {
        lock.withLock { processNext(AbsoluteStart(state), null) }
    }

    // TODO: support processing state other than the head?
    private tailrec fun processNext(next: Next, recipient: Recipient? = null) {

        val oldFocused: StackElement? = stack.firstOrNull()
        var focusedChanged = false

        tailrec fun processNextAndApplyStartResult(next: Next, recipient: Recipient?) {

            fun handleAbsolute(absoluteNext: AbsoluteNext): IndexedElement? = when (absoluteNext) {
                is Stay -> null
                is AbsoluteStart -> absoluteStart(absoluteNext)
                is AbsoluteClear -> absoluteClear(absoluteNext)
            }

            val newElement: IndexedElement? = if (recipient == null) {
                if (next is AbsoluteNext) {
                    handleAbsolute(next)
                } else {
                    // Should never happen.
                    log.error("Recipient is null but does not use AbsoluteNext, ignoring it. next: {}", next)
                    null
                }
            } else {
                updateAsync(next, recipient)
                val indexed = recipient.indexed
                when (next) {
                    is Goto -> goto(next, indexed)
                    is Start -> start(next, indexed)
                    is Done -> { done(indexed); null }
                    is Clear -> clear(next, indexed)
                    is AbsoluteNext -> handleAbsolute(next)
                }
            }

            if (stack.firstOrNull()?.state !== oldFocused?.state) {
                focusedChanged = true
            }

            if (newElement != null) {
                processNextAndApplyStartResult(
                    newElement.element.state.onStart(),
                    Recipient(newElement.element.state, newElement)
                )
            }
        }

        log.debug("Processing Next for State. next: {}, state {}", next, recipient?.indexed?.element?.state)
        processNextAndApplyStartResult(next, recipient)

        val newFocused: StackElement? = stack.firstOrNull()

        if (focusedChanged || (oldFocused?.state === recipient?.indexed?.element?.state && next is Goto)) {
            if (oldFocused != null) {
                log.debug("Focus lost for state {}", oldFocused.state)
                oldFocused.state.onFocusLost()
            }
            if (newFocused != null) {
                log.debug("Focus gained for state {}", newFocused.state)
                processNext(newFocused.state.onFocusGained(), Recipient(newFocused.state, IndexedElement(newFocused, 0)))
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

    private fun updateAsync(next: Next, recipient: Recipient) {
        val updates = next.asyncUpdate ?: return
        when (next) {
            // TODO: Have interface for async state updates
            is Start,
            is AbsoluteStart,
            // TODO: Clear if not inclusive
            is Stay -> {
                updates.timerUpdates.forEach { (_, update) ->
                    updateTimer(recipient, update)
                }
            }
        }
    }

    // TODO: Recipient should be StackElement or IndexStackElement
    private fun updateTimer(recipient: Recipient, timerUpdate: TimerUpdate) {
        if (timerUpdate is SetTimer
            && timerUpdate.passiveSet
            && timers[recipient.state]?.containsKey(timerUpdate.key) == true
        ) {
            return
        }

        // The timer will always be canceled either because this is a CancelTimer
        // or because we are resetting an existing timer.
        cancelTimer(recipient, timerUpdate.key)

        val future = when (timerUpdate) {
            is SetSingleTimer -> {
                asyncContext.setSingleTimer(timerUpdate) { key, message ->
                    lock.withLock { executeTimer(recipient, key, message, false) }
                }
            }
            is SetPeriodicTimer -> {
                asyncContext.setPeriodicTimer(timerUpdate) { key, message ->
                    lock.withLock { executeTimer(recipient, key, message, true) }
                }
            }
            is CancelTimer -> null
        }

        future?.also { timers.computeIfAbsent(recipient.state) { HashMap() }[timerUpdate.key] = it }
    }

    private fun executeTimer(recipient: Recipient, key: Any, message: Any, isPeriodic: Boolean) {
        val timerMap = timers[recipient.state]
        // Make sure the timer hasn't been canceled and that the future has not been canceled
        if (timerMap?.containsKey(key) == true && !Thread.currentThread().isInterrupted) {
            // The stack may have changed since the timer was scheduled,
            // so search the stack for the recipient of the message.
            val newRecipient = stack.asSequence().withIndex()
                .map { (index, stackElement) ->
                    when {
                        // Check the StackElement instead of its state
                        // in case the client put the same state on the stack in multiple positions.
                        stackElement === recipient.indexed.element -> stackElement.state
                        else -> stackElement.chain.withIndex()
                            // skip index 0 as that was checked above via the StackElement
                            .find { (chainIndex, state) -> chainIndex != 0 && recipient.state === state }?.value
                    }?.let { Recipient(it, IndexedElement(stackElement, index)) }
                }
                .firstOrNull { it != null }
            if (newRecipient == null) { // This should never happen.
                log.error(
                    "State has timer scheduled but is not on the stack! state: {}, timer key: {}, message: {}",
                    recipient,
                    key,
                    message)
            } else {
                checkAndHandle(message, newRecipient)
            }
            if (!isPeriodic) timerMap.remove(key)
        }
    }

    private fun cancelTimer(recipient: Recipient, key: Any) {
        val timerMap = timers[recipient.state]
        if (timerMap != null) {
            timerMap[key]?.cancel(true)
            timerMap.remove(key)
        }
    }
}

private class StackElement(val state: State) {
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

    override fun toString() = "StackElement(state=$state)"
}

private data class IndexedElement(val element: StackElement, val index: Int)

private data class Recipient(val state: State, val indexed: IndexedElement)
