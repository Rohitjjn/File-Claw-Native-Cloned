package com.example.ui.component

import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.glass
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import com.example.ui.theme.ClaudeMutedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaudeAppBar(
    title: String,
    onNavIconClick: (() -> Unit)? = null,
    navIcon: ImageVector? = Icons.Default.Menu,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glass = com.example.ui.theme.LocalGlass.current
    Surface(
        color = glass.fillStrong,
        modifier = modifier
            .testTag("claude_app_bar")
            .fillMaxWidth()
    ) {
        Column {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (onNavIconClick != null && navIcon != null) {
                        IconButton(onClick = onNavIconClick, modifier = Modifier.testTag("app_bar_nav_icon")) {
                            Icon(
                                imageVector = navIcon,
                                contentDescription = "Navigation Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.statusBars
            )
            androidx.compose.material3.HorizontalDivider(color = glass.strokeEnd)
        }
    }
}

@Composable
fun ClaudeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color? = null,
    testTag: String = "claude_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp), // Premium crisp Claude styling (rounded.md)
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = (containerColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier
            .testTag(testTag)
            .minimumInteractiveComponentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}


fun Modifier.premiumLoadingPulse(isLoading: Boolean): Modifier = this.then(
    if (isLoading) {
        Modifier.graphicsLayer {
            alpha = 0.5f // Simplified premium dim effect without heavy infinite animations
        }
    } else {
        Modifier
    }
)

@Composable
fun PremiumSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), RoundedCornerShape(8.dp))
    )
}

@Composable
fun PremiumLoadingIndicator(modifier: Modifier = Modifier, text: String? = null) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            modifier = Modifier.size(48.dp)
        )
        if (text != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.premiumLoadingPulse(true)
            )
        }
    }
}

@Composable
fun ClaudeCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardShape = RoundedCornerShape(16.dp)
    androidx.compose.material3.Surface(
        shape = cardShape,
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, borderColor) else null,
        modifier = modifier
            .glass(cardShape, strong = isSelected)
            .let {
                if (onClick != null) {
                    it.clickable(onClick = onClick)
                } else {
                    it
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun FileIcon(
    extension: String,
    modifier: Modifier = Modifier,
    size: Int = 40,
    isSample: Boolean = false,
    isRecent: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.ClaudeOnyx
    
    val iconBgColor = if (isRecent) {
        if (isDark) Color(0xFF3B2520) else Color(0xFFFBECE5)
    } else {
        if (isDark) Color(0xFF1F2E2B) else Color(0xFFE6F5F2)
    }

    val iconColor = if (isRecent) {
        Color(0xFFCC785C)
    } else {
        Color(0xFF5DB8A6)
    }

    val iconVector = when (extension.lowercase()) {
        "md" -> Icons.Outlined.Book
        "zip" -> Icons.Outlined.FolderZip
        "csv" -> Icons.Outlined.TableChart
        "kt" -> Icons.Outlined.Code
        "txt" -> Icons.Outlined.Description
        "pdf" -> Icons.Outlined.PictureAsPdf
        "docx" -> Icons.Outlined.InsertDriveFile
        "pptx" -> Icons.Outlined.BackupTable
        "ppt" -> Icons.Outlined.BackupTable
        "mp3" -> Icons.Outlined.MusicNote
        "wav" -> Icons.Outlined.MusicNote
        "m4a" -> Icons.Outlined.MusicNote
        "mp4" -> Icons.Outlined.PlayCircle
        "mkv" -> Icons.Outlined.PlayCircle
        "webm" -> Icons.Outlined.PlayCircle
        else -> Icons.Outlined.InsertDriveFile
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = iconBgColor,
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f)),
        modifier = modifier.size(size.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = iconVector,
                contentDescription = "$extension icon",
                tint = iconColor,
                modifier = Modifier.size((size * 0.55).dp)
            )
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}

fun formatElapsedTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 60 * 1000) return "just now"
    val mins = diff / (60 * 1000)
    if (mins < 60) return if (mins == 1L) "1 min ago" else "$mins mins ago"
    val hours = mins / 60
    if (hours < 24) return if (hours == 1L) "1 hour ago" else "$hours hours ago"
    val days = hours / 24
    return if (days == 1L) "1 day ago" else "$days days ago"
}

@Composable
fun FileExtensionBadge(extension: String, modifier: Modifier = Modifier) {
    val ext = extension.uppercase()
    val (badgeBg, badgeFg) = when (ext) {
        "PDF" -> Color(0xFFE53935).copy(alpha = 0.15f) to Color(0xFFD32F2F)
        "TXT", "LOG", "MD" -> Color(0xFFFF9800).copy(alpha = 0.15f) to Color(0xFFE65100)
        "PNG", "JPG", "JPEG", "WEBP", "GIF", "SVG" -> Color(0xFF2196F3).copy(alpha = 0.15f) to Color(0xFF1976D2)
        "JSON", "XML", "HTML", "JS", "KT", "PY" -> Color(0xFF9C27B0).copy(alpha = 0.15f) to Color(0xFF7B1FA2)
        "ZIP", "RAR", "7Z", "TAR", "GZ" -> Color(0xFF795548).copy(alpha = 0.15f) to Color(0xFF5D4037)
        "DOCX", "PPTX", "XLSX" -> Color(0xFF009688).copy(alpha = 0.15f) to Color(0xFF00796B)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeBg,
        modifier = modifier
    ) {
        Text(
            text = ext.take(4),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = badgeFg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val delayMillis = ((index % 12) * 35).toLong()
        kotlinx.coroutines.delay(delayMillis)
        isVisible = true
    }

    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 260,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "stagger_alpha"
    )

    val translationY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 0f else 28f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "stagger_trans_y"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
            }
    ) {
        content()
    }
}
