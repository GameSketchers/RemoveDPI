package com.anonimbiri.removedpi.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anonimbiri.removedpi.MainActivity
import com.anonimbiri.removedpi.R
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
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            "STOP" -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> {
                startVpn()
                START_STICKY
            }
        }
    }
    
    private fun startVpn() {
        if (running) return
        
        Log.i(TAG, "Starting Remove DPI - Mode: ${settings.bypassMode}")
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        val builder = Builder()
            .setSession("Remove DPI")
            .setMtu(MTU)
            .addAddress("10.8.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setBlocking(true)
        
        if (settings.customDnsEnabled) {
            builder.addDnsServer(settings.customDns)
            if (settings.customDns2.isNotEmpty()) {
                builder.addDnsServer(settings.customDns2)
            }
        } else {
            builder.addDnsServer("8.8.8.8")
            builder.addDnsServer("1.1.1.1")
        }
        
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {}
        
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
                    
                    if (packetsIn % 100 == 0L) {
                        updateStats()
                    }
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
        } catch (e: Exception) {
            Log.e(TAG, "Packet error: ${e.message}")
        }
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
                "Remove DPI",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "DPI bypass service"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, BypassVpnService::class.java).apply { action = "STOP" }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Remove DPI Aktif")
            .setContentText("DPI engelleri kaldırılıyor • ${settings.bypassMode.name}")
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_stop, "Durdur", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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