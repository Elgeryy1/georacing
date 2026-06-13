package com.georacing.georacing.data.ble

import com.georacing.georacing.domain.model.CircuitMode
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BlePayloadParser {

    // Circuit (v1) payload: 8 bytes, or 9 with trailing temperature.
    // Staff danger/evacuation (v2) payload: 13 bytes (adds a 4-byte source id).
    private const val V1_MIN_SIZE = 8
    private const val V1_MAX_SIZE = 9
    private const val V2_SIZE = 13
    private const val MANUFACTURER_ID = 0x1234 // Test ID

    fun parse(manufacturerId: Int, bytes: ByteArray?): BleCircuitSignal? {
        if (manufacturerId != MANUFACTURER_ID || bytes == null || bytes.isEmpty()) {
            return null
        }
        // Validate the size against the version in the first byte so the staff
        // v2 broadcast (13 bytes) is not silently dropped, while still rejecting
        // unrelated user beacons.
        val declaredVersion = bytes[0].toInt() and 0xFF
        val sizeOk = when (declaredVersion) {
            1 -> bytes.size in V1_MIN_SIZE..V1_MAX_SIZE
            2 -> bytes.size == V2_SIZE
            else -> false
        }
        if (!sizeOk) {
            return null
        }

        return try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

            val version = buffer.get().toInt() and 0xFF
            var sourceId: Int? = null
            
            // Version 2 (Staff) has ID right after version
            if (version == 2 && buffer.remaining() >= 12) {
                 sourceId = buffer.int
            }

            val zoneId = buffer.short.toInt() and 0xFFFF
            val modeByte = buffer.get().toInt()
            val flags = buffer.get().toInt() and 0xFF
            val sequence = buffer.short.toInt() and 0xFFFF
            val ttl = buffer.get().toInt() and 0xFF
            
            var temp: Int? = null
            // For V1 temp is at end. For V2 we are already larger.
            if (version == 1 && buffer.hasRemaining()) {
                 temp = buffer.get().toInt()
            }

            BleCircuitSignal(
                version = version,
                zoneId = zoneId,
                mode = mapByteToMode(modeByte),
                flags = flags,
                sequence = sequence,
                ttlSeconds = ttl,
                temperature = temp,
                sourceId = sourceId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mapByteToMode(byte: Int): CircuitMode {
        return when (byte) {
            0 -> CircuitMode.NORMAL
            1 -> CircuitMode.SAFETY_CAR // Using SAFETY_CAR as generic "Congestion/Warning" if needed, or update Enum
            2 -> CircuitMode.RED_FLAG
            3 -> CircuitMode.EVACUATION // Correctly mapped now
            else -> CircuitMode.UNKNOWN
        }
    }
}
