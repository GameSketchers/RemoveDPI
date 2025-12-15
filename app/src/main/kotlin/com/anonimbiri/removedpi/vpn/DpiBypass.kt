package com.anonimbiri.removedpi.vpn

import android.content.Context
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.data.DesyncMethod
import com.anonimbiri.removedpi.data.DpiSettings
import com.anonimbiri.removedpi.data.FakePacketMode
import java.io.FileDescriptor
import java.net.Socket
import java.nio.ByteBuffer
import java.security.SecureRandom
import kotlin.math.floor

class DpiBypass(
    private val context: Context,
    private val settings: DpiSettings
) {

    companion object {
        private val random = SecureRandom()
        
        // Firefox benzeri gerçekçi bir ClientHello paketi üretir
        private fun generateFakeClientHello(): ByteArray {
            val sessionId = ByteArray(32).apply { random.nextBytes(this) }
            val randomBytes = ByteArray(32).apply { random.nextBytes(this) }
            
            // DPI'ı yanıltmak için zararsız bir domain
            val sniHost = "google.com".toByteArray(Charsets.US_ASCII)
            val sniLen = sniHost.size
            
            val buffer = ByteBuffer.allocate(512)
            
            // TLS Record Header
            buffer.put(0x16.toByte())
            buffer.put(0x03.toByte())
            buffer.put(0x01.toByte()) 
            
            val recordLenPos = buffer.position()
            buffer.putShort(0) 

            // Handshake Header
            val handshakeStart = buffer.position()
            buffer.put(0x01.toByte())
            
            val handshakeLenPos = buffer.position()
            buffer.put(0x00.toByte())
            buffer.putShort(0)

            buffer.put(0x03.toByte()) // TLS 1.2
            buffer.put(0x03.toByte())

            buffer.put(randomBytes)
            
            buffer.put(32.toByte())
            buffer.put(sessionId)
            
            // Cipher Suites
            buffer.putShort(0x0022.toShort())
            val ciphers = shortArrayOf(
                0x1301, 0x1303, 0x1302,
                0xC02B.toShort(), 0xC02F.toShort(), 0xCCA9.toShort(), 0xCCA8.toShort(),
                0xC02C.toShort(), 0xC030.toShort(), 0xC00A.toShort(), 0xC009.toShort(),
                0xC013.toShort(), 0xC014.toShort(), 0x009C, 0x009D, 0x002F, 0x0035
            )
            for (cipher in ciphers) buffer.putShort(cipher)

            buffer.put(0x01.toByte())
            buffer.put(0x00.toByte())

            // Extensions
            val extLenPos = buffer.position()
            buffer.putShort(0)
            val extStart = buffer.position()

            // SNI
            buffer.putShort(0x0000.toShort())
            buffer.putShort((sniLen + 5).toShort())
            buffer.putShort((sniLen + 3).toShort())
            buffer.put(0x00.toByte())
            buffer.putShort(sniLen.toShort())
            buffer.put(sniHost)

            // Padding (Paket boyutunu rastgeleleştir)
            val paddingLen = random.nextInt(100) + 10
            buffer.putShort(0x0015.toShort())
            buffer.putShort(paddingLen.toShort())
            for (i in 0 until paddingLen) buffer.put(0x00.toByte())

            val extEnd = buffer.position()
            val extLen = extEnd - extStart
            buffer.putShort(extLenPos, extLen.toShort())

            val handshakeLen = extEnd - handshakeStart - 4
            buffer.put(handshakeLenPos, ((handshakeLen shr 16) and 0xFF).toByte())
            buffer.putShort(handshakeLenPos + 1, (handshakeLen and 0xFFFF).toShort())

            val recordLen = extEnd - recordLenPos - 2
            buffer.putShort(recordLenPos, recordLen.toShort())

            val result = ByteArray(buffer.position())
            buffer.rewind()
            buffer.get(result)
            return result
        }
    }

    fun sendWithBypass(socket: Socket, data: ByteArray, isHttps: Boolean) {
        val output = socket.getOutputStream()
        var processedData = data

        if (!isHttps && settings.mixHostCase) {
            processedData = mixHostCase(data)
        }

        when {
            isHttps && settings.desyncHttps -> {
                applyBypassMethod(socket, output, processedData)
            }
            !isHttps && settings.desyncHttp -> {
                applyBypassMethod(socket, output, processedData)
            }
            else -> {
                output.write(processedData)
                output.flush()
            }
        }
    }

    private fun applyBypassMethod(socket: Socket, output: java.io.OutputStream, data: ByteArray) {
        val ttlToUse = if (settings.autoTtl) {
            calculateAutoTtl(getSocketTtl(socket))
        } else {
            settings.ttlValue
        }

        if (settings.fakePacketMode != FakePacketMode.NONE) {
            sendFakePackets(socket, settings.fakePacketMode, ttlToUse)
        }

        when (settings.desyncMethod) {
            DesyncMethod.SPLIT -> applySplit(output, data)
            DesyncMethod.DISORDER -> applyDisorder(output, data)
            DesyncMethod.FRAG_BY_SNI -> applyFragBySni(output, data)
            DesyncMethod.TTL -> applyTtl(socket, output, data, ttlToUse)
            DesyncMethod.SNI_SPLIT -> applySniSplit(output, data)
        }
    }

    private fun mixHostCase(data: ByteArray): ByteArray {
        val str = String(data)
        val hostIndex = str.indexOf("\r\nHost: ", ignoreCase = true)
        if (hostIndex != -1) {
            val start = hostIndex + 8 
            val end = str.indexOf("\r\n", start)
            if (end != -1) {
                val hostVal = str.substring(start, end)
                val mixedHost = StringBuilder()
                for ((i, char) in hostVal.withIndex()) {
                    if (i % 2 == 1) mixedHost.append(char.uppercaseChar())
                    else mixedHost.append(char)
                }
                return (str.substring(0, start) + mixedHost.toString() + str.substring(end)).toByteArray()
            }
        }
        return data
    }

    private fun sendFakePackets(socket: Socket, mode: FakePacketMode, fakeTtl: Int) {
        try {
            val originalTtl = getSocketTtl(socket)
            
            val fakePayload = generateFakeClientHello()

            when (mode) {
                FakePacketMode.WRONG_SEQ, FakePacketMode.WRONG_CHKSUM, FakePacketMode.BOTH -> {
                    if (setSocketTtl(socket, fakeTtl)) {
                        socket.getOutputStream().write(fakePayload)
                        socket.getOutputStream().flush()
                        
                        if (mode == FakePacketMode.BOTH) {
                            Thread.sleep(1)
                            socket.getOutputStream().write(fakePayload)
                            socket.getOutputStream().flush()
                        }
                        
                        setSocketTtl(socket, originalTtl)
                    } else {
                        LogManager.w(context.getString(R.string.log_ttl_fake_cancel))
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            LogManager.w(context.getString(R.string.log_fake_send_error, e.message))
        }
    }

    private fun calculateAutoTtl(currentTtl: Int): Int {
        var nhops = 0
        if (currentTtl > 98 && currentTtl < 128) {
            nhops = 128 - currentTtl
        } else if (currentTtl > 34 && currentTtl < 64) {
            nhops = 64 - currentTtl
        } else {
            return settings.ttlValue
        }

        if (nhops <= settings.ttlValue || nhops < settings.minTtl) {
            return settings.ttlValue
        }

        var fakeTtl = nhops - settings.ttlValue 
        if (fakeTtl < settings.ttlValue) {
             fakeTtl = settings.ttlValue
        }
        
        return fakeTtl
    }

    private fun applySplit(output: java.io.OutputStream, data: ByteArray) {
        val splitPos = minOf(settings.firstPacketSize, data.size)
        val part1 = data.copyOfRange(0, splitPos)
        val part2 = data.copyOfRange(splitPos, data.size)

        output.write(part1)
        output.flush()

        if (settings.splitDelay > 0) {
            Thread.sleep(settings.splitDelay)
        }

        output.write(part2)
        output.flush()

        LogManager.i(context.getString(R.string.log_bypass_split, "TCP", splitPos))
    }

    private fun applyDisorder(output: java.io.OutputStream, data: ByteArray) {
        if (data.size < settings.splitCount) {
             output.write(data)
             output.flush()
             return
        }

        val chunkSize = data.size / settings.splitCount
        val chunks = mutableListOf<ByteArray>()

        for (i in 0 until settings.splitCount) {
            val start = i * chunkSize
            val end = if (i == settings.splitCount - 1) data.size else (i + 1) * chunkSize
            chunks.add(data.copyOfRange(start, end))
        }

        chunks.shuffle()

        chunks.forEach { chunk ->
            output.write(chunk)
            output.flush()
            if (settings.splitDelay > 0) {
                Thread.sleep(settings.splitDelay)
            }
        }

        LogManager.i(context.getString(R.string.log_bypass_disorder, "TCP", settings.splitCount))
    }

    private fun applyFragBySni(output: java.io.OutputStream, data: ByteArray) {
        val sniPos = findSniPosition(data)
        
        if (sniPos > 0) {
            val part1 = data.copyOfRange(0, sniPos)
            val part2 = data.copyOfRange(sniPos, data.size)

            output.write(part1)
            output.flush()

            if (settings.splitDelay > 0) {
                Thread.sleep(settings.splitDelay)
            }

            output.write(part2)
            output.flush()

            LogManager.i(context.getString(R.string.log_bypass_sni, "TCP", sniPos))
        } else {
            output.write(data)
            output.flush()
        }
    }

    private fun applyTtl(socket: Socket, output: java.io.OutputStream, data: ByteArray, ttl: Int) {
        try {
            val originalTtl = getSocketTtl(socket)
            if (setSocketTtl(socket, ttl)) {
                output.write(data)
                output.flush()
                setSocketTtl(socket, originalTtl)
                LogManager.i(context.getString(R.string.log_bypass_ttl, "TCP", ttl))
            } else {
                output.write(data)
                output.flush()
            }
        } catch (e: Exception) {
            LogManager.e(context.getString(R.string.log_ttl_error, e.message))
            output.write(data)
            output.flush()
        }
    }

    private fun applySniSplit(output: java.io.OutputStream, data: ByteArray) {
        val sniPos = findSniPosition(data)
        val splitPos = if (sniPos > 0) sniPos else minOf(settings.firstPacketSize, data.size)

        val part1 = data.copyOfRange(0, splitPos)
        val part2 = data.copyOfRange(splitPos, data.size)

        output.write(part1)
        output.flush()

        if (settings.splitDelay > 0) {
            Thread.sleep(settings.splitDelay)
        }

        output.write(part2)
        output.flush()

        LogManager.i(context.getString(R.string.log_bypass_sni_split, "TCP", splitPos))
    }

    private fun findSniPosition(data: ByteArray): Int {
        if (data.size < 43) return -1

        var ptr = 0
        while (ptr + 8 < data.size) {
            if (data[ptr] == 0x00.toByte() && data[ptr + 1] == 0x00.toByte() && 
                data[ptr + 2] == 0x00.toByte() && data[ptr + 4] == 0x00.toByte() && 
                data[ptr + 6] == 0x00.toByte() && data[ptr + 7] == 0x00.toByte()) {
                
                val extLen = (data[ptr + 3].toInt() and 0xFF)
                val listLen = (data[ptr + 5].toInt() and 0xFF)
                val nameLen = (data[ptr + 8].toInt() and 0xFF)

                if (extLen - listLen == 2 && listLen - nameLen == 3) {
                    if (ptr + 9 + nameLen <= data.size) {
                        return ptr + 9
                    }
                }
            }
            ptr++
        }
        return -1
    }

    private fun setSocketTtl(socket: Socket, ttl: Int): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val fd = getFileDescriptor(socket)
                if (fd != null) {
                    Os.setsockoptInt(fd, OsConstants.IPPROTO_IP, OsConstants.IP_TTL, ttl)
                    true
                } else false
            } else false
        } catch (e: Exception) {
            LogManager.w(context.getString(R.string.log_ttl_error, e.message))
            false
        }
    }

    private fun getSocketTtl(socket: Socket): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val fd = getFileDescriptor(socket)
                if (fd != null) {
                    val method = Os::class.java.getMethod(
                        "getsockoptInt",
                        FileDescriptor::class.java,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                    method.invoke(null, fd, OsConstants.IPPROTO_IP, OsConstants.IP_TTL) as? Int ?: 64
                } else {
                    64
                }
            } else {
                64
            }
        } catch (e: Exception) {
            64
        }
    }

    private fun getFileDescriptor(socket: Socket): FileDescriptor? {
        return try {
            val implField = socket.javaClass.getDeclaredField("impl")
            implField.isAccessible = true
            val impl = implField.get(socket)
            
            val fdField = impl.javaClass.getDeclaredField("fd")
            fdField.isAccessible = true
            fdField.get(impl) as? FileDescriptor
        } catch (e: Exception) {
            null
        }
    }
}