package xyz.umarmohammad.hermit.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.umarmohammad.hermit.data.local.NfcTagDao
import xyz.umarmohammad.hermit.data.model.NfcTagRecord

class TagRepository(private val dao: NfcTagDao) {

    val allTags: Flow<List<NfcTagRecord>> = dao.getAllTags()

    suspend fun getTagById(id: String): NfcTagRecord? = dao.getTagById(id)

    suspend fun getTagByPhysicalUid(uid: String): NfcTagRecord? = dao.getTagByPhysicalUid(uid)

    suspend fun saveTag(tag: NfcTagRecord) {
        dao.insertTag(tag)
    }

    suspend fun updateTag(tag: NfcTagRecord) {
        dao.updateTag(tag)
    }

    suspend fun markScanned(tagId: String) {
        val tag = dao.getTagById(tagId) ?: return
        dao.updateTag(tag.copy(lastScannedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTag(tag: NfcTagRecord) {
        dao.deleteTag(tag)
    }

    suspend fun deleteTagById(id: String) {
        dao.deleteTagById(id)
    }
}
