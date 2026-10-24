package xyz.umarmohammad.hermit.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import xyz.umarmohammad.hermit.ui.components.AppleDivider
import xyz.umarmohammad.hermit.ui.components.AppleGroupedCard
import xyz.umarmohammad.hermit.ui.components.AppleListRow
import xyz.umarmohammad.hermit.ui.components.AppleToggleRow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import xyz.umarmohammad.hermit.util.DeviceProtectionManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.local.HermitDatabase
import xyz.umarmohammad.hermit.data.preference.PreferenceManager
import xyz.umarmohammad.hermit.service.HermitAccessibilityService
import xyz.umarmohammad.hermit.ui.components.GlassCard
import xyz.umarmohammad.hermit.ui.components.HermitTopBar
import xyz.umarmohammad.hermit.ui.theme.Acento
import xyz.umarmohammad.hermit.ui.theme.Aviso
import xyz.umarmohammad.hermit.ui.theme.DuneDarkAcento
import xyz.umarmohammad.hermit.ui.theme.DuneLightAcento
import xyz.umarmohammad.hermit.ui.theme.Fondo
import xyz.umarmohammad.hermit.ui.theme.ForestDarkAcento
import xyz.umarmohammad.hermit.ui.theme.ForestLightAcento
import xyz.umarmohammad.hermit.ui.theme.OceanDarkAcento
import xyz.umarmohammad.hermit.ui.theme.OceanLightAcento
import xyz.umarmohammad.hermit.ui.theme.Secundario
import xyz.umarmohammad.hermit.ui.theme.Texto
import xyz.umarmohammad.hermit.updater.UpdateError
import xyz.umarmohammad.hermit.updater.UpdateManager
import xyz.umarmohammad.hermit.updater.UpdateState
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    preferenceManager: PreferenceManager,
    database: HermitDatabase,
    updateManager: UpdateManager,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentLocale by preferenceManager.appLocale.collectAsState(initial = "system")
    val currentTheme by preferenceManager.themeMode.collectAsState(initial = "system")
    val currentPalette by preferenceManager.themePalette.collectAsState(initial = "forest")
    val isAccessibilityRunning by HermitAccessibilityService.isServiceRunning.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    // Check NFC status
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val nfcStatusText = when {
        nfcAdapter == null -> stringResource(R.string.settings_nfc_unavailable)
        nfcAdapter.isEnabled -> stringResource(R.string.settings_nfc_available)
        else -> stringResource(R.string.settings_nfc_disabled)
    }

    // Check Battery Optimization
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isBatteryExempt = remember(context) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    var isDeviceAdminActive by remember { mutableStateOf(DeviceProtectionManager.isDeviceAdminActive(context)) }
    var showAdminDeactivateDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var deactivateCountdownRemaining by remember { mutableStateOf(15) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDeviceAdminActive = DeviceProtectionManager.isDeviceAdminActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(showAdminDeactivateDialog) {
        if (showAdminDeactivateDialog) {
            deactivateCountdownRemaining = 15
            for (i in 15 downTo 1) {
                deactivateCountdownRemaining = i
                delay(1000L)
            }
            deactivateCountdownRemaining = 0
        }
    }

    Scaffold(
        topBar = {
            HermitTopBar(
                title = stringResource(R.string.settings_title),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // General Section: Language & Theme
            SectionHeading(title = stringResource(R.string.settings_section_general))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val locales = listOf(
                        "system" to stringResource(R.string.settings_language_system),
                        "en" to stringResource(R.string.settings_language_en),
                        "es" to stringResource(R.string.settings_language_es)
                    )

                    locales.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        preferenceManager.setAppLocale(code)
                                    }
                                    val targetTag = if (code == "system") "" else code
                                    val appLocale = LocaleListCompat.forLanguageTags(targetTag)
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentLocale == code),
                                onClick = {
                                    coroutineScope.launch {
                                        preferenceManager.setAppLocale(code)
                                    }
                                    val targetTag = if (code == "system") "" else code
                                    val appLocale = LocaleListCompat.forLanguageTags(targetTag)
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_theme),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val themes = listOf(
                        "system" to stringResource(R.string.settings_theme_system),
                        "light" to stringResource(R.string.settings_theme_light),
                        "dark" to stringResource(R.string.settings_theme_dark)
                    )

                    themes.forEach { (themeCode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        preferenceManager.setThemeMode(themeCode)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentTheme == themeCode),
                                onClick = {
                                    coroutineScope.launch {
                                        preferenceManager.setThemeMode(themeCode)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_theme_palette),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val palettes = listOf(
                        Triple(
                            "forest",
                            stringResource(R.string.palette_forest),
                            stringResource(R.string.palette_forest_desc)
                        ) to listOf(ForestDarkAcento, ForestLightAcento),
                        Triple(
                            "ocean",
                            stringResource(R.string.palette_ocean),
                            stringResource(R.string.palette_ocean_desc)
                        ) to listOf(OceanDarkAcento, OceanLightAcento),
                        Triple(
                            "dune",
                            stringResource(R.string.palette_dune),
                            stringResource(R.string.palette_dune_desc)
                        ) to listOf(DuneDarkAcento, DuneLightAcento)
                    )

                    palettes.forEach { (info, colors) ->
                        val (code, name, desc) = info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        preferenceManager.setThemePalette(code)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentPalette.lowercase() == code),
                                onClick = {
                                    coroutineScope.launch {
                                        preferenceManager.setThemePalette(code)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // System Diagnostics Section
            SectionHeading(title = stringResource(R.string.settings_section_diagnostics))

            // NFC Diagnostic
            DiagnosticItemCard(
                icon = Icons.Default.Nfc,
                title = stringResource(R.string.settings_nfc_status),
                status = nfcStatusText,
                isHealthy = (nfcAdapter != null && nfcAdapter.isEnabled),
                actionText = if (nfcAdapter != null && !nfcAdapter.isEnabled) stringResource(R.string.settings_btn_open_nfc_settings) else null,
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Accessibility Diagnostic
            DiagnosticItemCard(
                icon = Icons.Default.Security,
                title = stringResource(R.string.settings_accessibility_status),
                status = if (isAccessibilityRunning) stringResource(R.string.settings_accessibility_enabled) else stringResource(R.string.settings_accessibility_disabled),
                isHealthy = isAccessibilityRunning,
                actionText = if (!isAccessibilityRunning) stringResource(R.string.settings_btn_open_accessibility) else null,
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Battery Optimization Diagnostic
            DiagnosticItemCard(
                icon = Icons.Default.BatteryAlert,
                title = stringResource(R.string.settings_battery_status),
                status = if (isBatteryExempt) stringResource(R.string.settings_battery_exempted) else stringResource(R.string.settings_battery_optimized),
                isHealthy = isBatteryExempt,
                actionText = if (!isBatteryExempt) stringResource(R.string.settings_btn_battery_opt) else null,
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Anti-Uninstall Device Administrator Diagnostic Card
            DiagnosticItemCard(
                icon = Icons.Default.Security,
                title = stringResource(R.string.settings_device_admin_title),
                status = if (isDeviceAdminActive) {
                    stringResource(R.string.settings_device_admin_desc_active)
                } else {
                    stringResource(R.string.settings_device_admin_desc_inactive)
                },
                isHealthy = isDeviceAdminActive,
                actionText = if (isDeviceAdminActive) {
                    stringResource(R.string.settings_device_admin_btn_disable)
                } else {
                    stringResource(R.string.settings_device_admin_btn_enable)
                },
                onAction = {
                    if (isDeviceAdminActive) {
                        showAdminDeactivateDialog = true
                    } else {
                        val intent = DeviceProtectionManager.createEnableAdminIntent(context)
                        context.startActivity(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy & Data Section
            SectionHeading(title = stringResource(R.string.settings_section_privacy))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_privacy_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, context.getString(R.string.settings_export_success), Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_export_data),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showResetDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_reset_data),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Updates Section
            UpdatesSection(
                updateManager = updateManager,
                preferenceManager = preferenceManager
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeading(title = "Hermit Open Source")

            AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                AppleListRow(
                    title = stringResource(R.string.settings_about_domain_title),
                    value = null,
                    icon = Icons.Default.Public,
                    showChevron = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hermit.umarmohammad.xyz"))
                        context.startActivity(intent)
                    }
                )

                AppleDivider(startIndent = true)

                AppleListRow(
                    title = stringResource(R.string.settings_about_repo_title),
                    value = null,
                    icon = Icons.Default.Code,
                    showChevron = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mr-umar/Hermit"))
                        context.startActivity(intent)
                    }
                )

                AppleDivider(startIndent = true)

                AppleListRow(
                    title = stringResource(R.string.settings_about_license_title),
                    subtitle = stringResource(R.string.settings_about_license_val),
                    icon = Icons.Default.Gavel,
                    showChevron = true,
                    onClick = { showLicenseDialog = true }
                )

                AppleDivider(startIndent = true)

                AppleListRow(
                    title = stringResource(R.string.settings_about_version_title),
                    value = stringResource(R.string.settings_about_version_val),
                    icon = Icons.Default.Info,
                    showChevron = false
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Reset Data Confirmation Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
                text = { Text(stringResource(R.string.settings_reset_confirm_desc)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                database.clearAllTables()
                                preferenceManager.clearAll()
                            }
                            showResetDialog = false
                            Toast.makeText(context, "Hermit reset to factory state", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // Mindful Reflection Delay Dialog for Deactivating Device Admin
        if (showAdminDeactivateDialog) {
            AlertDialog(
                onDismissRequest = { showAdminDeactivateDialog = false },
                title = { Text(stringResource(R.string.device_admin_deactivate_dialog_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.device_admin_deactivate_dialog_desc))
                        if (deactivateCountdownRemaining > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.device_admin_countdown_wait, deactivateCountdownRemaining),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAdminDeactivateDialog = false
                            DeviceProtectionManager.removeAdmin(context)
                            isDeviceAdminActive = DeviceProtectionManager.isDeviceAdminActive(context)
                        },
                        enabled = (deactivateCountdownRemaining == 0),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(R.string.device_admin_btn_confirm_deactivate))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdminDeactivateDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // Full Open Source License Explanation Dialog
        if (showLicenseDialog) {
            AlertDialog(
                onDismissRequest = { showLicenseDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.settings_license_dialog_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_about_license_val),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.settings_license_dialog_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLicenseDialog = false }) {
                        Text(stringResource(R.string.action_close))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showLicenseDialog = false
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mr-umar/Hermit/blob/main/LICENSE"))
                            context.startActivity(intent)
                        }
                    ) {
                        Text(stringResource(R.string.settings_about_repo_title))
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun DiagnosticItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    isHealthy: Boolean,
    actionText: String?,
    onAction: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
            if (actionText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
private fun UpdatesSection(
    updateManager: UpdateManager,
    preferenceManager: PreferenceManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val updateState by updateManager.updateState.collectAsState()
    val autoCheckUpdates by preferenceManager.autoCheckUpdates.collectAsState(initial = false)
    val lastCheckTimestamp by preferenceManager.lastUpdateCheckTimestamp.collectAsState(initial = 0L)

    var showPermissionDialog by remember { mutableStateOf(false) }
    var notesExpanded by remember { mutableStateOf(false) }

    val installedVersion = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    val lastCheckedStr = if (lastCheckTimestamp > 0L) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastCheckTimestamp))
    } else {
        stringResource(R.string.updates_last_checked_never)
    }

    SectionHeading(title = stringResource(R.string.settings_section_updates))

    AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
        AppleListRow(
            title = stringResource(R.string.updates_installed_version),
            value = "v$installedVersion",
            icon = Icons.Default.Info,
            showChevron = false
        )

        AppleDivider(startIndent = true)

        val latestVersionText = when (val s = updateState) {
            is UpdateState.Checking -> null
            is UpdateState.UpToDate -> "v${s.installedVersion}"
            is UpdateState.UpdateAvailable -> "v${s.latestVersion}"
            is UpdateState.Downloading -> "v${s.release.parsedVersion?.toString() ?: s.release.tagName}"
            is UpdateState.ReadyToInstall -> "v${s.release.parsedVersion?.toString() ?: s.release.tagName}"
            is UpdateState.Error -> "—"
            is UpdateState.Idle -> "—"
        }

        AppleListRow(
            title = stringResource(R.string.updates_latest_version),
            value = latestVersionText,
            icon = Icons.Default.SystemUpdate,
            showChevron = false
        )

        AppleDivider(startIndent = true)

        AppleListRow(
            title = stringResource(R.string.updates_last_checked),
            value = lastCheckedStr,
            icon = Icons.Default.Schedule,
            showChevron = false
        )

        AppleDivider(startIndent = true)

        AppleToggleRow(
            title = stringResource(R.string.updates_auto_check),
            subtitle = stringResource(R.string.updates_auto_check_desc),
            checked = autoCheckUpdates,
            onCheckedChange = { checked ->
                coroutineScope.launch(Dispatchers.IO) {
                    preferenceManager.setAutoCheckUpdates(checked)
                }
            }
        )

        AppleDivider(startIndent = true)

        // Check for updates action row
        if (updateState is UpdateState.Checking) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Acento.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Acento
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.updates_status_checking),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.5.sp
                    ),
                    color = Secundario
                )
            }
        } else {
            AppleListRow(
                title = stringResource(R.string.updates_btn_check),
                icon = Icons.Default.Refresh,
                iconTint = Acento,
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        updateManager.checkForUpdates(manual = true)
                    }
                }
            )
        }
    }

    // Dynamic State Cards (Up to date / Available / Downloading / Ready / Error)
    if (updateState !is UpdateState.Idle && updateState !is UpdateState.Checking) {
        Spacer(modifier = Modifier.height(10.dp))
    }

    when (val state = updateState) {
        is UpdateState.UpToDate -> {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Acento.copy(alpha = 0.08f),
                borderColor = Acento.copy(alpha = 0.25f),
                contentPadding = 12.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Acento,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.updates_status_up_to_date),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Acento
                    )
                }
            }
        }
        is UpdateState.UpdateAvailable -> {
            val release = state.release
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Acento,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.updates_status_available, state.latestVersion),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Texto,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (release.body.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { notesExpanded = !notesExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.updates_release_notes),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Acento
                            )
                            Icon(
                                imageVector = if (notesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Secundario,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (notesExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = release.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Texto,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.updates_btn_view_github), maxLines = 1)
                        }

                        Button(
                            onClick = {
                                updateManager.startDownload(release, coroutineScope)
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Acento, contentColor = Fondo)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.updates_btn_download), maxLines = 1)
                        }
                    }
                }
            }
        }
        is UpdateState.Downloading -> {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Acento
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val progressPercent = (state.progress * 100).toInt()
                        val text = if (state.totalBytes > 0) {
                            val curMb = state.bytesDownloaded / (1024f * 1024f)
                            val totalMb = state.totalBytes / (1024f * 1024f)
                            stringResource(R.string.updates_downloading_format, progressPercent, curMb, totalMb)
                        } else {
                            stringResource(R.string.updates_downloading_unknown)
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Texto,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Acento,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Acento,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { updateManager.cancelDownload(state.release) },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.updates_btn_cancel))
                    }
                }
            }
        }
        is UpdateState.ReadyToInstall -> {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Acento,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.updates_status_available, state.release.parsedVersion?.toString() ?: state.release.tagName),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Texto
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Verification Badges
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Acento, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.updates_badge_sha256_verified),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Acento
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Acento, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.updates_badge_cert_verified),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Acento
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.updates_ready_to_install_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Secundario
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (updateManager.canRequestPackageInstalls()) {
                                val launched = updateManager.launchPackageInstaller(state.apkFile)
                                if (!launched) {
                                    Toast.makeText(context, "Could not open installer", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                showPermissionDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Acento, contentColor = Fondo)
                    ) {
                        Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.updates_btn_install),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
        is UpdateState.Error -> {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Aviso.copy(alpha = 0.08f),
                borderColor = Aviso.copy(alpha = 0.25f),
                contentPadding = 12.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Aviso,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val errorText = when (state.error) {
                            is UpdateError.RateLimitExceeded -> stringResource(R.string.updates_error_rate_limit)
                            is UpdateError.NetworkUnavailable -> stringResource(R.string.updates_error_network)
                            is UpdateError.MissingApkAsset -> stringResource(R.string.updates_error_missing_asset)
                            is UpdateError.ChecksumMismatch -> stringResource(R.string.updates_error_checksum_mismatch)
                            is UpdateError.SecurityVerificationFailed -> stringResource(R.string.updates_error_security_mismatch, state.message)
                            else -> state.message
                        }
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Texto,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                updateManager.checkForUpdates(manual = true)
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.updates_btn_retry))
                    }
                }
            }
        }
        is UpdateState.Idle -> {
            // No extra banner
        }
        is UpdateState.Checking -> {
            // Progress is inside the card
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.updates_permission_dialog_title)) },
            text = { Text(stringResource(R.string.updates_permission_dialog_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        updateManager.openInstallPermissionSettings()
                    }
                ) {
                    Text(stringResource(R.string.updates_permission_dialog_open_settings), color = Acento)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
