package com.anonimbiri.removedpi.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("dpi_prefs", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: Flow<DpiSettings> = _settings.asStateFlow()
    
    private fun loadSettings(): DpiSettings {
        return DpiSettings(
            // Proxy
            proxyEnabled = prefs.getBoolean("proxy_enabled", true),
            listenAddress = prefs.getString("listen_address", "127.0.0.1") ?: "127.0.0.1",
            proxyPort = prefs.getInt("proxy_port", 1080),
            maxConnections = prefs.getInt("max_connections", 512),
            bufferSize = prefs.getInt("buffer_size", 32768),
            noDomain = prefs.getBoolean("no_domain", false),
            tcpFastOpen = prefs.getBoolean("tcp_fast_open", false),
            
            // Desync Method
            desyncMethod = try {
                DesyncMethod.valueOf(prefs.getString("desync_method", "SPLIT") ?: "SPLIT")
            } catch (e: Exception) { DesyncMethod.SPLIT },
            
            desyncHosts = prefs.getString("desync_hosts", "") ?: "",
            defaultTtl = prefs.getInt("default_ttl", 0),
            dropSack = prefs.getBoolean("drop_sack", true),
            
            // Protocols
            desyncHttp = prefs.getBoolean("desync_http", true),
            desyncHttps = prefs.getBoolean("desync_https", true),
            desyncUdp = prefs.getBoolean("desync_udp", false),
            
            // HTTP
            splitAtHost = prefs.getBoolean("split_at_host", false),
            splitPosition = prefs.getInt("split_position", 2),
            httpFragmentSize = prefs.getInt("http_fragment_size", 1),
            mixHostCase = prefs.getBoolean("mix_host_case", true),
            mixDomainCase = prefs.getBoolean("mix_domain_case", false),
            removeHostSpaces = prefs.getBoolean("remove_host_spaces", true),
            
            // HTTPS/TLS (Blind Split Settings)
            splitTlsRecord = prefs.getBoolean("split_tls_record", true),
            tlsRecordSplitPosition = prefs.getInt("tls_record_split_pos", 1),
            splitTlsRecordAtSni = prefs.getBoolean("split_tls_at_sni", false),
            httpsFragmentSize = prefs.getInt("https_fragment_size", 1),
            sniSplitPosition = prefs.getInt("sni_split_position", 1),
            tlsRecordFragmentation = prefs.getBoolean("tls_record_frag", true),
            tlsFragmentCount = prefs.getInt("tls_fragment_count", 5),
            splitBetweenSni = prefs.getBoolean("split_between_sni", true),
            
            // UDP
            udpFakeCount = prefs.getInt("udp_fake_count", 0),
            
            // TCP
            splitDelay = prefs.getLong("split_delay", 2L),
            firstPacketSplitSize = prefs.getInt("first_packet_split", 1),
            enableTcpNodelay = prefs.getBoolean("tcp_nodelay", true),
            
            // Gelişmiş
            multiSplitMode = prefs.getBoolean("multi_split", true),
            splitCount = prefs.getInt("split_count", 3),
            randomSplitDelay = prefs.getBoolean("random_delay", false),
            minSplitDelay = prefs.getLong("min_delay", 1L),
            maxSplitDelay = prefs.getLong("max_delay", 5L),
            
            // DNS (Burada varsayılan değer FALSE yapıldı)
            customDnsEnabled = prefs.getBoolean("dns_enabled", false),
            customDns = prefs.getString("dns1", "94.140.14.14") ?: "94.140.14.14",
            customDns2 = prefs.getString("dns2", "94.140.15.15") ?: "94.140.15.15",
            blockQuic = prefs.getBoolean("block_quic", true),
            
            // Debug
            enableLogs = prefs.getBoolean("logs", true),
            verboseLogs = prefs.getBoolean("verbose_logs", false),
            
            bypassMode = try {
                BypassMode.valueOf(prefs.getString("bypass_mode", "FULL") ?: "FULL")
            } catch (e: Exception) { BypassMode.FULL }
        )
    }
    
    suspend fun updateSettings(settings: DpiSettings) {
        prefs.edit().apply {
            putBoolean("proxy_enabled", settings.proxyEnabled)
            putString("listen_address", settings.listenAddress)
            putInt("proxy_port", settings.proxyPort)
            putInt("max_connections", settings.maxConnections)
            putInt("buffer_size", settings.bufferSize)
            putBoolean("no_domain", settings.noDomain)
            putBoolean("tcp_fast_open", settings.tcpFastOpen)
            
            putString("desync_method", settings.desyncMethod.name)
            putString("desync_hosts", settings.desyncHosts)
            putInt("default_ttl", settings.defaultTtl)
            putBoolean("drop_sack", settings.dropSack)
            
            putBoolean("desync_http", settings.desyncHttp)
            putBoolean("desync_https", settings.desyncHttps)
            putBoolean("desync_udp", settings.desyncUdp)
            
            putBoolean("split_at_host", settings.splitAtHost)
            putInt("split_position", settings.splitPosition)
            putInt("http_fragment_size", settings.httpFragmentSize)
            putBoolean("mix_host_case", settings.mixHostCase)
            putBoolean("mix_domain_case", settings.mixDomainCase)
            putBoolean("remove_host_spaces", settings.removeHostSpaces)
            
            putBoolean("split_tls_record", settings.splitTlsRecord)
            putInt("tls_record_split_pos", settings.tlsRecordSplitPosition)
            putBoolean("split_tls_at_sni", settings.splitTlsRecordAtSni)
            putInt("https_fragment_size", settings.httpsFragmentSize)
            putInt("sni_split_position", settings.sniSplitPosition)
            putBoolean("tls_record_frag", settings.tlsRecordFragmentation)
            putInt("tls_fragment_count", settings.tlsFragmentCount)
            putBoolean("split_between_sni", settings.splitBetweenSni)
            
            putInt("udp_fake_count", settings.udpFakeCount)
            
            putLong("split_delay", settings.splitDelay)
            putInt("first_packet_split", settings.firstPacketSplitSize)
            putBoolean("tcp_nodelay", settings.enableTcpNodelay)
            
            putBoolean("multi_split", settings.multiSplitMode)
            putInt("split_count", settings.splitCount)
            putBoolean("random_delay", settings.randomSplitDelay)
            putLong("min_delay", settings.minSplitDelay)
            putLong("max_delay", settings.maxSplitDelay)
            
            putBoolean("dns_enabled", settings.customDnsEnabled)
            putString("dns1", settings.customDns)
            putString("dns2", settings.customDns2)
            putBoolean("block_quic", settings.blockQuic)
            
            putBoolean("logs", settings.enableLogs)
            putBoolean("verbose_logs", settings.verboseLogs)
            putString("bypass_mode", settings.bypassMode.name)
            
            apply()
        }
        _settings.value = settings
    }
}