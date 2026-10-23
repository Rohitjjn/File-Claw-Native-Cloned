package com.example.formats.font.ui

import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.formats.common.ui.formatHumanBytes
import com.example.ui.component.NoticeBanner
import java.io.File

@Composable
fun FontViewer(fileEntity: RecentFileEntity, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val face = remember(fileEntity.path) { runCatching { Typeface.createFromFile(File(fileEntity.path)) }.getOrNull() }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            text = "${fileEntity.name} \u2022 ${formatHumanBytes(fileEntity.size)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        if (face == null) {
            NoticeBanner("This font file could not be loaded.", Modifier.padding(top = 12.dp))
            return@Column
        }
        val samples = listOf(
            12f to "The quick brown fox jumps over the lazy dog",
            18f to "Sphinx of black quartz, judge my vow",
            28f to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            28f to "abcdefghijklmnopqrstuvwxyz",
            28f to "0123456789 !?&@#%()[]{}",
            48f to "Aa Bb Cc",
            72f to "Gg"
        )
        AndroidView(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            factory = { ctx ->
                LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    for ((size, sample) in samples) {
                        addView(TextView(ctx).apply {
                            text = sample
                            textSize = size
                            setTextColor(textColor)
                            typeface = face
                            setPadding(0, 8, 0, 8)
                        })
                    }
                }
            }
        )
    }
}
