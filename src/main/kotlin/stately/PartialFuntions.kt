package stately

import io.vavr.PartialFunction
import io.vavr.Tuple2

internal fun <A, B> PartialFunction<in A, out B>.orElse(other: PartialFunction<in A, out B>): PartialFunction<A, B> {
    val pf = this;

    return object : PartialFunction<A, B> {
        override fun apply(value: A): B {
            return when {
                pf.isDefinedAt(value) -> pf.apply(value)
                other.isDefinedAt(value) -> other.apply(value)
                else -> throw IllegalStateException("Function is not defined at $value")
            }
        }

        override fun isDefinedAt(value: A): Boolean {
            return pf.isDefinedAt(value) || other.isDefinedAt(value)
        }
    }
}

internal val EMPTY: PartialFunction<Any, Nothing> = object : PartialFunction<Any, Nothing> {
    override fun apply(t: Any?): Nothing {
        throw IllegalStateException()
    }

    override fun isDefinedAt(value: Any?): Boolean {
        return false
    }
}
