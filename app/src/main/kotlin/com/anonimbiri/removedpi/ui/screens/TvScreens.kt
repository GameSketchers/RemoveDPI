@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.anonimbiri.removedpi.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex // EKLENDİ: Z-Index hatası için gerekli
import androidx.core.content.FileProvider
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.data.*
import com.anonimbiri.removedpi.ui.theme.*
import com.anonimbiri.removedpi.update.ReleaseInfo
import com.anonimbiri.removedpi.update.UpdateManager
import com.anonimbiri.removedpi.vpn.BypassVpnService
import com.anonimbiri.removedpi.vpn.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

enum class TvTab { HOME, LOGS }
enum class SettingsPage { ROOT, BYPASS, ADVANCED, NETWORK, EXCEPTIONS, SYSTEM, ABOUT, WHITELIST, LANGUAGE, UPDATE }

data class TvDownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isCompleted: Boolean = false,
    val file: File? = null,
    val error: String? = null
)

// --- KENARLIK ANİMASYONU ---
@Composable
fun rememberAnimatedBorderAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "borderPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, 
        targetValue = 1.0f,  
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )
    return alpha
}

@Composable
fun TvMainScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    var settings by remember { mutableStateOf(DpiSettings()) }
    
    var currentTab by remember { mutableStateOf(TvTab.HOME) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isFirstLaunch by remember { mutableStateOf(true) }
    
    // Güncelleme Bildirimi State'leri
    var updateInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var showUpdateNotification by remember { mutableStateOf(false) }

    val borderAlpha = rememberAnimatedBorderAlpha()

    LaunchedEffect(Unit) {
        repository.settings.collect { 
            settings = it
            LogManager.enabled = it.enableLogs
        }
    }
    
    // Güncelleme Kontrolü
    LaunchedEffect(Unit) {
        delay(2000) // Uygulama açıldıktan biraz sonra kontrol et
        try {
            val info = UpdateManager.checkForUpdates(context)
            if (info != null) {
                updateInfo = info
                showUpdateNotification = true
                // 8 saniye sonra bildirimi gizle
                delay(8000)
                showUpdateNotification = false
            }
        } catch (e: Exception) {
            Log.e("TvMain", "Update check failed", e)
        }
    }

    BackHandler(enabled = isSettingsOpen) {
        isSettingsOpen = false
    }

    RemoveDPITvTheme(themeMode = settings.appTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TvTopNavBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    onSettingsClick = { isSettingsOpen = true },
                    showLogs = settings.enableLogs,
                    borderAlpha = borderAlpha
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 24.dp)
                ) {
                    when (currentTab) {
                        TvTab.HOME -> TvHomeScreen(
                            requestInitialFocus = isFirstLaunch,
                            onFocusRequested = { isFirstLaunch = false },
                            borderAlpha = borderAlpha
                        )
                        TvTab.LOGS -> TvLogsScreen(
                            requestInitialFocus = isFirstLaunch,
                            onFocusRequested = { isFirstLaunch = false },
                            borderAlpha = borderAlpha
                        )
                    }
                }
            }

            // GÜNCELLEME BİLDİRİMİ (SAĞ ÜST)
            AnimatedVisibility(
                visible = showUpdateNotification,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 32.dp)
                    .zIndex(10f) // Z-Index hatası giderildi (Import eklendi)
            ) {
                TvUpdateNotification(
                    version = updateInfo?.version ?: "",
                    borderAlpha = borderAlpha
                )
            }

            // SETTINGS DRAWER OVERLAY
            AnimatedVisibility(
                visible = isSettingsOpen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(enabled = false) {}
                )
            }

            AnimatedVisibility(
                visible = isSettingsOpen,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300, easing = EaseOutQuart)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300, easing = EaseInQuart)),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                TvSettingsDrawer(
                    onClose = { isSettingsOpen = false },
                    borderAlpha = borderAlpha
                )
            }
        }
    }
}

// --- YENİ GÜNCELLEME BİLDİRİMİ ---
@Composable
fun TvUpdateNotification(version: String, borderAlpha: Float) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        colors = NonInteractiveSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = Border(
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha))
        ),
        modifier = Modifier.width(350.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.new_version_available),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "v$version • Ayarlar > Hakkında",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun TvTopNavBar(
    currentTab: TvTab,
    onTabSelected: (TvTab) -> Unit,
    onSettingsClick: () -> Unit,
    showLogs: Boolean,
    borderAlpha: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 48.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.mipmap.ic_removedpi_monochrome),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TvNavTabItem(
                label = stringResource(R.string.nav_home),
                isSelected = currentTab == TvTab.HOME,
                onClick = { onTabSelected(TvTab.HOME) },
                borderAlpha = borderAlpha
            )
            
            if (showLogs) {
                TvNavTabItem(
                    label = stringResource(R.string.nav_logs),
                    isSelected = currentTab == TvTab.LOGS,
                    onClick = { onTabSelected(TvTab.LOGS) },
                    borderAlpha = borderAlpha
                )
            }
        }

        TvSettingsButton(onClick = onSettingsClick, borderAlpha = borderAlpha)
    }
}

