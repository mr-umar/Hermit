package xyz.umarmohammad.hermit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a customizable focus profile specifying which apps are blocked,
 * session parameters, and exit restrictions.
 */
@Entity(tableName = "focus_profiles")
data class FocusProfile(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val iconName: String = "work",
    val colorHex: String = "#A5B092",
    val blockedPackages: List<String> = emptyList(),
    val durationMinutes: Int? = null,
    val exitBehavior: ExitBehavior = ExitBehavior.ALLOW_EMERGENCY,
    val strictMode: Boolean = false,
    val isDefault: Boolean = false,
    val isMonkMode: Boolean = false,
    val blockWebSocial: Boolean = true,
    val activationType: String = "BOTH", // "NFC", "TIME", or "BOTH"
    val zenAllowedPackages: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
