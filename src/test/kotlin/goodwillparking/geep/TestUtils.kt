package goodwillparking.geep

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.fail
import org.mockito.Mockito
import java.lang.Exception
import java.time.Duration
import java.time.Instant


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

inline fun <reified E : Exception> expectException(runnable: () -> Unit): Unit =
    try {
        runnable()
        fail("Expected exception of type ${E::class.java}, but no exception was thrown")
    } catch (e: Exception) {
        if (e !is E) {
            fail("Expected exception of type ${E::class.java}, but thrown exception was $e")
        } else Unit
    }

@Suppress("UNCHECKED_CAST")
fun <T> anyObject(): T {
    Mockito.anyObject<T>()
    return null as T
}

val Int.ms
    get() = Duration.ofMillis(this.toLong())

fun awaitCondition(
    maxWait: Duration = 1000.ms,
    delay: Duration = maxWait.dividedBy(50),
    condition: () -> Boolean
) {
    val end = Instant.now() + maxWait
    while(!condition() && Instant.now().isBefore(end)) {
        Thread.sleep(delay.toMillis())
    }
}
