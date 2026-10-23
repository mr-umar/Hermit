package xyz.umarmohammad.hermit.nfc

/**
 * Diagnostics and runtime capabilities of a scanned physical NFC tag.
 */
data class NfcCapabilityInfo(
    val tagUidHex: String,
    val techList: List<String>,
    val chipType: String,
    val isNdefSupported: Boolean,
    val isWritable: Boolean,
    val canMakeReadOnly: Boolean,
    val maxNdefSize: Int,
    val currentNdefSize: Int,
    val hasExistingData: Boolean,
    val existingSummary: String? = null,
    val existingPayload: HermitTagPayload? = null
)
