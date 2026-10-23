package xyz.umarmohammad.hermit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.umarmohammad.hermit.data.model.FocusProfile

class BlockingDecisionTest {

    private val essentialSystemPackages = setOf(
        "xyz.umarmohammad.hermit",
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.phone",
        "com.google.android.dialer"
    )

    private fun shouldBlockPackage(
        targetPackage: String,
        activeProfile: FocusProfile?,
        lastBlockedPackage: String?,
        lastBlockTimestamp: Long,
        currentTime: Long,
        debounceWindowMs: Long = 1200L
    ): Boolean {
        if (essentialSystemPackages.contains(targetPackage)) return false
        if (activeProfile == null) return false
        if (!activeProfile.blockedPackages.contains(targetPackage)) return false

        // Debounce test
        if (targetPackage == lastBlockedPackage && (currentTime - lastBlockTimestamp) < debounceWindowMs) {
            return false
        }

        return true
    }

    @Test
    fun testBlockedAppTriggersBarrierWhenActive() {
        val profile = FocusProfile(
            name = "Work",
            blockedPackages = listOf("com.instagram.android", "com.twitter.android")
        )

        val result = shouldBlockPackage(
            targetPackage = "com.instagram.android",
            activeProfile = profile,
            lastBlockedPackage = null,
            lastBlockTimestamp = 0L,
            currentTime = 10000L
        )

        assertTrue("Instagram should be blocked during active Work profile", result)
    }

    @Test
    fun testUnblockedAppIsAllowed() {
        val profile = FocusProfile(
            name = "Work",
            blockedPackages = listOf("com.instagram.android")
        )

        val result = shouldBlockPackage(
            targetPackage = "com.spotify.music",
            activeProfile = profile,
            lastBlockedPackage = null,
            lastBlockTimestamp = 0L,
            currentTime = 10000L
        )

        assertFalse("Unblocked app should not be intercepted", result)
    }

    @Test
    fun testEssentialSystemPackagesNeverBlockedEvenIfListed() {
        val profile = FocusProfile(
            name = "Overzealous Profile",
            blockedPackages = listOf(
                "xyz.umarmohammad.hermit",
                "com.android.settings",
                "com.android.systemui",
                "com.android.phone"
            )
        )

        for (pkg in profile.blockedPackages) {
            val result = shouldBlockPackage(
                targetPackage = pkg,
                activeProfile = profile,
                lastBlockedPackage = null,
                lastBlockTimestamp = 0L,
                currentTime = 10000L
            )
            assertFalse("Essential package $pkg must never be blocked", result)
        }
    }

    @Test
    fun testDebouncePreventsTightLoopRepeatedTriggers() {
        val profile = FocusProfile(
            name = "Work",
            blockedPackages = listOf("com.instagram.android")
        )

        val firstTrigger = shouldBlockPackage(
            targetPackage = "com.instagram.android",
            activeProfile = profile,
            lastBlockedPackage = null,
            lastBlockTimestamp = 0L,
            currentTime = 1000L
        )
        assertTrue(firstTrigger)

        // Rapid duplicate trigger 300ms later
        val duplicateTrigger = shouldBlockPackage(
            targetPackage = "com.instagram.android",
            activeProfile = profile,
            lastBlockedPackage = "com.instagram.android",
            lastBlockTimestamp = 1000L,
            currentTime = 1300L,
            debounceWindowMs = 1200L
        )
        assertFalse("Duplicate event within 1200ms must be debounced", duplicateTrigger)

        // Trigger after debounce window expires (1500ms later)
        val laterTrigger = shouldBlockPackage(
            targetPackage = "com.instagram.android",
            activeProfile = profile,
            lastBlockedPackage = "com.instagram.android",
            lastBlockTimestamp = 1000L,
            currentTime = 2500L,
            debounceWindowMs = 1200L
        )
        assertTrue("Trigger after debounce window should be permitted", laterTrigger)
    }

    @Test
    fun testSwitchingBetweenDifferentBlockedAppsBypassesDebounce() {
        val profile = FocusProfile(
            name = "Work",
            blockedPackages = listOf("com.instagram.android", "com.twitter.android")
        )

        // Swapping from Instagram to Twitter within 200ms
        val switchTrigger = shouldBlockPackage(
            targetPackage = "com.twitter.android",
            activeProfile = profile,
            lastBlockedPackage = "com.instagram.android",
            lastBlockTimestamp = 1000L,
            currentTime = 1200L,
            debounceWindowMs = 1200L
        )
        assertTrue("Switching to a different blocked app must trigger immediately", switchTrigger)
    }
}
