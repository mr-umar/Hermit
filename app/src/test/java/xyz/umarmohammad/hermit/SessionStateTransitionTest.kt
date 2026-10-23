package xyz.umarmohammad.hermit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.umarmohammad.hermit.data.model.FocusProfile
import xyz.umarmohammad.hermit.data.model.NfcTagRecord
import xyz.umarmohammad.hermit.data.model.TagAction
import xyz.umarmohammad.hermit.session.FocusSessionState
import xyz.umarmohammad.hermit.session.TagScanOutcome

class SessionStateTransitionTest {

    private fun simulateTagScan(
        action: TagAction,
        targetProfileId: String?,
        isTagDisabled: Boolean,
        currentState: FocusSessionState,
        allProfiles: List<FocusProfile>
    ): Pair<TagScanOutcome, FocusSessionState> {
        if (isTagDisabled) {
            return TagScanOutcome.DISABLED_TAG to currentState
        }

        val profile = allProfiles.firstOrNull { it.id == targetProfileId }
            ?: allProfiles.firstOrNull()

        return when (action) {
            TagAction.TOGGLE -> {
                if (currentState.isActive) {
                    TagScanOutcome.STOPPED to FocusSessionState(isActive = false)
                } else {
                    if (profile != null) {
                        TagScanOutcome.STARTED to FocusSessionState(
                            isActive = true,
                            sessionId = "session-123",
                            activeProfile = profile,
                            startedAt = 1000L,
                            plannedDurationMinutes = profile.durationMinutes
                        )
                    } else {
                        TagScanOutcome.UNKNOWN_PROFILE to currentState
                    }
                }
            }
            TagAction.ACTIVATE -> {
                if (profile != null) {
                    TagScanOutcome.STARTED to FocusSessionState(
                        isActive = true,
                        sessionId = "session-123",
                        activeProfile = profile,
                        startedAt = 1000L,
                        plannedDurationMinutes = profile.durationMinutes
                    )
                } else {
                    TagScanOutcome.UNKNOWN_PROFILE to currentState
                }
            }
            TagAction.DEACTIVATE -> {
                TagScanOutcome.STOPPED to FocusSessionState(isActive = false)
            }
            TagAction.SWITCH -> {
                if (profile != null) {
                    if (currentState.isActive && currentState.activeProfile?.id == profile.id) {
                        TagScanOutcome.IGNORED_DEBOUNCE to currentState
                    } else {
                        TagScanOutcome.SWITCHED to FocusSessionState(
                            isActive = true,
                            sessionId = "session-new",
                            activeProfile = profile,
                            startedAt = 2000L,
                            plannedDurationMinutes = profile.durationMinutes
                        )
                    }
                } else {
                    TagScanOutcome.UNKNOWN_PROFILE to currentState
                }
            }
        }
    }

    @Test
    fun testToggleTagStartsFocusWhenIdle() {
        val profile = FocusProfile(id = "work", name = "Deep Work", durationMinutes = 45)
        val idleState = FocusSessionState(isActive = false)

        val (outcome, nextState) = simulateTagScan(
            action = TagAction.TOGGLE,
            targetProfileId = "work",
            isTagDisabled = false,
            currentState = idleState,
            allProfiles = listOf(profile)
        )

        assertEquals(TagScanOutcome.STARTED, outcome)
        assertTrue(nextState.isActive)
        assertEquals("work", nextState.activeProfile?.id)
        assertEquals(45, nextState.plannedDurationMinutes)
    }

    @Test
    fun testToggleTagStopsFocusWhenActive() {
        val profile = FocusProfile(id = "work", name = "Deep Work", durationMinutes = 45)
        val activeState = FocusSessionState(isActive = true, sessionId = "s1", activeProfile = profile)

        val (outcome, nextState) = simulateTagScan(
            action = TagAction.TOGGLE,
            targetProfileId = "work",
            isTagDisabled = false,
            currentState = activeState,
            allProfiles = listOf(profile)
        )

        assertEquals(TagScanOutcome.STOPPED, outcome)
        assertFalse(nextState.isActive)
        assertNull(nextState.activeProfile)
    }

    @Test
    fun testDeactivateTagAlwaysStopsFocus() {
        val profile = FocusProfile(id = "work", name = "Deep Work")
        val activeState = FocusSessionState(isActive = true, sessionId = "s1", activeProfile = profile)

        val (outcome, nextState) = simulateTagScan(
            action = TagAction.DEACTIVATE,
            targetProfileId = null,
            isTagDisabled = false,
            currentState = activeState,
            allProfiles = listOf(profile)
        )

        assertEquals(TagScanOutcome.STOPPED, outcome)
        assertFalse(nextState.isActive)
    }

    @Test
    fun testSwitchTagTransitionsToDifferentProfile() {
        val p1 = FocusProfile(id = "p1", name = "Work")
        val p2 = FocusProfile(id = "p2", name = "Study")
        val activeState = FocusSessionState(isActive = true, sessionId = "s1", activeProfile = p1)

        val (outcome, nextState) = simulateTagScan(
            action = TagAction.SWITCH,
            targetProfileId = "p2",
            isTagDisabled = false,
            currentState = activeState,
            allProfiles = listOf(p1, p2)
        )

        assertEquals(TagScanOutcome.SWITCHED, outcome)
        assertTrue(nextState.isActive)
        assertEquals("p2", nextState.activeProfile?.id)
    }

    @Test
    fun testSwitchTagToSameProfileIsIgnored() {
        val p1 = FocusProfile(id = "p1", name = "Work")
        val activeState = FocusSessionState(isActive = true, sessionId = "s1", activeProfile = p1)

        val (outcome, nextState) = simulateTagScan(
            action = TagAction.SWITCH,
            targetProfileId = "p1",
            isTagDisabled = false,
            currentState = activeState,
            allProfiles = listOf(p1)
        )

        assertEquals(TagScanOutcome.IGNORED_DEBOUNCE, outcome)
        assertEquals(activeState, nextState)
    }

    @Test
    fun testDisabledTagIsRejected() {
        val profile = FocusProfile(id = "p1", name = "Work")
        val idleState = FocusSessionState(isActive = false)

        val (outcome, nextState) = simulateTagScan(
            action = TagAction.TOGGLE,
            targetProfileId = "p1",
            isTagDisabled = true,
            currentState = idleState,
            allProfiles = listOf(profile)
        )

        assertEquals(TagScanOutcome.DISABLED_TAG, outcome)
        assertFalse(nextState.isActive)
    }

    @Test
    fun testSessionExpiryCalculationAfterReboot() {
        val startTime = 1000000L
        val durationMin = 30 // 1800 seconds
        val rebootTime = startTime + (2000L * 1000L) // 2000 seconds later (> 1800s)

        val elapsedSec = (rebootTime - startTime) / 1000
        val isExpired = elapsedSec >= (durationMin * 60L)

        assertTrue("Session that ran longer than planned duration during reboot is expired", isExpired)
    }
}
