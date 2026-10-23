package xyz.umarmohammad.hermit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Historical record of a completed or aborted focus session.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val profileName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val plannedDurationMinutes: Int? = null,
    val endReason: String = "ACTIVE",
    val blockedAttemptsCount: Int = 0
)
