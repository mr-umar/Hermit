package xyz.umarmohammad.hermit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a registered physical NFC tag paired with Hermit.
 */
@Entity(tableName = "nfc_tags")
data class NfcTagRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val tagPhysicalUid: String = "",
    val name: String,
    val description: String = "",
    val action: TagAction = TagAction.TOGGLE,
    val targetProfileId: String? = null,
    val lastScannedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDisabled: Boolean = false
)
