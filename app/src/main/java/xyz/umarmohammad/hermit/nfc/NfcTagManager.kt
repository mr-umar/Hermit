package xyz.umarmohammad.hermit.nfc

import android.content.Intent
import android.os.Build
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Robust NFC operations manager handling tag inspection, capability detection,
 * NDEF serialization, safe writing, and read-back verification.
 */
object NfcTagManager {

    const val APPLICATION_PACKAGE = "xyz.umarmohammad.hermit"
    const val WEB_SCAN_URL = "https://hermit.umarmohammad.xyz/scan.html"

    /**
     * Converts a raw tag UID byte array into a colon-separated uppercase hex string.
     */
    fun bytesToHex(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return "UNKNOWN"
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Inspects a physical NFC tag and extracts detailed capability and payload information.
     */
    fun inspectTag(tag: Tag): NfcCapabilityInfo {
        val uidHex = bytesToHex(tag.id)
        val techList = tag.techList.map { it.substringAfterLast(".") }
        val ndef = Ndef.get(tag)

        if (ndef != null) {
            val isWritable = ndef.isWritable
            val canMakeReadOnly = ndef.canMakeReadOnly()
            val maxSize = ndef.maxSize
            val cachedMsg = ndef.cachedNdefMessage
            val currentSize = cachedMsg?.byteArrayLength ?: 0
            val hasData = cachedMsg != null && cachedMsg.records.isNotEmpty()

            val chipType = detectChipFamily(tag, maxSize, ndef.type)
            var hermitPayload: HermitTagPayload? = null
            var summary: String? = null

            if (cachedMsg != null && cachedMsg.records.isNotEmpty()) {
                for (record in cachedMsg.records) {
                    val mimeType = record.toMimeType()
                    if (mimeType == HermitTagPayload.MIME_TYPE) {
                        val parsed = HermitTagPayload.parse(record.payload)
                        if (parsed.isSuccess) {
                            hermitPayload = parsed.getOrNull()
                            summary = "Hermit Profile: ${hermitPayload?.action} (${hermitPayload?.id?.take(8)}...)"
                            break
                        }
                    } else if (summary == null) {
                        summary = decodeGenericRecord(record)
                    }
                }
            }

            return NfcCapabilityInfo(
                tagUidHex = uidHex,
                techList = techList,
                chipType = chipType,
                isNdefSupported = true,
                isWritable = isWritable,
                canMakeReadOnly = canMakeReadOnly,
                maxNdefSize = maxSize,
                currentNdefSize = currentSize,
                hasExistingData = hasData,
                existingSummary = summary,
                existingPayload = hermitPayload
            )
        }

        // Tag does not have NDEF yet, check if it can be formatted
        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            val chipType = detectChipFamily(tag, 0, "Formatable")
            return NfcCapabilityInfo(
                tagUidHex = uidHex,
                techList = techList,
                chipType = chipType,
                isNdefSupported = true,
                isWritable = true,
                canMakeReadOnly = false,
                maxNdefSize = 0,
                currentNdefSize = 0,
                hasExistingData = false,
                existingSummary = "Empty Formatable Tag",
                existingPayload = null
            )
        }

        // Raw or incompatible tag
        return NfcCapabilityInfo(
            tagUidHex = uidHex,
            techList = techList,
            chipType = "Non-NDEF Tag (${techList.joinToString(", ")})",
            isNdefSupported = false,
            isWritable = false,
            canMakeReadOnly = false,
            maxNdefSize = 0,
            currentNdefSize = 0,
            hasExistingData = false,
            existingSummary = "Unsupported format",
            existingPayload = null
        )
    }

