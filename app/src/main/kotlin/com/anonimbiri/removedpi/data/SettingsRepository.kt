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
        val defaults = DpiSettings()
        return DpiSettings(
            appTheme = try {
                AppTheme.valueOf(prefs.getString("app_theme", null) ?: defaults.appTheme.name)
            } catch (e: Exception) { defaults.appTheme },

            whitelist = prefs.getStringSet("whitelist", null) ?: defaults.whitelist,

            bufferSize = prefs.getInt("buffer_size", defaults.bufferSize),
            tcpFastOpen = prefs.getBoolean("tcp_fast_open", defaults.tcpFastOpen),
            enableTcpNodelay = prefs.getBoolean("tcp_nodelay", defaults.enableTcpNodelay),
            desyncMethod = try {
                DesyncMethod.valueOf(prefs.getString("desync_method", null) ?: defaults.desyncMethod.name)
            } catch (e: Exception) { defaults.desyncMethod },
            desyncHttp = prefs.getBoolean("desync_http", defaults.desyncHttp),
            desyncHttps = prefs.getBoolean("desync_https", defaults.desyncHttps),
            firstPacketSize = prefs.getInt("first_packet_size", defaults.firstPacketSize),
            splitDelay = prefs.getLong("split_delay", defaults.splitDelay),
            mixHostCase = prefs.getBoolean("mix_host_case", defaults.mixHostCase),
            splitCount = prefs.getInt("split_count", defaults.splitCount),
            ttlValue = prefs.getInt("ttl_value", defaults.ttlValue),
            autoTtl = prefs.getBoolean("auto_ttl", defaults.autoTtl),
            minTtl = prefs.getInt("min_ttl", defaults.minTtl),
            fakePacketMode = try {
                FakePacketMode.valueOf(prefs.getString("fake_packet_mode", null) ?: defaults.fakePacketMode.name)
            } catch (e: Exception) { defaults.fakePacketMode },
            blockQuic = prefs.getBoolean("block_quic", defaults.blockQuic),
            enableLogs = prefs.getBoolean("logs", defaults.enableLogs)
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
        editor.putInt("ttl_value", settings.ttlValue)
        editor.putBoolean("auto_ttl", settings.autoTtl)
        editor.putInt("min_ttl", settings.minTtl)
        editor.putString("fake_packet_mode", settings.fakePacketMode.name)
        editor.putBoolean("block_quic", settings.blockQuic)
        editor.putBoolean("logs", settings.enableLogs)
        editor.commit()
        
        _settings.value = settings
    }
}