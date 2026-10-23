package xyz.umarmohammad.hermit.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.session.SessionManager
import xyz.umarmohammad.hermit.ui.blocking.BlockOverlayActivity

/**
 * Genuine AccessibilityService that monitors foreground application changes to enforce
 * active focus profiles.
 *
 * Privacy guarantee:
 * Window content inspection is disabled. Only the top-level package name of foreground
 * windows is checked against the user's explicitly configured block list.
 */
class HermitAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var sessionManager: SessionManager

    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L

    companion object {
        private const val BLOCK_DEBOUNCE_WINDOW_MS = 1200L

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        // Critical system packages that must never be blocked to ensure user safety
        private val ESSENTIAL_SYSTEM_PACKAGES = setOf(
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
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceRunning.value = true
        sessionManager = SessionManager.getInstance(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // We only care about window state changes (foreground app switches)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val rawPkg = event.packageName?.toString() ?: return
        if (isExemptPackage(rawPkg)) return

        val state = sessionManager.sessionState.value
        if (!state.isActive || state.activeProfile == null) return

        val blockedPackages = state.activeProfile.blockedPackages
        if (blockedPackages.contains(rawPkg)) {
            val now = System.currentTimeMillis()
            // Avoid tight trigger loops if the blocking overlay or launcher transition takes a frame
            if (rawPkg == lastBlockedPackage && (now - lastBlockTimestamp) < BLOCK_DEBOUNCE_WINDOW_MS) {
                return
            }

            lastBlockedPackage = rawPkg
            lastBlockTimestamp = now

            // Record blocked attempt for session metrics
            sessionManager.recordBlockedAttempt(rawPkg)

            // Trigger the blocking barrier
            triggerBlockExperience(rawPkg, state.activeProfile.name)
        }
    }

    private fun triggerBlockExperience(blockedPackage: String, profileName: String) {
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra(BlockOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
            putExtra(BlockOverlayActivity.EXTRA_PROFILE_NAME, profileName)
        }
        startActivity(intent)
    }

    private fun isExemptPackage(pkg: String): Boolean {
        if (ESSENTIAL_SYSTEM_PACKAGES.contains(pkg)) return true

        // Check if package is current system home launcher
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(homeIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        if (resolveInfo?.activityInfo?.packageName == pkg) return true

        // Check if package is default dialer
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultDialer = telecomManager?.defaultDialerPackage
        if (defaultDialer == pkg) return true

        return false
    }

    override fun onInterrupt() {
        _isServiceRunning.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
    }
}
