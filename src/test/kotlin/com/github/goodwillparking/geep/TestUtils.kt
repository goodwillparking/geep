package com.github.goodwillparking.geep

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.mockito.Mockito
import java.lang.Exception


fun <V> assertIterableContents(actual: Iterable<V>, vararg expected: V) {
    if (!expected.iterator().hasNext()) {
        MatcherAssert.assertThat(actual, Matchers.emptyIterable())
    } else {
        MatcherAssert.assertThat(actual, Matchers.contains(*expected))
    }
}

fun StateMachine.assertStack(vararg states: State) {
    assertIterableContents(stack().asReversed(), *states)
}

inline fun <reified E : Exception> expectException(runnable: () -> Unit) {
    try {
        runnable()
        Assert.fail("Expected exception of type ${E::class.java}, but no exception was thrown")
    } catch (e: Exception) {
        if (e !is E) {
            Assert.fail("Expected exception of type ${E::class.java}, but thrown exception was $e")
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> anyObject(): T {
    Mockito.anyObject<T>()
    return null as T
}