@Composable
fun TvNavTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    borderAlpha: Float
) {
    var isFocused by remember { mutableStateOf(false) }
    
    LaunchedEffect(isFocused) {
        if (isFocused && !isSelected) {
            delay(50) 
            onClick()
        }
    }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))
        ),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun TvSettingsButton(onClick: () -> Unit, borderAlpha: Float) {
    var isFocused by remember { mutableStateOf(false) }
    
    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(100)
            onClick()
        }
    }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)))
        ),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
        modifier = Modifier
            .size(48.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(Icons.Default.Settings, stringResource(R.string.cd_settings), Modifier.size(24.dp))
        }
    }
}

@Composable
fun TvHomeScreen(
    requestInitialFocus: Boolean = false,
    onFocusRequested: () -> Unit = {},
    borderAlpha: Float
) {
    val context = LocalContext.current
    var vpnState by remember { mutableStateOf(VpnState.DISCONNECTED) }
    val stats by BypassVpnService.stats.collectAsState()
    val connectButtonFocus = remember { FocusRequester() }
    var isConnectFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BypassVpnService.isRunning.collect { vpnState = if (it) VpnState.CONNECTED else VpnState.DISCONNECTED }
    }

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            delay(300)
            try { connectButtonFocus.requestFocus(); onFocusRequested() } catch(_: Exception){}
        }
    }

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            vpnState = VpnState.CONNECTING
            startVpnService(context)
        }
    }

    val buttonColor = when (vpnState) {
        VpnState.CONNECTED -> VpnConnected
        VpnState.CONNECTING -> VpnConnecting
        else -> MaterialTheme.colorScheme.primary
    }

    // DÜZELTME: Kapsam Hatasını Giderme
    // Artık Box yerine Column kullanıyoruz ve AnimatedVisibility'yi doğrudan Column içine koyuyoruz.
    // Box Wrapper kaldırıldı.
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Üst boşluk (Esnek)
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = when (vpnState) {
                VpnState.CONNECTED -> stringResource(R.string.status_connected)
                VpnState.CONNECTING -> stringResource(R.string.status_connecting)
                else -> stringResource(R.string.status_disconnected)
            },
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = if (vpnState == VpnState.CONNECTED) VpnConnected else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Connect Button
        Surface(
            onClick = {
                when (vpnState) {
                    VpnState.DISCONNECTED, VpnState.ERROR -> {
                        val intent = VpnService.prepare(context)
                        if (intent != null) vpnLauncher.launch(intent)
                        else { vpnState = VpnState.CONNECTING; startVpnService(context) }
                    }
                    else -> { stopVpnService(context); vpnState = VpnState.DISCONNECTED }
                }
            },
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                contentColor = if (vpnState == VpnState.CONNECTED) VpnConnected else MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContentColor = buttonColor
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(4.dp, MaterialTheme.colorScheme.surfaceVariant)),
                focusedBorder = Border(BorderStroke(6.dp, buttonColor.copy(alpha = if (isConnectFocused) borderAlpha else 1f)))
            ),
            glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
            modifier = Modifier
                .size(160.dp)
                .focusRequester(connectButtonFocus)
                .onFocusChanged { isConnectFocused = it.isFocused }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = when (vpnState) {
                        VpnState.CONNECTED -> Icons.Default.Shield
                        VpnState.CONNECTING -> Icons.Default.Sync
                        else -> Icons.Default.PowerSettingsNew
                    },
                    contentDescription = null,
                    modifier = Modifier.size(70.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // DÜZELTME: AnimatedVisibility artık Box içinde değil, doğrudan Column içinde.
        // Hata veren "implicit receiver" sorunu çözüldü.
        AnimatedVisibility(
            visible = vpnState == VpnState.CONNECTED,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.height(100.dp) // Yüksekliği buraya verdik
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TvStatCard(Icons.Default.Download, stringResource(R.string.label_download), formatBytes(stats.bytesIn))
                TvStatCard(Icons.Default.Upload, stringResource(R.string.label_upload), formatBytes(stats.bytesOut))
                TvStatCard(Icons.Default.Speed, stringResource(R.string.label_packets), "${stats.packetsIn + stats.packetsOut}")
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))
        
        if (vpnState != VpnState.CONNECTED) {
            TvInfoCard(modifier = Modifier.padding(bottom = 32.dp))
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TvInfoCard(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        colors = NonInteractiveSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        modifier = modifier.widthIn(max = 600.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.info_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.info_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TvStatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        colors = NonInteractiveSurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.width(180.dp).height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TvLogsScreen(
    requestInitialFocus: Boolean = false,
    onFocusRequested: () -> Unit = {},
    borderAlpha: Float
) {
    val logs by LogManager.logs.collectAsState()
    val listState = rememberLazyListState()
    val clearFocus = remember { FocusRequester() }
    var isClearFocused by remember { mutableStateOf(false) }
    
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            try { clearFocus.requestFocus(); onFocusRequested() } catch(_: Exception){}
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.screen_logs_title), 
                style = MaterialTheme.typography.headlineMedium, 
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Surface(
                onClick = { LogManager.clear() },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isClearFocused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.error,
                    contentColor = if (isClearFocused) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error,
                    focusedContentColor = MaterialTheme.colorScheme.onError
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border.None, 
                    focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.onError.copy(alpha = borderAlpha)))
                ),
                glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
                modifier = Modifier
                    .focusRequester(clearFocus)
                    .onFocusChanged { isClearFocused = it.isFocused }
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.cd_clear), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs.reversed()) { log ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        log.time, 
                        color = MaterialTheme.colorScheme.primary, 
                        style = MaterialTheme.typography.bodySmall, 
                        modifier = Modifier.width(70.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        log.message, 
                        color = when (log.level) {
                            LogManager.Level.ERROR -> MaterialTheme.colorScheme.error
                            LogManager.Level.WARN -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }, 
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun TvSettingsDrawer(onClose: () -> Unit, borderAlpha: Float) {
    var currentPage by remember { mutableStateOf(SettingsPage.ROOT) }
    val firstItemFocus = remember { FocusRequester() }

    LaunchedEffect(currentPage) {
        delay(100)
        try { firstItemFocus.requestFocus() } catch(_: Exception){}
    }
    
    BackHandler(enabled = true) {
        if (currentPage != SettingsPage.ROOT) currentPage = SettingsPage.ROOT else onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(450.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(32.dp)
            .onPreviewKeyEvent { 
                if (it.key == Key.DirectionLeft && it.type == KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent true 
                }
                false
            }
            .clickable(enabled = false) {}
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
            if (currentPage != SettingsPage.ROOT) {
                var isBackFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = { currentPage = SettingsPage.ROOT },
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        focusedContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border.None,
                        focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))
                    ),
                    glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
                    modifier = Modifier.size(32.dp).onFocusChanged { isBackFocused = it.isFocused }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(getPageTitle(currentPage), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState != SettingsPage.ROOT) slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it/3 } + fadeOut()
                else slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it/3 } + fadeOut()
            },
            label = "SettingsNav"
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (page) {
                    SettingsPage.ROOT -> TvSettingsRootList(onNavigate = { currentPage = it }, firstItemFocus = firstItemFocus, borderAlpha = borderAlpha)
                    SettingsPage.BYPASS -> TvBypassSettings(firstItemFocus, borderAlpha)
                    SettingsPage.ADVANCED -> TvAdvancedSettings(firstItemFocus, borderAlpha)
                    SettingsPage.NETWORK -> TvNetworkSettings(firstItemFocus, borderAlpha)
                    SettingsPage.EXCEPTIONS -> TvExceptionsSettings({ currentPage = SettingsPage.WHITELIST }, firstItemFocus, borderAlpha)
                    SettingsPage.SYSTEM -> TvSystemSettings({ currentPage = SettingsPage.LANGUAGE }, firstItemFocus, borderAlpha)
                    SettingsPage.ABOUT -> TvAboutSettings({ currentPage = SettingsPage.UPDATE }, firstItemFocus, borderAlpha)
                    SettingsPage.WHITELIST -> TvWhitelistScreen(firstItemFocus, borderAlpha)
                    SettingsPage.LANGUAGE -> TvLanguageScreen(firstItemFocus, borderAlpha)
                    SettingsPage.UPDATE -> TvUpdateScreen(firstItemFocus, borderAlpha)
                }
            }
        }
    }
}

