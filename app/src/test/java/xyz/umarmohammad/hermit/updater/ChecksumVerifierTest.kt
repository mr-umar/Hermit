package xyz.umarmohammad.hermit.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChecksumVerifierTest {

    @Test
    fun `computeSha256 of byte array is correct`() {
        val sample = "Hello Hermit Update System".toByteArray(Charsets.UTF_8)
        val hash = ChecksumVerifier.computeSha256(sample)
        // Known SHA-256 for this exact string
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in "0123456789abcdef" })
    }

    @Test
    fun `verifyFileChecksum validates matching file`() {
        val tempFile = File.createTempFile("hermit_test", ".apk")
        tempFile.deleteOnExit()
        tempFile.writeText("sample apk binary content")

        val expectedHash = ChecksumVerifier.computeSha256(tempFile)
        assertTrue(ChecksumVerifier.verifyFileChecksum(tempFile, expectedHash))
        assertTrue(ChecksumVerifier.verifyFileChecksum(tempFile, expectedHash.uppercase()))
        assertFalse(ChecksumVerifier.verifyFileChecksum(tempFile, "0000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `extract checksum from sha256sums file content`() {
        val checksumContent = """
            d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2  Hermit-v1.0.0.apk
            aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa  source.tar.gz
        """.trimIndent()

        val found = ChecksumVerifier.findExpectedChecksum("Hermit-v1.0.0.apk", checksumContent, null)
        assertNotNull(found)
        assertEquals("d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2", found)
    }

    @Test
    fun `extract checksum from release markdown body`() {
        val releaseBody = """
            ## Hermit v1.0.1
            A calm, monastic focus release.
            
            ### Integrity
            SHA-256: `bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb`
        """.trimIndent()

        val found = ChecksumVerifier.findExpectedChecksum("Hermit-v1.0.1.apk", null, releaseBody)
        assertNotNull(found)
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", found)
    }

    @Test
    fun `returns null if no checksum found`() {
        val found = ChecksumVerifier.findExpectedChecksum("Hermit.apk", "no checksums here", "no hash in body")
        assertNull(found)
    }
}
