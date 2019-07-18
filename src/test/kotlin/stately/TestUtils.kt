package stately

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers


fun <V> assertIterableContents(actual: Iterable<V>, vararg expected: V) {
    if (!expected.iterator().hasNext()) {
        MatcherAssert.assertThat(actual, Matchers.emptyIterable())
    } else {
        MatcherAssert.assertThat(actual, Matchers.contains(*expected))
    }
}

fun Overseer.assertStack(vararg states: State) {
    assertIterableContents(stack().asReversed(), *states)
}
