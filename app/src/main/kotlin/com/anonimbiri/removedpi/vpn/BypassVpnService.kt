package com.anonimbiri.removedpi.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anonimbiri.removedpi.MainActivity
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.data.DesyncMethod
import com.anonimbiri.removedpi.data.DpiSettings
import com.anonimbiri.removedpi.data.FakePacketMode
import kotlinx.coroutines.*
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

        private const val REQUEST_CODE_STOP = 1001
        private const val REQUEST_CODE_OPEN = 1002
        private const val REQUEST_CODE_SETTINGS = 1003

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private val _stats = MutableStateFlow(Stats())
        val stats: StateFlow<Stats> = _stats
        
        private var startTime: Long = 0L

        @Volatile
        var settings: DpiSettings = DpiSettings()
            set(value) {
                field = value
                currentInstance?.applySettingsChange(value)
            }

        private var currentInstance: BypassVpnService? = null
        
        fun requestTileUpdate(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    TileService.requestListeningState(
                        context,
                        ComponentName(context, QuickTileService::class.java)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update tile: ${e.message}")
            }
        }
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

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        currentInstance = this
        loadSettingsDirectly()
    }

    private fun loadSettingsDirectly() {
        try {
            val prefs = getSharedPreferences("dpi_prefs", Context.MODE_PRIVATE)
            val defaults = DpiSettings()
            settings = DpiSettings(
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
                enableLogs = prefs.getBoolean("logs", defaults.enableLogs),
                whitelist = prefs.getStringSet("whitelist", null) ?: defaults.whitelist
            )
            Log.i(TAG, "Settings loaded")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings: ${e.message}")
        }
    }

    private fun applySettingsChange(newSettings: DpiSettings) {
        if (!running) return
        
        serviceScope.launch {
            try {
                LogManager.enabled = newSettings.enableLogs
                tcpHandler?.updateSettings(newSettings)
                udpHandler?.updateSettings(newSettings)
                Log.i(TAG, "Settings applied dynamically")
            } catch (e: Exception) {
                Log.e(TAG, "Error applying settings: ${e.message}")
            }
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
        
        startTime = System.currentTimeMillis()
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        try {
            val builder = Builder()
                .setSession(getString(R.string.app_name))
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

            builder.addDnsServer("8.8.8.8")
            builder.addDnsServer("8.8.4.4")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

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
            
            requestTileUpdate(this)

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
        
        requestTileUpdate(this)
        
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
                getString(R.string.notif_channel_name), 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, BypassVpnService::class.java).apply { 
            action = "STOP" 
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 
            REQUEST_CODE_STOP,
            stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 
            REQUEST_CODE_OPEN,
            openIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val settingsIntent = Intent(this, MainActivity::class.java).apply {
            action = "OPEN_SETTINGS"
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            this, 
            REQUEST_CODE_SETTINGS,
            settingsIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.mipmap.ic_removedpi_monochrome)
            .setContentIntent(openPendingIntent)
            .setUsesChronometer(true)
            .setWhen(startTime)
            .setChronometerCountDown(false)
            .setOngoing(true)
            .setSilent(false)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_stop, getString(R.string.action_stop), stopPendingIntent)
            .addAction(R.drawable.ic_settings, getString(R.string.action_settings), settingsPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .build()
    }

    override fun onDestroy() {
        currentInstance = null
        serviceScope.cancel()
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
}