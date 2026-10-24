package xyz.umarmohammad.hermit.ui.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.model.ExitBehavior
import xyz.umarmohammad.hermit.data.model.FocusProfile
import xyz.umarmohammad.hermit.data.repository.ProfileRepository
import xyz.umarmohammad.hermit.data.repository.TagRepository
import xyz.umarmohammad.hermit.ui.components.AppleSegmentedControl
import xyz.umarmohammad.hermit.ui.components.DurationPickerCard
import xyz.umarmohammad.hermit.ui.components.GlassCard
import xyz.umarmohammad.hermit.ui.components.HermitTopBar
import xyz.umarmohammad.hermit.ui.components.TactileButton
import xyz.umarmohammad.hermit.ui.theme.Acento
import xyz.umarmohammad.hermit.ui.theme.Aviso
import xyz.umarmohammad.hermit.ui.theme.Secundario
import xyz.umarmohammad.hermit.ui.theme.Superficie
import xyz.umarmohammad.hermit.ui.theme.Texto
import java.util.UUID

@Composable
fun ProfileEditScreen(
    profileId: String?,
    profileRepository: ProfileRepository,
    tagRepository: TagRepository,
    selectedBlockedPackages: List<String>,
    selectedZenPackages: List<String> = emptyList(),
    onSyncBlockedPackages: (List<String>) -> Unit = {},
    onSyncZenPackages: (List<String>) -> Unit = {},
    onSelectAppsClick: () -> Unit,
    onSelectZenAppsClick: () -> Unit = {},
    onSaved: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val hermitApp = context.applicationContext as? xyz.umarmohammad.hermit.HermitApplication
    val sessionState by hermitApp?.sessionManager?.sessionState?.collectAsState() ?: remember { mutableStateOf(xyz.umarmohammad.hermit.session.FocusSessionState()) }
    val isSessionActive = sessionState.isActive
    val registeredTags by tagRepository.allTags.collectAsState(initial = emptyList())
    val hasPhysicalTags = registeredTags.isNotEmpty()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("work") }
    // Duration in minutes, from 1 minute to 12 hours (720 min)
    var durationMinutes by remember { mutableIntStateOf(60) }
    var allowEmergencyExit by remember { mutableStateOf(true) } // Default: allowed after 60s
    var isMonkMode by remember { mutableStateOf(false) }
    var enableDnd by remember { mutableStateOf(false) }
    var enableGrayscale by remember { mutableStateOf(false) }
    var blockWebSocial by remember { mutableStateOf(true) }
    var activationType by remember { mutableStateOf(if (hasPhysicalTags) "NFC" else "TIME") }

    val icons = listOf("work", "school", "spa", "bedtime", "code", "book")

    LaunchedEffect(profileId) {
        if (!profileId.isNullOrBlank() && profileId != "new") {
            val existing = profileRepository.getProfileById(profileId)
            if (existing != null) {
                name = existing.name
                description = existing.description
                selectedIcon = existing.iconName
                durationMinutes = (existing.durationMinutes ?: 60).coerceIn(1, 720)
                allowEmergencyExit = (existing.exitBehavior == ExitBehavior.ALLOW_EMERGENCY)
                isMonkMode = existing.isMonkMode
                enableDnd = existing.enableDnd
                enableGrayscale = existing.enableGrayscale
                blockWebSocial = existing.blockWebSocial
                activationType = if (hasPhysicalTags) existing.activationType else "TIME"

                onSyncBlockedPackages(existing.blockedPackages)
                onSyncZenPackages(existing.zenAllowedPackages)
            }
        }
    }

    Scaffold(
        topBar = {
            HermitTopBar(
                title = if (profileId == null || profileId == "new") {
                    stringResource(R.string.profiles_add)
                } else {
                    stringResource(R.string.profiles_edit)
                },
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    TactileButton(
                        onClick = {
                            if (name.isBlank()) return@TactileButton

                            val isNfcMode = hasPhysicalTags && (activationType == "NFC")
                            val finalExitBehavior = if (isNfcMode) {
                                ExitBehavior.NFC_ONLY
                            } else if (allowEmergencyExit) {
                                ExitBehavior.ALLOW_EMERGENCY
                            } else {
                                ExitBehavior.TIMED_ONLY
                            }

                            val finalDuration = if (isNfcMode) {
                                720
                            } else {
                                durationMinutes.coerceIn(1, 720)
                            }

                            val effectiveZenPackages = if (isSessionActive && profileId == sessionState.activeProfile?.id) {
                                sessionState.activeProfile?.zenAllowedPackages?.take(5) ?: emptyList()
                            } else if (isMonkMode) {
                                selectedZenPackages.distinct().take(5)
                            } else {
                                emptyList()
                            }

                            val effectiveBlockedPackages = if (isSessionActive && profileId == sessionState.activeProfile?.id) {
                                sessionState.activeProfile?.blockedPackages ?: emptyList()
                            } else if (isMonkMode) {
                                emptyList()
                            } else {
                                selectedBlockedPackages
                            }

                            val profileToSave = FocusProfile(
                                id = if (profileId == null || profileId == "new") UUID.randomUUID().toString() else profileId,
                                name = name.trim(),
                                description = description.trim(),
                                iconName = selectedIcon,
                                colorHex = "#A5B092",
                                blockedPackages = effectiveBlockedPackages,
                                durationMinutes = finalDuration,
                                exitBehavior = finalExitBehavior,
                                strictMode = true, // 60s reflection delay when emergency exit allowed
                                isMonkMode = isMonkMode,
                                blockWebSocial = blockWebSocial,
                                activationType = if (hasPhysicalTags) activationType else "TIME",
                                zenAllowedPackages = effectiveZenPackages,
                                enableDnd = enableDnd,
                                enableGrayscale = enableGrayscale
                            )

                            coroutineScope.launch(Dispatchers.IO) {
                                profileRepository.saveProfile(profileToSave)
                                val app = context.applicationContext as? xyz.umarmohammad.hermit.HermitApplication
                                app?.sessionManager?.refreshActiveProfile()
                            }
                            onSaved()
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.profile_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.profile_desc_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Icon Selector
            Text(
                text = stringResource(R.string.profile_icon_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                icons.forEach { iconKey ->
                    val isSelected = selectedIcon == iconKey
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable { selectedIcon = iconKey }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getProfileIcon(iconKey),
                                contentDescription = iconKey,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Activation / Unlock Mode Selector (If user has physical tags)
            if (hasPhysicalTags) {
                Text(
                    text = stringResource(R.string.profile_activation_mode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                val nfcLabel = stringResource(R.string.profile_activation_nfc)
                val timeLabel = stringResource(R.string.profile_activation_time)
                AppleSegmentedControl(
                    items = listOf("NFC", "TIME"),
                    selectedItem = activationType,
                    onItemSelected = { activationType = it },
                    labelProvider = { if (it == "NFC") nfcLabel else timeLabel }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (activationType == "NFC") {
                        stringResource(R.string.profile_activation_nfc_desc)
                    } else {
                        stringResource(R.string.profile_activation_time_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Duration Picker (Shown if activationType is TIME, or if user has no NFC tags)
            if (!hasPhysicalTags || activationType == "TIME") {
                DurationPickerCard(
                    durationMinutes = durationMinutes,
                    onDurationChange = { durationMinutes = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Emergency Exit Option (Default: allowed after 60s, but user can disable it)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_emergency_exit_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (allowEmergencyExit) {
                                    stringResource(R.string.profile_emergency_exit_enabled_desc)
                                } else {
                                    stringResource(R.string.profile_emergency_exit_disabled_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (allowEmergencyExit) MaterialTheme.colorScheme.onSurfaceVariant else Aviso
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = allowEmergencyExit,
                            onCheckedChange = { allowEmergencyExit = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Zen Mode Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.zen_mode_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.zen_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isMonkMode,
                            onCheckedChange = { isMonkMode = it }
                        )
                    }

                    if (isMonkMode) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.profile_zen_minimalist_title),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.profile_zen_minimalist_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Up to 5 optional apps in Zen Launcher
                        OutlinedButton(
                            onClick = onSelectZenAppsClick,
                            enabled = !isSessionActive,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedZenPackages.isEmpty()) {
                                    stringResource(R.string.profile_zen_add_apps_btn)
                                } else {
                                    stringResource(R.string.profile_zen_apps_added_btn, selectedZenPackages.size.coerceAtMost(5))
                                }
                            )
                        }

                        if (isSessionActive) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.profile_apps_locked_during_session),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.profile_zen_dnd_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.profile_zen_dnd_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = enableDnd,
                                onCheckedChange = { isChecked ->
                                    enableDnd = isChecked
                                    if (isChecked && !xyz.umarmohammad.hermit.util.DndManager.hasDndPermission(context)) {
                                        xyz.umarmohammad.hermit.util.DndManager.requestDndPermission(context)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.profile_zen_grayscale_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.profile_zen_grayscale_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = enableGrayscale,
                                onCheckedChange = { isChecked ->
                                    enableGrayscale = isChecked
                                    if (isChecked && !xyz.umarmohammad.hermit.util.GrayscaleManager.hasWriteSecureSettings(context)) {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.profile_zen_grayscale_adb_toast),
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Standard Blocked Apps Card (Only relevant if NOT in Zen Mode, since Zen mode whitelist-blocks everything else)
            if (!isMonkMode) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Apps,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.profile_blocked_apps_label),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedBlockedPackages.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${selectedBlockedPackages.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedBlockedPackages.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = onSelectAppsClick,
                            enabled = !isSessionActive,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedBlockedPackages.isEmpty()) {
                                    stringResource(R.string.profile_select_apps)
                                } else {
                                    stringResource(R.string.app_select_save, selectedBlockedPackages.size)
                                }
                            )
                        }

                        if (isSessionActive) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.profile_apps_locked_during_session),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Web Browser Social Media Blocking Toggle
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_block_web_social_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_block_web_social),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = blockWebSocial,
                        onCheckedChange = { blockWebSocial = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
