package com.anonimbiri.removedpi.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.anonimbiri.removedpi.MainActivity

class QuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        try {
            // VPN izni verilmiş mi kontrol et
            val prepare = VpnService.prepare(this)
            if (prepare != null) {
                // İzin verilmemişse, kullanıcıyı uygulamaya gönder
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityAndCollapse(intent)
                Toast.makeText(this, "Lütfen önce uygulamayı açıp VPN izni verin.", Toast.LENGTH_LONG).show()
                return
            }

            if (BypassVpnService.isRunning.value) {
                // Çalışıyorsa DURDUR
                val intent = Intent(this, BypassVpnService::class.java).apply { action = "STOP" }
                startService(intent)
            } else {
                // Duruyorsa BAŞLAT
                val intent = Intent(this, BypassVpnService::class.java)
                // Android 8+ (Oreo) için Foreground başlatma zorunluluğu
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
            
            // Durumu geçici olarak güncelle (Servis başlayana kadar UI tepki versin)
            val tile = qsTile
            if (tile != null) {
                tile.state = if (BypassVpnService.isRunning.value) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
                tile.updateTile()
            }
            
            // Gerçek durumu kontrol etmek için kısa gecikme
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                updateTileState()
            }, 500)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = BypassVpnService.isRunning.value
        
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        
        
        // manifest'te zaten tanımlı olduğu için aşağıdakine gerek yoktur.
        
        //tile.label = "Remove DPI"
        
        // Android Quick Settings ikonları monokrom (tek renk) olmak zorundadır.
        // Sistem otomatik olarak aktifken tema rengine, pasifken griye boyar.
        // Renkli resim koysanız bile Android onu beyaza boyar.
        //tile.icon = Icon.createWithResource(this, R.drawable.ic_removedpi)
        
        tile.updateTile()
    }
}