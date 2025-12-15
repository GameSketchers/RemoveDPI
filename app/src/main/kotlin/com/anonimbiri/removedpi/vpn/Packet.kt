package com.anonimbiri.removedpi.vpn

import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Packet(buffer: ByteBuffer) {

    companion object {
        const val IP4_HEADER_SIZE = 20
        const val TCP_HEADER_SIZE = 20
        const val UDP_HEADER_SIZE = 8

        const val PROTOCOL_ICMP = 1
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17

        const val TCP_FIN = 0x01
        const val TCP_SYN = 0x02
        const val TCP_RST = 0x04
        const val TCP_PSH = 0x08
        const val TCP_ACK = 0x10
        const val TCP_URG = 0x20
    }

    private val rawData: ByteArray

    val version: Int
    val ipHeaderLength: Int
    val protocol: Int
    val totalLength: Int
    val identification: Int
    val ttl: Int
    val sourceAddress: InetAddress
    val destinationAddress: InetAddress

    val sourcePort: Int
    val destinationPort: Int

    val tcpHeaderLength: Int
    val tcpFlags: Int
    val sequenceNumber: Long
    val acknowledgmentNumber: Long
    val windowSize: Int

    init {
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.rewind()

        val length = buffer.remaining()
        rawData = ByteArray(length)
        buffer.get(rawData)
        buffer.rewind()

        version = (rawData[0].toInt() shr 4) and 0x0F
        ipHeaderLength = (rawData[0].toInt() and 0x0F) * 4
        totalLength = ((rawData[2].toInt() and 0xFF) shl 8) or (rawData[3].toInt() and 0xFF)
        identification = ((rawData[4].toInt() and 0xFF) shl 8) or (rawData[5].toInt() and 0xFF)
        ttl = rawData[8].toInt() and 0xFF
        protocol = rawData[9].toInt() and 0xFF

        val srcAddr = ByteArray(4)
        val dstAddr = ByteArray(4)
        System.arraycopy(rawData, 12, srcAddr, 0, 4)
        System.arraycopy(rawData, 16, dstAddr, 0, 4)
        sourceAddress = InetAddress.getByAddress(srcAddr)
        destinationAddress = InetAddress.getByAddress(dstAddr)

        if (rawData.size >= ipHeaderLength + 4) {
            sourcePort = ((rawData[ipHeaderLength].toInt() and 0xFF) shl 8) or 
                        (rawData[ipHeaderLength + 1].toInt() and 0xFF)
            destinationPort = ((rawData[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or 
                             (rawData[ipHeaderLength + 3].toInt() and 0xFF)
        } else {
            sourcePort = 0
            destinationPort = 0
        }

        if (protocol == PROTOCOL_TCP && rawData.size >= ipHeaderLength + 14) {
            tcpHeaderLength = ((rawData[ipHeaderLength + 12].toInt() shr 4) and 0x0F) * 4
            tcpFlags = rawData[ipHeaderLength + 13].toInt() and 0xFF

            sequenceNumber = (((rawData[ipHeaderLength + 4].toLong() and 0xFF) shl 24) or
                            ((rawData[ipHeaderLength + 5].toLong() and 0xFF) shl 16) or
                            ((rawData[ipHeaderLength + 6].toLong() and 0xFF) shl 8) or
                            (rawData[ipHeaderLength + 7].toLong() and 0xFF))

            acknowledgmentNumber = (((rawData[ipHeaderLength + 8].toLong() and 0xFF) shl 24) or
                                   ((rawData[ipHeaderLength + 9].toLong() and 0xFF) shl 16) or
                                   ((rawData[ipHeaderLength + 10].toLong() and 0xFF) shl 8) or
                                   (rawData[ipHeaderLength + 11].toLong() and 0xFF))

            windowSize = ((rawData[ipHeaderLength + 14].toInt() and 0xFF) shl 8) or
                        (rawData[ipHeaderLength + 15].toInt() and 0xFF)
        } else {
            tcpHeaderLength = 0
            tcpFlags = 0
            sequenceNumber = 0
            acknowledgmentNumber = 0
            windowSize = 0
        }
    }

    val isSyn: Boolean get() = (tcpFlags and TCP_SYN) != 0
    val isAck: Boolean get() = (tcpFlags and TCP_ACK) != 0
    val isFin: Boolean get() = (tcpFlags and TCP_FIN) != 0
    val isRst: Boolean get() = (tcpFlags and TCP_RST) != 0
    val isPsh: Boolean get() = (tcpFlags and TCP_PSH) != 0
    val isSynAck: Boolean get() = isSyn && isAck

    val payloadOffset: Int
        get() = when (protocol) {
            PROTOCOL_TCP -> ipHeaderLength + tcpHeaderLength
            PROTOCOL_UDP -> ipHeaderLength + UDP_HEADER_SIZE
            else -> ipHeaderLength
        }

    val payloadSize: Int
        get() = maxOf(0, totalLength - payloadOffset)

    val hasPayload: Boolean get() = payloadSize > 0

    fun getPayload(): ByteArray {
        if (payloadSize <= 0 || payloadOffset >= rawData.size) return ByteArray(0)
        val size = minOf(payloadSize, rawData.size - payloadOffset)
        return rawData.copyOfRange(payloadOffset, payloadOffset + size)
    }

    fun getRawData(): ByteArray = rawData.copyOf()

    val connectionKey: String
        get() = "${sourceAddress.hostAddress}:$sourcePort->${destinationAddress.hostAddress}:$destinationPort"

    val reverseConnectionKey: String
        get() = "${destinationAddress.hostAddress}:$destinationPort->${sourceAddress.hostAddress}:$sourcePort"

    val isHttp: Boolean
        get() {
            if (protocol != PROTOCOL_TCP || !hasPayload) return false
            val payload = getPayload()
            if (payload.size < 4) return false
            val start = String(payload.copyOfRange(0, minOf(8, payload.size)), Charsets.US_ASCII)
            return start.startsWith("GET ") || start.startsWith("POST ") ||
                   start.startsWith("HEAD ") || start.startsWith("PUT ") ||
                   start.startsWith("DELETE ") || start.startsWith("OPTIONS ") ||
                   start.startsWith("CONNECT ") || start.startsWith("PATCH ")
        }

    val isHttps: Boolean
        get() = destinationPort == 443

    val isTlsClientHello: Boolean
        get() {
            if (protocol != PROTOCOL_TCP || !hasPayload) return false
            val payload = getPayload()
            return payload.size > 5 &&
                   payload[0] == 0x16.toByte() && 
                   (payload[1] == 0x03.toByte()) && 
                   (payload[5] == 0x01.toByte())
        }

    val isDns: Boolean
        get() = protocol == PROTOCOL_UDP && (destinationPort == 53 || sourcePort == 53)
}

object PacketBuilder {
    fun buildTcpPacket(
        srcIp: InetAddress,
        dstIp: InetAddress,
        srcPort: Int,
        dstPort: Int,
        seqNum: Long,
        ackNum: Long,
        flags: Int,
        windowSize: Int = 65535,
        payload: ByteArray = ByteArray(0)
    ): ByteBuffer {
        val ipHeaderLen = 20
        val tcpHeaderLen = 20
        val totalLen = ipHeaderLen + tcpHeaderLen + payload.size

        val buffer = ByteBuffer.allocate(totalLen)
        buffer.order(ByteOrder.BIG_ENDIAN)

        buffer.put((0x45).toByte())
        buffer.put(0.toByte())
        buffer.putShort(totalLen.toShort())
        buffer.putShort(0.toShort())
        buffer.putShort(0x4000.toShort())
        buffer.put(64.toByte())
        buffer.put(Packet.PROTOCOL_TCP.toByte())
        buffer.putShort(0.toShort())
        buffer.put(srcIp.address)
        buffer.put(dstIp.address)

        buffer.putShort(srcPort.toShort())
        buffer.putShort(dstPort.toShort())
        buffer.putInt(seqNum.toInt())
        buffer.putInt(ackNum.toInt())
        buffer.put((0x50).toByte())
        buffer.put(flags.toByte())
        buffer.putShort(windowSize.toShort())
        buffer.putShort(0.toShort())
        buffer.putShort(0.toShort())

        if (payload.isNotEmpty()) {
            buffer.put(payload)
        }

        buffer.rewind()

        calculateIpChecksum(buffer)
        calculateTcpChecksum(buffer, srcIp, dstIp)

        buffer.rewind()
        return buffer
    }

    fun buildUdpPacket(
        srcIp: InetAddress,
        dstIp: InetAddress,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteBuffer {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val totalLen = ipHeaderLen + udpHeaderLen + payload.size

        val buffer = ByteBuffer.allocate(totalLen)
        buffer.order(ByteOrder.BIG_ENDIAN)

        buffer.put((0x45).toByte())
        buffer.put(0.toByte())
        buffer.putShort(totalLen.toShort())
        buffer.putShort(0.toShort())
        buffer.putShort(0x4000.toShort())
        buffer.put(64.toByte())
        buffer.put(Packet.PROTOCOL_UDP.toByte())
        buffer.putShort(0.toShort())
        buffer.put(srcIp.address)
        buffer.put(dstIp.address)

        buffer.putShort(srcPort.toShort())
        buffer.putShort(dstPort.toShort())
        buffer.putShort((udpHeaderLen + payload.size).toShort())
        buffer.putShort(0.toShort())

        buffer.put(payload)

        buffer.rewind()
        calculateIpChecksum(buffer)
        calculateUdpChecksum(buffer, srcIp, dstIp)
        buffer.rewind()

        return buffer
    }

    private fun calculateIpChecksum(buffer: ByteBuffer) {
        buffer.putShort(10, 0)

        var sum = 0L
        for (i in 0 until 20 step 2) {
            sum += (buffer.getShort(i).toLong() and 0xFFFF)
        }

        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        buffer.putShort(10, (sum.inv() and 0xFFFF).toShort())
    }

    private fun calculateTcpChecksum(buffer: ByteBuffer, srcIp: InetAddress, dstIp: InetAddress) {
        val ipHeaderLen = 20
        val tcpLength = buffer.capacity() - ipHeaderLen

        buffer.putShort(ipHeaderLen + 16, 0)

        var sum = 0L

        val srcBytes = srcIp.address
        val dstBytes = dstIp.address
        sum += ((srcBytes[0].toInt() and 0xFF) shl 8) or (srcBytes[1].toInt() and 0xFF)
        sum += ((srcBytes[2].toInt() and 0xFF) shl 8) or (srcBytes[3].toInt() and 0xFF)
        sum += ((dstBytes[0].toInt() and 0xFF) shl 8) or (dstBytes[1].toInt() and 0xFF)
        sum += ((dstBytes[2].toInt() and 0xFF) shl 8) or (dstBytes[3].toInt() and 0xFF)
        sum += Packet.PROTOCOL_TCP
        sum += tcpLength

        var i = ipHeaderLen
        while (i < buffer.capacity() - 1) {
            sum += buffer.getShort(i).toLong() and 0xFFFF
            i += 2
        }
        
        if (i < buffer.capacity()) {
            sum += (buffer.get(i).toLong() and 0xFF) shl 8
        }

        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        buffer.putShort(ipHeaderLen + 16, (sum.inv() and 0xFFFF).toShort())
    }

    private fun calculateUdpChecksum(buffer: ByteBuffer, srcIp: InetAddress, dstIp: InetAddress) {
        val ipHeaderLen = 20
        val udpLength = buffer.capacity() - ipHeaderLen

        buffer.putShort(ipHeaderLen + 6, 0)

        var sum = 0L

        val srcBytes = srcIp.address
        val dstBytes = dstIp.address
        sum += ((srcBytes[0].toInt() and 0xFF) shl 8) or (srcBytes[1].toInt() and 0xFF)
        sum += ((srcBytes[2].toInt() and 0xFF) shl 8) or (srcBytes[3].toInt() and 0xFF)
        sum += ((dstBytes[0].toInt() and 0xFF) shl 8) or (dstBytes[1].toInt() and 0xFF)
        sum += ((dstBytes[2].toInt() and 0xFF) shl 8) or (dstBytes[3].toInt() and 0xFF)
        sum += Packet.PROTOCOL_UDP
        sum += udpLength

        var i = ipHeaderLen
        while (i < buffer.capacity() - 1) {
            sum += buffer.getShort(i).toLong() and 0xFFFF
            i += 2
        }
        
        if (i < buffer.capacity()) {
            sum += (buffer.get(i).toLong() and 0xFF) shl 8
        }

        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        val checksum = (sum.inv() and 0xFFFF).toShort()
        buffer.putShort(ipHeaderLen + 6, if (checksum.toInt() == 0) 0xFFFF.toShort() else checksum)
    }
}