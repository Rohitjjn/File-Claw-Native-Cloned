package com.example.ui.component

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ViewerSyntaxTheme(
    val title: String,
    val subtitle: String,
    val bgColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val hexBg: String,
    val hexText: String,
    val hexAccent: String
) {
    AUTO(
        title = "Auto (App Theme)",
        subtitle = "Follows the app's light / dark mode",
        bgColor = Color(0xFFFAF9F5),
        textColor = Color(0xFF141413),
        accentColor = Color(0xFFCC785C),
        hexBg = "#faf9f5",
        hexText = "#141413",
        hexAccent = "#cc785c"
    ),
    CLAUDE_GLASS(
        title = "Claude Glass",
        subtitle = "Restrained, ultra-clean neutral surface",
        bgColor = Color(0xFF1E1E1E),
        textColor = Color(0xFFD4D4D4),
        accentColor = Color(0xFFD97706),
        hexBg = "#1e1e1e",
        hexText = "#d4d4d4",
        hexAccent = "#d97706"
    ),
    NORD_DARK(
        title = "Nord Dark",
        subtitle = "Arctic blues, soft frost syntax palette",
        bgColor = Color(0xFF2E3440),
        textColor = Color(0xFFD8DEE9),
        accentColor = Color(0xFF88C0D0),
        hexBg = "#2e3440",
        hexText = "#d8dee9",
        hexAccent = "#88c0d0"
    ),
    WARM_PARCHMENT(
        title = "Warm Parchment",
        subtitle = "Warm antique paper tone for long reading",
        bgColor = Color(0xFFFBF0D9),
        textColor = Color(0xFF433422),
        accentColor = Color(0xFFB45309),
        hexBg = "#fbf0d9",
        hexText = "#433422",
        hexAccent = "#b45309"
    );

    /** True for the pseudo-theme that defers to the app's light/dark palette. */
    val isAuto: Boolean get() = this == AUTO

    companion object {
        fun fromName(name: String?): ViewerSyntaxTheme {
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) || it.title.equals(name, ignoreCase = true) }
                ?: AUTO
        }
    }
}

object ViewerAppearancePrefs {
    private const val PREFS_NAME = "viewer_appearance_prefs"
    private const val KEY_THEME = "syntax_theme"
    private const val KEY_FONT_SIZE = "viewer_font_size_sp"
    private const val KEY_ZOOM_PERCENT = "viewer_zoom_percent"
    private const val KEY_DOCX_ZOOM_PERCENT = "docx_zoom_percent"

    fun getTheme(context: Context): ViewerSyntaxTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_THEME, ViewerSyntaxTheme.AUTO.name)
        return ViewerSyntaxTheme.fromName(name)
    }

    fun setTheme(context: Context, theme: ViewerSyntaxTheme) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun getFontSize(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FONT_SIZE, 16).coerceIn(11, 22)
    }

    fun setFontSize(context: Context, sizeSp: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_FONT_SIZE, sizeSp.coerceIn(11, 22)).apply()
    }

    /** Browser-style document zoom in percent (25 - 500, 100 = actual size). */
    fun getZoomPercent(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ZOOM_PERCENT, 100).coerceIn(25, 500)
    }

    fun setZoomPercent(context: Context, percent: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_ZOOM_PERCENT, percent.coerceIn(25, 500)).apply()
    }

    /**
     * Persisted DOCX zoom in percent (25 - 500). Defaults to the shipped 40% opening zoom.
     * Used by the preview screen to remember the user's last DOCX zoom across sessions.
     */
    fun getDocxZoomPercent(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DOCX_ZOOM_PERCENT, 40).coerceIn(25, 500)
    }

    fun setDocxZoomPercent(context: Context, percent: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DOCX_ZOOM_PERCENT, percent.coerceIn(25, 500)).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceBottomSheet(
    currentTheme: ViewerSyntaxTheme,
    currentFontSizeSp: Int,
    onThemeChanged: (ViewerSyntaxTheme) -> Unit,
    onFontSizeChanged: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    currentZoomPercent: Int = 100,
    onZoomChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("appearance_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Viewer Appearance",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Theme, text size & zoom controls",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close appearance sheet",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Themes
            Text(
                text = "SYNTAX THEME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewerSyntaxTheme.values().forEach { theme ->
                    val isSelected = currentTheme == theme
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeChanged(theme)
                                ViewerAppearancePrefs.setTheme(context, theme)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Theme color preview circle
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = theme.bgColor,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Aa",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = theme.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = theme.subtitle,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                }
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onThemeChanged(theme)
                                    ViewerAppearancePrefs.setTheme(context, theme)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Font Size Adjuster
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "FONT SIZE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Text & code viewer scaling",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val next = (currentFontSizeSp - 1).coerceAtLeast(11)
                            onFontSizeChanged(next)
                            ViewerAppearancePrefs.setFontSize(context, next)
                        },
                        enabled = currentFontSizeSp > 11,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease font size", modifier = Modifier.size(16.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.width(60.dp).height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${currentFontSizeSp}sp",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val next = (currentFontSizeSp + 1).coerceAtMost(22)
                            onFontSizeChanged(next)
                            ViewerAppearancePrefs.setFontSize(context, next)
                        },
                        enabled = currentFontSizeSp < 22,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase font size", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Browser-style Document Zoom (25% - 500%)
            Text(
                text = "DOCUMENT ZOOM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Like a browser, 25% - 500%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$currentZoomPercent%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val next = ((currentZoomPercent - 10) / 10) * 10
                                if (next >= 25) {
                                    onZoomChanged(next.coerceIn(25, 500))
                                    ViewerAppearancePrefs.setZoomPercent(context, next.coerceIn(25, 500))
                                }
                            },
                            enabled = currentZoomPercent > 25,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom out", modifier = Modifier.size(16.dp))
                        }

                        Slider(
                            value = currentZoomPercent.toFloat(),
                            onValueChange = { raw ->
                                val stepped = ((raw / 5).toInt() * 5).coerceIn(25, 500)
                                onZoomChanged(stepped)
                                ViewerAppearancePrefs.setZoomPercent(context, stepped)
                            },
                            valueRange = 25f..500f,
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val next = ((currentZoomPercent + 10) / 10) * 10
                                if (next <= 500) {
                                    onZoomChanged(next.coerceIn(25, 500))
                                    ViewerAppearancePrefs.setZoomPercent(context, next.coerceIn(25, 500))
                                }
                            },
                            enabled = currentZoomPercent < 500,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom in", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick presets, matching the browser zoom menu feel.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(50, 75, 100, 150, 200, 300, 500).forEach { preset ->
                            val isPresetSelected = currentZoomPercent == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPresetSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isPresetSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                onClick = {
                                    onZoomChanged(preset)
                                    ViewerAppearancePrefs.setZoomPercent(context, preset)
                                }
                            ) {
                                Text(
                                    text = "$preset%",
                                    fontSize = 11.sp,
                                    fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPresetSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Live Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = currentTheme.bgColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "// Live Code Preview",
                        fontSize = currentFontSizeSp.sp,
                        fontFamily = FontFamily.Monospace,
                        color = currentTheme.accentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "fun renderDocument(file: String) { println(file) }",
                        fontSize = currentFontSizeSp.sp,
                        fontFamily = FontFamily.Monospace,
                        color = currentTheme.textColor
                    )
                }
            }
        }
    }
}
