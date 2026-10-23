package xyz.umarmohammad.hermit.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkSecurityVerifierTest {

    @Test
    fun `computeCertFingerprint returns formatted hex string`() {
        val bytes = byteArrayOf(0x12, 0x34, 0xAB.toByte(), 0xCD.toByte())
        val fp = ApkSecurityVerifier.computeCertFingerprint(bytes)
        assertTrue(fp.contains(":"))
        assertEquals(32 * 2 + 31, fp.length) // 32 bytes hex separated by 31 colons
    }

    @Test
    fun `compareSignatures succeeds when certificates match`() {
        val certA = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val certB = byteArrayOf(9, 10, 11, 12)

        val installed = listOf(certA)
        val incoming = listOf(certA)

        assertTrue(ApkSecurityVerifier.compareSignatures(installed, incoming))
    }

    @Test
    fun `compareSignatures rejects mismatched certificates`() {
        val certInstalled = byteArrayOf(1, 2, 3, 4, 5)
        val certUntrusted = byteArrayOf(6, 7, 8, 9, 10)

        val installed = listOf(certInstalled)
        val incoming = listOf(certUntrusted)

        assertFalse("Signatures signed with different keys must be rejected",
            ApkSecurityVerifier.compareSignatures(installed, incoming))
    }

    @Test
    fun `compareSignatures rejects empty certificates`() {
        assertFalse(ApkSecurityVerifier.compareSignatures(emptyList(), emptyList()))
        assertFalse(ApkSecurityVerifier.compareSignatures(listOf(byteArrayOf(1)), emptyList()))
    }
}
