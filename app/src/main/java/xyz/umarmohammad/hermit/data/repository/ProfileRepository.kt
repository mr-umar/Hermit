package xyz.umarmohammad.hermit.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.umarmohammad.hermit.data.local.FocusProfileDao
import xyz.umarmohammad.hermit.data.local.HermitDatabase
import xyz.umarmohammad.hermit.data.model.FocusProfile

class ProfileRepository(private val dao: FocusProfileDao) {

    val allProfiles: Flow<List<FocusProfile>> = dao.getAllProfiles()

    suspend fun getProfileById(id: String): FocusProfile? = dao.getProfileById(id)

    fun getProfileByIdFlow(id: String): Flow<FocusProfile?> = dao.getProfileByIdFlow(id)

    suspend fun saveProfile(profile: FocusProfile) {
        val sanitized = profile.copy(
            blockedPackages = profile.blockedPackages.filter { it != "xyz.umarmohammad.hermit" }
        )
        dao.insertProfile(sanitized)
    }

    suspend fun updateProfile(profile: FocusProfile) {
        val sanitized = profile.copy(
            blockedPackages = profile.blockedPackages.filter { it != "xyz.umarmohammad.hermit" },
            updatedAt = System.currentTimeMillis()
        )
        dao.updateProfile(sanitized)
    }

    suspend fun deleteProfile(profile: FocusProfile) {
        dao.deleteProfile(profile)
    }

    suspend fun deleteProfileById(id: String) {
        dao.deleteProfileById(id)
    }
}
