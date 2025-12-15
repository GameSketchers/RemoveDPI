package com.anonimbiri.removedpi.vpn

import android.net.VpnService
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.data.DpiSettings
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class UdpConnection(
    private val vpnService: VpnService,
    private val vpnOutput: java.io.FileOutputStream,
    @Volatile private var settings: DpiSettings
) {
    companion object {
        private const val TIMEOUT = 30000
        private const val MAX_PACKET_SIZE = 65535
    }

    private val sessions = ConcurrentHashMap<String, UdpSession>()
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val bytesIn = AtomicLong(0)
    private val bytesOut = AtomicLong(0)
    private val settingsLock = ReentrantLock()

    @Volatile
    private var isRunning = true

    data class UdpSession(
        val key: String,
        val socket: DatagramSocket,
        val srcIp: InetAddress,
        val srcPort: Int,
        val dstIp: InetAddress,
        val dstPort: Int,
        var lastActivity: Long = System.currentTimeMillis()
    )

    fun updateSettings(newSettings: DpiSettings) {
        settingsLock.withLock {
            settings = newSettings
        }
    }

    fun processPacket(packet: Packet) {
        val payload = packet.getPayload()
        if (payload.isEmpty()) return

        val shouldBlockQuic = settingsLock.withLock { settings.blockQuic }
        
        if (shouldBlockQuic && packet.destinationPort == 443) {
            if (isQuicPacket(payload)) {
                LogManager.w(vpnService.getString(R.string.log_quic_blocked, packet.destinationAddress.hostAddress))
                return
            }
        }

        try {
            val key = packet.connectionKey
            val session = sessions.getOrPut(key) { createSession(packet) }

            session.lastActivity = System.currentTimeMillis()

            if (packet.isDns) {
                LogManager.i(vpnService.getString(R.string.log_dns_query, packet.destinationAddress.hostAddress))
            }

            val destPacket = DatagramPacket(payload, payload.size, packet.destinationAddress, packet.destinationPort)
            session.socket.send(destPacket)
            bytesOut.addAndGet(payload.size.toLong())

        } catch (e: Exception) {
            LogManager.e(vpnService.getString(R.string.log_udp_error, e.message))
        }
    }

    private fun isQuicPacket(payload: ByteArray): Boolean {
        if (payload.size < 1200) return false
        val firstByte = payload[0].toInt() and 0xFF
        if ((firstByte and 0xC0) == 0xC0 && payload.size > 1 && payload[1].toInt() == 0x01) {
            return true
        }
        return false
    }

    private fun createSession(packet: Packet): UdpSession {
        val socket = DatagramSocket()
        vpnService.protect(socket)
        socket.soTimeout = TIMEOUT

        val session = UdpSession(
            key = packet.connectionKey,
            socket = socket,
            srcIp = packet.sourceAddress,
            srcPort = packet.sourcePort,
            dstIp = packet.destinationAddress,
            dstPort = packet.destinationPort
        )
        startResponseListener(session)
        return session
    }

    private fun startResponseListener(session: UdpSession) {
        executor.submit {
            val buffer = ByteArray(MAX_PACKET_SIZE)
            val receivePacket = DatagramPacket(buffer, buffer.size)

            while (isRunning && sessions.containsKey(session.key)) {
                try {
                    session.socket.receive(receivePacket)
                    val data = receivePacket.data.copyOf(receivePacket.length)
                    bytesIn.addAndGet(data.size.toLong())
                    sendToClient(session, data)
                    session.lastActivity = System.currentTimeMillis()
                } catch (e: Exception) {
                    if (isRunning && sessions.containsKey(session.key)) {
                        val timeDiff = System.currentTimeMillis() - session.lastActivity
                        if (timeDiff > TIMEOUT) {
                            closeSession(session.key)
                            break
                        }
                    } else { 
                        break 
                    }
                }
            }
        }
    }

    private fun sendToClient(session: UdpSession, data: ByteArray) {
        val packet = PacketBuilder.buildUdpPacket(
            srcIp = session.dstIp,
            dstIp = session.srcIp,
            srcPort = session.dstPort,
            dstPort = session.srcPort,
            payload = data
        )
        synchronized(vpnOutput) {
            try {
                val packetData = ByteArray(packet.remaining())
                packet.get(packetData)
                vpnOutput.write(packetData)
                vpnOutput.flush()
            } catch (e: Exception) {
                LogManager.e(vpnService.getString(R.string.log_vpn_write_error_udp, e.message))
            }
        }
    }

    private fun closeSession(key: String) {
        sessions.remove(key)?.let { 
            try { 
                it.socket.close() 
            } catch (e: Exception) {} 
        }
    }

    fun getStats(): Pair<Long, Long> = bytesIn.get() to bytesOut.get()

    fun stop() {
        isRunning = false
        executor.shutdownNow()
        sessions.keys.toList().forEach { closeSession(it) }
    }
}