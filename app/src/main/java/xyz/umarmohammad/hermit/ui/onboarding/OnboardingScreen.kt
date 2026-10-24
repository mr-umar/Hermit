package xyz.umarmohammad.hermit.ui.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.Tag
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.umarmohammad.hermit.HermitApplication
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.model.ExitBehavior
import xyz.umarmohammad.hermit.data.model.FocusProfile
import xyz.umarmohammad.hermit.data.model.NfcTagRecord
import xyz.umarmohammad.hermit.data.model.TagAction
import xyz.umarmohammad.hermit.nfc.HermitTagPayload
import xyz.umarmohammad.hermit.nfc.NfcTagManager
import xyz.umarmohammad.hermit.service.HermitAccessibilityService
import xyz.umarmohammad.hermit.ui.apps.AppSelectionScreen
import xyz.umarmohammad.hermit.ui.components.AppleDivider
import xyz.umarmohammad.hermit.ui.components.AppleGroupedCard
import xyz.umarmohammad.hermit.ui.components.AppleSegmentedControl
import xyz.umarmohammad.hermit.ui.components.AppleToggleRow
import xyz.umarmohammad.hermit.ui.components.DurationPickerCard
import xyz.umarmohammad.hermit.ui.components.TactileButton
import xyz.umarmohammad.hermit.ui.theme.Acento
import xyz.umarmohammad.hermit.ui.theme.Aviso
import xyz.umarmohammad.hermit.ui.theme.BorderDark
import xyz.umarmohammad.hermit.ui.theme.Fondo
import xyz.umarmohammad.hermit.ui.theme.Secundario
import xyz.umarmohammad.hermit.ui.theme.Superficie
import xyz.umarmohammad.hermit.ui.theme.SuperficieSubtle
import xyz.umarmohammad.hermit.ui.theme.Texto
import xyz.umarmohammad.hermit.util.DeviceProtectionManager
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    app: HermitApplication,
    activePhysicalTag: Tag? = null,
    onTagHandled: () -> Unit = {},
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by remember { mutableIntStateOf(0) }
    var dailyHours by remember { mutableFloatStateOf(4f) }
    var hasNfcTagAdded by remember { mutableStateOf(false) }

    // Live permission states updated on resume
    var isBatteryExempt by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasUsagePermission by remember { mutableStateOf(false) }
    var isDeviceAdminActive by remember { mutableStateOf(false) }
    val isAccessibilityRunning by HermitAccessibilityService.isServiceRunning.collectAsState()

    fun checkAllPermissions() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        hasOverlayPermission = Settings.canDrawOverlays(context)

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps?.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps?.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        hasUsagePermission = mode == AppOpsManager.MODE_ALLOWED

        isDeviceAdminActive = DeviceProtectionManager.isDeviceAdminActive(context)
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
                    6 -> StepUninstallProtection(
                        isGranted = isDeviceAdminActive,
                        onAction = {
                            val intent = DeviceProtectionManager.createEnableAdminIntent(context)
                            context.startActivity(intent)
                        },
                        onNext = { currentStep = 7 }
                    )
                    7 -> StepNfcSetup(
                        app = app,
                        activePhysicalTag = activePhysicalTag,
                        onTagHandled = onTagHandled,
                        onTagPaired = {
                            hasNfcTagAdded = true
                            currentStep = 8
                        },
                        onSkip = {
                            hasNfcTagAdded = false
                            currentStep = 8
                        }
                    )
                    8 -> StepCreateProfile(
                        app = app,
                        hasNfcTag = hasNfcTagAdded,
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
                text = "Una estimación diaria para calcular cuánto tiempo le dedicas a la pantalla.",
                style = MaterialTheme.typography.bodyMedium,
                color = Secundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = stringResource(R.string.onboarding_hours_format, hours.toInt()),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.03).sp
                ),
                color = Acento,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            val annualDaysPreview = ((hours * 365) / 24).toInt()
            val lifetimeYearsPreview = (hours / 24.0) * 80.0
            Text(
                text = "Eso es mucho tiempo: con una esperanza media de vida de 80 años, pasarás ${"%.1f".format(lifetimeYearsPreview)} años de tu vida pegado al móvil ($annualDaysPreview días completos cada año).",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (hours >= 3f) Aviso else Secundario,
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
    val lifetimeYears = (hours / 24.0) * 80.0

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
                text = "El coste real de tu tiempo",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp
                ),
                color = Texto,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Highlight card: Lifetime impact
            AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${"%.1f".format(lifetimeYears)} años de tu vida",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Aviso,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "La esperanza media de vida es de 80 años. Usando el teléfono ${hours.toInt()} horas al día, estás perdiendo ${"%.1f".format(lifetimeYears)} años enteros de tu existencia únicamente mirando una pantalla.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Texto,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AppleDivider(startIndent = false)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Son $annualDays días de 24h completos cada año (${"%.1f".format(awakeMonths)} meses despierto)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
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
// Step 2, 4, 5: Single Permission Screens
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
// Step 3: Accessibility Permission Screen
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
// Step 6: Uninstall Protection Screen
// -------------------------------------------------------------
@Composable
private fun StepUninstallProtection(
    isGranted: Boolean,
    onAction: () -> Unit,
    onNext: () -> Unit
) {
    var showLaterNotice by remember { mutableStateOf(false) }

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
                    tint = if (isGranted) Acento else if (showLaterNotice) Secundario else Acento,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isGranted) {
                    stringResource(R.string.onboarding_uninstall_protection_active_title)
                } else if (showLaterNotice) {
                    stringResource(R.string.onboarding_uninstall_protection_later_title)
                } else {
                    stringResource(R.string.onboarding_uninstall_protection_title)
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Texto,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isGranted) {
                    stringResource(R.string.onboarding_uninstall_protection_active_desc)
                } else if (showLaterNotice) {
                    stringResource(R.string.onboarding_uninstall_protection_later_desc)
                } else {
                    stringResource(R.string.onboarding_uninstall_protection_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Secundario,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isGranted) {
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
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Acento,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.onboarding_perm_status_granted),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Acento
                            )
                        }
                    }
                }
            } else if (showLaterNotice) {
                AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Acento,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ajustes > Diagnósticos del sistema",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Texto
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.onboarding_uninstall_protection_later_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Secundario,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Acento,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.onboarding_uninstall_protection_change_mind),
                        color = Acento
                    )
                }
            } else {
                AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Acento.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Acento,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.onboarding_uninstall_protection_feature_commit_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Texto
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.onboarding_uninstall_protection_feature_commit_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secundario,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        AppleDivider()

                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Acento.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = Acento,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.onboarding_uninstall_protection_feature_safe_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Texto
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.onboarding_uninstall_protection_feature_safe_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secundario,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isGranted || showLaterNotice) {
                TactileButton(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.onboarding_btn_next))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            } else {
                TactileButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.onboarding_uninstall_protection_btn_now))
                }

                OutlinedButton(
                    onClick = { showLaterNotice = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        stringResource(R.string.onboarding_uninstall_protection_btn_later),
                        color = Secundario
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Step 7: NFC Tag Setup (Interactive write / pairing - Skippable)
// -------------------------------------------------------------
@Composable
private fun StepNfcSetup(
    app: HermitApplication,
    activePhysicalTag: Tag?,
    onTagHandled: () -> Unit,
    onTagPaired: () -> Unit,
    onSkip: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isPairingSuccess by remember { mutableStateOf(false) }

    // Pulsing NFC animation
    val pulse = rememberInfiniteTransition(label = "nfc_step_pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfc_scale"
    )

    // Automatically detect physical tag touched to phone during Step 6
    LaunchedEffect(activePhysicalTag) {
        val targetTag = activePhysicalTag
        if (targetTag != null && !isPairingSuccess) {
            coroutineScope.launch(Dispatchers.IO) {
                val tagId = UUID.randomUUID().toString()
                val payload = HermitTagPayload(
                    id = tagId,
                    action = TagAction.TOGGLE.name,
                    profileId = null,
                    name = "Mi Etiqueta Hermit"
                )
                val writeRes = NfcTagManager.writeTag(targetTag, payload)
                val physicalUid = NfcTagManager.bytesToHex(targetTag.id)
                val record = NfcTagRecord(
                    id = tagId,
                    tagPhysicalUid = physicalUid,
                    name = "Mi Etiqueta Hermit",
                    action = TagAction.TOGGLE,
                    targetProfileId = null,
                    lastScannedAt = System.currentTimeMillis()
                )
                app.database.nfcTagDao().insertTag(record)

                withContext(Dispatchers.Main) {
                    isPairingSuccess = true
                    onTagHandled()
                }
            }
        }
    }

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
                    .size(80.dp)
                    .graphicsLayer {
                        scaleX = if (isPairingSuccess) 1f else pulseScale
                        scaleY = if (isPairingSuccess) 1f else pulseScale
                    }
                    .clip(CircleShape)
                    .background(if (isPairingSuccess) Acento.copy(alpha = 0.2f) else Superficie),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPairingSuccess) Icons.Default.CheckCircle else Icons.Default.Nfc,
                    contentDescription = null,
                    tint = Acento,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isPairingSuccess) stringResource(R.string.onboarding_nfc_setup_success)
                else stringResource(R.string.onboarding_nfc_setup_header),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Texto,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isPairingSuccess) "Tu etiqueta física servirá para activar y desactivar tus perfiles de concentración."
                else stringResource(R.string.onboarding_nfc_setup_scan_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Secundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (!isPairingSuccess) {
                AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                    NfcGuideStepRow(step = "1", text = "Consigue una etiqueta NFC adhesiva o llavero compatible (NTAG213 / 215 / 216).")
                    AppleDivider(startIndent = true)
                    NfcGuideStepRow(step = "2", text = "Pégala en tu escritorio o en otra habitación.")
                    AppleDivider(startIndent = true)
                    NfcGuideStepRow(step = "3", text = "Acércala ahora mismo a la parte trasera para vincularla.")
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Acento.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Acento.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Etiqueta vinculada como interruptor físico",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Acento,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ahora configuraremos tu primer perfil de concentración.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Texto,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp)) {
            if (isPairingSuccess) {
                TactileButton(
                    onClick = onTagPaired,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crear mi primer perfil")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            } else {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.onboarding_nfc_setup_skip), color = Secundario)
                }
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
// Step 7: Create First Profile (Adaptive based on NFC tag presence)
// -------------------------------------------------------------
@Composable
private fun StepCreateProfile(
    app: HermitApplication,
    hasNfcTag: Boolean,
    onCompleted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Verify against actual registered NFC tags in the database
    val registeredTags by app.database.nfcTagDao().getAllTags().collectAsState(initial = emptyList())
    val hasActualNfcTag = registeredTags.isNotEmpty()

    // Profile settings
    var profileName by remember { mutableStateOf(if (hasActualNfcTag) "Mi Enfoque" else "Enfoque por Tiempo") }
    var filterSocial by remember { mutableStateOf(true) }
    var filterWork by remember { mutableStateOf(false) }
    var filterStreaming by remember { mutableStateOf(false) }
    var blockWebSocial by remember { mutableStateOf(true) }

    // If hasActualNfcTag: can choose "NFC" or "TIME". If !hasActualNfcTag: strictly "TIME"
    var unlockMode by remember { mutableStateOf(if (hasActualNfcTag) "NFC" else "TIME") }
    // Timer duration: 1 to 720 minutes (default 60m)
    var durationMinutes by remember { mutableIntStateOf(60) }
    // Emergency exit: default allowed after 60s delay
    var allowEmergencyExit by remember { mutableStateOf(true) }

    // Zen Mode
    var isZenSelected by remember { mutableStateOf(false) }
    val selectedZenApps = remember { mutableStateListOf<String>() }
    val selectedCustomBlockedApps = remember { mutableStateListOf<String>() }

    var showAppSelectionScreen by remember { mutableStateOf(false) }
    var showZenAppSelectionScreen by remember { mutableStateOf(false) }

    val socialPresets = remember {
        listOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.twitter.android",
            "com.facebook.katana",
            "com.reddit.frontpage",
            "com.snapchat.android",
            "com.google.android.youtube"
        )
    }
    val workPresets = remember {
        listOf(
            "com.slack",
            "com.microsoft.teams",
            "com.google.android.gm",
            "com.linkedin.android"
        )
    }
    val streamingPresets = remember {
        listOf(
            "com.netflix.mediaclient",
            "tv.twitch.android.app",
            "com.disney.disneyplus"
        )
    }

    LaunchedEffect(Unit) {
        if (selectedCustomBlockedApps.isEmpty() && filterSocial) {
            selectedCustomBlockedApps.addAll(socialPresets)
        }
    }

    if (showAppSelectionScreen) {
        AppSelectionScreen(
            initiallySelectedPackages = selectedCustomBlockedApps.toList(),
            title = "Seleccionar aplicaciones",
            onSaveSelection = { updatedList ->
                selectedCustomBlockedApps.clear()
                selectedCustomBlockedApps.addAll(updatedList)
                showAppSelectionScreen = false
            },
            onBackClick = { showAppSelectionScreen = false }
        )
        return
    }

    if (showZenAppSelectionScreen) {
        AppSelectionScreen(
            initiallySelectedPackages = selectedZenApps.toList(),
            title = stringResource(R.string.zen_select_apps_title),
            maxSelection = 5,
            onSaveSelection = { updatedList ->
                selectedZenApps.clear()
                selectedZenApps.addAll(updatedList)
                showZenAppSelectionScreen = false
            },
            onBackClick = { showZenAppSelectionScreen = false }
        )
        return
    }

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

            // State badge banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (hasActualNfcTag) Acento.copy(alpha = 0.12f) else SuperficieSubtle,
                border = androidx.compose.foundation.BorderStroke(
                    0.75.dp,
                    if (hasActualNfcTag) Acento.copy(alpha = 0.4f) else BorderDark
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasActualNfcTag) Icons.Default.Nfc else Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = if (hasActualNfcTag) Acento else Secundario,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (hasActualNfcTag) stringResource(R.string.onboarding_profile_nfc_badge)
                        else stringResource(R.string.onboarding_profile_timer_only_badge),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (hasActualNfcTag) Acento else Texto
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Name Input
            OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                label = { Text(stringResource(R.string.profile_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // If user has actual NFC tags, allow choosing NFC or TIME
            if (hasActualNfcTag) {
                Text(
                    text = stringResource(R.string.profile_activation_mode),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Secundario,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                val nfcLabel = stringResource(R.string.profile_activation_nfc)
                val timeLabel = stringResource(R.string.profile_activation_time)
                AppleSegmentedControl(
                    items = listOf("NFC", "TIME"),
                    selectedItem = unlockMode,
                    onItemSelected = { unlockMode = it },
                    labelProvider = { if (it == "NFC") nfcLabel else timeLabel }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (unlockMode == "NFC") {
                        stringResource(R.string.profile_activation_nfc_desc)
                    } else {
                        stringResource(R.string.profile_activation_time_desc)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Secundario,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Duration Picker (shown whenever in TIME mode or when no NFC tags exist)
            if (!hasActualNfcTag || unlockMode == "TIME") {
                DurationPickerCard(
                    durationMinutes = durationMinutes,
                    onDurationChange = { durationMinutes = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.profile_emergency_exit_title),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Texto
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (allowEmergencyExit) {
                                        stringResource(R.string.profile_emergency_exit_enabled_desc)
                                    } else {
                                        stringResource(R.string.profile_emergency_exit_disabled_desc)
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = if (allowEmergencyExit) Secundario else Aviso
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = allowEmergencyExit,
                                onCheckedChange = { allowEmergencyExit = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Zen Mode Option Card
            AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_zen_minimalist_title),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Texto
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (hasActualNfcTag && unlockMode == "NFC") {
                                    stringResource(R.string.profile_zen_minimalist_desc)
                                } else {
                                    "Launcher minimalista por tiempo. Al terminar el temporizador vuelve a tu pantalla normal."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Secundario
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isZenSelected,
                            onCheckedChange = { isZenSelected = it }
                        )
                    }

                    if (isZenSelected) {
                        Spacer(modifier = Modifier.height(14.dp))
                        AppleDivider(startIndent = false)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.profile_zen_apps_added_btn, selectedZenApps.size),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Acento
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showZenAppSelectionScreen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (selectedZenApps.isEmpty()) stringResource(R.string.profile_zen_add_apps_btn)
                                else stringResource(R.string.profile_zen_apps_added_btn, selectedZenApps.size)
                            )
                        }
                    }
                }
            }

            if (!isZenSelected) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aplicaciones a bloquear",
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
                        onClick = {
                            filterSocial = !filterSocial
                            if (filterSocial) {
                                socialPresets.forEach { if (!selectedCustomBlockedApps.contains(it)) selectedCustomBlockedApps.add(it) }
                            } else {
                                selectedCustomBlockedApps.removeAll(socialPresets)
                            }
                        },
                        label = { Text(stringResource(R.string.onboarding_filter_social)) }
                    )
                    FilterChip(
                        selected = filterWork,
                        onClick = {
                            filterWork = !filterWork
                            if (filterWork) {
                                workPresets.forEach { if (!selectedCustomBlockedApps.contains(it)) selectedCustomBlockedApps.add(it) }
                            } else {
                                selectedCustomBlockedApps.removeAll(workPresets)
                            }
                        },
                        label = { Text(stringResource(R.string.onboarding_filter_work)) }
                    )
                    FilterChip(
                        selected = filterStreaming,
                        onClick = {
                            filterStreaming = !filterStreaming
                            if (filterStreaming) {
                                streamingPresets.forEach { if (!selectedCustomBlockedApps.contains(it)) selectedCustomBlockedApps.add(it) }
                            } else {
                                selectedCustomBlockedApps.removeAll(streamingPresets)
                            }
                        },
                        label = { Text(stringResource(R.string.onboarding_filter_streaming)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showAppSelectionScreen = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedCustomBlockedApps.isEmpty()) "Seleccionar aplicaciones de tu móvil..."
                        else "${selectedCustomBlockedApps.size} aplicaciones seleccionadas"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                    AppleToggleRow(
                        title = "Navegadores",
                        subtitle = stringResource(R.string.onboarding_block_web_social),
                        checked = blockWebSocial,
                        onCheckedChange = { blockWebSocial = it },
                        icon = Icons.Default.Layers
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp)) {
            TactileButton(
                onClick = {
                    coroutineScope.launch {
                        val dao = app.database.focusProfileDao()

                        val isNfcSession = hasActualNfcTag && (unlockMode == "NFC")
                        val finalExitBehavior = if (isNfcSession) {
                            ExitBehavior.NFC_ONLY
                        } else if (allowEmergencyExit) {
                            ExitBehavior.ALLOW_EMERGENCY
                        } else {
                            ExitBehavior.TIMED_ONLY
                        }

                        val finalDuration = if (isNfcSession) {
                            720
                        } else {
                            durationMinutes.coerceIn(1, 720)
                        }

                        val customProfile = FocusProfile(
                            id = UUID.randomUUID().toString(),
                            name = if (isZenSelected) "Modo Zen" else profileName.ifBlank { "Mi Enfoque" },
                            description = if (isZenSelected) "Launcher minimalista." else "Perfil creado en la bienvenida.",
                            iconName = if (isZenSelected) "spa" else "work",
                            colorHex = "#A5B092",
                            blockedPackages = if (isZenSelected) emptyList() else selectedCustomBlockedApps.distinct(),
                            durationMinutes = finalDuration,
                            exitBehavior = finalExitBehavior,
                            strictMode = true,
                            isDefault = true,
                            isMonkMode = isZenSelected,
                            blockWebSocial = blockWebSocial,
                            activationType = if (isNfcSession) "NFC" else "TIME",
                            zenAllowedPackages = if (isZenSelected) selectedZenApps.distinct().take(5).toList() else emptyList()
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
