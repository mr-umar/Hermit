package xyz.umarmohammad.hermit.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import xyz.umarmohammad.hermit.data.model.FocusProfile

@Dao
interface FocusProfileDao {
    @Query("SELECT * FROM focus_profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): Flow<List<FocusProfile>>

    @Query("SELECT * FROM focus_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): FocusProfile?

    @Query("SELECT * FROM focus_profiles WHERE id = :id LIMIT 1")
    fun getProfileByIdFlow(id: String): Flow<FocusProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: FocusProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<FocusProfile>)

    @Update
    suspend fun updateProfile(profile: FocusProfile)

    @Delete
    suspend fun deleteProfile(profile: FocusProfile)

    @Query("DELETE FROM focus_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)

    @Query("SELECT COUNT(*) FROM focus_profiles")
    suspend fun countProfiles(): Int

    @Query("DELETE FROM focus_profiles")
    suspend fun deleteAll()
}
