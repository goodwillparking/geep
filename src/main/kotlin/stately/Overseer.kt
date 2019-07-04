package stately

import io.vavr.collection.List
import io.vavr.collection.Seq

class Overseer {
    private var stack: Seq<StackElement> = List.empty()

    fun handleMessage(message: Any) {
        stack.dropWhile {
            val next = it.cachedReceive.apply(message)
            
        }
    }

}

private fun process(stackElement: StackElement, message: Any) {
    val result = stackElement.cachedReceive.apply(message)
}

private data class StackElement(val state: State) {
    val cachedReceive = state.receiveAll();
}