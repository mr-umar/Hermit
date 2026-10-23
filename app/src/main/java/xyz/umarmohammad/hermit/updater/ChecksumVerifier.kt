package xyz.umarmohammad.hermit.updater

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

object ChecksumVerifier {

    private val SHA256_HEX_REGEX = Regex("""\b([a-fA-F0-9]{64})\b""")

    /**
     * Computes the SHA-256 checksum of [file] as a lowercase hex string.
     */
    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes SHA-256 of a byte array as a lowercase hex string.
     */
    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies that the SHA-256 of [file] equals [expectedChecksum].
     * Comparison is case-insensitive.
     */
    fun verifyFileChecksum(file: File, expectedChecksum: String): Boolean {
        if (!file.exists() || expectedChecksum.isBlank()) return false
        val actual = computeSha256(file)
        return actual.equals(expectedChecksum.trim(), ignoreCase = true)
    }

    /**
     * Extracts an expected SHA-256 for [assetName] from a checksum file content (e.g. SHA256SUMS.txt)
     * or release notes markdown body.
     */
    fun findExpectedChecksum(assetName: String, checksumFileContent: String?, releaseBody: String?): String? {
        // 1. Try to find in checksums file content (e.g. "sha256  Hermit-v1.0.0.apk")
        if (!checksumFileContent.isNullOrBlank()) {
            for (line in checksumFileContent.lines()) {
                val trimmed = line.trim()
                if (trimmed.contains(assetName, ignoreCase = true)) {
                    val match = SHA256_HEX_REGEX.find(trimmed)
                    if (match != null) {
                        return match.value.lowercase(Locale.US)
                    }
                }
            }
            // If the checksum file only has a single hash (e.g. Hermit.apk.sha256)
            val singleHashMatch = SHA256_HEX_REGEX.find(checksumFileContent.trim())
            if (singleHashMatch != null && checksumFileContent.lines().size <= 2) {
                return singleHashMatch.value.lowercase(Locale.US)
            }
        }

        // 2. Try to find in release body markdown
        if (!releaseBody.isNullOrBlank()) {
            for (line in releaseBody.lines()) {
                val trimmed = line.trim()
                if (trimmed.contains(assetName, ignoreCase = true)) {
                    val match = SHA256_HEX_REGEX.find(trimmed)
                    if (match != null) {
                        return match.value.lowercase(Locale.US)
                    }
                }
            }
            // Look for generic "SHA256: <hash>" or "SHA-256: <hash>" in release notes
            val bodyHashRegex = Regex("""(?:SHA-?256|Checksum):\s*`?([a-fA-F0-9]{64})`?""", RegexOption.IGNORE_CASE)
            val bodyMatch = bodyHashRegex.find(releaseBody)
            if (bodyMatch != null) {
                return bodyMatch.groupValues[1].lowercase(Locale.US)
            }
        }

        return null
    }
}
