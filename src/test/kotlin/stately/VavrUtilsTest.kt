package stately

import io.vavr.collection.List
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DivergenceRightTests {

    @Test
    fun identical() {
        val seq = List.of(1, 3, 5)
        val (ad, bd) = divergenceRight(seq, seq) { a, b -> a == b }
        assertTrue(ad.isEmpty)
        assertTrue(bd.isEmpty)
    }

    @Test
    fun nothingInCommon() {
        val a = List.of(1, 3, 5)
        val b = List.of(2, 4, 6)
        val (ad, bd) = divergenceRight(a, b) { a1, b1 -> a1 == b1 }
        assertEquals(a, ad)
        assertEquals(b, bd)
    }

    @Test
    fun someDivergence() {
        val a = List.of(1, 3, 5, 6, 7)
        val b = List.of(2, 4, 6, 7)
        val (ad, bd) = divergenceRight(a, b) { a1, b1 -> a1 == b1 }
        assertEquals(List.of(1, 3, 5), ad)
        assertEquals(List.of(2, 4), bd)
    }

    @Test
    fun commonInFront() {
        val a = List.of(1, 2, 3, 5)
        val b = List.of(1, 2, 3, 6)
        val (ad, bd) = divergenceRight(a, b) { a1, b1 -> a1 == b1 }
        assertEquals(a, ad)
        assertEquals(b, bd)
    }
}
