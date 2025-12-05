package com.anonimbiri.removedpi.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anonimbiri.removedpi.MainActivity
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.data.DesyncMethod
import com.anonimbiri.removedpi.data.DpiSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class BypassVpnService : VpnService() {
    
    companion object {
        private const val TAG = "RemoveDPI"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "remove_dpi_channel"
        private const val MTU = 1500
        
        // PendingIntent Request Codes
        private const val REQUEST_CODE_STOP = 1001
        private const val REQUEST_CODE_OPEN = 1002
        private const val REQUEST_CODE_SETTINGS = 1003
        
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning
        
        private val _stats = MutableStateFlow(Stats())
        val stats: StateFlow<Stats> = _stats
        
        var settings: DpiSettings = DpiSettings()
    }
    
    data class Stats(
        val packetsIn: Long = 0,
        val packetsOut: Long = 0,
        val bytesIn: Long = 0,
        val bytesOut: Long = 0
    )
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnInput: FileInputStream? = null
    private var vpnOutput: FileOutputStream? = null
    
    private var tcpHandler: TcpConnection? = null
    private var udpHandler: UdpConnection? = null
    
    @Volatile
    private var running = false
    
    private var packetsIn = 0L
    private var packetsOut = 0L

    override fun onCreate() {
        super.onCreate()
        loadSettingsDirectly()
    }
    
    private fun loadSettingsDirectly() {
        try {
            val prefs = getSharedPreferences("dpi_prefs", Context.MODE_PRIVATE)
            settings = DpiSettings(
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
                whitelist = prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
            )
            Log.i(TAG, "Settings loaded manually")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings manually: ${e.message}")
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            "STOP" -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> {
                loadSettingsDirectly()
                startVpn()
                START_STICKY
            }
        }
    }
    
    private fun startVpn() {
        if (running) return
        
        Log.i(TAG, "Starting Remove DPI...")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        try {
            val builder = Builder()
                .setSession("Remove DPI")
                .setMtu(MTU)
                .addAddress("10.8.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .setBlocking(true)
            
            builder.setConfigureIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            
            // DNS Ayarları
            if (settings.customDnsEnabled) {
                builder.addDnsServer(settings.customDns)
                if (settings.customDns2.isNotEmpty()) {
                    builder.addDnsServer(settings.customDns2)
                }
            } else {
                builder.addDnsServer("8.8.8.8")
            }
            
            // Metered bağlantı olarak işaretleme (Android Q+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }
            
            // Kendi uygulamamızı VPN dışında tut
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not exclude self from VPN")
            }
            
            vpnInterface = builder.establish()
            
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN")
                stopSelf()
                return
            }
            
            vpnInput = FileInputStream(vpnInterface!!.fileDescriptor)
            vpnOutput = FileOutputStream(vpnInterface!!.fileDescriptor)
            
            tcpHandler = TcpConnection(this, vpnOutput!!, settings)
            udpHandler = UdpConnection(this, vpnOutput!!, settings)
            
            running = true
            _isRunning.value = true
            
            Thread({ runVpnLoop() }, "RemoveDPI-Loop").start()
            Log.i(TAG, "Remove DPI started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN: ${e.message}")
            stopVpn()
        }
    }
    
    private fun runVpnLoop() {
        val buffer = ByteBuffer.allocate(MTU)
        val input = vpnInput ?: return
        
        while (running) {
            try {
                buffer.clear()
                val length = input.read(buffer.array())
                if (length > 0) {
                    buffer.limit(length)
                    processPacket(buffer, length)
                    packetsIn++
                    if (packetsIn % 100 == 0L) updateStats()
                } else if (length < 0) {
                    break
                }
            } catch (e: Exception) {
                if (running) Log.e(TAG, "Loop error: ${e.message}")
                break
            }
        }
    }
    
    private fun processPacket(buffer: ByteBuffer, length: Int) {
        try {
            val packet = Packet(buffer)
            if (packet.version != 4) return
            when (packet.protocol) {
                Packet.PROTOCOL_TCP -> {
                    tcpHandler?.processPacket(packet)
                    packetsOut++
                }
                Packet.PROTOCOL_UDP -> {
                    udpHandler?.processPacket(packet)
                    packetsOut++
                }
            }
        } catch (e: Exception) {}
    }
    
    private fun updateStats() {
        val tcpStats = tcpHandler?.getStats() ?: (0L to 0L)
        val udpStats = udpHandler?.getStats() ?: (0L to 0L)
        _stats.value = Stats(
            packetsIn = packetsIn,
            packetsOut = packetsOut,
            bytesIn = tcpStats.first + udpStats.first,
            bytesOut = tcpStats.second + udpStats.second
        )
    }
    
    private fun stopVpn() {
        Log.i(TAG, "Stopping Remove DPI...")
        running = false
        _isRunning.value = false
        tcpHandler?.stop()
        udpHandler?.stop()
        try {
            vpnInput?.close()
            vpnOutput?.close()
            vpnInterface?.close()
        } catch (e: Exception) {}
        vpnInput = null
        vpnOutput = null
        vpnInterface = null
        tcpHandler = null
        udpHandler = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "Remove DPI stopped")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, 
                "Remove DPI Service", 
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka plan VPN servisi"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // 1. DURDUR BUTONU
        val stopIntent = Intent(this, BypassVpnService::class.java).apply { 
            action = "STOP" 
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 
            REQUEST_CODE_STOP,
            stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 2. BİLDİRİME TIKLANINCA - Ana ekranı açar
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 
            REQUEST_CODE_OPEN,
            openIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 3. AYARLAR BUTONU - Ayarlar ekranını açar
        val settingsIntent = Intent(this, MainActivity::class.java).apply {
            action = "OPEN_SETTINGS"
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            this, 
            REQUEST_CODE_SETTINGS,
            settingsIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Remove DPI Aktif")
            .setContentText("DPI engelleri kaldırılıyor")
            .setSmallIcon(R.drawable.ic_removedpi)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_stop, "Durdur", stopPendingIntent)
            .addAction(R.drawable.ic_settings, "Ayarlar", settingsPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
    
    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
}