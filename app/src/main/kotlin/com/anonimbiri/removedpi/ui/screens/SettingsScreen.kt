package com.anonimbiri.removedpi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anonimbiri.removedpi.data.*
import com.anonimbiri.removedpi.vpn.BypassVpnService
import com.anonimbiri.removedpi.vpn.LogManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SettingsRepository(context) }
    val uriHandler = LocalUriHandler.current
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )
    
    var settings by remember { mutableStateOf(DpiSettings()) }
    
    LaunchedEffect(Unit) {
        repository.settings.collect { loadedSettings ->
            settings = loadedSettings
            BypassVpnService.settings = loadedSettings
            LogManager.enabled = loadedSettings.enableLogs
        }
    }
    
    fun updateSettings(newSettings: DpiSettings) {
        settings = newSettings
        BypassVpnService.settings = newSettings
        LogManager.enabled = newSettings.enableLogs
        scope.launch { repository.updateSettings(newSettings) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Ayarlar",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // === 1. BYPASS STRATEJİSİ ===
            item {
                SettingsSection(title = "Bypass Stratejisi") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "Yöntem (Desync Method)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(DesyncMethod.SPLIT, DesyncMethod.DISORDER, DesyncMethod.FAKE).forEach { method ->
                                FilterChip(
                                    selected = settings.desyncMethod == method,
                                    onClick = { updateSettings(settings.copy(desyncMethod = method)) },
                                    label = { Text(method.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    SettingsSwitchRow(
                        icon = Icons.Default.Https,
                        title = "HTTPS Desync",
                        subtitle = "Güvenli bağlantıları parçalar (SNI)",
                        checked = settings.desyncHttps,
                        onCheckedChange = { updateSettings(settings.copy(desyncHttps = it)) }
                    )
                    
                    SettingsSwitchRow(
                        icon = Icons.Default.Http,
                        title = "HTTP Desync",
                        subtitle = "Şifresiz siteleri parçalar",
                        checked = settings.desyncHttp,
                        onCheckedChange = { updateSettings(settings.copy(desyncHttp = it)) }
                    )
                }
            }

            // === 2. İNCE AYARLAR ===
            item {
                SettingsSection(title = "İnce Ayarlar") {
                    SettingsSwitchRow(
                        icon = Icons.Default.ContentCut,
                        title = "Kör Bölme (Blind Split)",
                        subtitle = "Paketleri analiz etmeden ilk byte'ı böler",
                        checked = !settings.splitTlsRecordAtSni,
                        onCheckedChange = { isBlind -> 
                            updateSettings(settings.copy(
                                splitTlsRecordAtSni = !isBlind,
                                splitTlsRecord = true,
                                tlsRecordSplitPosition = 1
                            )) 
                        }
                    )

                    SettingsSliderRow(
                        title = "Gecikme (Split Delay)",
                        value = settings.splitDelay.toFloat(),
                        valueRange = 0f..50f,
                        onValueChange = { updateSettings(settings.copy(splitDelay = it.toLong())) },
                        valueText = "${settings.splitDelay} ms"
                    )

                    SettingsSwitchRow(
                        icon = Icons.Default.TextFields,
                        title = "Host Karıştırma",
                        subtitle = "Host: site.com -> hoSt: site.com",
                        checked = settings.mixHostCase,
                        onCheckedChange = { updateSettings(settings.copy(mixHostCase = it)) }
                    )
                }
            }

            // === 3. DNS VE BAĞLANTI (ADBLOCK DNS'LERİ EKLENDİ) ===
            item {
                SettingsSection(title = "DNS ve Bağlantı") {
                    SettingsSwitchRow(
                        icon = Icons.Default.Dns,
                        title = "Özel DNS Kullan",
                        subtitle = "Reklam engelleme veya özel DNS için",
                        checked = settings.customDnsEnabled,
                        onCheckedChange = { updateSettings(settings.copy(customDnsEnabled = it)) }
                    )
                    
                    if (settings.customDnsEnabled) {
                        SingleChoiceRow(
                            selected = settings.customDns,
                            options = listOf(
                                "94.140.14.14" to "AdGuard",   // AdGuard Default
                                "76.76.2.2" to "Control D",    // Control D (Malware + Ads)
                                "194.242.2.2" to "Mullvad"     // Mullvad (Adblock)
                            ),
                            onSelect = { dns ->
                                val dns2 = when(dns) {
                                    "94.140.14.14" -> "94.140.15.15" // AdGuard Secondary
                                    "76.76.2.2" -> "76.76.10.2"      // Control D Secondary
                                    "194.242.2.2" -> "194.242.2.3"   // Mullvad Secondary
                                    else -> "8.8.4.4"                // Fallback (Google)
                                }
                                updateSettings(settings.copy(customDns = dns, customDns2 = dns2))
                            }
                        )
                        
                        Text(
                            text = "Seçilen DNS reklamları ve izleyicileri engeller.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                        )
                    }

                    SettingsSwitchRow(
                        icon = Icons.Outlined.Speed,
                        title = "QUIC Engelle",
                        subtitle = "YouTube/Instagram yavaşlatmasını çözer",
                        checked = settings.blockQuic,
                        onCheckedChange = { updateSettings(settings.copy(blockQuic = it)) }
                    )
                }
            }

            // === 4. SİSTEM ===
            item {
                SettingsSection(title = "Sistem") {
                    SettingsSwitchRow(
                        icon = Icons.Default.BugReport,
                        title = "Loglama",
                        subtitle = "Sorun giderirken açın",
                        checked = settings.enableLogs,
                        onCheckedChange = { updateSettings(settings.copy(enableLogs = it)) }
                    )
                    
                    if (settings.enableLogs) {
                         SettingsSwitchRow(
                            icon = Icons.Outlined.List,
                            title = "Detaylı Log",
                            subtitle = "Tüm paket hareketlerini göster",
                            checked = settings.verboseLogs,
                            onCheckedChange = { updateSettings(settings.copy(verboseLogs = it)) }
                        )
                    }
                }
            }
            
            // === 5. SIFIRLA ===
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { updateSettings(DpiSettings()) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Varsayılan Ayarlara Dön")
                    }
                }
            }

            // === 6. HAKKINDA ===
            item {
                SettingsSection(title = "Hakkında") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri("https://github.com/anonimbiri-IsBack") }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Geliştirici",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "anonimbiri",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri("https://github.com/GameSketchers/RemoveDPI") }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Orijinal Kaynak",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "GameSketchers/RemoveDPI",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// === COMPOSABLES ===

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if(checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if(checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
fun SettingsSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = 0,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SingleChoiceRow(
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
                leadingIcon = if (selected == value) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}