import com.example.core.archive.ArchiveEntryInfo
import com.example.core.archive.ArchiveLimits
import com.example.core.archive.ArchiveTreeBuilder
import com.example.core.format.FormatDetector
import com.example.core.io.TextLoader
import com.example.core.io.TextWindowReader
import java.io.BufferedOutputStream
import java.io.File

/**
 * Reproducible JVM micro-benchmarks for the claims in docs/CHANGELOG.md. These measure algorithms on a
 * desktop JVM, NOT on a phone: use them to compare old vs new, not as device numbers.
 *
 * Run (from the repo root, after compiling com.example.core):
 *   java -Xmx256m -cp <core-classes>:<bench-classes>:kotlin-stdlib.jar CoreBenchKt
 */
private class OldNode(val name: String) { val children = ArrayList<OldNode>() }

/** Faithful re-implementation of the pre-1.1.0 tree builder: linear scan of siblings per path part. */
private fun oldBuild(paths: List<String>) {
    val root = OldNode("")
    for (p in paths) {
        var cur = root
        for (part in p.split('/')) {
            var next = cur.children.find { it.name == part }
            if (next == null) { next = OldNode(part); cur.children.add(next) }
            cur = next
        }
    }
}

private fun ms(block: () -> Unit): Long { val t = System.nanoTime(); block(); return (System.nanoTime() - t) / 1_000_000 }

fun main() {
    val rt = Runtime.getRuntime()
    println("JVM max heap: ${rt.maxMemory() / 1024 / 1024} MB")

    // ---- 1. Large text file: format + charset detection ------------------------------------------
    val big = File.createTempFile("bench", ".txt")
    BufferedOutputStream(big.outputStream(), 1 shl 20).use { out ->
        val line = "The quick brown fox jumps over the lazy dog 0123456789\n".toByteArray()
        var written = 0L
        while (written < 500L * 1024 * 1024) { out.write(line); written += line.size }
    }
    println("test file: ${big.length() / 1024 / 1024} MB")

    // Old FileManager.determineCharset(): file.readBytes() just to look at the first 4 bytes.
    val old = try {
        val t = System.nanoTime(); val b = big.readBytes(); b.size.toString() + " bytes read in " + (System.nanoTime() - t) / 1_000_000 + " ms"
    } catch (e: OutOfMemoryError) { "OutOfMemoryError" }
    println("OLD  readBytes() on 500 MB file            : $old")

    val detectionMs = ms { FormatDetector.detect(big) }
    val loadMs = ms { TextLoader.load(big) }
    val loaded = TextLoader.load(big)
    val windowMs = ms { TextWindowReader.read(big, big.length() - 1_000_000, 64 * 1024) }
    println("NEW  FormatDetector.detect(500 MB file)    : $detectionMs ms")
    println("NEW  TextLoader.load (250k-char preview)   : $loadMs ms  (truncated=${loaded.truncated}, editable=${loaded.editable})")
    println("NEW  TextWindowReader at offset ~499 MB    : $windowMs ms")
    big.delete()

    // ---- 2. Archive tree construction ------------------------------------------------------------
    for (n in listOf(20_000, 50_000, 100_000)) {
        val flat = (0 until n).map { "file$it.txt" }
        val oldMs = ms { oldBuild(flat) }
        val newMs = ms {
            ArchiveTreeBuilder.build("x", flat.asSequence().map { ArchiveEntryInfo(it, false, 1) }, ArchiveLimits(maxEntries = 1_000_000))
        }
        println("tree, $n files in ONE folder : old=${oldMs} ms  new=${newMs} ms")
    }
    val nested = (0 until 100_000).map { "d${it % 100}/sub${it % 7}/file$it.txt" }
    val oldNested = ms { oldBuild(nested) }
    val newNested = ms { ArchiveTreeBuilder.build("x", nested.asSequence().map { ArchiveEntryInfo(it, false, 1) }, ArchiveLimits(maxEntries = 1_000_000)) }
    println("tree, 100000 files nested    : old=${oldNested} ms  new=${newNested} ms")
}
