package com.anonimbiri.removedpi.vpn

import com.anonimbiri.removedpi.data.DpiSettings
import java.io.OutputStream
import java.net.Socket

class DpiBypass(private val settings: DpiSettings) {
    
    companion object {
        private const val TAG = "DpiBypass"
    }
    
    fun sendWithBypass(socket: Socket, data: ByteArray, isHttps: Boolean): Boolean {
        if (data.isEmpty()) return false
        val output = socket.getOutputStream()
        
        return try {
            // HTTPS (Port 443)
            if (isHttps && settings.desyncHttps) {
                // Sadece TLS ClientHello olup olmadığına bak (Başlangıç paketi mi?)
                if (isTlsClientHello(data)) {
                    LogManager.bypass("HTTPS Detected - Blind Splitting (First Byte)")
                    // KÖR BÖLME: Direkt 1. byte'tan böl. En garanti yöntem.
                    return sendTwoParts(output, data, 1)
                }
            } 
            // HTTP (Port 80)
            else if (!isHttps && settings.desyncHttp) {
                 return splitHttp(output, data)
            }
            
            // Bypass gerektirmiyorsa direkt gönder
            sendDirect(output, data)
        } catch (e: Exception) {
            // Hata olursa normal göndermeyi dene
            try { sendDirect(output, data) } catch (e2: Exception) { false }
        }
    }
    
    private fun sendDirect(output: OutputStream, data: ByteArray): Boolean {
        output.write(data)
        output.flush()
        return true
    }
    
    // En önemli fonksiyon burası
    private fun sendTwoParts(output: OutputStream, data: ByteArray, splitAt: Int): Boolean {
        val pos = splitAt.coerceIn(1, data.size - 1)
        
        // 1. PARÇA (Genelde sadece ilk 1 byte)
        output.write(data, 0, pos)
        output.flush()
        
        // Gecikme (DPI kafası karışsın diye)
        if (settings.splitDelay > 0) {
            try { Thread.sleep(settings.splitDelay) } catch (e: Exception) {}
        }
        
        // 2. PARÇA (Geri kalanı)
        output.write(data, pos, data.size - pos)
        output.flush()
        
        return true
    }
    
    private fun splitHttp(output: OutputStream, data: ByteArray): Boolean {
        // HTTP için basit case manipulation
        var modifiedData = data
        if (settings.mixHostCase) {
             val httpStr = String(data, Charsets.ISO_8859_1)
             val hostIndex = httpStr.indexOf("Host:", ignoreCase = true)
             if (hostIndex > 0) {
                 modifiedData = manipulateHostCase(data.copyOf(), hostIndex)
             }
        }
        // HTTP'yi de 1. byte'tan bölmek genelde yeterlidir
        return sendTwoParts(output, modifiedData, 1)
    }
    
    private fun manipulateHostCase(data: ByteArray, hostIndex: Int): ByteArray {
        if (hostIndex + 4 <= data.size) {
            // Host -> hoSt
            data[hostIndex] = 'h'.code.toByte()
            data[hostIndex+1] = 'o'.code.toByte()
            data[hostIndex+2] = 'S'.code.toByte()
            data[hostIndex+3] = 't'.code.toByte()
        }
        return data
    }
    
    // Basit TLS kontrolü (Magic byte kontrolü)
    private fun isTlsClientHello(data: ByteArray): Boolean {
        if (data.size < 6) return false
        // 0x16 = Handshake, 0x01 = ClientHello
        return data[0] == 0x16.toByte() && data[5] == 0x01.toByte()
    }
}