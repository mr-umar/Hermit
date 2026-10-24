package xyz.umarmohammad.hermit.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.data.local.HermitDatabase
import xyz.umarmohammad.hermit.data.model.ExitBehavior
import xyz.umarmohammad.hermit.data.model.FocusProfile
import xyz.umarmohammad.hermit.data.model.FocusSessionRecord
import xyz.umarmohammad.hermit.data.model.TagAction
import xyz.umarmohammad.hermit.data.preference.PreferenceManager
import xyz.umarmohammad.hermit.data.repository.ProfileRepository
import xyz.umarmohammad.hermit.data.repository.SessionRepository
import xyz.umarmohammad.hermit.data.repository.TagRepository
import xyz.umarmohammad.hermit.nfc.HermitTagPayload
import xyz.umarmohammad.hermit.util.ScreenTimeManager
import java.util.UUID

data class FocusSessionState(
    val isActive: Boolean = false,
    val sessionId: String? = null,
    val activeProfile: FocusProfile? = null,
    val startedAt: Long = 0L,
    val plannedDurationMinutes: Int? = null,
    val remainingSeconds: Long? = null,
    val elapsedSeconds: Long = 0L,
    val blockedAttemptsCount: Int = 0,
    val startedWithNfc: Boolean = false
)

data class PendingCountdown(
    val profile: FocusProfile,
    val secondsRemaining: Int = 5,
    val source: String = "MANUAL_QUICK_START"
)

data class AppSessionStat(
    val packageName: String,
    val appName: String,
    val count: Int
)

data class SessionCompletionSummary(
    val profileName: String,
    val durationSeconds: Long,
    val unlockCount: Int,
    val totalBlockedCount: Int,
    val topApps: List<AppSessionStat> = emptyList(),
    val endedViaNfc: Boolean = true
)

enum class TagScanOutcome {
    STARTED,
    STOPPED,
    SWITCHED,
    IGNORED_DEBOUNCE,
    UNKNOWN_TAG,
    UNKNOWN_PROFILE,
    DISABLED_TAG
}

class SessionManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val db = HermitDatabase.getInstance(context)
    private val profileRepository = ProfileRepository(db.focusProfileDao())
    private val tagRepository = TagRepository(db.nfcTagDao())
    private val sessionRepository = SessionRepository(db.sessionRecordDao())
    private val preferenceManager = PreferenceManager(context)

    private val _sessionState = MutableStateFlow(FocusSessionState())
    val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()

    private val _pendingCountdown = MutableStateFlow<PendingCountdown?>(null)
    val pendingCountdown: StateFlow<PendingCountdown?> = _pendingCountdown.asStateFlow()

    private val _lastCompletedSessionSummary = MutableStateFlow<SessionCompletionSummary?>(null)
    val lastCompletedSessionSummary: StateFlow<SessionCompletionSummary?> = _lastCompletedSessionSummary.asStateFlow()

    fun dismissCompletedSessionSummary() {
        _lastCompletedSessionSummary.value = null
    }

    private var unlockCount: Int = 0
    private var isUnlockReceiverRegistered: Boolean = false
    private val blockedAppCounts = java.util.concurrent.ConcurrentHashMap<String, Int>()

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT && _sessionState.value.isActive) {
                unlockCount++
            }
        }
    }

    private fun registerUnlockReceiver() {
        if (isUnlockReceiverRegistered) return
        try {
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(unlockReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(unlockReceiver, filter)
            }
            isUnlockReceiverRegistered = true
        } catch (_: Exception) {}
    }

    private fun unregisterUnlockReceiver() {
        if (!isUnlockReceiverRegistered) return
        try {
            context.unregisterReceiver(unlockReceiver)
            isUnlockReceiverRegistered = false
        } catch (_: Exception) {}
    }

    private var countdownJob: Job? = null
    private var timerJob: Job? = null

    fun startCountdown(profile: FocusProfile, source: String = "MANUAL_QUICK_START") {
        if (_sessionState.value.isActive) return

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (sec in 5 downTo 1) {
                _pendingCountdown.value = PendingCountdown(profile, sec, source)
                delay(1000L)
            }
            val target = _pendingCountdown.value?.profile
            _pendingCountdown.value = null
            countdownJob = null
            if (target != null) {
                startFocus(target.id, source)
            }
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _pendingCountdown.value = null
    }

    init {
        scope.launch {
            restorePersistedSession()
        }
    }

    companion object {
        private const val SCAN_DEBOUNCE_MS = 2000L

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private suspend fun restorePersistedSession() {
        val activeSessionId = preferenceManager.activeSessionId.first()
        val activeProfileId = preferenceManager.activeProfileId.first()
        val startTime = preferenceManager.sessionStartTime.first()
        val durationMin = preferenceManager.sessionDurationMin.first()
        val blockedCount = preferenceManager.sessionBlockedCount.first()

        if (!activeSessionId.isNullOrBlank() && !activeProfileId.isNullOrBlank()) {
            val rawProfile = profileRepository.getProfileById(activeProfileId)
            val hasTags = tagRepository.allTags.first().isNotEmpty()
            val sanitizedExitBehavior = if (!hasTags && rawProfile?.exitBehavior == ExitBehavior.NFC_ONLY) {
                ExitBehavior.ALLOW_EMERGENCY
            } else {
                rawProfile?.exitBehavior ?: ExitBehavior.ALLOW_EMERGENCY
            }
            val sanitizedActivationType = if (!hasTags) "TIME" else (rawProfile?.activationType ?: "TIME")

            val profile = rawProfile?.copy(
                exitBehavior = sanitizedExitBehavior,
                activationType = sanitizedActivationType,
                blockedPackages = rawProfile.blockedPackages.filter {
                    it != context.packageName && it != "xyz.umarmohammad.hermit"
                }
            )
            val now = System.currentTimeMillis()
            val elapsedSec = (now - startTime) / 1000
            val maxSafetySec = 12 * 3600L // 12 hours max safety limit

            if (elapsedSec >= maxSafetySec) {
                sessionRepository.endSession(activeSessionId, "SAFETY_12H_TIMEOUT", blockedCount)
                preferenceManager.clearActiveSession()
                _sessionState.value = FocusSessionState()
                return
            }

            // Check if timed session already expired while process was killed
            if (durationMin != null && durationMin > 0) {
                val totalDurationSec = durationMin.coerceAtMost(720) * 60L
                if (elapsedSec >= totalDurationSec) {
                    // Session expired during sleep/reboot
                    sessionRepository.endSession(activeSessionId, "TIMED_COMPLETION", blockedCount)
                    preferenceManager.clearActiveSession()
                    _sessionState.value = FocusSessionState()
                    return
                }
            }

            val startedWithNfc = preferenceManager.sessionStartedWithNfc.first()
            _sessionState.value = FocusSessionState(
                isActive = true,
                sessionId = activeSessionId,
                activeProfile = profile,
                startedAt = startTime,
                plannedDurationMinutes = durationMin?.coerceAtMost(720),
                elapsedSeconds = elapsedSec.coerceAtLeast(0),
                remainingSeconds = durationMin?.let { (it.coerceAtMost(720) * 60L - elapsedSec).coerceAtLeast(0) },
                blockedAttemptsCount = blockedCount,
                startedWithNfc = startedWithNfc
            )
            if (profile?.enableDnd == true) {
                xyz.umarmohammad.hermit.util.DndManager.enableDnd(context)
            }
            if (profile?.enableGrayscale == true) {
                xyz.umarmohammad.hermit.util.GrayscaleManager.enableSystemGrayscale(context)
            }
            registerUnlockReceiver()
            startTicker()
        }
    }

    fun startFocus(profileId: String, reason: String = "MANUAL_START") {
        cancelCountdown()
        scope.launch {
            val rawProfile = profileRepository.getProfileById(profileId) ?: return@launch
            val registeredTags = tagRepository.allTags.first()
            val hasTags = registeredTags.isNotEmpty()

            // Strict rule: Under NO circumstances can NFC_ONLY or NFC mode be active without physical tags!
            val effectiveExitBehavior = if (!hasTags && rawProfile.exitBehavior == ExitBehavior.NFC_ONLY) {
                ExitBehavior.ALLOW_EMERGENCY
            } else {
                rawProfile.exitBehavior
            }

            val effectiveDuration = if (!hasTags && (rawProfile.durationMinutes == null || rawProfile.durationMinutes >= 720)) {
                60
            } else {
                rawProfile.durationMinutes?.coerceIn(1, 720) ?: 60
            }

            val effectiveActivationType = if (!hasTags) "TIME" else rawProfile.activationType

            val profile = rawProfile.copy(
                exitBehavior = effectiveExitBehavior,
                durationMinutes = effectiveDuration,
                activationType = effectiveActivationType,
                blockedPackages = rawProfile.blockedPackages.filter {
                    it != context.packageName && it != "xyz.umarmohammad.hermit"
                },
                zenAllowedPackages = rawProfile.zenAllowedPackages.distinct().take(5)
            )

            // Persist the sanitized profile if it was incorrectly configured for NFC without tags
            if (!hasTags && (rawProfile.exitBehavior == ExitBehavior.NFC_ONLY || rawProfile.activationType == "NFC")) {
                profileRepository.saveProfile(profile)
            }

            val now = System.currentTimeMillis()
            val sessionId = UUID.randomUUID().toString()

            val plannedMinutes = profile.durationMinutes?.coerceAtMost(720)
            val session = FocusSessionRecord(
                id = sessionId,
                profileId = profile.id,
                profileName = profile.name,
                startedAt = now,
                plannedDurationMinutes = plannedMinutes,
                endReason = "ACTIVE"
            )
            val startedWithNfc = reason == "NFC_TAG_TOGGLED" || reason.startsWith("NFC")
            preferenceManager.setActiveSession(
                sessionId = sessionId,
                profileId = profile.id,
                startTime = now,
                durationMinutes = plannedMinutes,
                startedWithNfc = startedWithNfc
            )

            _sessionState.value = FocusSessionState(
                isActive = true,
                sessionId = sessionId,
                activeProfile = profile,
                startedAt = now,
                plannedDurationMinutes = plannedMinutes,
                remainingSeconds = plannedMinutes?.let { it * 60L },
                elapsedSeconds = 0,
                blockedAttemptsCount = 0,
                startedWithNfc = startedWithNfc
            )
            if (profile.enableDnd) {
                xyz.umarmohammad.hermit.util.DndManager.enableDnd(context)
            }
            if (profile.enableGrayscale) {
                xyz.umarmohammad.hermit.util.GrayscaleManager.enableSystemGrayscale(context)
            }
            unlockCount = 0
            blockedAppCounts.clear()
            registerUnlockReceiver()
            startTicker()
        }
    }

    fun endFocus(endReason: String = "MANUAL_EXIT") {
        scope.launch {
            val current = _sessionState.value
            timerJob?.cancel()
            timerJob = null

            if (current.isActive && current.sessionId != null) {
                sessionRepository.endSession(
                    sessionId = current.sessionId,
                    endReason = endReason,
                    blockedAttempts = current.blockedAttemptsCount
                )
            }
            xyz.umarmohammad.hermit.util.DndManager.restoreDnd(context)
            xyz.umarmohammad.hermit.util.GrayscaleManager.restoreSystemGrayscale(context)
            unregisterUnlockReceiver()
            preferenceManager.clearActiveSession()
            _sessionState.value = FocusSessionState()
        }
    }

    fun refreshActiveProfile() {
        scope.launch {
            val current = _sessionState.value
            val profId = current.activeProfile?.id ?: return@launch
            val updated = profileRepository.getProfileById(profId) ?: return@launch
            if (updated.enableDnd && current.isActive) {
                xyz.umarmohammad.hermit.util.DndManager.enableDnd(context)
            } else if (!updated.enableDnd) {
                xyz.umarmohammad.hermit.util.DndManager.restoreDnd(context)
            }

            if (updated.enableGrayscale && current.isActive) {
                xyz.umarmohammad.hermit.util.GrayscaleManager.enableSystemGrayscale(context)
            } else if (!updated.enableGrayscale) {
                xyz.umarmohammad.hermit.util.GrayscaleManager.restoreSystemGrayscale(context)
            }
            _sessionState.update { it.copy(activeProfile = updated) }
        }
    }

    suspend fun handleTagScan(payload: HermitTagPayload, physicalUid: String): TagScanOutcome {
        val now = System.currentTimeMillis()

        // 1. Debounce rapid scans to avoid accidental bounce triggers
        val lastTagId = preferenceManager.activeSessionId.first()
        // Check local tag record in database
        val registeredTag = tagRepository.getTagById(payload.id)
            ?: tagRepository.getTagByPhysicalUid(physicalUid)

        if (registeredTag != null && registeredTag.isDisabled) {
            return TagScanOutcome.DISABLED_TAG
        }

        // Mark tag last scanned
        tagRepository.markScanned(payload.id)
        preferenceManager.updateLastScannedTag(payload.id, now)

        val current = _sessionState.value

        // Physical NFC tags serve exclusively as on/off toggles
        // for whichever profile is currently selected in the app under Enfoque.
        return if (current.isActive) {
            val now = System.currentTimeMillis()
            val durationSeconds = ((now - current.startedAt) / 1000).coerceAtLeast(0)
            val analysis = ScreenTimeManager.getSessionStatsAnalysis(context, current.startedAt, now)
            val effectiveUnlocks = maxOf(unlockCount, analysis.unlockCount)

            val mergedAppCounts = mutableMapOf<String, Int>()
            for ((pkg, count) in blockedAppCounts) {
                mergedAppCounts[pkg] = count
            }
            for ((pkg, count) in analysis.topAppViews) {
                mergedAppCounts[pkg] = maxOf(mergedAppCounts[pkg] ?: 0, count)
            }

            val topApps = mergedAppCounts.entries
                .sortedByDescending { it.value }
                .take(4)
                .map { entry ->
                    AppSessionStat(
                        packageName = entry.key,
                        appName = ScreenTimeManager.resolveAppName(context, entry.key),
                        count = entry.value
                    )
                }

            val summary = SessionCompletionSummary(
                profileName = current.activeProfile?.name ?: "Enfoque",
                durationSeconds = durationSeconds,
                unlockCount = effectiveUnlocks,
                totalBlockedCount = current.blockedAttemptsCount,
                topApps = topApps,
                endedViaNfc = true
            )
            _lastCompletedSessionSummary.value = summary

            endFocus("NFC_TAG_TOGGLED")
            TagScanOutcome.STOPPED
        } else if (_pendingCountdown.value != null) {
            cancelCountdown()
            TagScanOutcome.STOPPED
        } else {
            val profileToStartId = registeredTag?.targetProfileId
                ?: preferenceManager.selectedQuickProfileId.first()
                ?: profileRepository.allProfiles.first().firstOrNull()?.id

            if (profileToStartId != null) {
                val profile = profileRepository.getProfileById(profileToStartId)
                if (profile != null) {
                    startCountdown(profile, "NFC_TAG_TOGGLED")
                    TagScanOutcome.STARTED
                } else {
                    TagScanOutcome.UNKNOWN_PROFILE
                }
            } else {
                TagScanOutcome.UNKNOWN_PROFILE
            }
        }
    }

    fun recordBlockedAttempt(packageName: String) {
        blockedAppCounts[packageName] = (blockedAppCounts[packageName] ?: 0) + 1
        scope.launch {
            preferenceManager.incrementBlockedCount()
            _sessionState.update { current ->
                current.copy(blockedAttemptsCount = current.blockedAttemptsCount + 1)
            }
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val current = _sessionState.value
                if (!current.isActive) break

                val now = System.currentTimeMillis()
                val elapsed = ((now - current.startedAt) / 1000).coerceAtLeast(0)

                // 12-hour (43200s) internal safety cutoff
                val maxSafetySec = 12 * 3600L
                if (elapsed >= maxSafetySec) {
                    endFocus("SAFETY_12H_TIMEOUT")
                    break
                }

                val remaining = current.plannedDurationMinutes?.let { durMin ->
                    val totalSec = durMin.coerceAtMost(720) * 60L
                    val rem = totalSec - elapsed
                    if (rem <= 0) {
                        // Auto end expired session
                        endFocus("TIMED_COMPLETION")
                        0L
                    } else {
                        rem
                    }
                }

                if (_sessionState.value.isActive) {
                    _sessionState.update {
                        it.copy(
                            elapsedSeconds = elapsed,
                            remainingSeconds = remaining
                        )
                    }
                }
            }
        }
    }
}
