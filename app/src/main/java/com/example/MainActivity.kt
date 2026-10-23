package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import com.example.feature.navigation.AppNavigation
import com.example.feature.permission.PermissionGateScreen
import com.example.platform.intent.IncomingIntentRouter
import com.example.platform.permission.StoragePermissionPolicy
import com.example.ui.theme.FilesClawTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private companion object {
        const val STATE_SKIP_GATE = "skip_permission_gate"
    }

    private val permissionCheckState = mutableStateOf(false)
    private val mainViewModel: MainViewModel by viewModels()

    /** Intent actions are state so configuration changes never re-run a delivered shortcut. */
    private val pendingShortcutIntent = mutableStateOf<Intent?>(null)

    /** True after an external document intent or explicit continue-without-access choice. */
    private val skipPermissionGate = mutableStateOf(false)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SKIP_GATE, skipPermissionGate.value)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /** Runs exactly once per delivered intent (never on configuration changes). */
    private fun handleIncomingIntent(incoming: Intent) {
        IncomingIntentRouter.dispatch(
            intent = incoming,
            onExternalDocument = { uri: Uri ->
                // A document-provider grant is sufficient; do not request broad storage access.
                skipPermissionGate.value = true
                mainViewModel.importFileFromUri(uri)
            },
            onShortcut = { pendingShortcutIntent.value = it }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionCheckState.value = StoragePermissionPolicy.hasRequiredPermissions(this)
        skipPermissionGate.value = savedInstanceState?.getBoolean(STATE_SKIP_GATE, false) == true
        if (savedInstanceState == null) handleIncomingIntent(intent)

        val requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionCheckState.value = StoragePermissionPolicy.hasRequiredPermissions(this)
        }
        val requestStandardPermissions = {
            requestPermissionLauncher.launch(StoragePermissionPolicy.standardPermissions())
        }
        val requestManageAllFilesPermission = {
            StoragePermissionPolicy.openManageAllFilesSettings(this)
        }

        enableEdgeToEdge()
        setContent {
            val settings by mainViewModel.settingsState.collectAsState()
            val fileEvent by mainViewModel.fileEvent.collectAsState()
            val hasPermissions = permissionCheckState.value || skipPermissionGate.value

            LaunchedEffect(fileEvent) {
                fileEvent?.let { message ->
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                    mainViewModel.clearFileEvent()
                }
            }

            FilesClawTheme(themeSetting = settings.theme) {
                if (!hasPermissions) {
                    PermissionGateScreen(
                        onRequestPermissions = requestStandardPermissions,
                        onRequestAllFilesAccess = requestManageAllFilesPermission,
                        isManageAllFilesRequired = StoragePermissionPolicy.requiresManageAllFilesAccess(),
                        onContinueWithoutAccess = { skipPermissionGate.value = true }
                    )
                } else {
                    AppNavigation(
                        activity = this@MainActivity,
                        viewModel = mainViewModel,
                        settings = settings,
                        pendingShortcutIntent = pendingShortcutIntent.value
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionCheckState.value = StoragePermissionPolicy.hasRequiredPermissions(this)
    }
}
