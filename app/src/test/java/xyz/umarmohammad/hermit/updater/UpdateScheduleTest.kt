package xyz.umarmohammad.hermit.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class UpdateScheduleTest {

    @Test
    fun `shouldCheckForUpdates returns true on initial launch when never checked before`() {
        val result = UpdateManager.shouldCheckForUpdates(
            autoCheckEnabled = true,
            lastCheckedTimestamp = 0L,
            lastAttemptTimestamp = 0L,
            currentTime = 1000000L
        )
        assertTrue("Should check on first run when lastChecked is 0", result)
    }

    @Test
    fun `shouldCheckForUpdates returns false if autoCheckEnabled is disabled`() {
        val result = UpdateManager.shouldCheckForUpdates(
            autoCheckEnabled = false,
            lastCheckedTimestamp = 0L,
            currentTime = 1000000L
        )
        assertFalse("Should not check if auto-check preference is false", result)
    }

    @Test
    fun `shouldCheckForUpdates returns false on same calendar day within 24 hours`() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 25, 9, 0, 0) // 9:00 AM
        }
        val morningCheck = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 15) // 3:00 PM same day (6 hours later)
        val afternoon = calendar.timeInMillis

        val result = UpdateManager.shouldCheckForUpdates(
            autoCheckEnabled = true,
            lastCheckedTimestamp = morningCheck,
            lastAttemptTimestamp = morningCheck,
            currentTime = afternoon
        )
        assertFalse("Should not re-check on the same day when less than 24h has elapsed", result)
    }

    @Test
    fun `shouldCheckForUpdates returns true on next calendar day even if less than 24 hours`() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 24, 22, 0, 0) // 10:00 PM yesterday
        }
        val nightCheck = calendar.timeInMillis

        calendar.set(2026, Calendar.OCTOBER, 25, 8, 0, 0) // 8:00 AM today (10 hours later, next day)
        val morning = calendar.timeInMillis

        val result = UpdateManager.shouldCheckForUpdates(
            autoCheckEnabled = true,
            lastCheckedTimestamp = nightCheck,
            lastAttemptTimestamp = 0L,
            currentTime = morning
        )
        assertTrue("Should check on a new day upon opening the app", result)
    }

    @Test
    fun `shouldCheckForUpdates returns true when 24 hours or more have elapsed`() {
        val now = System.currentTimeMillis()
        val twentyFiveHoursAgo = now - (25 * 3600 * 1000L)

        val result = UpdateManager.shouldCheckForUpdates(
            autoCheckEnabled = true,
            lastCheckedTimestamp = twentyFiveHoursAgo,
            lastAttemptTimestamp = 0L,
            currentTime = now
        )
        assertTrue("Should check when >= 24h have elapsed", result)
    }

    @Test
    fun `shouldCheckForUpdates respects retry cooldown after failed attempt`() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 24, 20, 0, 0) // Checked yesterday 8 PM
        }
        val lastChecked = calendar.timeInMillis

        calendar.set(2026, Calendar.OCTOBER, 25, 8, 0, 0) // Next day 8:00 AM (failed attempt)
        val failedAttempt = calendar.timeInMillis

        calendar.set(2026, Calendar.OCTOBER, 25, 8, 10, 0) // Next day 8:10 AM (10 min later)
        val reOpen = calendar.timeInMillis

        val result = UpdateManager.shouldCheckForUpdates(
            autoCheckEnabled = true,
            lastCheckedTimestamp = lastChecked,
            lastAttemptTimestamp = failedAttempt,
            currentTime = reOpen
        )
        assertFalse("Should respect cooldown between rapid attempts when offline", result)

        calendar.set(2026, Calendar.OCTOBER, 25, 9, 30, 0) // Next day 9:30 AM (90 min later)
        val reOpenAfterCooldown = calendar.timeInMillis

        val resultAfterCooldown = UpdateManager.shouldCheckForUpdates(
            autoCheckEnabled = true,
            lastCheckedTimestamp = lastChecked,
            lastAttemptTimestamp = failedAttempt,
            currentTime = reOpenAfterCooldown
        )
        assertTrue("Should allow check once retry cooldown has elapsed", resultAfterCooldown)
    }

    @Test
    fun `isSameCalendarDay accurately compares timestamps`() {
        val cal1 = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 25, 1, 0, 0) }
        val cal2 = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 25, 23, 59, 59) }
        val cal3 = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 26, 0, 0, 1) }

        assertTrue(UpdateManager.isSameCalendarDay(cal1.timeInMillis, cal2.timeInMillis))
        assertFalse(UpdateManager.isSameCalendarDay(cal2.timeInMillis, cal3.timeInMillis))
        assertFalse(UpdateManager.isSameCalendarDay(0L, cal1.timeInMillis))
    }
}
