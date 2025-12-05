package com.anonimbiri.removedpi.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("dpi_prefs", Context.MODE_PRIVATE)

    // Singleton StateFlow (Veri ortak, hafızada tek bir kopya tutulur)
    companion object {
        private val _settings = MutableStateFlow(DpiSettings())
        // Sınıf üzerinden erişim için (SettingsRepository.settings)
        val globalSettings: Flow<DpiSettings> = _settings.asStateFlow()
    }

    // Nesne üzerinden erişim için (repository.settings) - BU SATIR HATAYI ÇÖZER
    val settings: Flow<DpiSettings> = globalSettings

    init {
        // Başlangıçta verileri yükle
        _settings.value = loadSettings()
    }
    
    private fun loadSettings(): DpiSettings {
        return DpiSettings(
            appTheme = try {
                AppTheme.valueOf(prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM")
            } catch (e: Exception) { AppTheme.SYSTEM },

            whitelist = prefs.getStringSet("whitelist", setOf(
                "garanti.com.tr", "ziraatbank.com.tr", "isbank.com.tr", 
                "yapikredi.com.tr", "akbank.com", "turkiye.gov.tr", 
                "enabiz.gov.tr", "gib.gov.tr", "vakifbank.com.tr",
                "halkbank.com.tr", "qnbfinansbank.com", "denizbank.com",
                "teb.com.tr", "kuveytturk.com.tr", "turkcell.com.tr",
                "vodafone.com.tr", "turktelekom.com.tr", "mhrs.gov.tr", "cimer.gov.tr"
            )) ?: emptySet(),

            bufferSize = prefs.getInt("buffer_size", 32768),
            tcpFastOpen = prefs.getBoolean("tcp_fast_open", false),
            enableTcpNodelay = prefs.getBoolean("tcp_nodelay", true),
            desyncMethod = try {
                DesyncMethod.valueOf(prefs.getString("desync_method", "SPLIT") ?: "SPLIT")
            } catch (e: Exception) { DesyncMethod.SPLIT },
            desyncHttp = prefs.getBoolean("desync_http", true),
            desyncHttps = prefs.getBoolean("desync_https", true),
            firstPacketSize = prefs.getInt("first_packet_size", 1),
            splitDelay = prefs.getLong("split_delay", 2L),
            mixHostCase = prefs.getBoolean("mix_host_case", true),
            splitCount = prefs.getInt("split_count", 3),
            fakeHex = prefs.getString("fake_hex", "474554202f20485454502f312e300d0a0d0a") ?: "474554202f20485454502f312e300d0a0d0a",
            fakeCount = prefs.getInt("fake_count", 1),
            customDnsEnabled = prefs.getBoolean("dns_enabled", false),
            customDns = prefs.getString("dns1", "94.140.14.14") ?: "94.140.14.14",
            customDns2 = prefs.getString("dns2", "94.140.15.15") ?: "94.140.15.15",
            blockQuic = prefs.getBoolean("block_quic", true),
            enableLogs = prefs.getBoolean("logs", true),
            bypassMode = try {
                BypassMode.valueOf(prefs.getString("bypass_mode", "FULL") ?: "FULL")
            } catch (e: Exception) { BypassMode.FULL }
        )
    }
    
    suspend fun updateSettings(settings: DpiSettings) {
        prefs.edit().apply {
            putString("app_theme", settings.appTheme.name)
            putStringSet("whitelist", settings.whitelist)
            putInt("buffer_size", settings.bufferSize)
            putBoolean("tcp_fast_open", settings.tcpFastOpen)
            putBoolean("tcp_nodelay", settings.enableTcpNodelay)
            putString("desync_method", settings.desyncMethod.name)
            putBoolean("desync_http", settings.desyncHttp)
            putBoolean("desync_https", settings.desyncHttps)
            putInt("first_packet_size", settings.firstPacketSize)
            putLong("split_delay", settings.splitDelay)
            putBoolean("mix_host_case", settings.mixHostCase)
            putInt("split_count", settings.splitCount)
            putString("fake_hex", settings.fakeHex)
            putInt("fake_count", settings.fakeCount)
            putBoolean("dns_enabled", settings.customDnsEnabled)
            putString("dns1", settings.customDns)
            putString("dns2", settings.customDns2)
            putBoolean("block_quic", settings.blockQuic)
            putBoolean("logs", settings.enableLogs)
            putString("bypass_mode", settings.bypassMode.name)
            apply()
        }
        _settings.value = settings
    }
}