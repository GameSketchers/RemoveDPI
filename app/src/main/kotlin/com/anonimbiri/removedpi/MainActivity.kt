package com.anonimbiri.removedpi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.anonimbiri.removedpi.data.DpiSettings
import com.anonimbiri.removedpi.data.SettingsRepository
import com.anonimbiri.removedpi.ui.screens.*
import com.anonimbiri.removedpi.ui.theme.RemoveDPITheme
import com.anonimbiri.removedpi.ui.theme.RemoveDPITvTheme
import com.anonimbiri.removedpi.update.ReleaseInfo
import com.anonimbiri.removedpi.update.UpdateManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val _navigationEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val navigationEvent = _navigationEvent.asSharedFlow()

    private var startDestination = "home"

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        startDestination = getDestinationFromIntent(intent)
        
        // Detect if we are on TV
        val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        val repository = SettingsRepository(applicationContext)

        setContent {
            if (isTv) {
                // TV UI Entry Point
                RemoveDPITvTheme {
                    androidx.tv.material3.Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TvMainScreen()
                    }
                }
            } else {
                // Phone UI Entry Point
                val settings by repository.settings.collectAsState(initial = DpiSettings())
                
                var updateInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
                var isUpdateChecked by remember { mutableStateOf(false) }
                var showSplash by remember { mutableStateOf(true) }
                var showUpdateSnackbar by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    try {
                        val result = UpdateManager.checkForUpdates(applicationContext)
                        updateInfo = result
                    } catch (e: Exception) {
                        Log.e(TAG, "Update check failed: ${e.message}")
                    } finally {
                        isUpdateChecked = true
                    }
                }

                LaunchedEffect(updateInfo, isUpdateChecked) {
                    if (isUpdateChecked && updateInfo != null) {
                        showUpdateSnackbar = true
                    }
                }

                RemoveDPITheme(themeMode = settings.appTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (showSplash) {
                            SplashScreen(
                                onSplashFinished = {
                                    showSplash = false
                                }
                            )
                        } else {
                            RemoveDpiApp(
                                startDestination = startDestination,
                                navigationEventFlow = navigationEvent,
                                updateInfo = updateInfo,
                                showUpdateSnackbar = showUpdateSnackbar,
                                onUpdateSnackbarDismissed = { showUpdateSnackbar = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val destination = getDestinationFromIntent(intent)
        if (destination == "settings") {
            lifecycleScope.launch {
                _navigationEvent.emit("settings")
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, 
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun getDestinationFromIntent(intent: Intent?): String {
        if (intent == null) return "home"
        return when (intent.action) {
            "android.service.quicksettings.action.QS_TILE_PREFERENCES" -> "settings"
            "OPEN_SETTINGS" -> "settings"
            "android.intent.action.APPLICATION_PREFERENCES" -> "settings"
            else -> "home"
        }
    }
}

@Composable
fun RemoveDpiApp(
    startDestination: String,
    navigationEventFlow: kotlinx.coroutines.flow.Flow<String>,
    updateInfo: ReleaseInfo?,
    showUpdateSnackbar: Boolean,
    onUpdateSnackbarDismissed: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        navigationEventFlow.collect { destination ->
            navController.navigate(destination) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(showUpdateSnackbar, updateInfo) {
        if (showUpdateSnackbar && updateInfo != null) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.update_available_msg, updateInfo.version),
                actionLabel = context.getString(R.string.action_update),
                duration = SnackbarDuration.Long
            )
            
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    navController.navigate("update/true") {
                        launchSingleTop = true
                    }
                }
                SnackbarResult.Dismissed -> { }
            }
            
            onUpdateSnackbarDismissed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            updateInfo = updateInfo,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    updateInfo: ReleaseInfo?,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { 
                    navController.navigate("settings") { launchSingleTop = true }
                },
                onNavigateToLogs = { 
                    navController.navigate("logs") { launchSingleTop = true }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { 
                    if (!navController.popBackStack()) {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                },
                onNavigateToWhitelist = { 
                    navController.navigate("whitelist") { launchSingleTop = true }
                },
                onNavigateToUpdate = {
                    navController.navigate("update/false") {
                        launchSingleTop = true
                    }
                },
                onNavigateToLanguage = {
                    navController.navigate("language") { launchSingleTop = true }
                }
            )
        }
        
        composable("language") {
            LanguageScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("whitelist") {
            WhitelistScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("logs") {
            LogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "update/{autoDownload}",
            arguments = listOf(
                navArgument("autoDownload") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val autoDownload = backStackEntry.arguments?.getBoolean("autoDownload") ?: false
            UpdateScreen(
                releaseInfo = updateInfo,
                onNavigateBack = { navController.popBackStack() },
                autoDownload = autoDownload
            )
        }
        
        composable("update") {
            UpdateScreen(
                releaseInfo = updateInfo,
                onNavigateBack = { navController.popBackStack() },
                autoDownload = false
            )
        }
    }
}