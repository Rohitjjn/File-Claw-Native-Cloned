import org.junit.*
import java.lang.reflect.InvocationTargetException

fun main(args: Array<String>) {
    var pass = 0; var fail = 0
    for (cn in args) {
        val k = Class.forName(cn)
        val methods = k.methods.filter { it.isAnnotationPresent(Test::class.java) }.sortedBy { it.name }
        val before = k.methods.filter { it.isAnnotationPresent(Before::class.java) }
        val after = k.methods.filter { it.isAnnotationPresent(After::class.java) }
        for (m in methods) {
            val inst = k.getDeclaredConstructor().newInstance()
            val expected = m.getAnnotation(Test::class.java).expected.java
            var err: Throwable? = null
            try {
                before.forEach { it.invoke(inst) }
                m.invoke(inst)
            } catch (e: InvocationTargetException) { err = e.targetException } catch (e: Throwable) { err = e }
            finally { after.forEach { try { it.invoke(inst) } catch (_: Throwable) {} } }
            val ok = if (expected != Test.None::class.java) (err != null && expected.isInstance(err)) else err == null
            if (ok) pass++ else { fail++; println("FAIL ${k.simpleName}.${m.name}: $err"); err?.stackTrace?.take(4)?.forEach { println("    at $it") } }
        }
    }
    println("RESULT passed=$pass failed=$fail")
    System.exit(if (fail == 0) 0 else 1)
}
