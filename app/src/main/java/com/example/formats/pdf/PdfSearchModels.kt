package com.example.formats.pdf

data class PdfPageSearchMatch(val pageNumber: Int)

data class PdfSearchSnapshot(
    val count: Int = 0,
    val truncated: Boolean = false,
    val matches: List<PdfPageSearchMatch> = emptyList()
)
