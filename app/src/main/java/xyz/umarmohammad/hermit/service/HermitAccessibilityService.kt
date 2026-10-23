package xyz.umarmohammad.hermit.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.MediaStore
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.umarmohammad.hermit.session.SessionManager
import xyz.umarmohammad.hermit.ui.blocking.BlockOverlayActivity

/**
 * Genuine AccessibilityService that monitors foreground application changes to enforce
 * active focus profiles, Monk mode whitelist restrictions, and browser social media blocks.
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

        // Monk Mode Allowed Essentials Whitelist (Phone, Camera, Essential Communication & Tools)
        private val MONK_ALLOWED_PACKAGES = setOf(
            "xyz.umarmohammad.hermit",
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.samsung.android.dialer",
            "com.google.android.GoogleCamera",
            "com.android.camera",
            "com.android.camera2",
            "com.sec.android.app.camera",
            "com.whatsapp",
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.google.android.apps.maps"
        )

        // Common Android Web Browsers for intercepting social media websites
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.duckduckgo.mobile.android"
        )

        // Social media domains to block inside web browsers
        private val SOCIAL_MEDIA_DOMAINS = listOf(
            "instagram.com",
            "tiktok.com",
            "twitter.com",
            "x.com",
            "facebook.com",
            "reddit.com",
            "threads.net",
            "snapchat.com"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceRunning.value = true
        sessionManager = SessionManager.getInstance(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val rawPkg = event.packageName?.toString() ?: return
        if (isExemptPackage(rawPkg)) return

        val state = sessionManager.sessionState.value
        if (!state.isActive || state.activeProfile == null) return

        val profile = state.activeProfile
        val now = System.currentTimeMillis()

        // 1. Monk Mode Check (Strict Whitelist: Phone, Camera, WhatsApp, Spotify, YT Music, Maps only)
        if (profile.isMonkMode || profile.name.equals("Monk", ignoreCase = true)) {
            if (!isMonkAllowed(rawPkg)) {
                if (rawPkg == lastBlockedPackage && (now - lastBlockTimestamp) < BLOCK_DEBOUNCE_WINDOW_MS) {
                    return
                }
                lastBlockedPackage = rawPkg
                lastBlockTimestamp = now
                sessionManager.recordBlockedAttempt(rawPkg)
                triggerBlockExperience(rawPkg, profile.name)
                return
            }
        }

        // 2. Standard App Blocking Check
        if (profile.blockedPackages.contains(rawPkg)) {
            if (rawPkg == lastBlockedPackage && (now - lastBlockTimestamp) < BLOCK_DEBOUNCE_WINDOW_MS) {
                return
            }
            lastBlockedPackage = rawPkg
            lastBlockTimestamp = now
            sessionManager.recordBlockedAttempt(rawPkg)
            triggerBlockExperience(rawPkg, profile.name)
            return
        }

        // 3. Browser Social Media Website Blocking Check
        if (profile.blockWebSocial && BROWSER_PACKAGES.contains(rawPkg)) {
            if (isSocialWebDetected()) {
                if (rawPkg == lastBlockedPackage && (now - lastBlockTimestamp) < (BLOCK_DEBOUNCE_WINDOW_MS * 2)) {
                    return
                }
                lastBlockedPackage = rawPkg
                lastBlockTimestamp = now
                sessionManager.recordBlockedAttempt(rawPkg)
                performGlobalAction(GLOBAL_ACTION_BACK)
                triggerBlockExperience(rawPkg, profile.name)
            }
        }
    }

    private fun isSocialWebDetected(): Boolean {
        return try {
            val rootNode = rootInActiveWindow ?: return false
            val textList = mutableListOf<String>()
            collectNodeTexts(rootNode, textList, depth = 0, maxDepth = 4)
            textList.any { text ->
                val lower = text.lowercase()
                SOCIAL_MEDIA_DOMAINS.any { domain -> lower.contains(domain) }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun collectNodeTexts(node: AccessibilityNodeInfo?, outList: MutableList<String>, depth: Int, maxDepth: Int) {
        if (node == null || depth > maxDepth || outList.size > 25) return
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            outList.add(text)
        }
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) {
            outList.add(desc)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            collectNodeTexts(child, outList, depth + 1, maxDepth)
        }
    }

    private fun isMonkAllowed(pkg: String): Boolean {
        if (MONK_ALLOWED_PACKAGES.contains(pkg)) return true
        if (isExemptPackage(pkg)) return true

        // Allow system camera intent handler
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(cameraIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        if (resolveInfo?.activityInfo?.packageName == pkg) return true

        return false
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
