package com.example.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One-line banner for read-only, truncated, or converted views. */
@Composable
fun NoticeBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