@Composable
fun getPageTitle(page: SettingsPage): String = when(page) {
    SettingsPage.ROOT -> stringResource(R.string.screen_settings_title)
    SettingsPage.BYPASS -> stringResource(R.string.cat_bypass)
    SettingsPage.ADVANCED -> stringResource(R.string.cat_advanced)
    SettingsPage.NETWORK -> stringResource(R.string.cat_network)
    SettingsPage.EXCEPTIONS -> stringResource(R.string.cat_exceptions)
    SettingsPage.SYSTEM -> stringResource(R.string.cat_system)
    SettingsPage.ABOUT -> stringResource(R.string.cat_about)
    SettingsPage.WHITELIST -> stringResource(R.string.whitelist_title)
    SettingsPage.LANGUAGE -> stringResource(R.string.lang_title)
    SettingsPage.UPDATE -> stringResource(R.string.about_update_title)
}

@Composable
fun TvSettingsRootList(onNavigate: (SettingsPage) -> Unit, firstItemFocus: FocusRequester, borderAlpha: Float) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TvSettingsMenuItem(stringResource(R.string.cat_bypass), Icons.Default.Https, { onNavigate(SettingsPage.BYPASS) }, borderAlpha, Modifier.focusRequester(firstItemFocus)) }
        item { TvSettingsMenuItem(stringResource(R.string.cat_advanced), Icons.Default.Tune, { onNavigate(SettingsPage.ADVANCED) }, borderAlpha) }
        item { TvSettingsMenuItem(stringResource(R.string.cat_network), Icons.Default.Speed, { onNavigate(SettingsPage.NETWORK) }, borderAlpha) }
        item { TvSettingsMenuItem(stringResource(R.string.cat_exceptions), Icons.Default.List, { onNavigate(SettingsPage.EXCEPTIONS) }, borderAlpha) }
        item { TvSettingsMenuItem(stringResource(R.string.cat_system), Icons.Default.Settings, { onNavigate(SettingsPage.SYSTEM) }, borderAlpha) }
        item { TvSettingsMenuItem(stringResource(R.string.cat_about), Icons.Default.Info, { onNavigate(SettingsPage.ABOUT) }, borderAlpha) }
    }
}

