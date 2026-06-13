package com.georacing.georacing.data.ble

import com.georacing.georacing.domain.model.CircuitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Unit tests for [BlePayloadParser].
 *
 * The parser decodes the GeoRacing circuit beacon advertisement payload. Two wire
 * formats are supported and are exercised here byte-for-byte:
 *
 *  - v1 (circuit flag): 8 bytes, or 9 bytes when a trailing temperature is present.
 *      [version=1][zoneId u16][mode u8][flags u8][sequence u16][ttl u8]( [temp i8] )
 *  - v2 (staff danger/evacuation): 13 bytes, inserting a 4-byte source id after the
 *      version byte.
 *      [version=2][sourceId u32][zoneId u16][mode u8][flags u8][sequence u16][ttl u8][reserved]
 *
 * All multi-byte fields are big-endian.
 */
class BlePayloadParserTest {

    private val manufacturerId = 0x1234

    /** Builds an 8-byte v1 payload (no temperature). */
    private fun v1Payload(
        zoneId: Int,
        mode: Int,
        flags: Int,
        sequence: Int,
        ttl: Int,
        temperature: Int? = null
    ): ByteArray {
        val size = if (temperature == null) 8 else 9
        val buf = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
        buf.put(1.toByte())
        buf.putShort(zoneId.toShort())
        buf.put(mode.toByte())
        buf.put(flags.toByte())
        buf.putShort(sequence.toShort())
        buf.put(ttl.toByte())
        if (temperature != null) buf.put(temperature.toByte())
        return buf.array()
    }

    /** Builds a 13-byte v2 payload including the source id. */
    private fun v2Payload(
        sourceId: Int,
        zoneId: Int,
        mode: Int,
        flags: Int,
        sequence: Int,
        ttl: Int
    ): ByteArray {
        val buf = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN)
        buf.put(2.toByte())
        buf.putInt(sourceId)
        buf.putShort(zoneId.toShort())
        buf.put(mode.toByte())
        buf.put(flags.toByte())
        buf.putShort(sequence.toShort())
        buf.put(ttl.toByte())
        buf.put(0.toByte()) // reserved trailing byte (ignored by parser)
        return buf.array()
    }

    @Test
    fun `v1 8-byte payload parses all fields and leaves temperature null`() {
        val bytes = v1Payload(zoneId = 0x0102, mode = 2, flags = 0xAB, sequence = 0x1234, ttl = 30)

        val signal = BlePayloadParser.parse(manufacturerId, bytes)!!

        assertEquals(1, signal.version)
        assertEquals(0x0102, signal.zoneId)
        assertEquals(CircuitMode.RED_FLAG, signal.mode)
        assertEquals(0xAB, signal.flags)
        assertEquals(0x1234, signal.sequence)
        assertEquals(30, signal.ttlSeconds)
        assertNull(signal.temperature)
        assertNull(signal.sourceId)
    }

    @Test
    fun `v1 9-byte payload parses trailing temperature including negative values`() {
        val warm = BlePayloadParser.parse(manufacturerId, v1Payload(1, 0, 0, 1, 10, temperature = 25))!!
        assertEquals(25, warm.temperature)

        // A two's-complement -3 must decode as a signed value, not 253.
        val cold = BlePayloadParser.parse(manufacturerId, v1Payload(1, 0, 0, 1, 10, temperature = -3))!!
        assertEquals(-3, cold.temperature)
    }

    @Test
    fun `v2 13-byte payload parses sourceId and all fields`() {
        val bytes = v2Payload(
            sourceId = 0x0A0B0C0D,
            zoneId = 0xFFFE,
            mode = 3,
            flags = 0x01,
            sequence = 7,
            ttl = 120
        )

        val signal = BlePayloadParser.parse(manufacturerId, bytes)!!

        assertEquals(2, signal.version)
        assertEquals(0x0A0B0C0D, signal.sourceId)
        assertEquals(0xFFFE, signal.zoneId)
        assertEquals(CircuitMode.EVACUATION, signal.mode)
        assertEquals(0x01, signal.flags)
        assertEquals(7, signal.sequence)
        assertEquals(120, signal.ttlSeconds)
        // Temperature is only a v1 concept; v2 must never expose one.
        assertNull(signal.temperature)
    }

    @Test
    fun `mode byte maps to the correct CircuitMode for every known code`() {
        val expected = mapOf(
            0 to CircuitMode.NORMAL,
            1 to CircuitMode.SAFETY_CAR,
            2 to CircuitMode.RED_FLAG,
            3 to CircuitMode.EVACUATION
        )
        for ((code, mode) in expected) {
            val signal = BlePayloadParser.parse(manufacturerId, v1Payload(1, code, 0, 1, 10))!!
            assertEquals("mode byte $code", mode, signal.mode)
        }
    }

    @Test
    fun `unknown mode byte maps to UNKNOWN`() {
        val signal = BlePayloadParser.parse(manufacturerId, v1Payload(1, 99, 0, 1, 10))!!
        assertEquals(CircuitMode.UNKNOWN, signal.mode)
    }

    @Test
    fun `wrong manufacturer id returns null`() {
        val bytes = v1Payload(1, 0, 0, 1, 10)
        assertNull(BlePayloadParser.parse(0x9999, bytes))
    }

    @Test
    fun `null or empty payload returns null`() {
        assertNull(BlePayloadParser.parse(manufacturerId, null))
        assertNull(BlePayloadParser.parse(manufacturerId, ByteArray(0)))
    }

    @Test
    fun `wrong size for declared version returns null`() {
        // v1 declared but only 7 bytes (below minimum of 8).
        assertNull(BlePayloadParser.parse(manufacturerId, ByteArray(7).also { it[0] = 1 }))
        // v1 declared but 10 bytes (above maximum of 9).
        assertNull(BlePayloadParser.parse(manufacturerId, ByteArray(10).also { it[0] = 1 }))
        // v2 declared but only 12 bytes (must be exactly 13).
        assertNull(BlePayloadParser.parse(manufacturerId, ByteArray(12).also { it[0] = 2 }))
    }

    @Test
    fun `unsupported version byte returns null even with plausible length`() {
        // Version 3 is not supported; an 8-byte buffer must still be rejected.
        assertNull(BlePayloadParser.parse(manufacturerId, ByteArray(8).also { it[0] = 3 }))
    }

    @Test
    fun `zoneId is decoded as unsigned 16-bit`() {
        // 0xFFFF would be -1 if treated as signed; the parser must return 65535.
        val signal = BlePayloadParser.parse(manufacturerId, v1Payload(0xFFFF, 0, 0, 1, 10))!!
        assertEquals(65535, signal.zoneId)
    }
}
