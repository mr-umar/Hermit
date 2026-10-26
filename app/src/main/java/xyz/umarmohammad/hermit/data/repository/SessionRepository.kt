package xyz.umarmohammad.hermit.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.umarmohammad.hermit.data.local.SessionRecordDao
import xyz.umarmohammad.hermit.data.model.FocusSessionRecord

class SessionRepository(private val dao: SessionRecordDao) {

    val allSessions: Flow<List<FocusSessionRecord>> = dao.getAllSessions()
    val completedSessionsCount: Flow<Int> = dao.getCompletedSessionsCount()
    val totalBlockedAttempts: Flow<Int?> = dao.getTotalBlockedAttempts()
    val totalFocusDurationSeconds: Flow<Long?> = dao.getTotalFocusDurationSeconds()

    suspend fun getActiveSession(): FocusSessionRecord? = dao.getActiveSession()

    suspend fun getSessionById(id: String): FocusSessionRecord? = dao.getSessionById(id)

    suspend fun startSession(session: FocusSessionRecord) {
        dao.insertSession(session)
    }

    suspend fun updateSession(session: FocusSessionRecord) {
        dao.updateSession(session)
    }

    suspend fun endSession(
        sessionId: String,
        endReason: String,
        blockedAttempts: Int,
        fallbackProfileId: String? = null,
        fallbackProfileName: String? = null,
        fallbackStartedAt: Long? = null,
        fallbackPlannedMinutes: Int? = null
    ) {
        val session = dao.getSessionById(sessionId)
        val now = System.currentTimeMillis()
        if (session != null) {
            dao.updateSession(
                session.copy(
                    endedAt = now,
                    endReason = endReason,
                    blockedAttemptsCount = blockedAttempts
                )
            )
        } else if (fallbackProfileId != null) {
            dao.insertSession(
                FocusSessionRecord(
                    id = sessionId,
                    profileId = fallbackProfileId,
                    profileName = fallbackProfileName ?: "Profile",
                    startedAt = fallbackStartedAt ?: now,
                    endedAt = now,
                    plannedDurationMinutes = fallbackPlannedMinutes,
                    endReason = endReason,
                    blockedAttemptsCount = blockedAttempts
                )
            )
        }
    }
}