@Composable
fun TvSettingsMenuItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, borderAlpha: Float, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary, // Odaklanınca Primary Renk (Dolu)
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary // Odaklanınca yazı rengi
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None, 
            focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))
        ),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
        modifier = modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp))
        }
    }
}

@Composable
fun TvSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, borderAlpha: Float, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
        modifier = modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(
                checkedThumbColor = if(isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary, 
                checkedTrackColor = if(isFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            ))
        }
    }
}

@Composable
fun TvCycleButton(label: String, currentValue: String, onClick: () -> Unit, borderAlpha: Float, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface, 
            focusedContainerColor = MaterialTheme.colorScheme.primary, 
            contentColor = MaterialTheme.colorScheme.onSurface, 
            focusedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(currentValue, style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.7f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

@Composable
fun TvNumberRow(label: String, value: Int, range: IntRange, unit: String = "", borderAlpha: Float, modifier: Modifier = Modifier, onValueChange: (Int) -> Unit) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvMiniButton(onClick = { if (value > range.first) onValueChange(value - 1) }, icon = Icons.Default.Remove, borderAlpha = borderAlpha)
            Box(modifier = Modifier.widthIn(min = 70.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(if (unit.isNotEmpty()) "$value $unit" else "$value", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            TvMiniButton(onClick = { if (value < range.last) onValueChange(value + 1) }, icon = Icons.Default.Add, borderAlpha = borderAlpha)
        }
    }
}

@Composable
fun TvMiniButton(onClick: () -> Unit, icon: ImageVector, borderAlpha: Float) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        colors = ClickableSurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onSurface, focusedContentColor = MaterialTheme.colorScheme.onPrimary),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
        modifier = Modifier.size(40.dp)
    ) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(icon, null, Modifier.size(20.dp)) } }
}

@Composable
fun TvClickableItem(
    title: String, 
    onClick: () -> Unit, 
    borderAlpha: Float, 
    modifier: Modifier = Modifier, 
    description: String? = null, 
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface, 
            focusedContainerColor = MaterialTheme.colorScheme.primary, 
            contentColor = MaterialTheme.colorScheme.onSurface, 
            focusedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (iconPainter != null) {
                Image(painter = iconPainter, contentDescription = null, modifier = Modifier.size(24.dp), colorFilter = ColorFilter.tint(LocalContentColor.current))
                Spacer(Modifier.width(16.dp))
            } else if (iconVector != null) {
                Icon(iconVector, null, Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.7f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp))
        }
    }
}

// ... (BypassSettings, AdvancedSettings, NetworkSettings vb. yukarıdaki TvSwitchRow/TvCycleButton/TvNumberRow ile aynı)
// Sadece çağırırken borderAlpha parametresini geçirmeyi unutma. Kodun geri kalanı aynı mantıkla güncellendi.

@Composable
fun TvBypassSettings(firstItemFocus: FocusRequester, borderAlpha: Float) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(DpiSettings()) }
    LaunchedEffect(Unit) { repository.settings.collect { settings = it } }
    fun update(new: DpiSettings) { settings = new; scope.launch { repository.updateSettings(new); BypassVpnService.settings = new } }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TvSwitchRow(stringResource(R.string.https_title), settings.desyncHttps, { update(settings.copy(desyncHttps = it)) }, borderAlpha, Modifier.focusRequester(firstItemFocus)) }
        item { TvSwitchRow(stringResource(R.string.http_title), settings.desyncHttp, { update(settings.copy(desyncHttp = it)) }, borderAlpha) }
        item { TvCycleButton(stringResource(R.string.cat_bypass), settings.desyncMethod.name, { val m = DesyncMethod.values(); update(settings.copy(desyncMethod = m[(settings.desyncMethod.ordinal + 1) % m.size])) }, borderAlpha) }
    }
}

