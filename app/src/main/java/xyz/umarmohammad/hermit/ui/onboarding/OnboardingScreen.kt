package xyz.umarmohammad.hermit.ui.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.HermitApplication
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.model.ExitBehavior
import xyz.umarmohammad.hermit.data.model.FocusProfile
import xyz.umarmohammad.hermit.service.HermitAccessibilityService
import xyz.umarmohammad.hermit.ui.components.AppleDivider
import xyz.umarmohammad.hermit.ui.components.AppleGroupedCard
import xyz.umarmohammad.hermit.ui.components.AppleSegmentedControl
import xyz.umarmohammad.hermit.ui.components.AppleToggleRow
import xyz.umarmohammad.hermit.ui.components.TactileButton
import xyz.umarmohammad.hermit.ui.theme.Acento
import xyz.umarmohammad.hermit.ui.theme.Aviso
import xyz.umarmohammad.hermit.ui.theme.BorderDark
import xyz.umarmohammad.hermit.ui.theme.Fondo
import xyz.umarmohammad.hermit.ui.theme.Secundario
import xyz.umarmohammad.hermit.ui.theme.Superficie
import xyz.umarmohammad.hermit.ui.theme.SuperficieSubtle
import xyz.umarmohammad.hermit.ui.theme.Texto
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    app: HermitApplication,
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by remember { mutableIntStateOf(0) }
    var dailyHours by remember { mutableFloatStateOf(4f) }

    // Live permission states updated on resume
    var isBatteryExempt by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasUsagePermission by remember { mutableStateOf(false) }
    val isAccessibilityRunning by HermitAccessibilityService.isServiceRunning.collectAsState()

    fun checkAllPermissions() {
        // Battery
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        // Overlay
        hasOverlayPermission = Settings.canDrawOverlays(context)

        // Usage stats
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps?.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps?.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        hasUsagePermission = mode == AppOpsManager.MODE_ALLOWED
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAllPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkAllPermissions()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = Fondo,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hermit",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.02).sp
                        ),
                        color = Texto
                    )
                },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = { currentStep -= 1 }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = Texto
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Fondo)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "onboarding_step_transition"
            ) { step ->
                when (step) {
                    0 -> StepHoursInquiry(
                        hours = dailyHours,
                        onHoursChange = { dailyHours = it },
                        onNext = { currentStep = 1 }
                    )
                    1 -> StepAnnualCalculation(
                        hours = dailyHours,
                        onNext = { currentStep = 2 }
                    )
                    2 -> StepPermissionSingle(
                        icon = Icons.Default.BatteryAlert,
                        title = stringResource(R.string.onboarding_perm_battery_title),
                        description = stringResource(R.string.onboarding_perm_battery_desc),
                        isGranted = isBatteryExempt,
                        actionLabel = stringResource(R.string.onboarding_perm_battery_btn),
                        onAction = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        onNext = { currentStep = 3 }
                    )
                    3 -> StepAccessibilityPermission(
                        isGranted = isAccessibilityRunning,
                        onAction = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        onNext = { currentStep = 4 }
                    )
                    4 -> StepPermissionSingle(
                        icon = Icons.Default.Layers,
                        title = stringResource(R.string.onboarding_perm_overlay_title),
                        description = stringResource(R.string.onboarding_perm_overlay_desc),
                        isGranted = hasOverlayPermission,
                        actionLabel = stringResource(R.string.onboarding_perm_overlay_btn),
                        onAction = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        onNext = { currentStep = 5 }
                    )
                    5 -> StepPermissionSingle(
                        icon = Icons.Default.Timeline,
                        title = stringResource(R.string.onboarding_perm_usage_title),
                        description = stringResource(R.string.onboarding_perm_usage_desc),
                        isGranted = hasUsagePermission,
                        actionLabel = stringResource(R.string.onboarding_perm_usage_btn),
                        onAction = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        onNext = { currentStep = 6 }
                    )
                    6 -> StepNfcGuide(
                        onSetupNow = { currentStep = 7 },
                        onSkip = { currentStep = 7 }
                    )
                    7 -> StepCreateProfile(
                        app = app,
                        onCompleted = onFinishOnboarding
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Step 0: Hours per day inquiry
// -------------------------------------------------------------
@Composable
private fun StepHoursInquiry(
    hours: Float,
    onHoursChange: (Float) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Superficie),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = Acento,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_hours_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp
                ),
                color = Texto,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboarding_hours_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Secundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Large tactile number display
            Text(
                text = stringResource(R.string.onboarding_hours_format, hours.toInt()),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.03).sp
                ),
                color = Acento,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = hours,
                onValueChange = { onHoursChange(it) },
                valueRange = 1f..12f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = Acento,
                    activeTrackColor = Acento,
                    inactiveTrackColor = SuperficieSubtle
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp)) {
            TactileButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_btn_calc))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------
