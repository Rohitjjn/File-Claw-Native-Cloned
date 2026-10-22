package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AllFilesScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.FilePreviewScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.FilesClawTheme
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private companion object {
        const val STATE_SKIP_GATE = "skip_permission_gate"
    }

    private val _permissionCheckState = mutableStateOf(false)

    /** Same instance Compose gets from viewModel(): both are scoped to this Activity. */
    private val mainViewModel: MainViewModel by viewModels()

    /**
     * Shortcut intents to act on. Deliberately Compose state that starts null on every (re)creation:
     * the old code keyed a LaunchedEffect on Activity.getIntent(), which is not state, so a rotation
     * re-ran it and re-imported the same file / re-opened the last file each time.
     */
    private val pendingShortcutIntent = mutableStateOf<android.content.Intent?>(null)

    /** True once the user opened a file from another app or chose to continue without all-files access. */
    private val skipPermissionGate = mutableStateOf(false)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SKIP_GATE, skipPermissionGate.value)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /** Runs exactly once per delivered intent (never on configuration changes). */
    private fun handleIncomingIntent(actIntent: android.content.Intent) {
        if (actIntent.action == android.content.Intent.ACTION_VIEW) {
            actIntent.data?.let { uri ->
                // Opening a content:// document needs no broad storage permission at all.
                skipPermissionGate.value = true
                mainViewModel.importFileFromUri(uri)
            }
        } else {
            pendingShortcutIntent.value = actIntent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _permissionCheckState.value = hasRequiredPermissions(this)
        skipPermissionGate.value = savedInstanceState?.getBoolean(STATE_SKIP_GATE, false) == true
        if (savedInstanceState == null) handleIncomingIntent(intent)

        val requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            _permissionCheckState.value = hasRequiredPermissions(this)
        }

        val requestStandardPermissions = {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.READ_MEDIA_IMAGES,
                        android.Manifest.permission.READ_MEDIA_VIDEO,
                        android.Manifest.permission.READ_MEDIA_AUDIO
                    )
                )
            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }

        val requestManageAllFilesPermission = {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val settings by mainViewModel.settingsState.collectAsState()
            val fileEvent by mainViewModel.fileEvent.collectAsState()
            val hasPermissions = _permissionCheckState.value || skipPermissionGate.value

            // Display dynamic notifications (e.g. Save completed, Import files complete)
            LaunchedEffect(fileEvent) {
                fileEvent?.let { message ->
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                    mainViewModel.clearFileEvent()
                }
            }

            FilesClawTheme(themeSetting = settings.theme) {
                if (!hasPermissions) {
                    val isManageAllFilesRequired = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
                    PermissionGateScreen(
                        onRequestPermissions = requestStandardPermissions,
                        onRequestAllFilesAccess = requestManageAllFilesPermission,
                        isManageAllFilesRequired = isManageAllFilesRequired,
                        onContinueWithoutAccess = { skipPermissionGate.value = true }
                    )
                } else {
                    val navController = rememberNavController()

                    // Dynamic Shortcuts Registration
                    LaunchedEffect(Unit) {
                        try {
                            val shortcutLastOpened = androidx.core.content.pm.ShortcutInfoCompat.Builder(this@MainActivity, "shortcut_last_opened")
                                .setShortLabel("Last Opened File")
                                .setLongLabel("Open the last opened file")
                                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this@MainActivity, R.drawable.ic_shortcut_last_opened))
                                .setIntent(
                                    android.content.Intent(this@MainActivity, MainActivity::class.java).apply {
                                        action = "com.example.action.LAST_OPENED"
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                )
                                .build()

                            val shortcutHistory = androidx.core.content.pm.ShortcutInfoCompat.Builder(this@MainActivity, "shortcut_history")
                                .setShortLabel("History")
                                .setLongLabel("View recent files history")
                                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this@MainActivity, R.drawable.ic_shortcut_history))
                                .setIntent(
                                    android.content.Intent(this@MainActivity, MainActivity::class.java).apply {
                                        action = "com.example.action.HISTORY"
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                )
                                .build()

                            val shortcutSettings = androidx.core.content.pm.ShortcutInfoCompat.Builder(this@MainActivity, "shortcut_settings")
                                .setShortLabel("Settings")
                                .setLongLabel("Open app settings")
                                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this@MainActivity, R.drawable.ic_shortcut_settings))
                                .setIntent(
                                    android.content.Intent(this@MainActivity, MainActivity::class.java).apply {
                                        action = "com.example.action.SETTINGS"
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                )
                                .build()

                            androidx.core.content.pm.ShortcutManagerCompat.setDynamicShortcuts(
                                this@MainActivity,
                                listOf(shortcutLastOpened, shortcutHistory, shortcutSettings)
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Handle App Shortcuts Intents
                    LaunchedEffect(pendingShortcutIntent.value) {
                        pendingShortcutIntent.value?.let { actIntent ->
                            val action = actIntent.action
                            if (action == "com.example.action.LAST_OPENED") {
                                try {
                                    val lastFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        val db = com.example.data.AppDatabase.getDatabase(applicationContext)
                                        db.recentFileDao().getAllRecentFiles().first().firstOrNull()
                                    }
                                    if (lastFile != null) {
                                        mainViewModel.openFile(lastFile)
                                        if (lastFile.extension.lowercase() == "txt" && settings.defaultToEditOnOpen) {
                                            navController.navigate("editor") {
                                                launchSingleTop = true
                                            }
                                        } else {
                                            navController.navigate("preview") {
                                                launchSingleTop = true
                                            }
                                        }
                                    } else {
                                        Toast.makeText(applicationContext, "No history found", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else if (action == "com.example.action.HISTORY") {
                                navController.navigate("home") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            } else if (action == "com.example.action.SETTINGS") {
                                navController.navigate("settings") {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                    
                    val navigateToPreviewWithEditOnOpen = {
                        val fileState = mainViewModel.currentFileState.value
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute != "preview" && currentRoute != "editor") {
                            if (fileState.opensInEditor() && settings.defaultToEditOnOpen) {
                                navController.navigate("editor") {
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate("preview") {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    // Collect navigation events via one-shot channel flow
                    LaunchedEffect(mainViewModel) {
                        mainViewModel.navigationEvent.collect { event ->
                            when (event) {
                                is MainViewModel.NavigationEvent.NavigateToPreview -> {
                                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                                    if (currentRoute != "preview" && currentRoute != "editor") {
                                        if (event.fileState.opensInEditor() && settings.defaultToEditOnOpen) {
                                            navController.navigate("editor") {
                                                launchSingleTop = true
                                            }
                                        } else {
                                            navController.navigate("preview") {
                                                launchSingleTop = true
                                            }
                                        }
                                    } else if (currentRoute == "preview" && event.fileState.opensInEditor() && settings.defaultToEditOnOpen) {
                                        navController.navigate("editor") {
                                            popUpTo("preview") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                                is MainViewModel.NavigationEvent.ShowError -> {
                                    Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                            scaleIn(
                                initialScale = 0.97f,
                                animationSpec = tween(80, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(80))
                        },
                        exitTransition = {
                            scaleOut(
                                targetScale = 1.03f,
                                animationSpec = tween(60, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(60))
                        },
                        popEnterTransition = {
                            scaleIn(
                                initialScale = 1.03f,
                                animationSpec = tween(80, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(80))
                        },
                        popExitTransition = {
                            scaleOut(
                                targetScale = 0.97f,
                                animationSpec = tween(60, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(60))
                        }
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onNavigateToHome = {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                viewModel = mainViewModel,
                                onNavigateToPreview = navigateToPreviewWithEditOnOpen,
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToAllFiles = {
                                    navController.navigate("all_files")
                                },
                                onNavigateToSearch = {
                                    navController.navigate("search")
                                }
                            )
                        }

                        composable("all_files") {
                            AllFilesScreen(
                                viewModel = mainViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onNavigateToPreview = navigateToPreviewWithEditOnOpen
                            )
                        }

                        composable("search") {
                            SearchScreen(
                                viewModel = mainViewModel,
                                onBackClick = {
                                    mainViewModel.updateSearchQuery("")
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("preview") {
                            FilePreviewScreen(
                                viewModel = mainViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onNavigateToEditor = {
                                    navController.navigate("editor")
                                }
                            )
                        }

                        composable("editor") {
                            EditorScreen(
                                viewModel = mainViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = mainViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _permissionCheckState.value = hasRequiredPermissions(this)
    }

    private fun hasRequiredPermissions(context: android.content.Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun PermissionGateScreen(
    onRequestPermissions: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    isManageAllFilesRequired: Boolean,
    onContinueWithoutAccess: () -> Unit = {}
) {
    com.example.ui.theme.GlassBackdrop {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(
                    state = rememberScrollState(),
                )
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Storage Access Required",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "To search, preview, and edit files on your device safely, File Claw requires storage access permissions.",
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onContinueWithoutAccess) {
                Text("Continue without full access")
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (!isManageAllFilesRequired) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Standard Storage Access", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Required to read and scan documents, images, and audio across standard directory indices.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_permission_btn")
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Manage All Files Access", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Allowing 'All Files Access' ensures File Claw can scan search indices, parse subfolders, and edit documents safely.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRequestAllFilesAccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_manage_storage_btn")
                        ) {
                            Text("Enable in Settings")
                        }
                    }
                }
            }
        }
    }
    }
}

/** Only fully loaded, writable text files go straight into the editor; truncated or converted views stay read-only. */
private fun MainViewModel.FileContentState.opensInEditor(): Boolean =
    this is MainViewModel.FileContentState.TextSuccess && this.editable
