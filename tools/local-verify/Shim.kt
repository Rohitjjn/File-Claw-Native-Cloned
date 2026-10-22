package org.junit

import kotlin.reflect.KClass

annotation class Test(val expected: KClass<out Throwable> = Test.None::class) {
    class None : Throwable()
}
annotation class Before
annotation class After

object Assert {
    @JvmStatic fun fail(msg: String?): Nothing = throw AssertionError(msg)
    @JvmStatic fun assertTrue(c: Boolean) { if (!c) throw AssertionError("expected true") }
    @JvmStatic fun assertTrue(m: String?, c: Boolean) { if (!c) throw AssertionError(m) }
    @JvmStatic fun assertFalse(c: Boolean) { if (c) throw AssertionError("expected false") }
    @JvmStatic fun assertFalse(m: String?, c: Boolean) { if (c) throw AssertionError(m) }
    @JvmStatic fun assertNull(o: Any?) { if (o != null) throw AssertionError("expected null but was $o") }
    @JvmStatic fun assertNotNull(o: Any?) { if (o == null) throw AssertionError("expected not null") }
    @JvmStatic fun assertSame(a: Any?, b: Any?) { if (a !== b) throw AssertionError("not same") }
    @JvmStatic fun assertEquals(a: Any?, b: Any?) { if (a != b) throw AssertionError("expected <$a> but was <$b>") }
    @JvmStatic fun assertEquals(m: String?, a: Any?, b: Any?) { if (a != b) throw AssertionError("$m expected <$a> but was <$b>") }
    @JvmStatic fun assertEquals(a: Long, b: Long) { if (a != b) throw AssertionError("expected <$a> but was <$b>") }
    @JvmStatic fun assertEquals(a: Double, b: Double, d: Double) { if (Math.abs(a - b) > d) throw AssertionError("expected <$a> but was <$b>") }
    @JvmStatic fun assertEquals(a: Float, b: Float, d: Float) { if (Math.abs(a - b) > d) throw AssertionError("expected <$a> but was <$b>") }
    @JvmStatic fun assertArrayEquals(a: ByteArray, b: ByteArray) { if (!a.contentEquals(b)) throw AssertionError("byte arrays differ") }
}
