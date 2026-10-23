package com.example.feature.preview.model

/** Where the in-document viewer should scroll next (outline tap or go-to-line). */
data class ViewerScrollRequest(
    val lineNumber: Int,
    val headingIndex: Int? = null
)