// Step 1: Calculation results
// -------------------------------------------------------------
@Composable
private fun StepAnnualCalculation(
    hours: Float,
    onNext: () -> Unit
) {
    val annualDays = ((hours * 365) / 24).toInt()
    val awakeMonths = (hours * 365.0) / (16.0 * 30.4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_calc_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp
                ),
                color = Texto,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_calc_days, annualDays),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Aviso,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.onboarding_calc_months, awakeMonths),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Texto,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AppleDivider(startIndent = false)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.onboarding_calc_body),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                        color = Secundario,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp)) {
            TactileButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_btn_start_setup))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------
// Step 2, 4, 5: Concise 1-screen-per-permission
// -------------------------------------------------------------
@Composable
private fun StepPermissionSingle(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Superficie),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Acento else Aviso,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Texto,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Secundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Apple-style permission status card
            AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Estado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Secundario
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isGranted) Acento else Aviso,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGranted) stringResource(R.string.onboarding_perm_status_granted)
                            else stringResource(R.string.onboarding_perm_status_pending),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isGranted) Acento else Aviso
                        )
                    }
                }
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(actionLabel, color = Acento)
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp)) {
            TactileButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_btn_next))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------
// Step 3: Accessibility Permission + Android 13+ restricted tip
// -------------------------------------------------------------
@Composable
private fun StepAccessibilityPermission(
    isGranted: Boolean,
    onAction: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Superficie),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isGranted) Acento else Aviso,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_perm_access_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Texto,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboarding_perm_access_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Secundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Restricted Settings Tip Box (crucial for Android 13+)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SuperficieSubtle)
                    .border(0.75.dp, Aviso.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Aviso,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.onboarding_perm_access_restricted_tip),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                        color = Texto
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status card
            AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Estado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Secundario
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isGranted) Acento else Aviso,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGranted) stringResource(R.string.onboarding_perm_status_granted)
                            else stringResource(R.string.onboarding_perm_status_pending),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isGranted) Acento else Aviso
                        )
                    }
                }
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.onboarding_perm_access_btn), color = Acento)
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp)) {
            TactileButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_btn_next))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------
// Step 6: NFC Tag Setup Guide
// -------------------------------------------------------------
@Composable
private fun StepNfcGuide(
    onSetupNow: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Superficie),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = Acento,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_nfc_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Texto,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboarding_nfc_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Secundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step by step guide
            AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                NfcGuideStepRow(step = "1", text = "Consigue una etiqueta NFC adhesiva o llavero.")
                AppleDivider(startIndent = true)
                NfcGuideStepRow(step = "2", text = "Pégala en tu escritorio o en otra habitación.")
                AppleDivider(startIndent = true)
                NfcGuideStepRow(step = "3", text = "Acerca el móvil para bloquear o desbloquear.")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compatible tags box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Superficie)
                    .border(0.75.dp, BorderDark, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_nfc_compatible_box),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                    color = Acento
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, top = 24.dp)
        ) {
            TactileButton(
                onClick = onSetupNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_nfc_btn_setup))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.onboarding_nfc_btn_skip), color = Secundario)
            }
        }
    }
}

@Composable
private fun NfcGuideStepRow(step: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Acento.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Acento
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = Texto
        )
    }
}