    /**
     * Determines the chip family based on memory size and tech signatures.
     */
    private fun detectChipFamily(tag: Tag, maxSize: Int, ndefType: String): String {
        val hasUltralight = tag.techList.any { it.contains("MifareUltralight") }
        if (hasUltralight || ndefType.contains("Type 2", ignoreCase = true)) {
            return when {
                maxSize in 130..160 -> "NTAG213 (NFC Forum Type 2 - 144B)"
                maxSize in 480..520 -> "NTAG215 (NFC Forum Type 2 - 504B)"
                maxSize in 860..900 -> "NTAG216 (NFC Forum Type 2 - 888B)"
                maxSize > 0 -> "NFC Forum Type 2 Compatible (${maxSize}B)"
                else -> "NFC Forum Type 2 / NTAG Compatible"
            }
        }
        if (ndefType.contains("Type 4", ignoreCase = true) || tag.techList.any { it.contains("IsoDep") }) {
            return "NFC Forum Type 4 / ISO 14443-4 (${maxSize}B)"
        }
        return if (maxSize > 0) "Standard NDEF Tag ($ndefType, ${maxSize}B)" else "NDEF Compatible Tag"
    }

    private fun decodeGenericRecord(record: NdefRecord): String {
        return try {
            when (record.tnf) {
                NdefRecord.TNF_WELL_KNOWN -> "Well-known NDEF Record"
                NdefRecord.TNF_MIME_MEDIA -> "MIME: ${record.toMimeType() ?: "Unknown"}"
                NdefRecord.TNF_ABSOLUTE_URI -> "URI: ${String(record.payload, StandardCharsets.UTF_8)}"
                else -> "NDEF Record (TNF: ${record.tnf})"
            }
        } catch (_: Exception) {
            "NDEF Record"
        }
    }

    fun writeTag(tag: Tag, payload: HermitTagPayload): Result<HermitTagPayload> {
        val payloadBytes = HermitTagPayload.serialize(payload)

        // Include tag metadata in web fallback URI for seamless handover if opened in browser
        val tagScanUrl = buildString {
            append(WEB_SCAN_URL)
            append("?id=").append(payload.id)
            append("&act=").append(payload.action)
            if (!payload.profileId.isNullOrBlank()) {
                append("&pid=").append(payload.profileId)
            }
        }
        // Record 1 (index 0): Custom MIME type record carrying the structured Hermit focus payload.
        // Android NFC Tag Dispatch inspects index 0; placing MIME first ensures instant matching with
        // Hermit's NDEF_DISCOVERED filter with highest priority, bypassing browsers completely.
        val mimeRecord = NdefRecord.createMime(HermitTagPayload.MIME_TYPE, payloadBytes)
        // Record 2 (index 1): Web URI fallback with tag parameters for uninstalled devices
        val uriRecord = NdefRecord.createUri(tagScanUrl)

        val message = NdefMessage(arrayOf(mimeRecord, uriRecord))
        val messageSize = message.byteArrayLength

        // 1. If this is an NTAG previously write-protected by Hermit, unlock it first so Ndef write succeeds
        unlockNtagIfProtected(tag)

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return Result.failure(IllegalStateException("READ_ONLY"))
                }
                if (messageSize > ndef.maxSize) {
                    ndef.close()
                    return Result.failure(
                        IllegalArgumentException("CAPACITY_EXCEEDED: needed $messageSize bytes, available ${ndef.maxSize} bytes")
                    )
                }

                ndef.writeNdefMessage(message)

                // Read-back verification
                val verifyMessage = ndef.ndefMessage
                var verified = false
                if (verifyMessage != null) {
                    for (rec in verifyMessage.records) {
                        if (rec.toMimeType() == HermitTagPayload.MIME_TYPE) {
                            val parsed = HermitTagPayload.parse(rec.payload)
                            if (parsed.isSuccess && parsed.getOrNull()?.id == payload.id) {
                                verified = true
                                break
                            }
                        }
                    }
                }
                ndef.close()

                if (!verified) {
                    return Result.failure(IllegalStateException("VERIFICATION_FAILED"))
                }

                // 2. Lock tag with Hermit's universal password protection (read is open, write is protected)
                applyNtagProtection(tag)

