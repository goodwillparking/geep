package stately

import io.vavr.Tuple2

internal operator fun <A, B> Tuple2<A, B>.component1() = _1
internal operator fun <A, B> Tuple2<A, B>.component2() = _2
