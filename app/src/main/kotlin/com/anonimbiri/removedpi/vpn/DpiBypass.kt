package com.anonimbiri.removedpi.vpn

import com.anonimbiri.removedpi.data.DesyncMethod
import com.anonimbiri.removedpi.data.DpiSettings
import java.io.OutputStream
import java.net.Socket
import kotlin.math.min

class DpiBypass(private val settings: DpiSettings) {
    
    fun sendWithBypass(socket: Socket, data: ByteArray, isHttps: Boolean): Boolean {
        if (data.isEmpty()) return false
        val output = socket.getOutputStream()
        
        return try {
            // Whitelist kontrolü
            val hostname = if (isHttps) extractSni(data) else extractHostHeader(data)
            if (hostname != null && isWhitelisted(hostname)) {
                return sendDirect(output, data)
            }
            
            val shouldBypass = (isHttps && settings.desyncHttps && isTlsClientHello(data)) ||
                               (!isHttps && settings.desyncHttp)
            
            if (shouldBypass) {
                when (settings.desyncMethod) {
                    DesyncMethod.SPLIT -> sendSplit(output, data)
                    DesyncMethod.DISORDER -> sendShredded(output, data)
                    DesyncMethod.FAKE -> sendFake(output, data)
                }
            } else {
                sendDirect(output, data)
            }
        } catch (e: Exception) {
            try { sendDirect(output, data) } catch (e2: Exception) { false }
        }
    }

    private fun isWhitelisted(host: String): Boolean {
        return settings.whitelist.any { domain -> host.contains(domain, ignoreCase = true) }
    }
    
    private fun sendDirect(output: OutputStream, data: ByteArray): Boolean {
        output.write(data)
        output.flush()
        return true
    }
    
    private fun sendSplit(output: OutputStream, data: ByteArray): Boolean {
        val splitPos = settings.firstPacketSize.coerceIn(1, data.size - 1)
        output.write(data, 0, splitPos)
        output.flush()
        delay()
        output.write(data, splitPos, data.size - splitPos)
        output.flush()
        return true
    }
    
    private fun sendShredded(output: OutputStream, data: ByteArray): Boolean {
        val count = settings.splitCount.coerceIn(2, 20)
        val chunkSize = (data.size / count).coerceAtLeast(1)
        var offset = 0
        while (offset < data.size) {
            val len = min(chunkSize, data.size - offset)
            output.write(data, offset, len)
            output.flush()
            delay()
            offset += len
        }
        return true
    }
    
    private fun sendFake(output: OutputStream, data: ByteArray): Boolean {
        try {
            val fakeData = hexStringToByteArray(settings.fakeHex)
            if (fakeData.isNotEmpty()) {
                output.write(fakeData)
                output.flush()
                delay()
            }
        } catch (e: Exception) {}
        return sendSplit(output, data)
    }
    
    private fun delay() {
        if (settings.splitDelay > 0) {
            try { Thread.sleep(settings.splitDelay) } catch (e: Exception) {}
        }
    }
    
    private fun isTlsClientHello(data: ByteArray): Boolean {
        if (data.size < 6) return false
        return data[0] == 0x16.toByte() && data[5] == 0x01.toByte()
    }
    
    private fun extractSni(data: ByteArray): String? {
        try {
            if (data.size < 43) return null
            var offset = 43
            if (offset >= data.size) return null
            val sessionIdLen = data[offset].toInt() and 0xFF
            offset += 1 + sessionIdLen
            if (offset + 2 > data.size) return null
            val cipherSuitesLen = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            offset += 2 + cipherSuitesLen
            if (offset >= data.size) return null
            val compressionLen = data[offset].toInt() and 0xFF
            offset += 1 + compressionLen
            if (offset + 2 > data.size) return null
            val extensionsLen = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            offset += 2
            val extensionsEnd = offset + extensionsLen
            while (offset + 4 < extensionsEnd && offset + 4 < data.size) {
                val extType = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
                val extLen = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
                if (extType == 0x0000 && extLen > 5) {
                    val sniListStart = offset + 4
                    if (sniListStart + 5 < data.size) {
                        val nameType = data[sniListStart + 2].toInt() and 0xFF
                        val nameLen = ((data[sniListStart + 3].toInt() and 0xFF) shl 8) or (data[sniListStart + 4].toInt() and 0xFF)
                        if (nameType == 0 && nameLen > 0) {
                             val hostnameOffset = sniListStart + 5
                             if (hostnameOffset + nameLen <= data.size) {
                                 return String(data, hostnameOffset, nameLen, Charsets.US_ASCII)
                             }
                        }
                    }
                }
                offset += 4 + extLen
            }
        } catch (e: Exception) {}
        return null
    }
    
    private fun extractHostHeader(data: ByteArray): String? {
        try {
            val httpStr = String(data, Charsets.ISO_8859_1)
            val lines = httpStr.split("\r\n", "\n")
            for (line in lines) {
                if (line.startsWith("Host:", ignoreCase = true)) {
                    return line.substring(5).trim()
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}