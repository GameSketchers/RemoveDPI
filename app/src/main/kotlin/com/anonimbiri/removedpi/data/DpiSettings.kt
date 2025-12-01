package com.anonimbiri.removedpi.data

data class DpiSettings(
    val bypassMode: BypassMode = BypassMode.FULL,
    
    val proxyEnabled: Boolean = true,
    val listenAddress: String = "127.0.0.1",
    val proxyPort: Int = 1080,
    val maxConnections: Int = 512,
    val bufferSize: Int = 32768,
    val noDomain: Boolean = false,
    val tcpFastOpen: Boolean = false,
    
    // === KRİTİK AYARLAR (TÜRKİYE İÇİN KÖR BÖLME) ===
    val desyncMethod: DesyncMethod = DesyncMethod.SPLIT,
    
    // HTTP
    val desyncHttp: Boolean = true,
    val splitAtHost: Boolean = false,
    val httpFragmentSize: Int = 1,
    val mixHostCase: Boolean = true,
    
    // HTTPS (Blind Split - İlk 1 byte'ı bölme)
    val desyncHttps: Boolean = true,
    val splitTlsRecord: Boolean = true,
    val tlsRecordSplitPosition: Int = 1,      // İlk byte'ı ayır
    val splitTlsRecordAtSni: Boolean = false, // SNI aramayı kapat
    val randomizeSniCase: Boolean = false,
    
    // Genel
    val splitDelay: Long = 2L,
    val enableTcpNodelay: Boolean = true,
    
    // === DNS AYARLARI (GÜNCELLENDİ) ===
    // Varsayılan olarak KAPALI (İsteğin üzerine)
    val customDnsEnabled: Boolean = false, 
    // Varsayılan değerler (UI'da seçili gelmesi için, ama aktif değil)
    val customDns: String = "94.140.14.14", // AdGuard Default
    val customDns2: String = "94.140.15.15",
    val blockQuic: Boolean = true, // YouTube hızlandırma için açık kalmalı
    
    // Diğer Standart Ayarlar
    val desyncHosts: String = "",
    val defaultTtl: Int = 0,
    val dropSack: Boolean = true,
    val desyncUdp: Boolean = false,
    val splitPosition: Int = 2,
    val mixDomainCase: Boolean = false,
    val removeHostSpaces: Boolean = true,
    val addSpaceAfterHost: Boolean = false,
    val addTabAfterHost: Boolean = false,
    val removeSpaceAfterColon: Boolean = true,
    val httpVersion: String = "1.1",
    val httpsFragmentSize: Int = 1,
    val sniSplitPosition: Int = 1,
    val tlsRecordFragmentation: Boolean = true,
    val tlsFragmentCount: Int = 5,
    val splitBetweenSni: Boolean = true,
    val udpFakeCount: Int = 0,
    val firstPacketSplitSize: Int = 1,
    val connectionTimeout: Int = 15000,
    val readTimeout: Int = 30000,
    val windowSize: Int = 65535,
    val keepAlive: Boolean = true,
    val multiSplitMode: Boolean = true,
    val splitCount: Int = 3,
    val randomSplitDelay: Boolean = false,
    val minSplitDelay: Long = 1L,
    val maxSplitDelay: Long = 5L,
    val oobEnabled: Boolean = false,
    val fakePacketEnabled: Boolean = false,
    val fakeTtl: Int = 3,
    val fakePacketSize: Int = 10,
    val sendFakeBeforeReal: Boolean = false,
    val enableDoH: Boolean = false,
    val dohServer: String = "https://cloudflare-dns.com/dns-query",
    
    // Loglama
    val enableLogs: Boolean = true,
    val verboseLogs: Boolean = false
)

enum class BypassMode { NONE, SPLIT, DISORDER, FAKE, OOB, FULL }
enum class DesyncMethod { NONE, SPLIT, DISORDER, FAKE, OOB, SPLIT_DISORDER, SPLIT_FAKE }
enum class VpnState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }