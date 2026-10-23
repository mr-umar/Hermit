package xyz.umarmohammad.hermit.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import xyz.umarmohammad.hermit.data.model.FocusSessionRecord

@Dao
interface SessionRecordDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<FocusSessionRecord>>

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): FocusSessionRecord?

    @Query("SELECT * FROM focus_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): FocusSessionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionRecord)

    @Update
    suspend fun updateSession(session: FocusSessionRecord)

    @Delete
    suspend fun deleteSession(session: FocusSessionRecord)

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE endedAt IS NOT NULL")
    fun getCompletedSessionsCount(): Flow<Int>

    @Query("SELECT SUM(blockedAttemptsCount) FROM focus_sessions")
    fun getTotalBlockedAttempts(): Flow<Int?>

    @Query("SELECT SUM((endedAt - startedAt) / 1000) FROM focus_sessions WHERE endedAt IS NOT NULL")
    fun getTotalFocusDurationSeconds(): Flow<Long?>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
