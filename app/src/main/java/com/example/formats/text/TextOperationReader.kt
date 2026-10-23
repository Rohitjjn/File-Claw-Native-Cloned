package com.example.formats.text

import com.example.core.io.TextLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Bounded reads used by print/copy actions; the Compose screen does not open text streams itself. */
object TextOperationReader {
    const val MAX_OPERATION_CHARS = 2_000_000

    suspend fun readComplete(file: File, charsetName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val loaded = TextLoader.load(file, charsetName, maxChars = MAX_OPERATION_CHARS)
                check(!loaded.truncated) {
                    "This file exceeds the bounded $MAX_OPERATION_CHARS-character limit for this operation."
                }
                loaded.text
            }
        }
}
