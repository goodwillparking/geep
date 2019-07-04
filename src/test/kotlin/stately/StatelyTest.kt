package stately

import io.vavr.PartialFunction

class StatelyTest {
}

class Parent(override val childState: Child) : ParentState {
    override fun receive(): PartialFunction<Any, out Next> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class Child(override val childState: GrandChild) : ParentState {
    override fun receive(): PartialFunction<Any, out Next> {
        TODO("not implemented")
    }
}

class GrandChild : State {
    var string = ""

    override fun receive(): PartialFunction<Any, out Next> {
        return EMPTY
    }
}