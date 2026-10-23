package xyz.umarmohammad.hermit.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.HermitApplication
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.session.SessionManager
import xyz.umarmohammad.hermit.ui.blocking.BlockOverlayActivity

/**
 * High-performance AccessibilityService that enforces active focus profiles, Monk/Zen mode
 * whitelist restrictions, browser social media blocks, and friction barriers against impulsive
 * uninstallation during active sessions.
 *
 * Engineered with zero-cost idle exits, in-memory O(1) package caching, and throttled inspection
 * to ensure virtually zero CPU, memory, and battery overhead.
 */
class HermitAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var sessionManager: SessionManager

    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L

    // Debounce timestamps to throttle expensive AccessibilityNodeInfo tree traversals
    private var lastBrowserCheckTimestamp = 0L
    private var lastTamperCheckTimestamp = 0L

    // In-memory cache for exempt system packages, keyboards, and dialer
    @Volatile
    private var cachedCoreEssentialPackages: Set<String> = emptySet()
    @Volatile
    private var cachedLauncherPackages: Set<String> = emptySet()
    private var lastExemptCacheTimestamp: Long = 0L

    // In-memory cache for default camera handler
    @Volatile
    private var cachedCameraPackage: String? = null
    private var lastCameraCacheTimestamp: Long = 0L

    companion object {
        private const val BLOCK_DEBOUNCE_WINDOW_MS = 1200L
        private const val NODE_CHECK_THROTTLE_MS = 600L
        private const val CACHE_TTL_MS = 180_000L // 3 minutes

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

        // Base Zen Mode Allowed packages: Phone, Camera, Spotify, YT Music
        private val BASE_ZEN_PACKAGES = setOf(
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
            "com.spotify.music",
            "com.google.android.apps.youtube.music"
        )

        // Web Browsers monitored for social media URL domain blocking
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
        refreshExemptPackages()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. FAST ZERO-COST GUARD: If focus session is NOT active, Hermit does ZERO work.
        val state = sessionManager.sessionState.value
        if (!state.isActive || state.activeProfile == null) {
            return
        }

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val rawPkg = event.packageName?.toString() ?: return

        // 2. Never intercept Hermit itself
        if (rawPkg == packageName || rawPkg == "xyz.umarmohammad.hermit") {
            return
        }

        // 3. Fast app-in-foreground check using memory flag (ZERO IPC overhead)
        val app = applicationContext as? HermitApplication
        if (app?.isAppInForeground == true && (rawPkg == packageName || rawPkg == "xyz.umarmohammad.hermit")) {
            return
        }

        val now = System.currentTimeMillis()

        // 4. Uninstall and Tamper Protection during active focus session
        val isSettingsOrInstaller = rawPkg == "com.android.settings" ||
                rawPkg == "com.google.android.packageinstaller" ||
                rawPkg == "com.android.packageinstaller" ||
                rawPkg == "com.google.android.permissioncontroller"

        if (isSettingsOrInstaller) {
            // Throttle tamper text inspections to at most once per 600ms
            if ((now - lastTamperCheckTimestamp) > NODE_CHECK_THROTTLE_MS) {
                lastTamperCheckTimestamp = now
                if (isHermitUninstallScreenDetected()) {
                    if ((now - lastBlockTimestamp) > (BLOCK_DEBOUNCE_WINDOW_MS * 2)) {
                        lastBlockTimestamp = now
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        showToast(getString(R.string.uninstall_intercept_toast))
                    }
                    return
                }
            }
        }

        // 5. Fast Core Essential Check (Hermit, Keyboards, SystemUI, Dialer)
        if (isCoreEssentialPackage(rawPkg)) return

        val profile = state.activeProfile

        // 6. Social Web Domain Blocking inside Web Browsers
        if (profile.blockWebSocial && BROWSER_PACKAGES.contains(rawPkg)) {
            // Throttle browser node inspections to at most once per 600ms
            if ((now - lastBrowserCheckTimestamp) > NODE_CHECK_THROTTLE_MS) {
                lastBrowserCheckTimestamp = now
                if (isSocialWebDetected()) {
                    if (rawPkg == lastBlockedPackage && (now - lastBlockTimestamp) < (BLOCK_DEBOUNCE_WINDOW_MS * 2)) {
                        return
                    }
                    lastBlockedPackage = rawPkg
                    lastBlockTimestamp = now
                    sessionManager.recordBlockedAttempt(rawPkg)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    triggerBlockExperience(rawPkg, profile.name)
                    return
                }
            }
        }

        // 7. Standard & Zen App Blocking ONLY executes on TYPE_WINDOW_STATE_CHANGED
        // (Content changes for normal apps must be discarded immediately without any work)
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // If Hermit is currently in foreground, don't trigger blocks
        if (app?.isAppInForeground == true) {
            return
        }

        // 8. Zen / Monk Mode Check: Redirect system home to ZenLauncher, or block non-whitelisted apps
        if (profile.isMonkMode || profile.name.equals("Monk", ignoreCase = true) || profile.name.equals("Zen", ignoreCase = true)) {
            // When user navigates to regular home launcher, automatically show Zen Launcher
            if (isSystemLauncher(rawPkg) && rawPkg != packageName) {
                if ((now - lastBlockTimestamp) > BLOCK_DEBOUNCE_WINDOW_MS) {
                    lastBlockTimestamp = now
                    val intent = Intent(this, xyz.umarmohammad.hermit.ui.launcher.ZenLauncherActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
                return
            }

            // Verify against Zen whitelist: Phone, Camera, Spotify, YT Music, and up to 5 optional apps
            if (!isZenAllowed(rawPkg, profile.zenAllowedPackages)) {
                if (rawPkg == lastBlockedPackage && (now - lastBlockTimestamp) < BLOCK_DEBOUNCE_WINDOW_MS) {
                    return
                }
                lastBlockedPackage = rawPkg
                lastBlockTimestamp = now
                sessionManager.recordBlockedAttempt(rawPkg)
                triggerBlockExperience(rawPkg, profile.name)
                return
            }
            return
        }

        // 9. Standard Focus Profile: Home launchers are permitted
        if (isSystemLauncher(rawPkg)) return

        // Standard App Blocking Check
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
    }

    private fun isHermitUninstallScreenDetected(): Boolean {
        return try {
            val rootNode = rootInActiveWindow ?: return false
            val textList = mutableListOf<String>()
            collectNodeTexts(rootNode, textList, depth = 0, maxDepth = 5)
            val combined = textList.joinToString(" ").lowercase()

            val hasHermitIdentifier = combined.contains("xyz.umarmohammad.hermit") ||
                    (combined.contains("hermit") && !combined.contains("hermitage"))

            val hasTamperIntent = combined.contains("desinstalar") ||
                    combined.contains("uninstall") ||
                    combined.contains("forzar detención") ||
                    combined.contains("force stop") ||
                    combined.contains("desactivar") ||
                    combined.contains("deactivate") ||
                    combined.contains("eliminar")

            hasHermitIdentifier && hasTamperIntent
        } catch (_: Exception) {
            false
        }
    }

    private fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
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
        if (node == null || depth > maxDepth || outList.size > 20) return
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

    private fun refreshExemptPackages() {
        try {
            val essentials = mutableSetOf<String>()
            essentials.addAll(ESSENTIAL_SYSTEM_PACKAGES)
            essentials.add(packageName)
            essentials.add("xyz.umarmohammad.hermit")

            // 1. Keyboards (Input Method Editors) - never block keyboards
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.enabledInputMethodList?.forEach { essentials.add(it.packageName) }

            // 2. Default dialer
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.defaultDialerPackage?.let { essentials.add(it) }

            cachedCoreEssentialPackages = essentials

            // 3. Home launchers (tracked separately so Zen mode can intercept them)
            val launchers = mutableSetOf<String>()
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val homeResolves = packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)
            homeResolves.forEach { launchers.add(it.activityInfo.packageName) }
            cachedLauncherPackages = launchers

            lastExemptCacheTimestamp = System.currentTimeMillis()
        } catch (_: Exception) {
            // Fallback: preserve existing cached set
        }
    }

    private fun isCoreEssentialPackage(pkg: String): Boolean {
        if (pkg == packageName || pkg == "xyz.umarmohammad.hermit") return true
        if (ESSENTIAL_SYSTEM_PACKAGES.contains(pkg)) return true

        val now = System.currentTimeMillis()
        if (cachedCoreEssentialPackages.isEmpty() || (now - lastExemptCacheTimestamp) > CACHE_TTL_MS) {
            refreshExemptPackages()
        }
        return cachedCoreEssentialPackages.contains(pkg)
    }

    private fun isSystemLauncher(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        if (cachedLauncherPackages.isEmpty() || (now - lastExemptCacheTimestamp) > CACHE_TTL_MS) {
            refreshExemptPackages()
        }
        return cachedLauncherPackages.contains(pkg)
    }

    private fun isZenAllowed(pkg: String, optionalZenApps: List<String>): Boolean {
        if (BASE_ZEN_PACKAGES.contains(pkg)) return true
        if (isCoreEssentialPackage(pkg)) return true
        if (optionalZenApps.take(5).contains(pkg)) return true

        val now = System.currentTimeMillis()
        if (cachedCameraPackage == null || (now - lastCameraCacheTimestamp) > CACHE_TTL_MS) {
            try {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.resolveActivity(cameraIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.resolveActivity(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
                }
                cachedCameraPackage = resolveInfo?.activityInfo?.packageName
                lastCameraCacheTimestamp = now
            } catch (_: Exception) {}
        }

        return pkg == cachedCameraPackage
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

    override fun onInterrupt() {
        _isServiceRunning.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
    }
}
