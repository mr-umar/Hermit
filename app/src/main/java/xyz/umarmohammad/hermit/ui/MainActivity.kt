package xyz.umarmohammad.hermit.ui

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.HermitApplication
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.repository.ProfileRepository
import xyz.umarmohammad.hermit.data.repository.SessionRepository
import xyz.umarmohammad.hermit.data.repository.TagRepository
import xyz.umarmohammad.hermit.nfc.NfcTagManager
import xyz.umarmohammad.hermit.session.TagScanOutcome
import xyz.umarmohammad.hermit.ui.apps.AppSelectionScreen
import xyz.umarmohammad.hermit.ui.home.HomeScreen
import xyz.umarmohammad.hermit.ui.onboarding.OnboardingScreen
import xyz.umarmohammad.hermit.ui.profiles.ProfileEditScreen
import xyz.umarmohammad.hermit.ui.profiles.ProfilesScreen
import xyz.umarmohammad.hermit.ui.settings.SettingsScreen
import xyz.umarmohammad.hermit.ui.stats.StatsScreen
import xyz.umarmohammad.hermit.ui.tags.ReadTagScreen
import xyz.umarmohammad.hermit.ui.tags.TagListScreen
import xyz.umarmohammad.hermit.ui.tags.WriteTagScreen
import xyz.umarmohammad.hermit.ui.theme.HermitTheme

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Focus : Screen("focus", R.string.nav_focus, Icons.Default.NotificationsActive)
    data object Profiles : Screen("profiles", R.string.nav_profiles, Icons.Default.AutoAwesome)
    data object Tags : Screen("tags", R.string.nav_tags, Icons.Default.Nfc)
    data object Stats : Screen("stats", R.string.nav_stats, Icons.Default.Timeline)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var activePhysicalTag by mutableStateOf<Tag?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as HermitApplication
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        handleIncomingNfcIntent(intent)

        setContent {
            val prefManager = app.preferenceManager
            val themeMode by prefManager.themeMode.collectAsState(initial = "system")
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            HermitTheme(darkTheme = isDark) {
                val isFirstLaunch by prefManager.isFirstLaunch.collectAsState(initial = false)

                if (isFirstLaunch) {
                    OnboardingScreen(
                        onFinishOnboarding = {
                            lifecycleScope.launch {
                                prefManager.setFirstLaunchCompleted()
                            }
                        }
                    )
                } else {
                    MainAppScaffold(
                        app = app,
                        activePhysicalTag = activePhysicalTag,
                        onTagHandled = { activePhysicalTag = null }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingNfcIntent(intent)
    }

    private fun handleIncomingNfcIntent(intent: Intent?) {
        if (intent == null) return
        val payload = NfcTagManager.extractHermitPayload(intent) ?: return
        val app = application as HermitApplication
        lifecycleScope.launch {
            val outcome = app.sessionManager.handleTagScan(payload, "INTENT_SCAN")
            triggerHapticFeedback()
            showOutcomeToast(outcome)
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        activePhysicalTag = tag

        // If not actively handled by sub-screens, check for standard Hermit focus toggling
        val payload = NfcTagManager.readHermitPayload(tag)
        if (payload != null) {
            val app = application as HermitApplication
            val uid = NfcTagManager.bytesToHex(tag.id)
            lifecycleScope.launch {
                val outcome = app.sessionManager.handleTagScan(payload, uid)
                triggerHapticFeedback()
                showOutcomeToast(outcome)
            }
        }
    }

    private fun showOutcomeToast(outcome: TagScanOutcome) {
        runOnUiThread {
            when (outcome) {
                TagScanOutcome.STARTED -> Toast.makeText(this, "Focus session started", Toast.LENGTH_SHORT).show()
                TagScanOutcome.STOPPED -> Toast.makeText(this, "Focus session ended", Toast.LENGTH_SHORT).show()
                TagScanOutcome.SWITCHED -> Toast.makeText(this, "Focus profile switched", Toast.LENGTH_SHORT).show()
                TagScanOutcome.DISABLED_TAG -> Toast.makeText(this, "This NFC tag is disabled", Toast.LENGTH_SHORT).show()
                TagScanOutcome.UNKNOWN_PROFILE -> Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                else -> Unit
            }
        }
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
}

@Composable
fun MainAppScaffold(
    app: HermitApplication,
    activePhysicalTag: Tag?,
    onTagHandled: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val profileRepo = remember { ProfileRepository(app.database.focusProfileDao()) }
    val tagRepo = remember { TagRepository(app.database.nfcTagDao()) }
    val sessionRepo = remember { SessionRepository(app.database.sessionRecordDao()) }

    // Staged list of blocked packages for currently editing profile
    val stagedBlockedPackages = remember { mutableStateListOf<String>() }

    val bottomNavItems = listOf(
        Screen.Focus,
        Screen.Profiles,
        Screen.Tags,
        Screen.Stats,
        Screen.Settings
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(imageVector = screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.titleRes)) },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Focus.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Focus.route) {
                HomeScreen(
                    sessionManager = app.sessionManager,
                    profileRepository = profileRepo,
                    tagRepository = tagRepo,
                    onNavigateToTags = { navController.navigate(Screen.Tags.route) }
                )
            }
            composable(Screen.Profiles.route) {
                ProfilesScreen(
                    profileRepository = profileRepo,
                    onEditProfile = { profileId ->
                        navController.navigate("profile_edit/$profileId")
                    },
                    onCreateProfile = {
                        navController.navigate("profile_edit/new")
                    }
                )
            }
            composable("profile_edit/{profileId}") { backStackEntry ->
                val profileId = backStackEntry.arguments?.getString("profileId")

                ProfileEditScreen(
                    profileId = profileId,
                    profileRepository = profileRepo,
                    selectedBlockedPackages = stagedBlockedPackages.toList(),
                    onSelectAppsClick = {
                        navController.navigate("app_selection")
                    },
                    onSaved = { navController.popBackStack() },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("app_selection") {
                AppSelectionScreen(
                    initiallySelectedPackages = stagedBlockedPackages.toList(),
                    onSaveSelection = { updatedList ->
                        stagedBlockedPackages.clear()
                        stagedBlockedPackages.addAll(updatedList)
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Tags.route) {
                TagListScreen(
                    tagRepository = tagRepo,
                    profileRepository = profileRepo,
                    onWriteTagClick = { navController.navigate("write_tag") },
                    onReadTagClick = { navController.navigate("read_tag") }
                )
            }
            composable("write_tag") {
                WriteTagScreen(
                    tagRepository = tagRepo,
                    profileRepository = profileRepo,
                    activePhysicalTag = activePhysicalTag,
                    onTagHandled = onTagHandled,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("read_tag") {
                ReadTagScreen(
                    tagRepository = tagRepo,
                    activePhysicalTag = activePhysicalTag,
                    onTagHandled = onTagHandled,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen(sessionRepository = sessionRepo)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    preferenceManager = app.preferenceManager,
                    database = app.database
                )
            }
        }
    }
}
