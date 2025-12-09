package com.anonimbiri.removedpi.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("dpi_prefs", Context.MODE_PRIVATE)

    companion object {
        private val _settings = MutableStateFlow(DpiSettings())
        val globalSettings: Flow<DpiSettings> = _settings.asStateFlow()
    }

    val settings: Flow<DpiSettings> = globalSettings

    init {
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
        fakeHex = prefs.getString("fake_hex", "160301") ?: "160301",
        fakeCount = prefs.getInt("fake_count", 10),
        ttlValue = prefs.getInt("ttl_value", 8),
        customDnsEnabled = prefs.getBoolean("dns_enabled", false),
        customDns = prefs.getString("dns1", "94.140.14.14") ?: "94.140.14.14",
        customDns2 = prefs.getString("dns2", "94.140.15.15") ?: "94.140.15.15",
        blockQuic = prefs.getBoolean("block_quic", true),
        enableLogs = prefs.getBoolean("logs", true)
    )
}

suspend fun updateSettings(settings: DpiSettings) {
    val editor = prefs.edit()
    editor.putString("app_theme", settings.appTheme.name)
    editor.putStringSet("whitelist", settings.whitelist)
    editor.putInt("buffer_size", settings.bufferSize)
    editor.putBoolean("tcp_fast_open", settings.tcpFastOpen)
    editor.putBoolean("tcp_nodelay", settings.enableTcpNodelay)
    editor.putString("desync_method", settings.desyncMethod.name)
    editor.putBoolean("desync_http", settings.desyncHttp)
    editor.putBoolean("desync_https", settings.desyncHttps)
    editor.putInt("first_packet_size", settings.firstPacketSize)
    editor.putLong("split_delay", settings.splitDelay)
    editor.putBoolean("mix_host_case", settings.mixHostCase)
    editor.putInt("split_count", settings.splitCount)
    editor.putString("fake_hex", settings.fakeHex)
    editor.putInt("fake_count", settings.fakeCount)
    editor.putInt("ttl_value", settings.ttlValue)
    editor.putBoolean("dns_enabled", settings.customDnsEnabled)
    editor.putString("dns1", settings.customDns)
    editor.putString("dns2", settings.customDns2)
    editor.putBoolean("block_quic", settings.blockQuic)
    editor.putBoolean("logs", settings.enableLogs)
    editor.commit()
    
    _settings.value = settings
}
}