// -------------------------------------------------------------
// Step 7: Create First Profile & Monk Mode
// -------------------------------------------------------------
@Composable
private fun StepCreateProfile(
    app: HermitApplication,
    onCompleted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Quick filters
    var filterSocial by remember { mutableStateOf(true) }
    var filterWork by remember { mutableStateOf(false) }
    var filterStreaming by remember { mutableStateOf(false) }

    var blockWebSocial by remember { mutableStateOf(true) }
    var unlockMode by remember { mutableStateOf("NFC") } // "NFC" or "TIME"
    var isMonkModeSelected by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_profile_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Texto,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboarding_profile_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Secundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Category Filters
            Text(
                text = "Filtros rápidos de bloqueo",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Secundario,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterSocial,
                    onClick = { filterSocial = !filterSocial },
                    label = { Text(stringResource(R.string.onboarding_filter_social)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Acento.copy(alpha = 0.2f),
                        selectedLabelColor = Acento,
                        containerColor = Superficie,
                        labelColor = Secundario
                    )
                )

                FilterChip(
                    selected = filterWork,
                    onClick = { filterWork = !filterWork },
                    label = { Text(stringResource(R.string.onboarding_filter_work)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Acento.copy(alpha = 0.2f),
                        selectedLabelColor = Acento,
                        containerColor = Superficie,
                        labelColor = Secundario
                    )
                )

                FilterChip(
                    selected = filterStreaming,
                    onClick = { filterStreaming = !filterStreaming },
                    label = { Text(stringResource(R.string.onboarding_filter_streaming)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Acento.copy(alpha = 0.2f),
                        selectedLabelColor = Acento,
                        containerColor = Superficie,
                        labelColor = Secundario
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Browser Web Social Media Blocking Toggle
            AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                AppleToggleRow(
                    title = "Navegadores",
                    subtitle = stringResource(R.string.onboarding_block_web_social),
                    checked = blockWebSocial,
                    onCheckedChange = { blockWebSocial = it },
                    icon = Icons.Default.Layers
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Unlock mode selection: NFC or Timer
            Text(
                text = "Modo de desbloqueo",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Secundario,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            AppleSegmentedControl(
                items = listOf("NFC", "TIME"),
                selectedItem = unlockMode,
                onItemSelected = { unlockMode = it },
                labelProvider = { if (it == "NFC") "Físico (NFC)" else "Por Tiempo" }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (unlockMode == "NFC") stringResource(R.string.onboarding_unlock_nfc_only)
                else stringResource(R.string.onboarding_unlock_time_only),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Secundario,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Monk Mode Sanctuary Card
            AppleGroupedCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (isMonkModeSelected) SuperficieSubtle else Superficie
            ) {
                AppleToggleRow(
                    title = stringResource(R.string.onboarding_monk_banner_title),
                    subtitle = stringResource(R.string.onboarding_monk_banner_desc),
                    checked = isMonkModeSelected,
                    onCheckedChange = { isMonkModeSelected = it },
                    icon = Icons.Default.SelfImprovement,
                    iconTint = Acento
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp)) {
            TactileButton(
                onClick = {
                    coroutineScope.launch {
                        val dao = app.database.focusProfileDao()

                        val blockedList = mutableListOf<String>()
                        if (filterSocial) {
                            blockedList.addAll(listOf(
                                "com.instagram.android",
                                "com.zhiliaoapp.musically",
                                "com.twitter.android",
                                "com.facebook.katana",
                                "com.reddit.frontpage",
                                "com.snapchat.android",
                                "com.google.android.youtube"
                            ))
                        }
                        if (filterWork) {
                            blockedList.addAll(listOf(
                                "com.slack",
                                "com.microsoft.teams",
                                "com.google.android.gm",
                                "com.linkedin.android"
                            ))
                        }
                        if (filterStreaming) {
                            blockedList.addAll(listOf(
                                "com.netflix.mediaclient",
                                "tv.twitch.android.app",
                                "com.disney.disneyplus"
                            ))
                        }

                        val customProfile = FocusProfile(
                            id = UUID.randomUUID().toString(),
                            name = if (isMonkModeSelected) "Monk" else "Mi Enfoque",
                            description = if (isMonkModeSelected) "Santuario absoluto." else "Perfil creado durante la bienvenida.",
                            iconName = if (isMonkModeSelected) "spa" else "work",
                            colorHex = "#A5B092",
                            blockedPackages = blockedList.distinct(),
                            durationMinutes = if (unlockMode == "TIME") 45 else null,
                            exitBehavior = if (unlockMode == "NFC") ExitBehavior.NFC_ONLY else ExitBehavior.ALLOW_EMERGENCY,
                            strictMode = (unlockMode == "NFC"),
                            isDefault = true,
                            isMonkMode = isMonkModeSelected,
                            blockWebSocial = blockWebSocial,
                            activationType = unlockMode
                        )

                        dao.insertProfile(customProfile)
                        onCompleted()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_btn_enter))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}
