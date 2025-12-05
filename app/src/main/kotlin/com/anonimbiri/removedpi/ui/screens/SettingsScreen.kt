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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.anonimbiri.removedpi.data.*
import com.anonimbiri.removedpi.vpn.BypassVpnService
import com.anonimbiri.removedpi.vpn.LogManager
import com.anonimbiri.removedpi.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWhitelist: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SettingsRepository(context) }
    val uriHandler = LocalUriHandler.current
    
    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${pInfo.versionName}"
        } catch (e: Exception) { "v1.0" }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var settings by remember { mutableStateOf(DpiSettings()) }
    
    LaunchedEffect(Unit) {
        // ARTIK HATA VERMEYECEK
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
                title = { Text("Ayarlar", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
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

            // === 1. BYPASS YÖNTEMİ ===
            item {
                SettingsSection(title = "Bypass Yöntemi") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DesyncMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = settings.desyncMethod == method,
                                    onClick = { updateSettings(settings.copy(desyncMethod = method)) },
                                    label = { Text(method.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    SettingsSwitchRow(Icons.Default.Https, "HTTPS Desync", "Güvenli bağlantıları (TLS) işle", settings.desyncHttps) { updateSettings(settings.copy(desyncHttps = it)) }
                    SettingsSwitchRow(Icons.Default.Http, "HTTP Desync", "Şifresiz bağlantıları işle", settings.desyncHttp) { updateSettings(settings.copy(desyncHttp = it)) }
                }
            }
            
            // === 2. GELİŞMİŞ AYARLAR ===
            item {
                SettingsSection(title = "Gelişmiş") {
                    if (settings.desyncMethod != DesyncMethod.DISORDER) {
                        SettingsSliderRow("Split Pozisyonu", settings.firstPacketSize.toFloat(), 1f..10f, { updateSettings(settings.copy(firstPacketSize = it.toInt())) }, "${settings.firstPacketSize}. byte")
                    }
                    if (settings.desyncMethod == DesyncMethod.DISORDER) {
                        SettingsSliderRow("Parça Sayısı", settings.splitCount.toFloat(), 2f..20f, { updateSettings(settings.copy(splitCount = it.toInt())) }, "${settings.splitCount}")
                    }
                    if (settings.desyncMethod == DesyncMethod.FAKE) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text("Fake Hex Verisi", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = settings.fakeHex,
                                onValueChange = { updateSettings(settings.copy(fakeHex = it)) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                singleLine = true
                            )
                        }
                    }
                    SettingsSliderRow("Gecikme (ms)", settings.splitDelay.toFloat(), 0f..50f, { updateSettings(settings.copy(splitDelay = it.toLong())) }, "${settings.splitDelay}")
                    SettingsSwitchRow(Icons.Default.TextFields, "Host Karıştırma", "Host -> hoSt", settings.mixHostCase) { updateSettings(settings.copy(mixHostCase = it)) }
                }
            }

            // === 3. DNS ve BAĞLANTI ===
            item {
                SettingsSection(title = "DNS ve Bağlantı") {
                    SettingsSwitchRow(Icons.Default.Dns, "Özel DNS", "Reklam engelleme ve gizlilik", settings.customDnsEnabled) { updateSettings(settings.copy(customDnsEnabled = it)) }
                    if (settings.customDnsEnabled) {
                        SingleChoiceRow(settings.customDns, listOf("94.140.14.14" to "AdGuard", "76.76.2.2" to "Control D", "1.1.1.1" to "Cloudflare")) { 
                            val dns2 = when(it) { "94.140.14.14"->"94.140.15.15" "76.76.2.2"->"76.76.10.2" else->"1.0.0.1" }
                            updateSettings(settings.copy(customDns = it, customDns2 = dns2)) 
                        }
                    }
                    SettingsSwitchRow(Icons.Outlined.Speed, "QUIC Engelle", "YouTube hızlandırma", settings.blockQuic) { updateSettings(settings.copy(blockQuic = it)) }
                    SettingsSwitchRow(Icons.Outlined.Bolt, "TCP NoDelay", "Paketleri bekletmeden gönder", settings.enableTcpNodelay) { updateSettings(settings.copy(enableTcpNodelay = it)) }
                }
            }

            // === 4. GÖRÜNÜM ===
            item {
                SettingsSection(title = "Görünüm") {
                    SingleChoiceRow(
                        selected = settings.appTheme.name,
                        options = listOf(
                            AppTheme.SYSTEM.name to "Sistem",
                            AppTheme.AMOLED.name to "Amoled",
                            AppTheme.ANIME.name to "Anime"
                        ),
                        onSelect = { themeName ->
                            updateSettings(settings.copy(appTheme = AppTheme.valueOf(themeName)))
                        }
                    )
                }
            }

            // === 5. BEYAZ LİSTE ===
            item {
                SettingsSection(title = "İstisnalar") {
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
                            Text("Beyaz Liste", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Harici tutmak istediğiniz siteler", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // === 6. SİSTEM ===
            item {
                SettingsSection(title = "Sistem") {
                    SettingsSwitchRow(Icons.Default.BugReport, "Loglama", "Hata ayıklama kayıtları", settings.enableLogs) { updateSettings(settings.copy(enableLogs = it)) }
                }
            }
            
            // === 7. SIFIRLA ===
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { updateSettings(DpiSettings()) }) { Text("Varsayılan Ayarlara Dön", color = MaterialTheme.colorScheme.error) }
                }
            }
            
            // === 8. HAKKINDA (DÜZELTİLDİ: Anonimbiri Üstte) ===
            item {
                SettingsSection(title = "Hakkında") {
                    Row(modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri("https://github.com/anonimbiri-IsBack") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(painter = painterResource(id = R.drawable.ic_removedpi), null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            // İSİM ÜSTTE VE BÜYÜK
                            Text("Anonimbiri", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            // UNVAN ALTTA VE KÜÇÜK
                            Text("Geliştirici", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Row(modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri("https://github.com/GameSketchers/RemoveDPI") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(painter = painterResource(id = R.drawable.ic_github), null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Github", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Açık kaynak kod deposu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            // === 9. SÜRÜM BİLGİSİ ===
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Remove DPI • $appVersion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// === HELPER COMPOSABLES ===

@Composable fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(24.dp)) { Column(modifier = Modifier.padding(vertical = 8.dp), content = content) }
    }
}

@Composable fun SettingsSwitchRow(icon: ImageVector, title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().toggleable(value = checked, onValueChange = onCheckedChange).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if(checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if(checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { 
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable fun SettingsSliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float)->Unit, text: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { 
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) 
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable fun SingleChoiceRow(selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (val_, lbl) -> FilterChip(selected = selected == val_, onClick = { onSelect(val_) }, label = { Text(lbl) }, modifier = Modifier.weight(1f)) }
    }
}