package xyz.umarmohammad.hermit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.umarmohammad.hermit.data.model.TagAction
import xyz.umarmohammad.hermit.nfc.HermitTagPayload
import java.nio.charset.StandardCharsets

class NfcPayloadTest {

    @Test
    fun testPayloadSerializationAndParsingRoundTrip() {
        val original = HermitTagPayload(
            version = 1,
            id = "test-uuid-12345",
            action = TagAction.ACTIVATE.name,
            profileId = "deep-work-profile",
            name = "Office Desk"
        )

        val bytes = HermitTagPayload.serialize(original)
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val result = HermitTagPayload.parse(bytes)
        assertTrue(result.isSuccess)

        val parsed = result.getOrNull()
        assertNotNull(parsed)
        assertEquals(original.version, parsed?.version)
        assertEquals(original.id, parsed?.id)
        assertEquals(original.action, parsed?.action)
        assertEquals(original.profileId, parsed?.profileId)
        assertEquals(original.name, parsed?.name)
        assertEquals(TagAction.ACTIVATE, parsed?.getResolvedAction())
    }

    @Test
    fun testPayloadRejectUnsupportedFutureVersion() {
        val futureJson = """
            {"v":999,"id":"future-id","act":"TOGGLE","pid":"any"}
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        val result = HermitTagPayload.parse(futureJson)
        assertFalse("Future payload version should be rejected", result.isSuccess)
    }

    @Test
    fun testPayloadRejectMalformedOrBlankId() {
        val malformedJson = """
            {"v":1,"id":"","act":"TOGGLE"}
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        val result = HermitTagPayload.parse(malformedJson)
        assertFalse("Payload without stable ID should be rejected", result.isSuccess)
    }

    @Test
    fun testPayloadRejectCorruptedGarbageBytes() {
        val garbage = byteArrayOf(0x00, 0x12, 0x55, 0xFF.toByte(), 0xAA.toByte())
        val result = HermitTagPayload.parse(garbage)
        assertFalse("Corrupted binary payload must fail gracefully", result.isSuccess)
    }

    @Test
    fun testPayloadDefaultActionFallbackOnUnknownActionString() {
        val unknownActJson = """
            {"v":1,"id":"valid-uuid","act":"NON_EXISTENT_ACTION"}
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        val result = HermitTagPayload.parse(unknownActJson)
        assertTrue(result.isSuccess)
        val payload = result.getOrNull()
        assertEquals(TagAction.TOGGLE, payload?.getResolvedAction())
    }

    @Test
    fun testPayloadCompactSizeConstraint() {
        // Enforce that typical payload is strictly below 130 bytes to easily fit inside NTAG213 (144 bytes)
        val typical = HermitTagPayload(
            id = "c1a2b3d4-e5f6-7890-abcd-ef1234567890",
            action = TagAction.TOGGLE.name,
            profileId = "default_deep_work",
            name = "Desk"
        )
        val bytes = HermitTagPayload.serialize(typical)
        assertTrue("NDEF payload size (${bytes.size} bytes) must fit inside NTAG213 limit of 144B", bytes.size < 130)
    }

    @Test
    fun testWebScanUrlConstant() {
        assertEquals("https://hermit.umarmohammad.xyz/scan.html", xyz.umarmohammad.hermit.nfc.NfcTagManager.WEB_SCAN_URL)
    }
}

