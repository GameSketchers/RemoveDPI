package com.anonimbiri.removedpi.vpn

import android.net.VpnService
import com.anonimbiri.removedpi.R
import com.anonimbiri.removedpi.data.DpiSettings
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TcpConnection(
    private val vpnService: VpnService,
    private val vpnOutput: java.io.FileOutputStream,
    @Volatile private var settings: DpiSettings
) {
    companion object {
        private const val MAX_CONNECTIONS = 512
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 30000
        private const val WINDOW_SIZE = 65535
        private const val KEEP_ALIVE = true
    }

    private val sessions = ConcurrentHashMap<String, TcpSession>()
    private val executor: ExecutorService = Executors.newFixedThreadPool(MAX_CONNECTIONS)
    private val settingsLock = ReentrantLock()

    @Volatile private var isRunning = true
    private val bytesIn = AtomicLong(0)
    private val bytesOut = AtomicLong(0)

    enum class SessionState { SYN_RECEIVED, CONNECTING, ESTABLISHED, FIN_WAIT, CLOSED }

    data class TcpSession(
        val key: String,
        val srcIp: java.net.InetAddress,
        val srcPort: Int,
        val dstIp: java.net.InetAddress,
        val dstPort: Int,
        @Volatile var socket: Socket? = null,
        @Volatile var state: SessionState = SessionState.SYN_RECEIVED,
        var mySeqNum: Long = (System.nanoTime() and 0x7FFFFFFFL),
        var myAckNum: Long = 0,
        var theirSeqNum: Long = 0,
        var theirAckNum: Long = 0,
        @Volatile var firstDataSent: Boolean = false,
        val isHttps: Boolean = dstPort == 443,
        val pendingData: LinkedBlockingQueue<ByteArray> = LinkedBlockingQueue(50),
        var lastActivity: Long = System.currentTimeMillis(),
        val writeLock: ReentrantLock = ReentrantLock()
    )

    fun updateSettings(newSettings: DpiSettings) {
        settingsLock.withLock {
            settings = newSettings
        }
    }

    fun processPacket(packet: Packet) {
        if (!isRunning) return
        val key = packet.connectionKey
        try {
            when {
                packet.isSyn && !packet.isAck -> handleSyn(packet, key)
                packet.isAck && packet.hasPayload -> handleData(packet, key)
                packet.isAck && !packet.hasPayload -> handleAck(packet, key)
                packet.isFin -> handleFin(packet, key)
                packet.isRst -> handleRst(key)
            }
        } catch (e: Exception) {
            LogManager.e(vpnService.getString(R.string.log_tcp_error, key, e.message))
            closeSession(key)
        }
    }

    private fun handleSyn(packet: Packet, key: String) {
        val dest = "${packet.destinationAddress.hostAddress}:${packet.destinationPort}"
        LogManager.i(vpnService.getString(R.string.log_tcp_start, dest))

        sessions[key]?.let { if (it.state != SessionState.CLOSED) closeSession(key) }

        val session = TcpSession(
            key = key,
            srcIp = packet.sourceAddress,
            srcPort = packet.sourcePort,
            dstIp = packet.destinationAddress,
            dstPort = packet.destinationPort,
            theirSeqNum = packet.sequenceNumber
        )
        sessions[key] = session
        session.myAckNum = packet.sequenceNumber + 1
        sendTcpPacket(session, Packet.TCP_SYN or Packet.TCP_ACK)
        session.mySeqNum++
        session.state = SessionState.CONNECTING
        executor.submit { connectToServer(session) }
    }

    private fun connectToServer(session: TcpSession) {
        val dest = "${session.dstIp.hostAddress}:${session.dstPort}"
        var socket: Socket? = null
        try {
            socket = Socket()
            vpnService.protect(socket)

            val tcpNoDelayEnabled = settingsLock.withLock { settings.enableTcpNodelay }
            socket.tcpNoDelay = tcpNoDelayEnabled
            socket.soTimeout = READ_TIMEOUT
            socket.keepAlive = KEEP_ALIVE
            socket.setSoLinger(true, 0)

            socket.connect(InetSocketAddress(session.dstIp, session.dstPort), CONNECT_TIMEOUT)

            if (!isRunning || session.state == SessionState.CLOSED) {
                socket.close()
                return
            }

            session.socket = socket
            session.state = SessionState.ESTABLISHED

            LogManager.i(vpnService.getString(R.string.log_conn_established, dest))

            processPendingData(session)
            readFromServer(session)
        } catch (e: Exception) {
            LogManager.e(vpnService.getString(R.string.log_conn_failed, dest, e.message))
            socket?.close()
            sendRst(session)
            closeSession(session.key)
        }
    }

    private fun processPendingData(session: TcpSession) {
        session.writeLock.withLock {
            while (!session.pendingData.isEmpty() && session.state == SessionState.ESTABLISHED) {
                val data = session.pendingData.poll(100, TimeUnit.MILLISECONDS) ?: break
                sendToServerInternal(session, data)
            }
        }
    }

    private fun readFromServer(session: TcpSession) {
        val socket = session.socket ?: return
        val currentBufferSize = settingsLock.withLock { settings.bufferSize }
        val buffer = ByteArray(currentBufferSize)
        try {
            val input = socket.getInputStream()
            while (isRunning && session.state == SessionState.ESTABLISHED && !socket.isClosed) {
                val bytesRead = input.read(buffer)
                if (bytesRead > 0) {
                    bytesIn.addAndGet(bytesRead.toLong())
                    session.lastActivity = System.currentTimeMillis()
                    sendToClient(session, buffer.copyOf(bytesRead))
                } else if (bytesRead == -1) {
                    LogManager.i(vpnService.getString(R.string.log_server_closed, session.dstIp.hostAddress))
                    sendFin(session)
                    break
                }
            }
        } catch (e: IOException) {
            if (session.state != SessionState.CLOSED && isRunning) {
                LogManager.e(vpnService.getString(R.string.log_read_error, session.dstIp.hostAddress, e.message))
            }
        } finally { 
            closeSession(session.key) 
        }
    }

    private fun handleData(packet: Packet, key: String) {
        val session = sessions[key] ?: return
        val payload = packet.getPayload()
        if (payload.isEmpty()) return

        session.lastActivity = System.currentTimeMillis()
        session.theirSeqNum = packet.sequenceNumber
        session.myAckNum = packet.sequenceNumber + payload.size
        sendTcpPacket(session, Packet.TCP_ACK)

        if (session.state == SessionState.ESTABLISHED) {
            executor.submit { sendToServer(session, payload) }
        } else if (session.state == SessionState.CONNECTING || session.state == SessionState.SYN_RECEIVED) {
            if (!session.pendingData.offer(payload.copyOf())) {
                LogManager.w(vpnService.getString(R.string.log_queue_full, session.key))
            }
        }
    }

    private fun sendToServer(session: TcpSession, data: ByteArray) {
        session.writeLock.withLock {
            sendToServerInternal(session, data)
        }
    }

    private fun sendToServerInternal(session: TcpSession, data: ByteArray) {
        val socket = session.socket ?: return
        if (socket.isClosed) return

        try {
            if (!session.firstDataSent) {
                session.firstDataSent = true
                
                val currentSettings = DpiSettings()
                settingsLock.withLock {
                    currentSettings
                }.let { 
                    // DpiBypass sınıfına context (vpnService) eklendi
                    val bypass = DpiBypass(vpnService, settings)
                    bypass.sendWithBypass(socket, data, session.isHttps)
                }
            } else {
                socket.getOutputStream().apply {
                    write(data)
                    flush()
                }
            }
            bytesOut.addAndGet(data.size.toLong())
            session.lastActivity = System.currentTimeMillis()
        } catch (e: IOException) { 
            LogManager.e(vpnService.getString(R.string.log_send_error, e.message))
            closeSession(session.key) 
        }
    }

    private fun handleAck(packet: Packet, key: String) {
        val session = sessions[key] ?: return
        session.theirAckNum = packet.acknowledgmentNumber
        session.lastActivity = System.currentTimeMillis()
    }

    private fun handleFin(packet: Packet, key: String) {
        val session = sessions[key] ?: return
        LogManager.i(vpnService.getString(R.string.log_client_fin, session.dstIp.hostAddress))
        session.myAckNum = packet.sequenceNumber + 1
        session.state = SessionState.FIN_WAIT
        sendTcpPacket(session, Packet.TCP_FIN or Packet.TCP_ACK)
        session.mySeqNum++
        closeSession(key)
    }

    private fun handleRst(key: String) {
        closeSession(key)
    }

    private fun sendToClient(session: TcpSession, data: ByteArray) {
        if (session.state == SessionState.CLOSED) return
        var offset = 0
        val mss = 1400
        synchronized(vpnOutput) {
            while (offset < data.size) {
                val chunkSize = minOf(mss, data.size - offset)
                val packet = PacketBuilder.buildTcpPacket(
                    srcIp = session.dstIp, dstIp = session.srcIp,
                    srcPort = session.dstPort, dstPort = session.srcPort,
                    seqNum = session.mySeqNum, ackNum = session.myAckNum,
                    flags = Packet.TCP_PSH or Packet.TCP_ACK,
                    windowSize = WINDOW_SIZE,
                    payload = data.copyOfRange(offset, offset + chunkSize)
                )
                writeToVpn(packet)
                session.mySeqNum += chunkSize
                offset += chunkSize
            }
        }
    }

    private fun sendTcpPacket(session: TcpSession, flags: Int) {
        val packet = PacketBuilder.buildTcpPacket(
            srcIp = session.dstIp, dstIp = session.srcIp,
            srcPort = session.dstPort, dstPort = session.srcPort,
            seqNum = session.mySeqNum, ackNum = session.myAckNum,
            flags = flags, 
            windowSize = WINDOW_SIZE,
            payload = ByteArray(0)
        )
        writeToVpn(packet)
    }

    private fun sendFin(session: TcpSession) {
        if (session.state == SessionState.CLOSED) return
        session.state = SessionState.FIN_WAIT
        sendTcpPacket(session, Packet.TCP_FIN or Packet.TCP_ACK)
        session.mySeqNum++
    }

    private fun sendRst(session: TcpSession) = sendTcpPacket(session, Packet.TCP_RST or Packet.TCP_ACK)

    private fun writeToVpn(packet: ByteBuffer) {
        try {
            val data = ByteArray(packet.remaining())
            packet.get(data)
            synchronized(vpnOutput) { 
                vpnOutput.write(data)
                vpnOutput.flush() 
            }
        } catch (e: Exception) {
            LogManager.e(vpnService.getString(R.string.log_vpn_write_error, e.message))
        }
    }

    private fun closeSession(key: String) {
        sessions.remove(key)?.let { session ->
            session.state = SessionState.CLOSED
            session.pendingData.clear()
            try { 
                session.socket?.close() 
            } catch (e: Exception) {}
            session.socket = null
        }
    }

    fun getStats(): Pair<Long, Long> = bytesIn.get() to bytesOut.get()

    fun stop() {
        isRunning = false
        sessions.keys.toList().forEach { closeSession(it) }
        executor.shutdownNow()
    }
}