package xyz.umarmohammad.hermit.ui.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.HermitApplication
import xyz.umarmohammad.hermit.nfc.NfcTagManager
import xyz.umarmohammad.hermit.session.SessionManager
import xyz.umarmohammad.hermit.session.TagScanOutcome
import xyz.umarmohammad.hermit.ui.theme.HermitTheme

class BlockOverlayActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private lateinit var sessionManager: SessionManager
    private var nfcAdapter: NfcAdapter? = null

    private var blockedAppName by mutableStateOf("Application")
    private var blockedAppIcon by mutableStateOf<Drawable?>(null)
    private var profileName by mutableStateOf("Focus Mode")
    private var strictCountdownRemaining by mutableStateOf<Int?>(null)
    private var countdownJob: Job? = null

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_PROFILE_NAME = "extra_profile_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager.getInstance(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val blockedPkg = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Focus Profile"

        // Resolve friendly application name and real launcher icon
        resolveBlockedAppDetails(blockedPkg)

        // Prevent bypassing via Back gesture: redirect safely to home
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToHome()
            }
        })

        // Observe session state: finish overlay if session ends
        lifecycleScope.launch {
            sessionManager.sessionState.collect { state ->
                if (!state.isActive) {
                    finish()
                }
            }
        }

        setContent {
            val app = application as HermitApplication
            val prefManager = app.preferenceManager
            val themeMode by prefManager.themeMode.collectAsState(initial = "system")
            val themePalette by prefManager.themePalette.collectAsState(initial = "forest")
            val localeCode by prefManager.appLocale.collectAsState(initial = "system")

            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val baseContext = LocalContext.current
            val baseConfig = LocalConfiguration.current

            val localizedContext = remember(baseContext, localeCode) {
                if (localeCode == "en" || localeCode == "es") {
                    val locale = java.util.Locale(localeCode)
                    val config = android.content.res.Configuration(baseContext.resources.configuration)
                    config.setLocale(locale)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        config.setLocales(android.os.LocaleList(locale))
                    }
                    baseContext.createConfigurationContext(config)
                } else {
                    baseContext
                }
            }

            val localizedConfig = remember(baseConfig, localeCode) {
                if (localeCode == "en" || localeCode == "es") {
                    val locale = java.util.Locale(localeCode)
                    android.content.res.Configuration(baseConfig).apply {
                        setLocale(locale)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            setLocales(android.os.LocaleList(locale))
                        }
                    }
                } else {
                    baseConfig
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig
            ) {
                HermitTheme(darkTheme = isDark, palette = themePalette) {
                    val state by sessionManager.sessionState.collectAsState()

                    val isEmergencyAllowed = !state.startedWithNfc

                    BlockOverlayScreen(
                        blockedAppName = blockedAppName,
                        blockedAppIcon = blockedAppIcon,
                        profileName = state.activeProfile?.name ?: profileName,
                        remainingSeconds = state.remainingSeconds,
                        strictCountdownRemaining = strictCountdownRemaining,
                        allowEmergencyExit = isEmergencyAllowed,
                        onGoHome = { navigateToHome() },
                        onEmergencyExit = { handleEmergencyExit(state.activeProfile?.strictMode == true) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        val payload = NfcTagManager.readHermitPayload(tag) ?: return
        val uid = NfcTagManager.bytesToHex(tag.id)

        lifecycleScope.launch {
            val outcome = sessionManager.handleTagScan(payload, uid)
            if (outcome == TagScanOutcome.STOPPED || outcome == TagScanOutcome.SWITCHED) {
                triggerHapticFeedback()
                if (outcome == TagScanOutcome.STOPPED) {
                    val intent = Intent(this@BlockOverlayActivity, xyz.umarmohammad.hermit.ui.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
                finish()
            }
        }
    }

    private fun resolveBlockedAppDetails(packageName: String) {
        if (packageName.isBlank()) return
        try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            blockedAppName = packageManager.getApplicationLabel(appInfo).toString()
            blockedAppIcon = packageManager.getApplicationIcon(appInfo)
        } catch (_: Exception) {
            blockedAppName = packageName.substringAfterLast(".")
        }
    }

    private fun handleEmergencyExit(isStrictMode: Boolean) {
        val currentProfile = sessionManager.sessionState.value.activeProfile
        if (currentProfile?.exitBehavior != xyz.umarmohammad.hermit.data.model.ExitBehavior.ALLOW_EMERGENCY) {
            return
        }

        // Require 60 seconds reflection pause in strict mode
        if (isStrictMode) {
            if (countdownJob != null && countdownJob?.isActive == true) return
            strictCountdownRemaining = 60
            countdownJob = lifecycleScope.launch {
                for (i in 60 downTo 1) {
                    strictCountdownRemaining = i
                    delay(1000L)
                }
                strictCountdownRemaining = null
                sessionManager.endFocus("EMERGENCY_OVERRIDE_STRICT")
                finish()
            }
        } else {
            sessionManager.endFocus("EMERGENCY_OVERRIDE")
            finish()
        }
    }

    private fun navigateToHome() {
        val isZenActive = sessionManager.sessionState.value.activeProfile?.isMonkMode == true
        if (isZenActive) {
            val zenIntent = Intent(this, xyz.umarmohammad.hermit.ui.launcher.ZenLauncherActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(zenIntent)
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
        finish()
    }

    private fun triggerHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80)
        }
    }
}
