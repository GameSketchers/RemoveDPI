package com.anonimbiri.removedpi.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anonimbiri.removedpi.data.*
import com.anonimbiri.removedpi.vpn.BypassVpnService
import com.anonimbiri.removedpi.vpn.LogManager
import com.anonimbiri.removedpi.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToUpdate: () -> Unit,
    onNavigateToLanguage: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SettingsRepository(context) }
    val uriHandler = LocalUriHandler.current

    val isVpnRunning by BypassVpnService.isRunning.collectAsState()

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${pInfo.versionName}"
        } catch (e: Exception) { "v1.0" }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var settings by remember { mutableStateOf(DpiSettings()) }

    LaunchedEffect(Unit) {
        repository.settings.collect { loadedSettings ->
            settings = loadedSettings
            if (!isVpnRunning) {
                BypassVpnService.settings = loadedSettings
            }
            LogManager.enabled = loadedSettings.enableLogs
        }
    }

    fun updateSettings(newSettings: DpiSettings) {
        settings = newSettings
        LogManager.enabled = newSettings.enableLogs
        scope.launch { 
            repository.updateSettings(newSettings)
            if (isVpnRunning) {
                BypassVpnService.settings = newSettings
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.screen_settings_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { 
                    IconButton(onClick = onNavigateBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back)) 
                    } 
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    stringResource(R.string.theme_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                SingleChoiceRow(
                                    selected = settings.appTheme.name,
                                    options = listOf(
                                        AppTheme.SYSTEM.name to stringResource(R.string.theme_system),
                                        AppTheme.AMOLED.name to stringResource(R.string.theme_amoled),
                                        AppTheme.ANIME.name to stringResource(R.string.theme_anime)
                                    ),
                                    onSelect = { themeName ->
                                        updateSettings(settings.copy(appTheme = AppTheme.valueOf(themeName)))
                                    }
                                )
                            }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToLanguage() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Language,
                                        null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.lang_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        stringResource(R.string.lang_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.cat_bypass)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.desyncMethod == DesyncMethod.SPLIT,
                                onClick = { updateSettings(settings.copy(desyncMethod = DesyncMethod.SPLIT)) },
                                label = { Text("SPLIT") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.desyncMethod == DesyncMethod.DISORDER,
                                onClick = { updateSettings(settings.copy(desyncMethod = DesyncMethod.DISORDER)) },
                                label = { Text("DISORDER") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.desyncMethod == DesyncMethod.FAKE,
                                onClick = { updateSettings(settings.copy(desyncMethod = DesyncMethod.FAKE)) },
                                label = { Text("FAKE") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.desyncMethod == DesyncMethod.TTL,
                                onClick = { updateSettings(settings.copy(desyncMethod = DesyncMethod.TTL)) },
                                label = { Text("TTL") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.desyncMethod == DesyncMethod.FAKE_SPLIT,
                                onClick = { updateSettings(settings.copy(desyncMethod = DesyncMethod.FAKE_SPLIT)) },
                                label = { Text("FAKE+SPLIT") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    SettingsSwitchRow(
                        Icons.Default.Https,
                        stringResource(R.string.https_title),
                        stringResource(R.string.https_desc),
                        settings.desyncHttps
                    ) { 
                        updateSettings(settings.copy(desyncHttps = it)) 
                    }
                    SettingsSwitchRow(
                        Icons.Default.Http,
                        stringResource(R.string.http_title),
                        stringResource(R.string.http_desc),
                        settings.desyncHttp
                    ) { 
                        updateSettings(settings.copy(desyncHttp = it)) 
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.cat_advanced)) {
                    if (settings.desyncMethod != DesyncMethod.DISORDER && settings.desyncMethod != DesyncMethod.TTL) {
                        SettingsSliderRow(
                            stringResource(R.string.adv_split_pos),
                            settings.firstPacketSize.toFloat(), 
                            1f..10f, 
                            { updateSettings(settings.copy(firstPacketSize = it.toInt())) }, 
                            "${settings.firstPacketSize} ${stringResource(R.string.unit_byte)}"
                        )
                    }
                    if (settings.desyncMethod == DesyncMethod.DISORDER) {
                        SettingsSliderRow(
                            stringResource(R.string.adv_split_count),
                            settings.splitCount.toFloat(), 
                            2f..20f, 
                            { updateSettings(settings.copy(splitCount = it.toInt())) }, 
                            "${settings.splitCount}"
                        )
                    }
                    if (settings.desyncMethod == DesyncMethod.FAKE || settings.desyncMethod == DesyncMethod.FAKE_SPLIT) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                stringResource(R.string.adv_fake_hex),
                                style = MaterialTheme.typography.bodyMedium, 
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedTextField(
                                value = settings.fakeHex,
                                onValueChange = { updateSettings(settings.copy(fakeHex = it)) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                singleLine = true,
                                placeholder = { Text("160301") },
                                shape = RoundedCornerShape(12.dp)
                            )
                            Text(
                                stringResource(R.string.adv_fake_hex_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        SettingsSliderRow(
                            stringResource(R.string.adv_fake_count),
                            settings.fakeCount.toFloat(), 
                            1f..20f, 
                            { updateSettings(settings.copy(fakeCount = it.toInt())) }, 
                            "${settings.fakeCount}"
                        )
                    }
                    if (settings.desyncMethod == DesyncMethod.TTL) {
                        SettingsSliderRow(
                            stringResource(R.string.adv_ttl),
                            settings.ttlValue.toFloat(), 
                            1f..64f, 
                            { updateSettings(settings.copy(ttlValue = it.toInt())) }, 
                            "${settings.ttlValue}"
                        )
                    }
                    SettingsSliderRow(
                        stringResource(R.string.adv_delay),
                        settings.splitDelay.toFloat(), 
                        0f..50f, 
                        { updateSettings(settings.copy(splitDelay = it.toLong())) }, 
                        "${settings.splitDelay}"
                    )
                    SettingsSwitchRow(
                        Icons.Default.TextFields, 
                        stringResource(R.string.adv_host_mix),
                        stringResource(R.string.adv_host_mix_desc),
                        settings.mixHostCase
                    ) { 
                        updateSettings(settings.copy(mixHostCase = it)) 
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.cat_dns)) {
                    SettingsSwitchRow(
                        Icons.Default.Dns, 
                        stringResource(R.string.dns_custom),
                        stringResource(R.string.dns_custom_desc),
                        settings.customDnsEnabled
                    ) { 
                        updateSettings(settings.copy(customDnsEnabled = it)) 
                    }
                    if (settings.customDnsEnabled) {
                        SingleChoiceRow(
                            settings.customDns, 
                            listOf(
                                "94.140.14.14" to "AdGuard", 
                                "76.76.2.2" to "Control D", 
                                "1.1.1.1" to "Cloudflare"
                            )
                        ) { 
                            val dns2 = when(it) { 
                                "94.140.14.14" -> "94.140.15.15" 
                                "76.76.2.2" -> "76.76.10.2" 
                                else -> "1.0.0.1" 
                            }
                            updateSettings(settings.copy(customDns = it, customDns2 = dns2)) 
                        }
                    }
                    SettingsSwitchRow(
                        Icons.Outlined.Speed, 
                        stringResource(R.string.dns_block_quic),
                        stringResource(R.string.dns_block_quic_desc),
                        settings.blockQuic
                    ) { 
                        updateSettings(settings.copy(blockQuic = it)) 
                    }
                    SettingsSwitchRow(
                        Icons.Outlined.Bolt, 
                        stringResource(R.string.dns_tcp_nodelay),
                        stringResource(R.string.dns_tcp_nodelay_desc),
                        settings.enableTcpNodelay
                    ) { 
                        updateSettings(settings.copy(enableTcpNodelay = it)) 
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.cat_exceptions)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToWhitelist() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.whitelist_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.whitelist_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.cat_system)) {
                    SettingsSwitchRow(
                        Icons.Default.BugReport, 
                        stringResource(R.string.sys_logging),
                        stringResource(R.string.sys_logging_desc),
                        settings.enableLogs
                    ) { 
                        updateSettings(settings.copy(enableLogs = it)) 
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { updateSettings(DpiSettings()) }) { 
                        Text(stringResource(R.string.sys_reset), color = MaterialTheme.colorScheme.error) 
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.cat_about)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri("https://github.com/anonimbiri-IsBack") }
                            .padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer), 
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_removedpi), null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Anonimbiri", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.about_dev), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer), 
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_github), null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.about_repo_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.about_repo_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToUpdate() }
                            .padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Update, 
                                null, 
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.about_update_title),
                                style = MaterialTheme.typography.bodyLarge, 
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                stringResource(R.string.about_update_desc),
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos, 
                            null, 
                            modifier = Modifier.size(16.dp), 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "${stringResource(R.string.app_name)} • $appVersion",
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable 
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            title, 
            style = MaterialTheme.typography.titleSmall, 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.Bold, 
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ), 
            shape = RoundedCornerShape(24.dp)
        ) { 
            Column(modifier = Modifier.padding(vertical = 8.dp), content = content) 
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
            .toggleable(value = checked, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if(checked) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                null, 
                tint = if(checked) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { 
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(
                    subtitle, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable 
fun SettingsSliderRow(
    title: String, 
    value: Float, 
    range: ClosedFloatingPointRange<Float>, 
    onChange: (Float) -> Unit, 
    text: String
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, 
            modifier = Modifier.fillMaxWidth()
        ) { 
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) 
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable 
fun SingleChoiceRow(
    selected: String, 
    options: List<Pair<String, String>>, 
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), 
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (val_, lbl) -> 
            FilterChip(
                selected = selected == val_, 
                onClick = { onSelect(val_) }, 
                label = { Text(lbl) }, 
                modifier = Modifier.weight(1f)
            ) 
        }
    }
}