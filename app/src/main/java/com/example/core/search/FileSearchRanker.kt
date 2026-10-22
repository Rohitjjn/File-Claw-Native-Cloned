package com.example.core.search

/**
 * Pure ranking + highlight logic for the global file-name search (v1.3.0).
 *
 * Device search used to return index-walk order with no relevance signal.
 * This policy sorts matches by how well the file name matches the query:
 * exact > prefix > word-boundary > substring, then by shorter/canonical name.
 */
object FileSearchRanker {

    data class HighlightRange(val start: Int, val end: Int)

    /** Relevance buckets (higher = better). */
    fun relevanceScore(query: String, name: String): Int {
        val q = query.trim()
        if (q.isEmpty()) return 0
        val qi = q.lowercase()
        val ni = name.lowercase()
        return when {
            ni == qi -> 400
            ni.startsWith(qi) -> 300
            // Word-boundary match: query starts after a separator character.
            indexOfWordBoundary(qi, ni) >= 0 -> 200
            ni.contains(qi) -> 100
            else -> 0
        }
    }

    private fun indexOfWordBoundary(queryLower: String, nameLower: String): Int {
        var index = nameLower.indexOf(queryLower)
        while (index > 0) {
            val previous = nameLower[index - 1]
            if (!previous.isLetterOrDigit()) return index
            index = nameLower.indexOf(queryLower, index + 1)
        }
        return if (index == 0) 0 else -1
    }

    /** First case-insensitive occurrence of [query] in [name], for UI highlight spans. */
    fun firstMatchRange(query: String, name: String): HighlightRange? {
        val q = query.trim()
        if (q.isEmpty()) return null
        val index = name.lowercase().indexOf(q.lowercase())
        return if (index >= 0) HighlightRange(index, index + q.length) else null
    }

    /**
     * Ranks [items] by relevance to [query] (descending), tie-broken by shorter
     * name first and then alphabetically. Items scoring 0 are dropped.
     * Returns at most [limit] items.
     */
    fun <T> rank(
        items: List<T>,
        query: String,
        nameOf: (T) -> String,
        limit: Int
    ): List<T> {
        if (query.trim().isEmpty()) return emptyList()
        return items
            .asSequence()
            .map { item -> item to relevanceScore(query, nameOf(item)) }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<T, Int>> { it.second }
                    .thenBy { nameOf(it.first).length }
                    .thenBy { nameOf(it.first).lowercase() }
            )
            .take(limit)
            .map { it.first }
            .toList()
    }

    /** True when more than [limit] matches exist (used for the "showing first N" notice). */
    fun isTruncated(matchCount: Int, limit: Int): Boolean = matchCount > limit
}
