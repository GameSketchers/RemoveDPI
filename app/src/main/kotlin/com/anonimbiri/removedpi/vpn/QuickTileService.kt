package com.anonimbiri.removedpi.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.anonimbiri.removedpi.MainActivity
import com.anonimbiri.removedpi.R
import kotlinx.coroutines.*

class QuickTileService : TileService() {

    private var updateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            BypassVpnService.isRunning.collect { isRunning ->
                updateTileState(isRunning)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        updateJob?.cancel()
        updateJob = null
    }

    override fun onClick() {
        super.onClick()

        try {
            val prepare = VpnService.prepare(this)
            if (prepare != null) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivityAndCollapse(intent)
                Toast.makeText(this, getString(R.string.tile_permission_error), Toast.LENGTH_LONG).show()
                return
            }

            if (BypassVpnService.isRunning.value) {
                val intent = Intent(this, BypassVpnService::class.java).apply { 
                    action = "STOP" 
                }
                startService(intent)
            } else {
                val intent = Intent(this, BypassVpnService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.tile_error, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTileState(isRunning: Boolean) {
        qsTile?.let { tile ->
            tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(if (isRunning) R.string.tile_on else R.string.tile_off)
            }
            
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        updateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}