package xyz.umarmohammad.hermit.nfc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.umarmohammad.hermit.data.model.TagAction
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Compact, versioned NDEF payload stored on physical NFC tags.
 * Kept intentionally minimal (&lt;120 bytes) to guarantee compatibility with
 * NTAG213 (144B), NTAG215 (504B), and NTAG216 (888B).
 */
@Serializable
data class HermitTagPayload(
    @SerialName("v")
    val version: Int = CURRENT_VERSION,
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerialName("act")
    val action: String = TagAction.TOGGLE.name,
    @SerialName("pid")
    val profileId: String? = null,
    @SerialName("name")
    val name: String? = null
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val MIME_TYPE = "application/vnd.xyz.umarmohammad.hermit"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /**
         * Serializes the payload to UTF-8 bytes for NDEF MIME record creation.
         */
        fun serialize(payload: HermitTagPayload): ByteArray {
            val jsonString = json.encodeToString(payload)
            return jsonString.toByteArray(StandardCharsets.UTF_8)
        }

        /**
         * Safely parses raw bytes from an NDEF record.
         * Validates version compatibility and guards against arbitrary or corrupted data.
         */
        fun parse(bytes: ByteArray): Result<HermitTagPayload> {
            return try {
                val jsonString = String(bytes, StandardCharsets.UTF_8)
                val parsed = json.decodeFromString<HermitTagPayload>(jsonString)
                if (parsed.version > CURRENT_VERSION) {
                    Result.failure(IllegalArgumentException("Unsupported payload version: ${parsed.version}"))
                } else if (parsed.id.isBlank()) {
                    Result.failure(IllegalArgumentException("Invalid payload: missing stable identifier"))
                } else {
                    Result.success(parsed)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getResolvedAction(): TagAction = TagAction.fromString(action)
}
