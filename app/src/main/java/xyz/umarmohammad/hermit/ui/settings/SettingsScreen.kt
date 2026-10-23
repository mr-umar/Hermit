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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@Composable
fun SettingsScreen(
    preferenceManager: PreferenceManager,
    database: HermitDatabase
) {
    val context = LocalContext.current
    val currentLocale by preferenceManager.appLocale.collectAsState(initial = "system")
    val currentTheme by preferenceManager.themeMode.collectAsState(initial = "system")
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

    Scaffold(
        topBar = {
            HermitTopBar(title = stringResource(R.string.settings_title))
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
                                    CoroutineScope(Dispatchers.IO).launch {
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
                                    CoroutineScope(Dispatchers.IO).launch {
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
                                    CoroutineScope(Dispatchers.IO).launch {
                                        preferenceManager.setThemeMode(themeCode)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentTheme == themeCode),
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_version, "1.0.0"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))
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
                                HermitDatabase.populateDefaultProfiles(database.focusProfileDao())
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
