package stately

import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Playground {

    @Test
    fun guicy() {
        val injector = Guice.createInjector(Module { b -> b.bind(String::class.java).toInstance("foo") })
        assertEquals("foo", injector.getInstance(String::class.java))
        assertTrue(injector.getInstance(InjectedInjector::class.java).injector === injector)
    }

    class InjectedInjector @Inject constructor(val injector: Injector)
}
