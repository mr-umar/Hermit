package xyz.umarmohammad.hermit.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hermit_preferences")

class PreferenceManager(private val context: Context) {

    companion object {
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("key_first_launch")
        val KEY_APP_LOCALE = stringPreferencesKey("key_app_locale")
        val KEY_THEME_MODE = stringPreferencesKey("key_theme_mode")

        // Active Session Persistence across process kills and reboots
        val KEY_ACTIVE_SESSION_ID = stringPreferencesKey("key_active_session_id")
        val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("key_active_profile_id")
        val KEY_SESSION_START_TIME = longPreferencesKey("key_session_start_time")
        val KEY_SESSION_DURATION_MIN = intPreferencesKey("key_session_duration_min")
        val KEY_BLOCKED_COUNT = intPreferencesKey("key_session_blocked_count")

        // NFC scan debounce protection
        val KEY_LAST_SCANNED_TAG_ID = stringPreferencesKey("key_last_scanned_tag_id")
        val KEY_LAST_SCANNED_TIMESTAMP = longPreferencesKey("key_last_scanned_timestamp")

        // GitHub Release Update Preferences
        val KEY_AUTO_CHECK_UPDATES = booleanPreferencesKey("key_auto_check_updates")
        val KEY_LAST_UPDATE_CHECK_TIMESTAMP = longPreferencesKey("key_last_update_check_timestamp")

        // Active selected profile in Enfoque home screen
        val KEY_SELECTED_QUICK_PROFILE_ID = stringPreferencesKey("key_selected_quick_profile_id")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_FIRST_LAUNCH] ?: true
    }

    val appLocale: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_APP_LOCALE] ?: "system"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "system"
    }

    val autoCheckUpdates: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_CHECK_UPDATES] ?: true
    }

    val lastUpdateCheckTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_UPDATE_CHECK_TIMESTAMP] ?: 0L
    }

    val activeSessionId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_SESSION_ID]
    }

    val activeProfileId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_PROFILE_ID]
    }

    val sessionStartTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_SESSION_START_TIME] ?: 0L
    }

    val sessionDurationMin: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[KEY_SESSION_DURATION_MIN]
    }

    val sessionBlockedCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCKED_COUNT] ?: 0
    }

    val selectedQuickProfileId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_QUICK_PROFILE_ID]
    }

    suspend fun setSelectedQuickProfileId(profileId: String) {
        context.dataStore.edit { it[KEY_SELECTED_QUICK_PROFILE_ID] = profileId }
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { it[KEY_FIRST_LAUNCH] = false }
    }

    suspend fun setAppLocale(locale: String) {
        context.dataStore.edit { it[KEY_APP_LOCALE] = locale }
    }

    suspend fun setThemeMode(theme: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = theme }
    }

    suspend fun setActiveSession(
        sessionId: String,
        profileId: String,
        startTime: Long,
        durationMinutes: Int?
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_SESSION_ID] = sessionId
            prefs[KEY_ACTIVE_PROFILE_ID] = profileId
            prefs[KEY_SESSION_START_TIME] = startTime
            if (durationMinutes != null) {
                prefs[KEY_SESSION_DURATION_MIN] = durationMinutes
            } else {
                prefs.remove(KEY_SESSION_DURATION_MIN)
            }
            prefs[KEY_BLOCKED_COUNT] = 0
        }
    }

    suspend fun clearActiveSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACTIVE_SESSION_ID)
            prefs.remove(KEY_ACTIVE_PROFILE_ID)
            prefs.remove(KEY_SESSION_START_TIME)
            prefs.remove(KEY_SESSION_DURATION_MIN)
            prefs.remove(KEY_BLOCKED_COUNT)
        }
    }

    suspend fun incrementBlockedCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BLOCKED_COUNT] ?: 0
            prefs[KEY_BLOCKED_COUNT] = current + 1
        }
    }

    suspend fun updateLastScannedTag(tagId: String, timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SCANNED_TAG_ID] = tagId
            prefs[KEY_LAST_SCANNED_TIMESTAMP] = timestamp
        }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun setLastUpdateCheckTimestamp(timestamp: Long) {
        context.dataStore.edit { it[KEY_LAST_UPDATE_CHECK_TIMESTAMP] = timestamp }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
