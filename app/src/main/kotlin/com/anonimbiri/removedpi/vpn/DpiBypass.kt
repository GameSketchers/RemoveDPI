package com.anonimbiri.removedpi.vpn

import android.content.Context
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.data.DesyncMethod
import com.anonimbiri.removedpi.data.DpiSettings
import java.io.OutputStream
import java.net.Socket
import kotlin.math.min

class DpiBypass(private val context: Context, private val settings: DpiSettings) {
    
    fun sendWithBypass(socket: Socket, data: ByteArray, isHttps: Boolean): Boolean {
        if (data.isEmpty()) return false
        val output = socket.getOutputStream()
        
        return try {
            val hostname = if (isHttps) extractSni(data) else extractHostHeader(data)
            
            if (hostname != null && isWhitelisted(hostname)) {
                LogManager.i(context.getString(R.string.log_whitelist_match, hostname))
                return sendDirect(output, data)
            }
            
            val shouldBypass = (isHttps && settings.desyncHttps && isTlsClientHello(data)) ||
                               (!isHttps && settings.desyncHttp)
            
            if (shouldBypass) {
                val protocol = if(isHttps) "HTTPS" else "HTTP"
                when (settings.desyncMethod) {
                    DesyncMethod.SPLIT -> {
                        LogManager.bypass(context.getString(R.string.log_bypass_split, protocol, hostname))
                        sendSplit(output, data)
                    }
                    DesyncMethod.DISORDER -> {
                        LogManager.bypass(context.getString(R.string.log_bypass_disorder, protocol, hostname))
                        sendShredded(output, data)
                    }
                    DesyncMethod.FAKE -> {
                        LogManager.bypass(context.getString(R.string.log_bypass_fake, protocol, hostname))
                        sendFakePackets(output, data, isHttps)
                    }
                    DesyncMethod.TTL -> {
                        LogManager.bypass(context.getString(R.string.log_bypass_ttl, protocol, hostname))
                        sendWithTtl(output, data)
                    }
                    DesyncMethod.FAKE_SPLIT -> {
                        LogManager.bypass(context.getString(R.string.log_bypass_fake_split, protocol, hostname))
                        sendFakeThenSplit(output, data, isHttps)
                    }
                }
            } else {
                sendDirect(output, data)
            }
        } catch (e: Exception) {
            LogManager.e(context.getString(R.string.log_error_bypass, e.message))
            try { 
                sendDirect(output, data) 
            } catch (e2: Exception) { 
                false 
            }
        }
    }

    private fun isWhitelisted(host: String): Boolean {
        val normalizedHost = host.lowercase().trim('.')
        return settings.whitelist.any { domain -> 
            val normalizedDomain = domain.lowercase()
            normalizedHost == normalizedDomain || normalizedHost.endsWith(".$normalizedDomain")
        }
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
    
    private fun sendFakePackets(output: OutputStream, data: ByteArray, isHttps: Boolean): Boolean {
        val fakePayload = if (isHttps) {
            createTlsFakePacket()
        } else {
            createHttpFakePacket()
        }
        
        repeat(settings.fakeCount) {
            try {
                output.write(fakePayload)
                output.flush()
                delay(1)
            } catch (e: Exception) {
            }
        }
        
        delay(5)
        return sendSplit(output, data)
    }
    
    private fun sendFakeThenSplit(output: OutputStream, data: ByteArray, isHttps: Boolean): Boolean {
        val fakePayload = if (isHttps) {
            createTlsFakePacket()
        } else {
            createHttpFakePacket()
        }
        
        repeat(settings.fakeCount.coerceIn(1, 5)) {
            try {
                output.write(fakePayload)
                output.flush()
            } catch (e: Exception) {
            }
        }
        
        delay(3)
        return sendSplit(output, data)
    }
    
    private fun sendWithTtl(output: OutputStream, data: ByteArray): Boolean {
        return sendSplit(output, data)
    }
    
    private fun createTlsFakePacket(): ByteArray {
        val hexStr = settings.fakeHex.replace("\\s".toRegex(), "")
        val fakeData = hexStringToByteArray(hexStr)
        
        return if (fakeData.isNotEmpty() && fakeData.size >= 3) {
            fakeData
        } else {
            byteArrayOf(
                0x16.toByte(), 0x03.toByte(), 0x01.toByte(),
                0x00.toByte(), 0x00.toByte()
            )
        }
    }
    
    private fun createHttpFakePacket(): ByteArray {
        return "GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n".toByteArray(Charsets.US_ASCII)
    }
    
    private fun delay(ms: Long = settings.splitDelay) {
        if (ms > 0) {
            try { 
                Thread.sleep(ms) 
            } catch (e: Exception) {}
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
            
            while (offset + 4 <= extensionsEnd && offset + 4 <= data.size) {
                val extType = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
                val extLen = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
                
                if (extLen < 0 || extLen > 10000) return null
                
                if (extType == 0x0000 && extLen > 5) {
                    val sniListStart = offset + 4
                    if (sniListStart + 5 <= data.size) {
                        val nameType = data[sniListStart + 2].toInt() and 0xFF
                        val nameLen = ((data[sniListStart + 3].toInt() and 0xFF) shl 8) or (data[sniListStart + 4].toInt() and 0xFF)
                        
                        if (nameType == 0 && nameLen > 0 && nameLen < 256) {
                            val hostnameOffset = sniListStart + 5
                            if (hostnameOffset + nameLen <= data.size) {
                                return String(data, hostnameOffset, nameLen, Charsets.US_ASCII)
                            }
                        }
                    }
                }
                offset += 4 + extLen
            }
        } catch (e: Exception) {
            LogManager.e(context.getString(R.string.log_error_sni, e.message))
        }
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
        val cleaned = s.replace("\\s".toRegex(), "")
        if (cleaned.length % 2 != 0) return ByteArray(0)
        
        val len = cleaned.length
        val data = ByteArray(len / 2)
        
        return try {
            var i = 0
            while (i < len) {
                val digit1 = Character.digit(cleaned[i], 16)
                val digit2 = Character.digit(cleaned[i + 1], 16)
                if (digit1 == -1 || digit2 == -1) return ByteArray(0)
                data[i / 2] = ((digit1 shl 4) + digit2).toByte()
                i += 2
            }
            data
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
}