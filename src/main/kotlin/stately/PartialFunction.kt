package stately

interface PartialFunction<T, R> {

    companion object {

        fun <T, R> empty(): PartialFunction<T, R> = object : PartialFunction<T, R> {
            override fun apply(t: T) = throw IllegalStateException("Function not defined at $t")
            override fun isDefinedAt(value: T) = false
        }

    }

    fun apply(t: T): R

    fun isDefinedAt(value: T): Boolean

    fun orElse(other: PartialFunction<T, R>): PartialFunction<T, R> =
        object : PartialFunction<T, R> {
            override fun apply(t: T) =
                if (this@PartialFunction.isDefinedAt(t)) this@PartialFunction.apply(t) else other.apply(t)

            override fun isDefinedAt(value: T) = this@PartialFunction.isDefinedAt(value) || other.isDefinedAt(value)
        }
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