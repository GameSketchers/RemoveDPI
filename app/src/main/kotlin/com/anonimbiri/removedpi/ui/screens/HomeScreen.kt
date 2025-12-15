package com.anonimbiri.removedpi.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.data.VpnState
import com.anonimbiri.removedpi.ui.components.ConnectionStatusCard
import com.anonimbiri.removedpi.ui.components.VpnConnectionButton
import com.anonimbiri.removedpi.vpn.BypassVpnService
import com.anonimbiri.removedpi.vpn.LogManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val context = LocalContext.current
    
    var vpnState by remember { mutableStateOf(VpnState.DISCONNECTED) }
    var packetsProcessed by remember { mutableLongStateOf(0L) }
    var bytesIn by remember { mutableLongStateOf(0L) }
    var bytesOut by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(Unit) {
        BypassVpnService.isRunning.collect { isConnected ->
            vpnState = if (isConnected) VpnState.CONNECTED else VpnState.DISCONNECTED
        }
    }
    
    LaunchedEffect(Unit) {
        BypassVpnService.stats.collect { stats ->
            packetsProcessed = stats.packetsIn + stats.packetsOut
            bytesIn = stats.bytesIn
            bytesOut = stats.bytesOut
        }
    }
    
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vpnState = VpnState.CONNECTING
            startVpnService(context)
        }
    }
    
    LaunchedEffect(vpnState) {
        if (vpnState == VpnState.CONNECTING) {
            delay(3000)
            if (BypassVpnService.isRunning.value) {
                vpnState = VpnState.CONNECTED
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.mipmap.ic_removedpi_monochrome),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (LogManager.enabled) {
                        IconButton(onClick = onNavigateToLogs) {
                            Icon(Icons.Default.Article, stringResource(R.string.cd_logs))
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.cd_settings))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            VpnConnectionButton(
                vpnState = vpnState,
                onClick = {
                    when (vpnState) {
                        VpnState.DISCONNECTED, VpnState.ERROR -> {
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                vpnLauncher.launch(intent)
                            } else {
                                vpnState = VpnState.CONNECTING
                                startVpnService(context)
                            }
                        }
                        VpnState.CONNECTED, VpnState.CONNECTING -> {
                            stopVpnService(context)
                            vpnState = VpnState.DISCONNECTED
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedContent(
                targetState = vpnState,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                },
                label = "statusText"
            ) { state ->
                Text(
                    text = when (state) {
                        VpnState.CONNECTED -> stringResource(R.string.home_status_connected)
                        VpnState.CONNECTING -> stringResource(R.string.status_connecting)
                        VpnState.DISCONNECTED -> stringResource(R.string.home_status_disconnected)
                        VpnState.ERROR -> stringResource(R.string.home_status_error)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (vpnState) {
                    VpnState.CONNECTED -> stringResource(R.string.desc_connected)
                    VpnState.CONNECTING -> stringResource(R.string.desc_connecting)
                    VpnState.DISCONNECTED -> stringResource(R.string.desc_disconnected)
                    VpnState.ERROR -> stringResource(R.string.desc_error)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            ConnectionStatusCard(
                vpnState = vpnState,
                packetsProcessed = packetsProcessed,
                bytesIn = bytesIn,
                bytesOut = bytesOut
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.info_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.info_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun startVpnService(context: android.content.Context) {
    val intent = Intent(context, BypassVpnService::class.java)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopVpnService(context: android.content.Context) {
    val intent = Intent(context, BypassVpnService::class.java).apply { action = "STOP" }
    context.startService(intent)
}