                return Result.success(payload)
            } catch (e: TagLostException) {
                return Result.failure(e)
            } catch (e: FormatException) {
                return Result.failure(e)
            } catch (e: IOException) {
                return Result.failure(e)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        // Try formatting if not yet formatted
        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            try {
                formatable.connect()
                formatable.format(message)
                formatable.close()

                applyNtagProtection(tag)
                return Result.success(payload)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        return Result.failure(UnsupportedOperationException("UNSUPPORTED_TAG_TECHNOLOGY"))
    }

    // Universal Hermit Tag Protection Password & PACK
    // 4-byte PWD: ASCII "Herm" = [0x48, 0x65, 0x72, 0x6D]
    // 2-byte PACK: ASCII "HT" = [0x48, 0x54]
    val HERMIT_PWD: ByteArray = byteArrayOf(0x48.toByte(), 0x65.toByte(), 0x72.toByte(), 0x6D.toByte())
    val HERMIT_PACK: ByteArray = byteArrayOf(0x48.toByte(), 0x54.toByte())

    data class NtagConfig(
        val auth0Page: Int,
        val accessPage: Int,
        val pwdPage: Int,
        val packPage: Int
    )

    private fun detectNtagConfig(ultralight: MifareUltralight, maxNdefSize: Int): NtagConfig? {
        try {
            val version = ultralight.transceive(byteArrayOf(0x60.toByte()))
            if (version != null && version.size >= 8) {
                val storageSize = version[6].toInt() and 0xFF
                return when (storageSize) {
                    0x0F -> NtagConfig(auth0Page = 41, accessPage = 42, pwdPage = 43, packPage = 44) // NTAG213
                    0x11 -> NtagConfig(auth0Page = 131, accessPage = 132, pwdPage = 133, packPage = 134) // NTAG215
                    0x13 -> NtagConfig(auth0Page = 227, accessPage = 228, pwdPage = 229, packPage = 230) // NTAG216
                    else -> null
                }
            }
        } catch (_: Exception) {
            // Fallback to maxNdefSize if GET_VERSION not supported
        }

        return when {
            maxNdefSize in 100..200 -> NtagConfig(41, 42, 43, 44) // NTAG213 (~144 bytes)
            maxNdefSize in 400..600 -> NtagConfig(131, 132, 133, 134) // NTAG215 (~504 bytes)
            maxNdefSize in 800..1000 -> NtagConfig(227, 228, 229, 230) // NTAG216 (~888 bytes)
            else -> null
        }
    }

    private fun authenticateTag(ultralight: MifareUltralight): Boolean {
        return try {
            val cmd = byteArrayOf(
                0x1B.toByte(),
                HERMIT_PWD[0],
                HERMIT_PWD[1],
                HERMIT_PWD[2],
                HERMIT_PWD[3]
            )
            val resp = ultralight.transceive(cmd)
            resp != null && resp.size >= 2
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Unprotects an NTAG21x tag if previously locked by Hermit, so NDEF write operations succeed.
     */
    fun unlockNtagIfProtected(tag: Tag): Boolean {
        val ultralight = MifareUltralight.get(tag) ?: return false
        return try {
            ultralight.connect()
            val config = detectNtagConfig(ultralight, Ndef.get(tag)?.maxSize ?: 0)
            if (config != null) {
                if (authenticateTag(ultralight)) {
                    // Temporarily set AUTH0 to 0xFF so Ndef can write freely
                    ultralight.writePage(config.auth0Page, byteArrayOf(0x00, 0x00, 0x00, 0xFF.toByte()))
                }
            }
            ultralight.close()
            true
        } catch (_: Exception) {
            try { ultralight.close() } catch (_: Exception) {}
            false
        }
    }

    /**
     * Applies Hermit's write-protection password (PROT=0: write protected, read completely open).
     */
    fun applyNtagProtection(tag: Tag): Boolean {
        val ultralight = MifareUltralight.get(tag) ?: return false
        return try {
            ultralight.connect()
            val config = detectNtagConfig(ultralight, Ndef.get(tag)?.maxSize ?: 0)
            if (config != null) {
                // 1. Authenticate first if already locked
                authenticateTag(ultralight)
                // 2. Write PWD and PACK
                ultralight.writePage(config.pwdPage, HERMIT_PWD)
                ultralight.writePage(config.packPage, byteArrayOf(HERMIT_PACK[0], HERMIT_PACK[1], 0x00, 0x00))
                // 3. Write ACCESS: PROT = 0 (write protected, read is open to all), CFGLCK=0, AUTHLIM=0
                ultralight.writePage(config.accessPage, byteArrayOf(0x00, 0x00, 0x00, 0x00))
                // 4. Write AUTH0: Page 4 (protect user memory from page 4 to end)
                ultralight.writePage(config.auth0Page, byteArrayOf(0x00, 0x00, 0x00, 0x04))
            }
            ultralight.close()
            true
        } catch (_: Exception) {
            try { ultralight.close() } catch (_: Exception) {}
            false
        }
    }

    /**
     * Erases an NFC tag: unlocks with Hermit password, resets password to factory default,
     * and clears the NDEF content.
     */
    fun eraseTag(tag: Tag): Result<Unit> {
        val ultralight = MifareUltralight.get(tag)
        if (ultralight != null) {
            try {
                ultralight.connect()
                val config = detectNtagConfig(ultralight, Ndef.get(tag)?.maxSize ?: 0)
                if (config != null) {
                    authenticateTag(ultralight)
                    // Disable password protection by setting AUTH0 = 0xFF
                    ultralight.writePage(config.auth0Page, byteArrayOf(0x00, 0x00, 0x00, 0xFF.toByte()))
                    // Reset ACCESS
                    ultralight.writePage(config.accessPage, byteArrayOf(0x00, 0x00, 0x00, 0x00))
                    // Reset PWD to factory default (0xFF, 0xFF, 0xFF, 0xFF)
                    ultralight.writePage(config.pwdPage, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
                    // Reset PACK to factory default (0x00, 0x00, 0x00, 0x00)
                    ultralight.writePage(config.packPage, byteArrayOf(0x00, 0x00, 0x00, 0x00))
                }
                ultralight.close()
            } catch (_: Exception) {
                try { ultralight.close() } catch (_: Exception) {}
            }
        }

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val emptyRecord = NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))
                ndef.writeNdefMessage(NdefMessage(arrayOf(emptyRecord)))
                ndef.close()
                return Result.success(Unit)
            } catch (e: Exception) {
                try { ndef.close() } catch (_: Exception) {}
                return Result.failure(e)
            }
        }

        return Result.success(Unit)
    }

    /**
     * Extracts a HermitTagPayload from an incoming Android NFC Intent, supporting both
     * direct NDEF message bundles and raw EXTRA_TAG fallbacks.
     */
    fun extractHermitPayload(intent: Intent): HermitTagPayload? {
        val rawMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, android.os.Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }

        if (rawMessages != null) {
            for (rawMsg in rawMessages) {
                val msg = rawMsg as? NdefMessage ?: continue
                for (record in msg.records) {
                    if (record.toMimeType() == HermitTagPayload.MIME_TYPE) {
                        val parsed = HermitTagPayload.parse(record.payload)
                        if (parsed.isSuccess) {
                            return parsed.getOrNull()
                        }
                    }
                }
            }
        }

        // Fallback: extract directly from EXTRA_TAG if EXTRA_NDEF_MESSAGES was not delivered
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        if (tag != null) {
            return readHermitPayload(tag)
        }

        // Fallback: extract from intent data URI query params if redirected from scan.html / VIEW intent
        val uri = intent.data
        if (uri != null && (uri.scheme == "hermit" || uri.host == "hermit.umarmohammad.xyz")) {
            val id = uri.getQueryParameter("id")
            if (!id.isNullOrBlank()) {
                val act = uri.getQueryParameter("act") ?: "TOGGLE"
                val pid = uri.getQueryParameter("pid")
                return HermitTagPayload(
                    id = id,
                    action = act,
                    profileId = pid
                )
            }
        }

        return null
    }

    /**
     * Extracts the physical UID hex string from an incoming NFC Intent if available.
     */
    fun extractTagUid(intent: Intent): String {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        return bytesToHex(tag?.id)
    }

    /**
     * Reads a HermitTagPayload directly from a live Tag object if present.
     */
    fun readHermitPayload(tag: Tag): HermitTagPayload? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val msg = ndef.ndefMessage
            ndef.close()
            if (msg == null) return null
            for (record in msg.records) {
                if (record.toMimeType() == HermitTagPayload.MIME_TYPE) {
                    val parsed = HermitTagPayload.parse(record.payload)
                    if (parsed.isSuccess) {
                        return parsed.getOrNull()
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