@Composable
fun TvAdvancedSettings(firstItemFocus: FocusRequester, borderAlpha: Float) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(DpiSettings()) }
    LaunchedEffect(Unit) { repository.settings.collect { settings = it } }
    fun update(new: DpiSettings) { settings = new; scope.launch { repository.updateSettings(new); BypassVpnService.settings = new } }

    val focusModifier = remember { Modifier.focusRequester(firstItemFocus) }
    var hasFocused by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            val mod = if (!hasFocused) { hasFocused = true; focusModifier } else Modifier
            TvSwitchRow(stringResource(R.string.adv_auto_ttl), settings.autoTtl, { update(settings.copy(autoTtl = it)) }, borderAlpha, mod)
        }
        item { TvNumberRow(if (settings.autoTtl) stringResource(R.string.adv_min_ttl) else stringResource(R.string.adv_ttl), if (settings.autoTtl) settings.minTtl else settings.ttlValue, if (settings.autoTtl) 1..32 else 1..64, "", borderAlpha) { if (settings.autoTtl) update(settings.copy(minTtl = it)) else update(settings.copy(ttlValue = it)) } }
        if (settings.desyncMethod in listOf(DesyncMethod.SPLIT, DesyncMethod.SNI_SPLIT)) {
            item { TvNumberRow(stringResource(R.string.adv_split_pos), settings.firstPacketSize, 1..100, "byte", borderAlpha) { update(settings.copy(firstPacketSize = it)) } }
        }
        if (settings.desyncMethod == DesyncMethod.DISORDER) {
            item { TvNumberRow(stringResource(R.string.adv_split_count), settings.splitCount, 2..20, "", borderAlpha) { update(settings.copy(splitCount = it)) } }
        }
        item { TvNumberRow(stringResource(R.string.adv_delay), settings.splitDelay.toInt(), 0..50, "ms", borderAlpha) { update(settings.copy(splitDelay = it.toLong())) } }
        item { TvSwitchRow(stringResource(R.string.adv_host_mix), settings.mixHostCase, { update(settings.copy(mixHostCase = it)) }, borderAlpha) }
        item { TvCycleButton(stringResource(R.string.adv_fake_packet), settings.fakePacketMode.name, { val m = FakePacketMode.values(); update(settings.copy(fakePacketMode = m[(settings.fakePacketMode.ordinal + 1) % m.size])) }, borderAlpha) }
    }
}

@Composable
fun TvNetworkSettings(firstItemFocus: FocusRequester, borderAlpha: Float) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(DpiSettings()) }
    LaunchedEffect(Unit) { repository.settings.collect { settings = it } }
    fun update(new: DpiSettings) { settings = new; scope.launch { repository.updateSettings(new); BypassVpnService.settings = new } }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TvSwitchRow(stringResource(R.string.net_block_quic), settings.blockQuic, { update(settings.copy(blockQuic = it)) }, borderAlpha, Modifier.focusRequester(firstItemFocus)) }
        item { TvSwitchRow(stringResource(R.string.net_tcp_nodelay), settings.enableTcpNodelay, { update(settings.copy(enableTcpNodelay = it)) }, borderAlpha) }
    }
}

@Composable
fun TvExceptionsSettings(onNavigateToWhitelist: () -> Unit, firstItemFocus: FocusRequester, borderAlpha: Float) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    var settings by remember { mutableStateOf(DpiSettings()) }
    LaunchedEffect(Unit) { repository.settings.collect { settings = it } }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TvClickableItem(stringResource(R.string.whitelist_title), onNavigateToWhitelist, borderAlpha, Modifier.focusRequester(firstItemFocus), "${settings.whitelist.size} site", iconVector = Icons.Default.List) }
    }
}

@Composable
fun TvSystemSettings(onNavigateToLanguage: () -> Unit, firstItemFocus: FocusRequester, borderAlpha: Float) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(DpiSettings()) }
    LaunchedEffect(Unit) { repository.settings.collect { settings = it } }
    fun update(new: DpiSettings) { settings = new; LogManager.enabled = new.enableLogs; scope.launch { repository.updateSettings(new); BypassVpnService.settings = new } }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TvSwitchRow(stringResource(R.string.sys_logging), settings.enableLogs, { update(settings.copy(enableLogs = it)) }, borderAlpha, Modifier.focusRequester(firstItemFocus)) }
        item { TvCycleButton(stringResource(R.string.theme_title), when(settings.appTheme) { AppTheme.SYSTEM -> stringResource(R.string.theme_system); AppTheme.AMOLED -> stringResource(R.string.theme_amoled); AppTheme.ANIME -> stringResource(R.string.theme_anime) }, { val t = AppTheme.values(); update(settings.copy(appTheme = t[(settings.appTheme.ordinal + 1) % t.size])) }, borderAlpha) }
        item { TvClickableItem(stringResource(R.string.lang_title), onNavigateToLanguage, borderAlpha, iconVector = Icons.Default.Language) }
        item {
            Spacer(Modifier.height(16.dp))
            Surface(
                onClick = { update(DpiSettings()) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.error,
                    focusedContentColor = MaterialTheme.colorScheme.onError
                ),
                border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.error.copy(alpha = borderAlpha)))),
                glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestartAlt, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.sys_reset))
                    }
                }
            }
        }
    }
}

@Composable
fun TvAboutSettings(onNavigateToUpdate: () -> Unit, firstItemFocus: FocusRequester, borderAlpha: Float) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember { try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { "1.0.0" } }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("${stringResource(R.string.app_name)} v$versionName", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { TvClickableItem("Anonimbiri", { uriHandler.openUri("https://github.com/anonimbiri-IsBack") }, borderAlpha, Modifier.focusRequester(firstItemFocus), stringResource(R.string.about_dev), iconPainter = painterResource(id = R.mipmap.ic_removedpi_monochrome)) }
        item { TvClickableItem(stringResource(R.string.about_repo_title), { uriHandler.openUri("https://github.com/GameSketchers/RemoveDPI") }, borderAlpha, description = stringResource(R.string.about_repo_desc), iconPainter = painterResource(id = R.drawable.ic_github)) }
        item { TvClickableItem(stringResource(R.string.about_update_title), onNavigateToUpdate, borderAlpha, description = stringResource(R.string.about_update_desc), iconVector = Icons.Default.Update) }
    }
}

