package xyz.umarmohammad.hermit.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import xyz.umarmohammad.hermit.data.model.NfcTagRecord

@Dao
interface NfcTagDao {
    @Query("SELECT * FROM nfc_tags ORDER BY createdAt DESC")
    fun getAllTags(): Flow<List<NfcTagRecord>>

    @Query("SELECT * FROM nfc_tags WHERE id = :id LIMIT 1")
    suspend fun getTagById(id: String): NfcTagRecord?

    @Query("SELECT * FROM nfc_tags WHERE tagPhysicalUid = :uid LIMIT 1")
    suspend fun getTagByPhysicalUid(uid: String): NfcTagRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: NfcTagRecord)

    @Update
    suspend fun updateTag(tag: NfcTagRecord)

    @Delete
    suspend fun deleteTag(tag: NfcTagRecord)

    @Query("DELETE FROM nfc_tags WHERE id = :id")
    suspend fun deleteTagById(id: String)

    @Query("DELETE FROM nfc_tags")
    suspend fun deleteAll()
}
