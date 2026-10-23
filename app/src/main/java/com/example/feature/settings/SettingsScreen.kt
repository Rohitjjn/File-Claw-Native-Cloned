package com.example.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Storage
import android.widget.Toast
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.component.ClaudeAppBar
import com.example.ui.component.ClaudeCard
import com.example.utils.CacheManager
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Redesigned SettingsScreen in Claude style:
 * Premium, minimalistic, warm glass cards, streamlined options, and clear storage management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settingsState.collectAsState()
    val context = LocalContext.current
    val cacheManager = remember(context) { CacheManager.getInstance(context) }

    var cacheRefreshKey by remember { mutableIntStateOf(0) }
    val cacheSize = remember(cacheRefreshKey) { cacheManager.getTotalSize() }
    val cacheBreakdown = remember(cacheRefreshKey) { cacheManager.getBreakdown() }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val fontSizes = remember { listOf("Small", "Medium", "Large") }
    val tabSizes = remember { listOf(2, 4) }
    val encodings = remember { listOf("Auto", "UTF-8", "ASCII", "ISO-8859-1", "Windows-1252") }

    Scaffold(
        topBar = {
            ClaudeAppBar(
                title = "Settings",
                onNavIconClick = onBackClick,
                navIcon = Icons.AutoMirrored.Filled.ArrowBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("settings_screen")
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. APPEARANCE & THEME
            SettingsSectionHeader(title = "APPEARANCE")
            ClaudeCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Color Theme",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 3-Segment Theme Picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("Light", "Light", Icons.Outlined.LightMode),
                            Triple("Dark", "Dark", Icons.Outlined.DarkMode),
                            Triple("System", "System", Icons.Outlined.BrightnessAuto)
                        ).forEach { (key, label, icon) ->
                            val isSelected = settings.theme == key
                            Surface(
                                onClick = { viewModel.updateSettings(settings.copy(theme = key)) },
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Reading Font Size
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Reading & Code Text Size",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            fontSizes.forEach { size ->
                                val isSelected = settings.fontSize == size
                                Surface(
                                    onClick = { viewModel.updateSettings(settings.copy(fontSize = size)) },
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = size,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. READING & EDITOR
            SettingsSectionHeader(title = "READING & EDITOR")
            ClaudeCard {
                SettingsToggleRow(
                    title = "Show Line Numbers",
                    description = "Display gutter line numbers in text and code files",
                    checked = settings.showLineNumbers,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(showLineNumbers = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SettingsToggleRow(
                    title = "Word Wrap",
                    description = "Soft-wrap long lines to fit the reading viewport",
                    checked = settings.wordWrap,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(wordWrap = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SettingsToggleRow(
                    title = "Auto-save Edits",
                    description = "Automatically save modified files when leaving editor",
                    checked = settings.autoSaveDrafts,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(autoSaveDrafts = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SettingsToggleRow(
                    title = "Default to Edit Mode",
                    description = "Open editable text files directly in edit mode",
                    checked = settings.defaultToEditOnOpen,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(defaultToEditOnOpen = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Tab spacing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tab Indentation",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Number of spaces inserted for tab key",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tabSizes.forEach { spaces ->
                            val isSelected = settings.tabSize == spaces
                            Surface(
                                onClick = { viewModel.updateSettings(settings.copy(tabSize = spaces)) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(
                                        text = "$spaces spaces",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. STORAGE & PERFORMANCE
            SettingsSectionHeader(title = "STORAGE & PERFORMANCE")
            ClaudeCard {
                // Cache manager row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Preview & Render Cache",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Currently using $cacheSize",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Button(
                        onClick = { showClearCacheDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Load Benchmark Sample Files
                SettingsActionRow(
                    title = "Generate Benchmark Sample Files",
                    subtitle = "Creates formatted DOCX, PDF, LOG, TXT & MD test files",
                    icon = Icons.Outlined.Description,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        viewModel.seedBenchmarkSampleFiles { count ->
                            Toast.makeText(context, "$count benchmark files generated in Recent Files", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Clear Recent History
                SettingsActionRow(
                    title = "Clear Recent Files History",
                    subtitle = "Remove document history entries without deleting disk files",
                    icon = Icons.Outlined.History,
                    iconColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearHistoryDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Default File Encoding
                Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Default Text Encoding",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        encodings.forEach { enc ->
                            val isSelected = settings.defaultEncoding == enc
                            Surface(
                                onClick = { viewModel.updateSettings(settings.copy(defaultEncoding = enc)) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = enc,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. ABOUT & PRIVACY
            SettingsSectionHeader(title = "ABOUT")
            ClaudeCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "File Claw v1.4.0",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "100% Offline Document Previewer & Editor",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Zero Telemetry • All Data Stays On Device",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("This will free up $cacheSize of preview and temporary rendering data. All documents remain intact on your device.") },
            confirmButton = {
                TextButton(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        cacheManager.nuke()
                        withContext(Dispatchers.Main) {
                            cacheRefreshKey++
                        }
                    }
                    showClearCacheDialog = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Clear History Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Recent Files") },
            text = { Text("Are you sure you want to clear your recent file history? Your actual files will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearRecentFiles()
                    showClearHistoryDialog = false
                }) {
                    Text("Clear History", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (iconColor == MaterialTheme.colorScheme.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}