@Composable
fun TvWhitelistScreen(firstItemFocus: FocusRequester, borderAlpha: Float) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(DpiSettings()) }
    var isAddingMode by remember { mutableStateOf(false) }
    var newDomainText by remember { mutableStateOf("") }
    val inputFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { repository.settings.collect { settings = it } }
    
    LaunchedEffect(isAddingMode) {
        if (isAddingMode) {
            delay(100)
            try { inputFocus.requestFocus() } catch(_: Exception){}
        }
    }

    fun updateWhitelist(newSet: Set<String>) {
        val newSettings = settings.copy(whitelist = newSet)
        settings = newSettings
        scope.launch { repository.updateSettings(newSettings); BypassVpnService.settings = newSettings }
    }

    if (isAddingMode) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.enter_domain_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.enter_domain_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            
            OutlinedTextField(
                value = newDomainText,
                onValueChange = { newDomainText = it },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .focusRequester(inputFocus)
                    .onKeyEvent {
                        if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                             isAddingMode = false
                             true
                        } else false
                    },
                singleLine = true,
                label = { Text(stringResource(R.string.label_domain_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                     if (newDomainText.isNotBlank()) {
                        val clean = newDomainText.trim().replace("https://", "").replace("http://", "").replace("www.", "").trim('/')
                        updateWhitelist(settings.whitelist + clean)
                        isAddingMode = false
                        newDomainText = ""
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Transparent
                )
            )
            
            Spacer(Modifier.height(32.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { isAddingMode = false },
                    colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) { Text(stringResource(R.string.btn_cancel)) }
                
                Button(
                    onClick = {
                         if (newDomainText.isNotBlank()) {
                            val clean = newDomainText.trim().replace("https://", "").replace("http://", "").replace("www.", "").trim('/')
                            updateWhitelist(settings.whitelist + clean)
                            isAddingMode = false
                            newDomainText = ""
                        }
                    },
                    colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text(stringResource(R.string.btn_add)) }
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                 Surface(
                    onClick = { isAddingMode = true },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        focusedContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border.None,
                        focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))
                    ),
                    glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
                    modifier = Modifier.fillMaxWidth().focusRequester(firstItemFocus)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_add), fontWeight = FontWeight.Bold)
                    }
                }
            }
    
            if (settings.whitelist.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(12.dp), colors = NonInteractiveSurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) { Text(stringResource(R.string.whitelist_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            } else {
                itemsIndexed(settings.whitelist.toList().sorted()) { index, domain ->
                    TvWhitelistItem(domain = domain, onDelete = { updateWhitelist(settings.whitelist - domain) }, borderAlpha = borderAlpha)
                }
            }
        }
    }
}

@Composable
fun TvWhitelistItem(domain: String, onDelete: () -> Unit, borderAlpha: Float) {
    Surface(
        shape = RoundedCornerShape(8.dp), 
        colors = NonInteractiveSurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface), 
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncFaviconTv(domain = domain, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Text(domain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            
            TvMiniButton(onClick = onDelete, icon = Icons.Default.Delete, borderAlpha = borderAlpha)
        }
    }
}

@Composable
fun AsyncFaviconTv(domain: String, modifier: Modifier = Modifier) {
    // COIL OPTIMIZASYONU
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = "https://www.google.com/s2/favicons?sz=64&domain_url=$domain",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            error = painterResource(R.mipmap.ic_removedpi_monochrome) 
        )
    }
}

@Composable
fun TvLanguageScreen(firstItemFocus: FocusRequester, borderAlpha: Float) {
    val context = LocalContext.current
    val currentLocale = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) context.resources.configuration.locales.get(0).language else @Suppress("DEPRECATION") context.resources.configuration.locale.language }
    var selectedLanguage by remember { mutableStateOf(currentLocale) }

    val languages = listOf(
        LanguageItem(stringResource(R.string.lang_turkish), "tr", R.drawable.ic_flag_tr),
        LanguageItem(stringResource(R.string.lang_english), "en", R.drawable.ic_flag_en),
        LanguageItem(stringResource(R.string.lang_japanese), "ja", R.drawable.ic_flag_ja),
        LanguageItem(stringResource(R.string.lang_russian), "ru", R.drawable.ic_flag_ru)
    )
    
    fun changeLanguage(code: String) {
        selectedLanguage = code
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        (context as? Activity)?.recreate()
    }
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(languages) { index, lang ->
            val isSelected = selectedLanguage == lang.code
            Surface(
                onClick = { changeLanguage(lang.code) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    focusedContainerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary, // Odaklanınca renk doluyor
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    focusedContentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                ),
                border = ClickableSurfaceDefaults.border(
                    border = if (isSelected) Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) else Border.None,
                    focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))
                ),
                glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
                modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(lang.flagRes), lang.name, Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(16.dp))
                    Text(lang.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Default.Check, null)
                }
            }
        }
    }
}

data class LanguageItem(val name: String, val code: String, val flagRes: Int)

