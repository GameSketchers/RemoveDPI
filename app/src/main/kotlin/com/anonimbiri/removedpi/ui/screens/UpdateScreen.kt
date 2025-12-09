package com.anonimbiri.removedpi.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.update.ReleaseInfo
import com.anonimbiri.removedpi.update.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class DownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val isCompleted: Boolean = false,
    val error: String? = null,
    val downloadedFile: File? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    releaseInfo: ReleaseInfo?,
    onNavigateBack: () -> Unit,
    autoDownload: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var downloadState by remember { mutableStateOf(DownloadState()) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }
    
    var updateReleaseInfo by remember { mutableStateOf(releaseInfo) }
    
    var currentVersionReleaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var isLoadingCurrentRelease by remember { mutableStateOf(false) }
    
    val isDebug = remember { UpdateManager.isDebugBuild(context) }
    val currentVersion = remember { UpdateManager.getCurrentVersionName(context) }
    
    fun canInstallApk(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }
    
    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    fun tryInstallApk(file: File) {
        if (!canInstallApk()) {
            showInstallPermissionDialog = true
            return
        }
        
        try {
            installApk(context, file)
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.install_failed_msg, e.message),
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    LaunchedEffect(currentVersion) {
        if (currentVersion != "0.0.0" && currentVersion != "Unknown") {
            isLoadingCurrentRelease = true
            try {
                currentVersionReleaseInfo = UpdateManager.getReleaseByTag(currentVersion)
            } catch (_: Exception) {
            } finally {
                isLoadingCurrentRelease = false
            }
        }
    }
    
    LaunchedEffect(autoDownload, updateReleaseInfo) {
        if (autoDownload && updateReleaseInfo != null && !downloadState.isDownloading) {
            val apkUrl = if (isDebug) updateReleaseInfo!!.debugApkUrl else updateReleaseInfo!!.releaseApkUrl
            if (apkUrl.isNotBlank()) {
                scope.launch {
                    downloadApk(
                        context = context,
                        url = apkUrl,
                        version = updateReleaseInfo!!.version,
                        onProgress = { state -> downloadState = state },
                        onError = { error ->
                            downloadState = downloadState.copy(isDownloading = false, error = error)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = error,
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    )
                }
            }
        }
    }
    
    if (showInstallPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showInstallPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    stringResource(R.string.permission_title),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    stringResource(R.string.permission_desc),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInstallPermissionDialog = false
                        openInstallPermissionSettings()
                    }
                ) {
                    Text(stringResource(R.string.btn_go_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermissionDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_update_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AppInfoCard(
                currentVersion = currentVersion,
                isDebug = isDebug,
                isCheckingUpdates = isCheckingUpdates,
                downloadState = downloadState,
                updateReleaseInfo = updateReleaseInfo,
                onCheckUpdates = {
                    isCheckingUpdates = true
                    checkError = null
                    scope.launch {
                        try {
                            updateReleaseInfo = UpdateManager.checkForUpdates(context)
                            if (updateReleaseInfo != null) {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.new_version_found, updateReleaseInfo!!.version),
                                    duration = SnackbarDuration.Short
                                )
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.already_latest),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } catch (e: Exception) {
                            checkError = e.message
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.check_failed, e.message),
                                duration = SnackbarDuration.Long
                            )
                        } finally {
                            isCheckingUpdates = false
                        }
                    }
                },
                onDownload = {
                    updateReleaseInfo?.let { release ->
                        val apkUrl = if (isDebug) release.debugApkUrl else release.releaseApkUrl
                        if (apkUrl.isNotBlank()) {
                            scope.launch {
                                downloadApk(
                                    context = context,
                                    url = apkUrl,
                                    version = release.version,
                                    onProgress = { state -> downloadState = state },
                                    onError = { error ->
                                        downloadState = downloadState.copy(isDownloading = false, error = error)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = error,
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                )
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.apk_url_not_found),
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    }
                },
                onInstall = {
                    downloadState.downloadedFile?.let { file ->
                        tryInstallApk(file)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            checkError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.error_prefix, error),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (updateReleaseInfo == null && !isCheckingUpdates && checkError == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.status_latest),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            if (updateReleaseInfo != null) {
                ReleaseCard(
                    title = stringResource(R.string.new_version_available),
                    icon = Icons.Default.NewReleases,
                    iconTint = MaterialTheme.colorScheme.primary,
                    releaseInfo = updateReleaseInfo!!,
                    isDebug = isDebug,
                    isHighlighted = true
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Text(
                stringResource(R.string.current_version_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )
            
            if (isLoadingCurrentRelease) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            } else if (currentVersionReleaseInfo != null) {
                ReleaseCard(
                    title = null,
                    icon = null,
                    iconTint = null,
                    releaseInfo = currentVersionReleaseInfo!!,
                    isDebug = isDebug,
                    isHighlighted = false
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "v$currentVersion",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Surface(
                                color = if (isDebug) MaterialTheme.colorScheme.tertiaryContainer 
                                        else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (isDebug) stringResource(R.string.build_type_debug) else stringResource(R.string.build_type_release),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            stringResource(R.string.no_release_notes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AppInfoCard(
    currentVersion: String,
    isDebug: Boolean,
    isCheckingUpdates: Boolean,
    downloadState: DownloadState,
    updateReleaseInfo: ReleaseInfo?,
    onCheckUpdates: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Image(
                painter = painterResource(id = R.drawable.ic_removedpi),
                contentDescription = stringResource(R.string.cd_app_icon),
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "v$currentVersion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            when {
                isCheckingUpdates -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.checking_updates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                downloadState.isDownloading -> {
                    WaveProgressBar(
                        progress = downloadState.progress,
                        downloadedBytes = downloadState.downloadedBytes,
                        totalBytes = downloadState.totalBytes,
                        speedBytesPerSec = downloadState.speedBytesPerSec
                    )
                }
                
                downloadState.isCompleted && downloadState.downloadedFile != null -> {
                    Button(
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_install_now))
                    }
                }
                
                updateReleaseInfo != null -> {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_download_update, updateReleaseInfo.version))
                    }
                }
                
                else -> {
                    Button(
                        onClick = onCheckUpdates,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_check_updates))
                    }
                }
            }
        }
    }
}

@Composable
fun WaveProgressBar(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    speedBytesPerSec: Long
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f * shimmer),
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (progress > 0.4f) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (speedBytesPerSec > 0) {
                        Text(
                            "${formatBytes(speedBytesPerSec)}/s",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (progress > 0.4f) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (progress > 0.7f) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (speedBytesPerSec > 0 && totalBytes > downloadedBytes) {
            val remainingBytes = totalBytes - downloadedBytes
            val remainingSeconds = remainingBytes / speedBytesPerSec
            Text(
                stringResource(R.string.time_remaining, formatTime(context, remainingSeconds)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReleaseCard(
    title: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    iconTint: Color?,
    releaseInfo: ReleaseInfo,
    isDebug: Boolean,
    isHighlighted: Boolean
) {
    if (title != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null && iconTint != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "v${releaseInfo.version}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (releaseInfo.publishedAt.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            formatDate(releaseInfo.publishedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (isDebug) MaterialTheme.colorScheme.tertiaryContainer 
                                else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (isDebug) stringResource(R.string.build_type_debug) else stringResource(R.string.build_type_release),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDebug) MaterialTheme.colorScheme.onTertiaryContainer 
                                    else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    if (releaseInfo.isPrerelease) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.badge_prerelease),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            if (releaseInfo.releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                MarkdownText(releaseInfo.releaseNotes)
            }
        }
    }
}

@Composable
fun MarkdownText(markdown: String) {
    val lines = markdown.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            when {
                line.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val text = line.removePrefix("- ").removePrefix("* ")
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            cleanMarkdownFormatting(text),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                line.startsWith("![") && line.contains("](") -> {
                    val imageUrl = line.substringAfter("](").substringBefore(")")
                    if (imageUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                line.startsWith("**") && line.endsWith("**") -> {
                    Text(
                        line.removeSurrounding("**"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                line.isNotBlank() && !line.startsWith("http") && !line.contains("Full Changelog") -> {
                    Text(
                        cleanMarkdownFormatting(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun cleanMarkdownFormatting(text: String): String {
    return text
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        .replace(Regex("\\*([^*]+)\\*"), "$1")
        .replace(Regex("__([^_]+)__"), "$1")
        .replace(Regex("_([^_]+)_"), "$1")
}

private suspend fun downloadApk(
    context: Context,
    url: String,
    version: String,
    onProgress: (DownloadState) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        Log.d("UpdateScreen", "Starting download: $url")
        
        val updateDir = File(context.filesDir, "updates")
        if (!updateDir.exists()) {
            updateDir.mkdirs()
        }
        
        updateDir.listFiles()?.forEach { it.delete() }
        
        val fileName = "RemoveDPI-$version.apk"
        val outputFile = File(updateDir, fileName)
        
        var connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "RemoveDPI-App")
            connectTimeout = 30000
            readTimeout = 30000
            instanceFollowRedirects = true
        }
        
        var redirectCount = 0
        while (connection.responseCode in 301..302 && redirectCount < 5) {
            val newUrl = connection.getHeaderField("Location")
            connection = URL(newUrl).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "RemoveDPI-App")
                connectTimeout = 30000
                readTimeout = 30000
            }
            redirectCount++
        }
        
        val responseCode = connection.responseCode
        if (responseCode != 200) {
            onError(context.getString(R.string.download_failed_http, responseCode))
            return@withContext
        }
        
        val totalBytes = connection.contentLengthLong
        var downloadedBytes = 0L
        var lastUpdateTime = System.currentTimeMillis()
        var lastDownloadedBytes = 0L
        var currentSpeed = 0L
        
        onProgress(DownloadState(
            isDownloading = true,
            progress = 0f,
            downloadedBytes = 0,
            totalBytes = totalBytes,
            speedBytesPerSec = 0
        ))
        
        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime >= 100) {
                        val timeDiff = (now - lastUpdateTime) / 1000.0
                        val bytesDiff = downloadedBytes - lastDownloadedBytes
                        currentSpeed = (bytesDiff / timeDiff).toLong()
                        
                        lastUpdateTime = now
                        lastDownloadedBytes = downloadedBytes
                        
                        val progress = if (totalBytes > 0) {
                            downloadedBytes.toFloat() / totalBytes.toFloat()
                        } else 0f
                        
                        withContext(Dispatchers.Main) {
                            onProgress(DownloadState(
                                isDownloading = true,
                                progress = progress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                speedBytesPerSec = currentSpeed
                            ))
                        }
                    }
                }
            }
        }
        
        Log.d("UpdateScreen", "Download completed: ${outputFile.absolutePath}")
        
        withContext(Dispatchers.Main) {
            onProgress(DownloadState(
                isDownloading = false,
                progress = 1f,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                speedBytesPerSec = 0,
                isCompleted = true,
                downloadedFile = outputFile
            ))
        }
        
    } catch (e: Exception) {
        Log.e("UpdateScreen", "Download failed", e)
        withContext(Dispatchers.Main) {
            onError(context.getString(R.string.download_error, e.message))
        }
    }
}

private fun installApk(context: Context, file: File) {
    Log.d("UpdateScreen", "Installing APK: ${file.absolutePath}")
    
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    
    context.startActivity(intent)
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatTime(context: Context, seconds: Long): String {
    return when {
        seconds < 60 -> context.getString(R.string.time_fmt_seconds, seconds)
        seconds < 3600 -> context.getString(R.string.time_fmt_min_sec, seconds / 60, seconds % 60)
        else -> context.getString(R.string.time_fmt_hour_min, seconds / 3600, (seconds % 3600) / 60)
    }
}

private fun formatDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        inputFormat.parse(dateString)?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) { dateString }
}