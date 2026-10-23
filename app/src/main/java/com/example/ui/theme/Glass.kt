package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Glass design tokens (see docs/DESIGN.md). The effect is built from translucent fills, a top sheen,
 * a gradient hairline and soft ambient colour behind the content. Radial gradients stand in for blur,
 * so it costs almost nothing on any Android version and never blurs the text itself.
 */
@Immutable
data class GlassTokens(
    val fill: Color,
    val fillStrong: Color,
    val strokeStart: Color,
    val strokeEnd: Color,
    val sheen: Color,
    val glowA: Color,
    val glowB: Color,
    val glowC: Color
)

val LightGlass = GlassTokens(
    fill = Color.White.copy(alpha = 0.62f),
    fillStrong = Color.White.copy(alpha = 0.80f),
    strokeStart = Color.White.copy(alpha = 0.95f),
    strokeEnd = ClaudeTerracotta.copy(alpha = 0.20f),
    sheen = Color.White.copy(alpha = 0.55f),
    glowA = ClaudeTerracotta.copy(alpha = 0.22f),
    glowB = ClaudeOrangeAccent.copy(alpha = 0.18f),
    glowC = Color(0xFFB9A6D9).copy(alpha = 0.16f)
)

val DarkGlass = GlassTokens(
    fill = Color(0xFF2B2926).copy(alpha = 0.58f),
    fillStrong = Color(0xFF32302C).copy(alpha = 0.80f),
    strokeStart = Color.White.copy(alpha = 0.18f),
    strokeEnd = Color.White.copy(alpha = 0.04f),
    sheen = Color.White.copy(alpha = 0.07f),
    glowA = ClaudeTerracotta.copy(alpha = 0.28f),
    glowB = ClaudeOrangeAccent.copy(alpha = 0.14f),
    glowC = Color(0xFF6B7FD7).copy(alpha = 0.14f)
)

val LocalGlass = staticCompositionLocalOf { LightGlass }

/** Translucent rounded surface with a gradient hairline border. */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(16.dp), strong: Boolean = false): Modifier {
    val g = LocalGlass.current
    return this
        .clip(shape)
        .background(if (strong) g.fillStrong else g.fill)
        .background(Brush.verticalGradient(listOf(g.sheen, Color.Transparent)))
        .border(1.dp, Brush.linearGradient(listOf(g.strokeStart, g.strokeEnd)), shape)
}

/** Solid app background plus three soft ambient glows; put full-screen content inside it. */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val g = LocalGlass.current
    val base = MaterialTheme.colorScheme.background
    Box(modifier = modifier.fillMaxSize().background(base)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension
            drawRect(Brush.radialGradient(listOf(g.glowA, Color.Transparent), center = Offset(size.width * 0.05f, size.height * 0.04f), radius = r * 0.95f))
            drawRect(Brush.radialGradient(listOf(g.glowB, Color.Transparent), center = Offset(size.width * 0.98f, size.height * 0.40f), radius = r * 0.85f))
            drawRect(Brush.radialGradient(listOf(g.glowC, Color.Transparent), center = Offset(size.width * 0.30f, size.height * 1.02f), radius = r * 0.90f))
        }
        content()
    }
}