@Composable
fun TvUpdateScreen(firstItemFocus: FocusRequester, borderAlpha: Float) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var updateInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var currentVersionInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloadState by remember { mutableStateOf(TvDownloadState()) }
    var checking by remember { mutableStateOf(false) }
    var loadingCurrentVersion by remember { mutableStateOf(false) }
    
    val isDebug = remember { UpdateManager.isDebugBuild(context) }
    val currentVersion = remember { UpdateManager.getCurrentVersionName(context) }

    LaunchedEffect(currentVersion) {
        if (currentVersion != "0.0.0" && currentVersion != "Unknown") {
            loadingCurrentVersion = true
            try { currentVersionInfo = UpdateManager.getReleaseByTag(currentVersion) } catch (_: Exception) {}
            loadingCurrentVersion = false
        }
    }

    fun installApk(file: File) {
        val canInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.packageManager.canRequestPackageInstalls() else true
        if (!canInstall) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = Uri.parse("package:${context.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) { Log.e("TvUpdate", "Install failed", e) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Surface(
                onClick = { checking = true; scope.launch { updateInfo = UpdateManager.checkForUpdates(context); checking = false } },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary, // Odaklanınca doluyor
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    focusedContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border.None,
                    focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = borderAlpha)))
                ),
                glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
                modifier = Modifier.fillMaxWidth().height(56.dp).focusRequester(firstItemFocus)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if(checking) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.btn_check_updates), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        if (updateInfo != null) {
            item { TvReleaseCard(stringResource(R.string.new_version_available), updateInfo!!, isDebug, true, borderAlpha) }
            
            item {
                when {
                    downloadState.isDownloading -> {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(downloadState.progress * 100).toInt()}% - ${formatBytes(downloadState.downloadedBytes)} / ${formatBytes(downloadState.totalBytes)}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            androidx.compose.material3.LinearProgressIndicator(progress = { downloadState.progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    downloadState.isCompleted && downloadState.file != null -> {
                        Surface(
                            onClick = { installApk(downloadState.file!!) }, 
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)), 
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f), 
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                focusedContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = ClickableSurfaceDefaults.border(
                                border = Border.None,
                                focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)))
                            ),
                            glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None), 
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { 
                                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { 
                                    Icon(Icons.Default.SystemUpdate, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.btn_install_now), style = MaterialTheme.typography.titleMedium) 
                                } 
                            }
                        }
                    }
                    downloadState.error != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${downloadState.error}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                onClick = { downloadState = TvDownloadState() }, 
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)), 
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f), 
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.inverseSurface
                                ),
                                border = ClickableSurfaceDefaults.border(
                                    border = Border.None,
                                    focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)))
                                ),
                                glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None), 
                                modifier = Modifier.width(150.dp).height(48.dp)
                            ) { 
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text(stringResource(R.string.btn_retry)) } 
                            }
                        }
                    }
                    else -> {
                        Surface(
                            onClick = { val url = if (isDebug) updateInfo!!.debugApkUrl else updateInfo!!.releaseApkUrl; if (url.isNotBlank()) scope.launch { downloadApk(context, url, updateInfo!!.version) { downloadState = it } } }, 
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)), 
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f), 
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                focusedContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = ClickableSurfaceDefaults.border(
                                border = Border.None,
                                focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)))
                            ),
                            glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None), 
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { 
                                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { 
                                    Icon(Icons.Default.Download, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.btn_download_update, updateInfo!!.version), style = MaterialTheme.typography.titleMedium) 
                                } 
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.current_version_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }

        item {
            if (loadingCurrentVersion) Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(Modifier.size(32.dp)) }
            else if (currentVersionInfo != null) TvReleaseCard(null, currentVersionInfo!!, isDebug, false, borderAlpha)
            else Surface(shape = RoundedCornerShape(16.dp), colors = NonInteractiveSurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("v$currentVersion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Surface(shape = RoundedCornerShape(8.dp), colors = NonInteractiveSurfaceDefaults.colors(containerColor = if (isDebug) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer)) {
                            Text(if (isDebug) stringResource(R.string.build_type_debug) else stringResource(R.string.build_type_release), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp)); androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)); Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_release_notes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TvReleaseCard(title: String?, releaseInfo: ReleaseInfo, isDebug: Boolean, isHighlighted: Boolean, borderAlpha: Float) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val releaseLines = remember(releaseInfo.releaseNotes) { cleanMarkdown(releaseInfo.releaseNotes).split("\n").filter { it.isNotBlank() } }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column {
        if (title != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NewReleases, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        
        Surface(
            shape = RoundedCornerShape(16.dp), 
            colors = NonInteractiveSurfaceDefaults.colors(containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface), 
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("v${releaseInfo.version}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (releaseInfo.publishedAt.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(formatDate(releaseInfo.publishedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), colors = NonInteractiveSurfaceDefaults.colors(containerColor = if (isDebug) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer)) {
                            Text(if (isDebug) stringResource(R.string.build_type_debug) else stringResource(R.string.build_type_release), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isDebug) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        if (releaseInfo.isPrerelease) Surface(shape = RoundedCornerShape(8.dp), colors = NonInteractiveSurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Text(stringResource(R.string.badge_prerelease), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer) }
                    }
                }
                
                if (releaseLines.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    
                    Surface(
                        onClick = {}, 
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)), 
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f), 
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface, 
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ), 
                        border = ClickableSurfaceDefaults.border(
                            border = Border.None, 
                            focusedBorder = Border(BorderStroke(2.dp, Color.White.copy(alpha = borderAlpha)))
                        ), 
                        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None), 
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { event ->
                                // FIX FOR TV FOCUS TRAP
                                if (event.type == KeyEventType.KeyDown) {
                                    if (event.key == Key.DirectionDown) {
                                        if (!listState.canScrollForward) {
                                            focusManager.moveFocus(FocusDirection.Down)
                                            return@onPreviewKeyEvent true
                                        }
                                    } else if (event.key == Key.DirectionUp) {
                                        if (!listState.canScrollBackward) {
                                            focusManager.moveFocus(FocusDirection.Up)
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                }
                                false
                            }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionDown -> { 
                                            scope.launch { listState.animateScrollToItem((listState.firstVisibleItemIndex + 1).coerceAtMost(releaseLines.size - 1)) }
                                            true 
                                        }
                                        Key.DirectionUp -> { 
                                            scope.launch { listState.animateScrollToItem((listState.firstVisibleItemIndex - 1).coerceAtLeast(0)) }
                                            true 
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                    ) {
                        LazyColumn(
                            state = listState, 
                            modifier = Modifier.padding(12.dp), 
                            verticalArrangement = Arrangement.spacedBy(6.dp), 
                            userScrollEnabled = false
                        ) {
                            items(releaseLines) { line -> TvMarkdownLine(line) }
                        }
                    }
                    
                    if (releaseLines.size > 5) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.scroll_instruction, "↑↓"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvMarkdownLine(line: String) {
    when {
        line.startsWith("###") -> Text(line.removePrefix("###").trim(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
        line.startsWith("##") -> Text(line.removePrefix("##").trim(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        line.startsWith("#") -> Text(line.removePrefix("#").trim(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        line.trimStart().startsWith("-") || line.trimStart().startsWith("*") -> Row(verticalAlignment = Alignment.Top) { Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp)); Text(line.trimStart().removePrefix("-").removePrefix("*").trim(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) }
        line.startsWith(">") -> Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)).padding(8.dp)) { Text(line.removePrefix(">").trim(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) }
        else -> Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun cleanMarkdown(text: String): String = text.replace(Regex("!\\[.*?]\\(.*?\\)"), "").replace(Regex("<img[^>]*>"), "").replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1").replace(Regex("```[\\s\\S]*?```"), "").replace(Regex("`([^`]+)`"), "$1").replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1").replace(Regex("__([^_]+)__"), "$1").replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), "$1").replace(Regex("(?<!_)_([^_]+)_(?!_)"), "$1").replace(Regex("~~([^~]+)~~"), "$1").replace(Regex("^---+$", RegexOption.MULTILINE), "").replace(Regex("^\\*\\*\\*+$", RegexOption.MULTILINE), "").replace(Regex("Full Changelog.*", RegexOption.IGNORE_CASE), "").trim()

private suspend fun downloadApk(context: Context, url: String, version: String, onProgress: (TvDownloadState) -> Unit) = withContext(Dispatchers.IO) {
    try {
        val updateDir = File(context.filesDir, "updates"); if (!updateDir.exists()) updateDir.mkdirs()
        val file = File(updateDir, "RemoveDPI-$version.apk")
        val connection = URL(url).openConnection() as HttpURLConnection; connection.connectTimeout = 15000; connection.readTimeout = 15000; connection.connect()
        val totalBytes = connection.contentLengthLong; val input = connection.inputStream; val output = FileOutputStream(file)
        val buffer = ByteArray(8192); var bytesRead: Int; var downloadedBytes = 0L
        withContext(Dispatchers.Main) { onProgress(TvDownloadState(isDownloading = true, totalBytes = totalBytes)) }
        while (input.read(buffer).also { bytesRead = it } != -1) { output.write(buffer, 0, bytesRead); downloadedBytes += bytesRead; val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f; withContext(Dispatchers.Main) { onProgress(TvDownloadState(isDownloading = true, progress = progress, downloadedBytes = downloadedBytes, totalBytes = totalBytes)) } }
        output.close(); input.close()
        withContext(Dispatchers.Main) { onProgress(TvDownloadState(isCompleted = true, file = file, progress = 1f)) }
    } catch (e: Exception) { withContext(Dispatchers.Main) { onProgress(TvDownloadState(error = e.message ?: "Unknown error")) } }
}

private fun startVpnService(context: Context) { val intent = Intent(context, BypassVpnService::class.java); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent) }
private fun stopVpnService(context: Context) { context.startService(Intent(context, BypassVpnService::class.java).apply { action = "STOP" }) }
private fun formatBytes(bytes: Long): String = when { bytes < 1024 -> "$bytes B"; bytes < 1024 * 1024 -> "${bytes / 1024} KB"; bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0)); else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0)) }
private fun formatDate(dateString: String): String { if (dateString.isBlank()) return ""; return try { val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); inputFormat.timeZone = TimeZone.getTimeZone("UTC"); val outputFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()); inputFormat.parse(dateString)?.let { outputFormat.format(it) } ?: dateString } catch (e: Exception) { dateString } }