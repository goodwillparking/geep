package stately

import io.vavr.PartialFunction
import io.vavr.Tuple2

internal operator fun <A, B> Tuple2<A, B>.component1() = _1
internal operator fun <A, B> Tuple2<A, B>.component2() = _2

fun <T, R> empty(): PartialFunction<T, R> = object : PartialFunction<T, R> {
    override fun apply(t: T) = throw IllegalStateException("PF not defined")
    override fun isDefinedAt(value: T) = false
}

fun <T, R> PartialFunction<T, R>.orElse(other: PartialFunction<T, R>): PartialFunction<T, R> =
    object : PartialFunction<T, R> {
        override fun apply(t: T) = if (this@orElse.isDefinedAt(t)) this@orElse.apply(t) else other.apply(t)
        override fun isDefinedAt(value: T) = this@orElse.isDefinedAt(value) || other.isDefinedAt(value)
    }

inline fun <T, R, reified C : T> PartialFunction<T, R>.match(
    crossinline predicate: (C) -> Boolean = { true },
    crossinline apply: (C) -> R
): PartialFunction<T, R> = orElse(PFBuilder.match(predicate, apply))

class PFBuilder<T, R> {

    companion object {
        inline fun <T, R, reified C : T> match(
            crossinline predicate: (C) -> Boolean = { true },
            crossinline apply: (C) -> R
        ): PartialFunction<T, R> =
            object : PartialFunction<T, R> {
                override fun apply(t: T) = apply(t as C)
                override fun isDefinedAt(value: T) = value is C && predicate(value)
            }
    }

    inline fun <reified C : T> match(
        crossinline predicate: (C) -> Boolean = { true },
        crossinline apply: (C) -> R
    ): PartialFunction<T, R> = PFBuilder.match(predicate, apply)
}