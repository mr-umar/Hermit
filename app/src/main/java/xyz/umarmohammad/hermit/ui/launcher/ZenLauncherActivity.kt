package xyz.umarmohammad.hermit.ui.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.umarmohammad.hermit.HermitApplication
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.nfc.NfcTagManager
import xyz.umarmohammad.hermit.session.SessionManager
import xyz.umarmohammad.hermit.session.TagScanOutcome
import xyz.umarmohammad.hermit.ui.MainActivity
import xyz.umarmohammad.hermit.ui.theme.HermitTheme
import xyz.umarmohammad.hermit.service.HermitAccessibilityService
import kotlinx.coroutines.flow.first

class ZenLauncherActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private lateinit var sessionManager: SessionManager
    private var nfcAdapter: NfcAdapter? = null
    private var appsList by mutableStateOf<List<ZenLauncherAppItem>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyImmersiveMode()

        sessionManager = SessionManager.getInstance(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Lock back button during active Zen mode
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val isZenActive = sessionManager.sessionState.value.activeProfile?.isMonkMode == true
                if (!isZenActive) {
                    finish()
                }
            }
        })

        // Observe session state and refresh allowed apps
        lifecycleScope.launch {
            sessionManager.sessionState.collect { state ->
                val prof = state.activeProfile
                if (prof != null) {
                    loadAllowedApps(prof.zenAllowedPackages)
                } else {
                    val app = application as HermitApplication
                    val allProfiles = app.database.focusProfileDao().getAllProfiles().first()
                    val zenProfile = allProfiles.firstOrNull { it.isMonkMode } ?: allProfiles.firstOrNull()
                    loadAllowedApps(zenProfile?.zenAllowedPackages ?: emptyList())
                }
            }
        }

        setContent {
            HermitTheme(darkTheme = true) {
                val state by sessionManager.sessionState.collectAsState()
                val isZenActive = state.isActive && state.activeProfile?.isMonkMode == true
                val isNfcExit = state.startedWithNfc

                ZenLauncherScreen(
                    isZenActive = isZenActive,
                    isNfcExit = isNfcExit,
                    profileName = state.activeProfile?.name ?: "Zen",
                    remainingSeconds = state.remainingSeconds,
                    apps = appsList,
                    onLaunchApp = { pkg -> launchTargetApp(pkg) },
                    onOpenHermit = { openHermitApp() },
                    onExitLauncher = { exitToSystemHome() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
        val profile = sessionManager.sessionState.value.activeProfile
        if (profile != null) {
            if (profile.enableDnd) {
                xyz.umarmohammad.hermit.util.DndManager.enableDnd(this)
            }
            if (profile.enableGrayscale) {
                xyz.umarmohammad.hermit.util.GrayscaleManager.applyWindowGrayscale(window, true)
                xyz.umarmohammad.hermit.util.GrayscaleManager.enableSystemGrayscale(this)
            } else {
                xyz.umarmohammad.hermit.util.GrayscaleManager.applyWindowGrayscale(window, false)
            }
            loadAllowedApps(profile.zenAllowedPackages)
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val app = application as HermitApplication
                val allProfiles = app.database.focusProfileDao().getAllProfiles().first()
                val zenProfile = allProfiles.firstOrNull { it.isMonkMode } ?: allProfiles.firstOrNull()
                withContext(Dispatchers.Main) {
                    if (zenProfile?.enableDnd == true) {
                        xyz.umarmohammad.hermit.util.DndManager.enableDnd(this@ZenLauncherActivity)
                    }
                    if (zenProfile?.enableGrayscale == true) {
                        xyz.umarmohammad.hermit.util.GrayscaleManager.applyWindowGrayscale(window, true)
                        xyz.umarmohammad.hermit.util.GrayscaleManager.enableSystemGrayscale(this@ZenLauncherActivity)
                    } else {
                        xyz.umarmohammad.hermit.util.GrayscaleManager.applyWindowGrayscale(window, false)
                    }
                }
                loadAllowedApps(zenProfile?.zenAllowedPackages ?: emptyList())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val state = sessionManager.sessionState.value
        val isZenActive = state.isActive && (state.activeProfile?.isMonkMode == true ||
                state.activeProfile?.name?.equals("Zen", true) == true ||
                state.activeProfile?.name?.equals("Monk", true) == true)

        if (!hasFocus && isZenActive) {
            // Dismiss notification panel / status bar to prevent opening quick settings
            HermitAccessibilityService.dismissNotificationShade()
            collapseStatusBarPanels()
        } else if (hasFocus) {
            applyImmersiveMode()
        }
    }

    private fun collapseStatusBarPanels() {
        try {
            val statusBarService = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val collapseMethod = statusBarManager.getMethod("collapsePanels")
            collapseMethod.isAccessible = true
            collapseMethod.invoke(statusBarService)
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeIntent)
        } catch (_: Exception) {}
    }

    private fun applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(android.view.WindowInsets.Type.statusBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        val payload = NfcTagManager.readHermitPayload(tag) ?: return
        val uid = NfcTagManager.bytesToHex(tag.id)

        lifecycleScope.launch {
            val outcome = sessionManager.handleTagScan(payload, uid)
            if (outcome == TagScanOutcome.STOPPED) {
                triggerHapticFeedback()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ZenLauncherActivity, getString(R.string.toast_scan_stopped), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun loadAllowedApps(optionalPackages: List<String>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = mutableListOf<ZenLauncherAppItem>()
            val pm = packageManager

            // 1. Phone / Dialer
            val dialerPkg = resolveDialerPackage()
            if (dialerPkg != null) {
                val launchIntent = pm.getLaunchIntentForPackage(dialerPkg)
                val ri = if (launchIntent != null) pm.resolveActivity(launchIntent, 0) else null
                val label = ri?.loadLabel(pm)?.toString()
                    ?: try { pm.getApplicationLabel(pm.getApplicationInfo(dialerPkg, 0)).toString() } catch (_: Exception) { "Teléfono" }
                val icon = ri?.loadIcon(pm) ?: try { pm.getApplicationIcon(dialerPkg) } catch (_: Exception) { null }
                list.add(ZenLauncherAppItem(dialerPkg, label, icon, Icons.Default.Phone, isSystemEssential = true))
            }

            // 2. Camera
            val cameraPkg = resolveCameraPackage()
            if (cameraPkg != null && cameraPkg != dialerPkg) {
                val launchIntent = pm.getLaunchIntentForPackage(cameraPkg)
                val ri = if (launchIntent != null) pm.resolveActivity(launchIntent, 0) else null
                val label = ri?.loadLabel(pm)?.toString()
                    ?: try { pm.getApplicationLabel(pm.getApplicationInfo(cameraPkg, 0)).toString() } catch (_: Exception) { "Cámara" }
                val icon = ri?.loadIcon(pm) ?: try { pm.getApplicationIcon(cameraPkg) } catch (_: Exception) { null }
                list.add(ZenLauncherAppItem(cameraPkg, label, icon, Icons.Default.CameraAlt, isSystemEssential = true))
            }

            // 3. Spotify (Only if installed)
            val spotifyPkg = "com.spotify.music"
            val spotifyIntent = pm.getLaunchIntentForPackage(spotifyPkg)
            if (spotifyIntent != null) {
                val ri = pm.resolveActivity(spotifyIntent, 0)
                val label = ri?.loadLabel(pm)?.toString() ?: "Spotify"
                val icon = ri?.loadIcon(pm)
                list.add(ZenLauncherAppItem(spotifyPkg, label, icon))
            }

            // 4. YouTube Music (Only if installed)
            val ytMusicPkg = "com.google.android.apps.youtube.music"
            val ytMusicIntent = pm.getLaunchIntentForPackage(ytMusicPkg)
            if (ytMusicIntent != null) {
                val ri = pm.resolveActivity(ytMusicIntent, 0)
                val label = ri?.loadLabel(pm)?.toString() ?: "YT Music"
                val icon = ri?.loadIcon(pm)
                list.add(ZenLauncherAppItem(ytMusicPkg, label, icon))
            }

            // 5. User selected apps (strictly capped at 5 apps)
            optionalPackages.distinct().take(5).forEach { pkg ->
                if (pkg != dialerPkg && pkg != cameraPkg && pkg != spotifyPkg && pkg != ytMusicPkg && pkg != packageName && pkg != "xyz.umarmohammad.hermit") {
                    var label: String? = null
                    var icon: Drawable? = null

                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        val ri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.resolveActivity(launchIntent, PackageManager.ResolveInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.resolveActivity(launchIntent, 0)
                        }
                        label = ri?.loadLabel(pm)?.toString()
                        icon = ri?.loadIcon(pm)
                    }

                    if (label == null) {
                        try {
                            val intent = Intent(Intent.ACTION_MAIN).setPackage(pkg).addCategory(Intent.CATEGORY_LAUNCHER)
                            val resolves = pm.queryIntentActivities(intent, 0)
                            val ri = resolves.firstOrNull()
                            if (ri != null) {
                                label = ri.loadLabel(pm).toString()
                                icon = ri.loadIcon(pm)
                            }
                        } catch (_: Exception) {}
                    }

                    if (label == null) {
                        try {
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            label = pm.getApplicationLabel(appInfo).toString()
                            icon = pm.getApplicationIcon(appInfo)
                        } catch (_: Exception) {}
                    }

                    if (label != null) {
                        list.add(ZenLauncherAppItem(pkg, label, icon))
                    }
                }
            }

            withContext(Dispatchers.Main) {
                appsList = list
            }
        }
    }

    private fun resolveDialerPackage(): String? {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultDialer = telecomManager?.defaultDialerPackage
        if (!defaultDialer.isNullOrBlank()) return defaultDialer

        val intent = Intent(Intent.ACTION_DIAL)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName ?: "com.google.android.dialer"
    }

    private fun resolveCameraPackage(): String? {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(pkg, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getAppLabel(pkg: String, fallback: String): String {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(pkg, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            fallback
        }
    }

    private fun getAppIcon(pkg: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(pkg)
        } catch (_: Exception) {
            null
        }
    }

    private fun launchTargetApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else if (packageName == resolveDialerPackage()) {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            startActivity(dialIntent)
        } else if (packageName == resolveCameraPackage()) {
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            startActivity(cameraIntent)
        } else {
            Toast.makeText(this, "No se pudo abrir la aplicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openHermitApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun exitToSystemHome() {
        val isStillDndActive = sessionManager.sessionState.value.isActive && sessionManager.sessionState.value.activeProfile?.enableDnd == true
        if (!isStillDndActive) {
            xyz.umarmohammad.hermit.util.DndManager.restoreDnd(this)
        }
        val isStillGrayscaleActive = sessionManager.sessionState.value.isActive && sessionManager.sessionState.value.activeProfile?.enableGrayscale == true
        if (!isStillGrayscaleActive) {
            xyz.umarmohammad.hermit.util.GrayscaleManager.restoreSystemGrayscale(this)
        }
        xyz.umarmohammad.hermit.util.GrayscaleManager.applyWindowGrayscale(window, false)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun triggerHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(70)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val isStillDndActive = sessionManager.sessionState.value.isActive && sessionManager.sessionState.value.activeProfile?.enableDnd == true
        if (!isStillDndActive) {
            xyz.umarmohammad.hermit.util.DndManager.restoreDnd(this)
        }
        val isStillGrayscaleActive = sessionManager.sessionState.value.isActive && sessionManager.sessionState.value.activeProfile?.enableGrayscale == true
        if (!isStillGrayscaleActive) {
            xyz.umarmohammad.hermit.util.GrayscaleManager.restoreSystemGrayscale(this)
        }
        xyz.umarmohammad.hermit.util.GrayscaleManager.applyWindowGrayscale(window, false)
    }
}
