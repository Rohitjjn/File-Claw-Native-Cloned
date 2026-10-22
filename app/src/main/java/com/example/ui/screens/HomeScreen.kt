package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.Star
import com.example.ui.component.FileExtensionBadge
import com.example.ui.component.StaggeredAnimatedItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.ui.component.ClaudeAppBar
import com.example.ui.component.ClaudeButton
import com.example.ui.component.ClaudeCard
import com.example.ui.component.FileIcon
import com.example.ui.component.premiumLoadingPulse
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToPreview: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAllFiles: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val files by viewModel.recentFilesState.collectAsState()
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFileState by viewModel.currentFileState.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()
    val loadingFilePathState = viewModel.loadingFilePath.collectAsState()
    
    val context = LocalContext.current
    var isSearchActive by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var isGridView by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showFavoritesOnly by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) {
            viewModel.updateSearchQuery("")
        }
    }

    // Launcher for Android Document Picker
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFileFromUri(it)
        }
    }

    // Dynamic greeting based on system hour
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier.testTag("home_screen"),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    // Drawer Header with Version details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(45.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Files Claw",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your Files",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Row {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    onNavigateToSearch()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Files",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(onClick = { pickerLauncher.launch(arrayOf("*/*")) }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Import File",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    var selectedDrawerTab by remember { mutableStateOf(0) }
                    TabRow(
                        selectedTabIndex = selectedDrawerTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedDrawerTab == 0,
                            onClick = { selectedDrawerTab = 0 },
                            text = { Text("RECENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                        )
                        Tab(
                            selected = selectedDrawerTab == 1,
                            onClick = { selectedDrawerTab = 1 },
                            text = { Text("FILE SYSTEM", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedDrawerTab == 0) {
                        val settings by viewModel.settingsState.collectAsState()
                        val drawerFiles by remember(files, settings.historyLimit) { 
                            derivedStateOf { files.take(settings.historyLimit) }
                        }
                        LazyColumn(
                            state = rememberLazyListState(),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(drawerFiles, key = { it.id }, contentType = { "DrawerFile" }) { file ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                drawerState.close()
                                                viewModel.openFile(file)
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val isLoading by remember(file.path) { androidx.compose.runtime.derivedStateOf { loadingFilePathState.value == file.path } }
                                            FileIcon(
                                                extension = file.extension, 
                                                size = 32, 
                                                isSample = file.isSample,
                                                modifier = Modifier.premiumLoadingPulse(isLoading)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = file.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val elapsedText = remember(file.lastOpened) { formatElapsedTime(file.lastOpened) }
                                                Text(
                                                    text = elapsedText,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteRecentFile(file) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Remove from history",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (files.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No files tracked yet",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val rootDir = android.os.Environment.getExternalStorageDirectory()
                        FileTreeComponent(
                            rootDir = rootDir,
                            onFileClick = { f ->
                                coroutineScope.launch {
                                    drawerState.close()
                                    viewModel.openFile(
                                        com.example.data.RecentFileEntity(
                                            name = f.name,
                                            path = f.absolutePath,
                                            extension = f.extension,
                                            size = f.length(),
                                            lastOpened = System.currentTimeMillis()
                                        )
                                    )
                                }
                            },
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Text(
                        text = "v1.0.0",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isSearchActive) {
                    // Inline beautiful search bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .statusBarsPadding()
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            IconButton(
                                onClick = { isSearchActive = false; viewModel.updateSearchQuery("") },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close Search", modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Search files...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                            )
                                        }
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.updateSearchQuery(it) },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(searchFocusRequester)
                                        )
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.updateSearchQuery("") },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        LaunchedEffect(Unit) {
                            searchFocusRequester.requestFocus()
                        }
                    }
                } else {
                    ClaudeAppBar(
                        title = "Files Claw",
                        onNavIconClick = { coroutineScope.launch { drawerState.open() } },
                        navIcon = Icons.Default.Menu,
                        actions = {
                            IconButton(onClick = onNavigateToSearch) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isInitialized) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    val isEmpty = files.isEmpty()
                    if (isEmpty) {
                        // Minimal, striking empty state centered exactly like screenshots 3 and 4
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.ClaudeOnyx
                                    val containerColor = if (isDark) Color(0xFF261D1A) else Color(0xFFFBECE5)
                                    val iconColor = Color(0xFFD97757)

                                    Surface(
                                        shape = RoundedCornerShape(28.dp),
                                        color = containerColor,
                                        modifier = Modifier.size(126.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = iconColor,
                                                modifier = Modifier.size(56.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        text = "Open a file to begin",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Select any file from your device to preview or edit.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    Spacer(modifier = Modifier.height(28.dp))

                                    ClaudeButton(
                                        text = "Select File",
                                        onClick = { pickerLauncher.launch(arrayOf("*/*")) },
                                        icon = Icons.Default.Add,
                                        testTag = "empty_select_file_button"
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        text = "Recent files appear here",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val targetFiles by remember(files, showFavoritesOnly, favoritePaths) {
                                derivedStateOf {
                                    if (showFavoritesOnly) files.filter { favoritePaths.contains(it.path) } else files
                                }
                            }
                            val displayedFiles by remember(targetFiles, isGridView) {
                                derivedStateOf { targetFiles.take(if (isGridView) 6 else 5) }
                            }
                            val favCount by remember(files, favoritePaths) {
                                derivedStateOf { files.count { favoritePaths.contains(it.path) } }
                            }

                            // Striking filled list state with custom header and view all button
                            LazyColumn(
                                state = rememberLazyListState(),
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 100.dp)
                            ) {
                                // Header greeting exactly matching screen
                                item(contentType = "Header") {
                                    Column {
                                        Spacer(modifier = Modifier.height(16.dp)) // Safe top margin buffer to prevent overlapping under header bar
                                        Text(
                                            text = greeting,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "What would you like to open today?",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                item(contentType = "RecentFilesHeader") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Recent Files",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Grid / List Toggle
                                                IconButton(
                                                    onClick = { isGridView = !isGridView },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                                                        contentDescription = "Toggle Grid or List View",
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(4.dp))

                                                if (files.isNotEmpty()) {
                                                    Text(
                                                        text = "View All",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier
                                                            .clickable { onNavigateToAllFiles() }
                                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                                            .testTag("view_all_button")
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Filter Chips: All vs Favorites
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilterChip(
                                                selected = !showFavoritesOnly,
                                                onClick = { showFavoritesOnly = false },
                                                label = { Text("All (${files.size})", fontSize = 12.sp) },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            FilterChip(
                                                selected = showFavoritesOnly,
                                                onClick = { showFavoritesOnly = true },
                                                label = { Text("★ Favorites ($favCount)", fontSize = 12.sp) },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFFE5A93C).copy(alpha = 0.18f),
                                                    selectedLabelColor = Color(0xFFD49228)
                                                )
                                            )
                                        }
                                    }
                                }

                                if (displayedFiles.isEmpty() && showFavoritesOnly) {
                                    item(contentType = "EmptyFavorites") {
                                        ClaudeCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.StarOutline,
                                                    contentDescription = null,
                                                    tint = Color(0xFFE5A93C),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "No favorites starred yet",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Tap the star icon on any file item to add it to your favorites.",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else if (isGridView) {
                                    // 2-Column Grid Layout with Staggered Spring Animation
                                    val rows = displayedFiles.chunked(2)
                                    itemsIndexed(rows, key = { _, row -> row.joinToString("-") { it.id.toString() } }, contentType = { _, _ -> "GridRow" }) { rowIndex, rowFiles ->
                                        StaggeredAnimatedItem(index = rowIndex) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                for (file in rowFiles) {
                                                    val isFav = favoritePaths.contains(file.path)
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        ClaudeCard(
                                                            onClick = { viewModel.openFile(file) }
                                                        ) {
                                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    FileExtensionBadge(extension = file.extension)
                                                                    IconButton(
                                                                        onClick = { viewModel.toggleFavorite(file.path) },
                                                                        modifier = Modifier.size(28.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                                            contentDescription = "Toggle favorite",
                                                                            tint = if (isFav) Color(0xFFE5A93C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                                                            modifier = Modifier.size(18.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                Box(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    val isLoading by remember(file.path) { androidx.compose.runtime.derivedStateOf { loadingFilePathState.value == file.path } }
                                                                    FileIcon(
                                                                        extension = file.extension,
                                                                        size = 46,
                                                                        isSample = file.isSample,
                                                                        modifier = Modifier.premiumLoadingPulse(isLoading)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(10.dp))
                                                                Text(
                                                                    text = file.name,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                val formattedSize = remember(file.size) { formatFileSize(file.size) }
                                                                Text(
                                                                    text = formattedSize,
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                if (rowFiles.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // List Layout
                                    itemsIndexed(displayedFiles, key = { _, it -> it.id }, contentType = { _, _ -> "RecentFile" }) { fileIndex, file ->
                                        val isFav = favoritePaths.contains(file.path)
                                        StaggeredAnimatedItem(index = fileIndex) {
                                            ClaudeCard(
                                                onClick = { viewModel.openFile(file) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        val isLoading by remember(file.path) { androidx.compose.runtime.derivedStateOf { loadingFilePathState.value == file.path } }
                                                        FileIcon(
                                                            extension = file.extension, 
                                                            size = 42, 
                                                            isSample = file.isSample,
                                                            modifier = Modifier.premiumLoadingPulse(isLoading)
                                                        )
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Column {
                                                            Text(
                                                                text = file.name,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            val formattedSize = remember(file.size) { formatFileSize(file.size) }
                                                            val formattedTime = remember(file.lastOpened) { formatElapsedTime(file.lastOpened) }
                                                            Text(
                                                                text = "${file.extension.uppercase()}  •  $formattedSize  •  $formattedTime",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = { viewModel.toggleFavorite(file.path) },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                                contentDescription = "Toggle favorite",
                                                                tint = if (isFav) Color(0xFFE5A93C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        Icon(
                                                            imageVector = Icons.Default.ChevronRight,
                                                            contentDescription = "Open Preview",
                                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    // FIXED bottom layout Open New File button
                    if (files.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                        ) {
                            ClaudeButton(
                                text = "Open New File",
                                onClick = { pickerLauncher.launch(arrayOf("*/*")) },
                                icon = Icons.Default.Add,
                                testTag = "open_new_file_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helpers for formatted visual indicators
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
