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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import xyz.umarmohammad.hermit.ui.components.RevokedPermissionsDialog
import xyz.umarmohammad.hermit.util.RequiredPermissionManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.umarmohammad.hermit.ui.components.TactileButton
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.HermitApplication
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.repository.ProfileRepository
import xyz.umarmohammad.hermit.data.repository.SessionRepository
import xyz.umarmohammad.hermit.data.repository.TagRepository
import xyz.umarmohammad.hermit.nfc.NfcTagManager
import xyz.umarmohammad.hermit.session.TagScanOutcome
import xyz.umarmohammad.hermit.ui.apps.AppSelectionScreen
import xyz.umarmohammad.hermit.ui.components.AppleTabBar
import xyz.umarmohammad.hermit.ui.components.AppleTabItem
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
    data object Focus : Screen("focus", R.string.nav_focus, Icons.Default.HourglassEmpty)
    data object Profiles : Screen("profiles", R.string.nav_profiles, Icons.Default.Shield)
    data object Tags : Screen("tags", R.string.nav_tags, Icons.Default.Nfc)
    data object Stats : Screen("stats", R.string.nav_stats, Icons.Default.BarChart)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

data class UnregisteredTagPrompt(
    val payload: xyz.umarmohammad.hermit.nfc.HermitTagPayload,
    val physicalUid: String,
    val defaultName: String
)

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var activePhysicalTag by mutableStateOf<Tag?>(null)
    private var isNfcIntercepted by mutableStateOf(false)
    private var pendingUnregisteredTag by mutableStateOf<UnregisteredTagPrompt?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as HermitApplication
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        handleIncomingNfcIntent(intent)

        // Lightweight check for updates on launch (if enabled and > 6 hours since last check)
        lifecycleScope.launch(Dispatchers.IO) {
            val autoCheck = app.preferenceManager.autoCheckUpdates.first()
            if (autoCheck) {
                val lastChecked = app.preferenceManager.lastUpdateCheckTimestamp.first()
                val now = System.currentTimeMillis()
                if (now - lastChecked > 6 * 3600 * 1000L) {
                    app.updateManager.checkForUpdates(manual = false)
                }
            }
        }

        setContent {
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
                    val isFirstLaunch by prefManager.isFirstLaunch.collectAsState(initial = false)
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    var requiredPermissionsStatus by remember {
                        mutableStateOf(RequiredPermissionManager.checkPermissions(localizedContext))
                    }

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                requiredPermissionsStatus = RequiredPermissionManager.checkPermissions(localizedContext)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    if (isFirstLaunch) {
                        OnboardingScreen(
                            app = app,
                            activePhysicalTag = activePhysicalTag,
                            onTagHandled = { activePhysicalTag = null },
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
                            onTagHandled = { activePhysicalTag = null },
                            onSetNfcIntercept = { isNfcIntercepted = it },
                            pendingUnregisteredTag = pendingUnregisteredTag,
                            onClearPendingUnregisteredTag = { pendingUnregisteredTag = null }
                        )

                        if (requiredPermissionsStatus.hasRevokedPermissions) {
                            RevokedPermissionsDialog(status = requiredPermissionsStatus)
                        }
                    }
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
        val fallbackPayload = if (intent.data?.scheme == "hermit" || intent.data?.host == "hermit.umarmohammad.xyz") {
            xyz.umarmohammad.hermit.nfc.HermitTagPayload(
                id = intent.data?.getQueryParameter("id") ?: "web-scan-tap",
                action = intent.data?.getQueryParameter("act") ?: "TOGGLE",
                profileId = intent.data?.getQueryParameter("pid")
            )
        } else null

        val payload = NfcTagManager.extractHermitPayload(intent) ?: fallbackPayload ?: return
        val uid = NfcTagManager.extractTagUid(intent)
        val app = application as HermitApplication
        lifecycleScope.launch {
            val registeredTag = app.database.nfcTagDao().getTagById(payload.id)
                ?: app.database.nfcTagDao().getTagByPhysicalUid(uid)

            if (registeredTag == null && !app.sessionManager.sessionState.value.isActive) {
                pendingUnregisteredTag = UnregisteredTagPrompt(
                    payload = payload,
                    physicalUid = uid,
                    defaultName = payload.name ?: getString(R.string.tag_detail_default_name)
                )
            } else {
                val outcome = app.sessionManager.handleTagScan(payload, uid)
                triggerHapticFeedback()
                showOutcomeToast(outcome)
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        activePhysicalTag = tag

        // If a sub-screen (WriteTagScreen or ReadTagScreen) is actively intercepting,
        // do not trigger session toggle!
        if (isNfcIntercepted) {
            return
        }

        val payload = NfcTagManager.readHermitPayload(tag)
        if (payload != null) {
            val app = application as HermitApplication
            val uid = NfcTagManager.bytesToHex(tag.id)
            lifecycleScope.launch {
                val registeredTag = app.database.nfcTagDao().getTagById(payload.id)
                    ?: app.database.nfcTagDao().getTagByPhysicalUid(uid)

                if (registeredTag == null && !app.sessionManager.sessionState.value.isActive) {
                    pendingUnregisteredTag = UnregisteredTagPrompt(
                        payload = payload,
                        physicalUid = uid,
                        defaultName = payload.name ?: getString(R.string.tag_detail_default_name)
                    )
                } else {
                    val outcome = app.sessionManager.handleTagScan(payload, uid)
                    triggerHapticFeedback()
                    showOutcomeToast(outcome)
                }
            }
        }
    }

    private fun showOutcomeToast(outcome: TagScanOutcome) {
        runOnUiThread {
            when (outcome) {
                TagScanOutcome.STARTED -> Toast.makeText(this, getString(R.string.countdown_starting_title), Toast.LENGTH_SHORT).show()
                TagScanOutcome.STOPPED -> Unit
                TagScanOutcome.SWITCHED -> Toast.makeText(this, getString(R.string.toast_scan_switched), Toast.LENGTH_SHORT).show()
                TagScanOutcome.DISABLED_TAG -> Toast.makeText(this, getString(R.string.toast_scan_disabled), Toast.LENGTH_SHORT).show()
                TagScanOutcome.UNKNOWN_PROFILE -> Toast.makeText(this, getString(R.string.toast_scan_no_profile), Toast.LENGTH_SHORT).show()
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
    onTagHandled: () -> Unit,
    onSetNfcIntercept: (Boolean) -> Unit,
    pendingUnregisteredTag: UnregisteredTagPrompt? = null,
    onClearPendingUnregisteredTag: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val completedSessionSummary by app.sessionManager.lastCompletedSessionSummary.collectAsState()

    androidx.compose.runtime.LaunchedEffect(currentRoute) {
        val isTagScreen = currentRoute == "write_tag" || currentRoute == "read_tag"
        onSetNfcIntercept(isTagScreen)
    }

    val profileRepo = remember { ProfileRepository(app.database.focusProfileDao()) }
    val tagRepo = remember { TagRepository(app.database.nfcTagDao()) }
    val sessionRepo = remember { SessionRepository(app.database.sessionRecordDao()) }

    // Staged list of blocked packages for currently editing profile
    val stagedBlockedPackages = remember { mutableStateListOf<String>() }
    // Staged list of allowed packages for Zen mode
    val stagedZenPackages = remember { mutableStateListOf<String>() }

    val bottomNavItems = listOf(
        Screen.Focus,
        Screen.Profiles,
        Screen.Tags,
        Screen.Stats
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    val bottomBarRoutes = remember {
        setOf(
            Screen.Focus.route,
            Screen.Profiles.route,
            Screen.Tags.route,
            Screen.Stats.route,
            Screen.Settings.route
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                AppleTabBar {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        AppleTabItem(
                            selected = isSelected,
                            icon = screen.icon,
                            label = stringResource(screen.titleRes),
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
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Focus.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                val fromRoute = initialState.destination.route
                val toRoute = targetState.destination.route
                if ((fromRoute == null || fromRoute in bottomBarRoutes) && toRoute in bottomBarRoutes) {
                    EnterTransition.None
                } else {
                    fadeIn(animationSpec = tween(200))
                }
            },
            exitTransition = {
                val fromRoute = initialState.destination.route
                val toRoute = targetState.destination.route
                if (fromRoute in bottomBarRoutes && toRoute in bottomBarRoutes) {
                    ExitTransition.None
                } else {
                    fadeOut(animationSpec = tween(200))
                }
            },
            popEnterTransition = {
                val fromRoute = initialState.destination.route
                val toRoute = targetState.destination.route
                if (fromRoute in bottomBarRoutes && toRoute in bottomBarRoutes) {
                    EnterTransition.None
                } else {
                    fadeIn(animationSpec = tween(200))
                }
            },
            popExitTransition = {
                val fromRoute = initialState.destination.route
                val toRoute = targetState.destination.route
                if (fromRoute in bottomBarRoutes && toRoute in bottomBarRoutes) {
                    ExitTransition.None
                } else {
                    fadeOut(animationSpec = tween(200))
                }
            }
        ) {
            composable(Screen.Focus.route) {
                HomeScreen(
                    sessionManager = app.sessionManager,
                    profileRepository = profileRepo,
                    tagRepository = tagRepo,
                    onNavigateToTags = {
                        navController.navigate(Screen.Tags.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfiles = {
                        navController.navigate(Screen.Profiles.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Profiles.route) {
                ProfilesScreen(
                    profileRepository = profileRepo,
                    tagRepository = tagRepo,
                    onEditProfile = { profileId ->
                        navController.navigate("profile_edit/$profileId")
                    },
                    onCreateProfile = {
                        navController.navigate("profile_edit/new")
                    },
                    onAddTagClick = {
                        navController.navigate("write_tag")
                    }
                )
            }
            composable("profile_edit/{profileId}") { backStackEntry ->
                val profileId = backStackEntry.arguments?.getString("profileId")

                ProfileEditScreen(
                    profileId = profileId,
                    profileRepository = profileRepo,
                    tagRepository = tagRepo,
                    selectedBlockedPackages = stagedBlockedPackages.toList(),
                    selectedZenPackages = stagedZenPackages.toList(),
                    onSyncBlockedPackages = { list ->
                        stagedBlockedPackages.clear()
                        stagedBlockedPackages.addAll(list)
                    },
                    onSyncZenPackages = { list ->
                        stagedZenPackages.clear()
                        stagedZenPackages.addAll(list)
                    },
                    onSelectAppsClick = {
                        navController.navigate("app_selection")
                    },
                    onSelectZenAppsClick = {
                        navController.navigate("app_selection_zen")
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
            composable("app_selection_zen") {
                AppSelectionScreen(
                    initiallySelectedPackages = stagedZenPackages.toList(),
                    title = stringResource(R.string.zen_select_apps_title),
                    maxSelection = 5,
                    onSaveSelection = { updatedList ->
                        stagedZenPackages.clear()
                        stagedZenPackages.addAll(updatedList.take(5))
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Tags.route) {
                TagListScreen(
                    tagRepository = tagRepo,
                    onWriteTagClick = { navController.navigate("write_tag") },
                    onReadTagClick = { navController.navigate("read_tag") }
                )
            }
            composable("write_tag") {
                WriteTagScreen(
                    tagRepository = tagRepo,
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
                    database = app.database,
                    updateManager = app.updateManager,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        val pendingCountdown by app.sessionManager.pendingCountdown.collectAsState()

        // 5-second countdown dialog before starting session (NFC scan or manual start)
        pendingCountdown?.let { countdown ->
            Dialog(
                onDismissRequest = { app.sessionManager.cancelCountdown() },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { countdown.secondsRemaining / 5f },
                                modifier = Modifier.size(96.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 5.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "${countdown.secondsRemaining}",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = stringResource(R.string.countdown_starting_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.countdown_starting_desc, countdown.secondsRemaining),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = countdown.profile.name,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        TactileButton(
                            onClick = { app.sessionManager.cancelCountdown() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.countdown_btn_cancel))
                        }
                    }
                }
            }
        }

        // Unregistered Tag Import Confirmation Dialog
        pendingUnregisteredTag?.let { prompt ->
            var customName by remember(prompt) { mutableStateOf(prompt.defaultName) }
            AlertDialog(
                onDismissRequest = onClearPendingUnregisteredTag,
                title = { Text(stringResource(R.string.tag_import_prompt_title)) },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.tag_import_prompt_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text(stringResource(R.string.tag_detail_name)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val tagRecord = xyz.umarmohammad.hermit.data.model.NfcTagRecord(
                                id = prompt.payload.id,
                                tagPhysicalUid = prompt.physicalUid,
                                name = customName.trim().ifBlank { prompt.defaultName },
                                action = prompt.payload.getResolvedAction(),
                                targetProfileId = prompt.payload.profileId,
                                lastScannedAt = System.currentTimeMillis()
                            )
                            coroutineScope.launch {
                                tagRepo.saveTag(tagRecord)
                                app.sessionManager.handleTagScan(prompt.payload, prompt.physicalUid)
                            }
                            onClearPendingUnregisteredTag()
                        },
                        enabled = customName.isNotBlank()
                    ) {
                        Text(stringResource(R.string.tag_import_btn_add))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                app.sessionManager.handleTagScan(prompt.payload, prompt.physicalUid)
                            }
                            onClearPendingUnregisteredTag()
                        }
                    ) {
                        Text(stringResource(R.string.tag_import_btn_activate_only))
                    }
                }
            )
        }

        if (completedSessionSummary != null) {
            xyz.umarmohammad.hermit.ui.components.SessionCompletionSummaryDialog(
                summary = completedSessionSummary!!,
                onDismiss = {
                    app.sessionManager.dismissCompletedSessionSummary()
                }
            )
        }
    }
}

