package com.example.formats.media

/** Formats media positions as mm:ss, matching the existing audio player surface. */
fun formatMediaTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
