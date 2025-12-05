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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anonimbiri.removedpi.data.DpiSettings
import com.anonimbiri.removedpi.data.SettingsRepository
import com.anonimbiri.removedpi.ui.screens.HomeScreen
import com.anonimbiri.removedpi.ui.screens.LogScreen
import com.anonimbiri.removedpi.ui.screens.SettingsScreen
import com.anonimbiri.removedpi.ui.screens.SplashScreen
import com.anonimbiri.removedpi.ui.screens.WhitelistScreen
import com.anonimbiri.removedpi.ui.theme.RemoveDPITheme
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Native splash - hemen geç (sadece eski telefonlar için fallback)
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestNotificationPermissionIfNeeded()
        
        startDestination = getDestinationFromIntent(intent)
        Log.d(TAG, "onCreate - startDestination: $startDestination, action: ${intent?.action}")
        
        val repository = SettingsRepository(applicationContext)
        
        setContent {
            val settings by repository.settings.collectAsState(initial = DpiSettings())
            
            // Compose Splash Screen state
            var showSplash by remember { mutableStateOf(true) }
            
            RemoveDPITheme(themeMode = settings.appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        // Compose Splash Screen - asıl splash bu!
                        SplashScreen(
                            onSplashFinished = {
                                showSplash = false
                            }
                        )
                    } else {
                        // Ana uygulama
                        RemoveDpiApp(
                            startDestination = startDestination,
                            navigationEventFlow = navigationEvent
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        val destination = getDestinationFromIntent(intent)
        Log.d(TAG, "onNewIntent - destination: $destination, action: ${intent.action}")
        
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
    navigationEventFlow: kotlinx.coroutines.flow.Flow<String>
) {
    val navController = rememberNavController()
    
    LaunchedEffect(Unit) {
        navigationEventFlow.collect { destination ->
            Log.d("RemoveDpiApp", "Navigation event: $destination")
            navController.navigate(destination) {
                launchSingleTop = true
            }
        }
    }
    
    AppNavHost(
        navController = navController,
        startDestination = startDestination
    )
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
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
                }
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
    }
}