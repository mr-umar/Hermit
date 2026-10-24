package xyz.umarmohammad.hermit.ui.launcher

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.ui.components.AppleDivider
import xyz.umarmohammad.hermit.ui.components.AppleGroupedCard
import xyz.umarmohammad.hermit.ui.components.TactileButton
import xyz.umarmohammad.hermit.ui.theme.Acento
import xyz.umarmohammad.hermit.ui.theme.BorderDark
import xyz.umarmohammad.hermit.ui.theme.Fondo
import xyz.umarmohammad.hermit.ui.theme.Secundario
import xyz.umarmohammad.hermit.ui.theme.Superficie
import xyz.umarmohammad.hermit.ui.theme.SuperficieSubtle
import xyz.umarmohammad.hermit.ui.theme.Texto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ZenLauncherAppItem(
    val packageName: String,
    val label: String,
    val iconDrawable: Drawable? = null,
    val vectorIcon: ImageVector? = null,
    val isSystemEssential: Boolean = false
)

@Composable
fun ZenLauncherScreen(
    isZenActive: Boolean,
    isNfcExit: Boolean = true,
    profileName: String,
    remainingSeconds: Long?,
    elapsedSeconds: Long = 0L,
    apps: List<ZenLauncherAppItem>,
    onLaunchApp: (String) -> Unit,
    onOpenHermit: () -> Unit,
    onExitLauncher: () -> Unit
) {
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }
    var showExitInfoDialog by remember { mutableStateOf(false) }

    // Live clock ticker
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL, Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now).replaceFirstChar { it.uppercase() }
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfc_pulse_scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Fondo
    ) {
        if (!isZenActive) {
            // Idle screen when Zen mode is not running
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Superficie),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = Acento,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.zen_launcher_idle_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Texto,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.zen_launcher_idle_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secundario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                TactileButton(
                    onClick = onOpenHermit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.zen_launcher_open_hermit))
                }

                Spacer(modifier = Modifier.height(12.dp))

                TactileButton(
                    onClick = onExitLauncher,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.zen_launcher_go_system_home))
                }
            }
        } else {
            // Active Zen Mode Launcher
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Top Minimal Header: Clock & Date
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTimeString.ifEmpty { "--:--" },
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.04).sp
                        ),
                        color = Texto
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = currentDateString,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.01).sp
                        ),
                        color = Secundario
                    )

                    if (remainingSeconds != null && remainingSeconds > 0) {
                        val hours = remainingSeconds / 3600
                        val mins = (remainingSeconds % 3600) / 60
                        val remainingText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.zen_launcher_remaining_label, remainingText),
                            style = MaterialTheme.typography.labelSmall,
                            color = Secundario.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Allowed Apps List (Scrollable if needed, beautifully styled)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        ZenAppRow(
                            app = app,
                            onClick = { onLaunchApp(app.packageName) }
                        )
                    }

                    item {
                        // Hermit app row to inspect status
                        ZenAppRow(
                            app = ZenLauncherAppItem(
                                packageName = "xyz.umarmohammad.hermit",
                                label = "Hermit",
                                vectorIcon = Icons.Default.SelfImprovement
                            ),
                            onClick = onOpenHermit
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Bottom deactivation section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Superficie)
                        .border(0.75.dp, BorderDark, RoundedCornerShape(18.dp))
                        .clickable(enabled = isNfcExit) {
                            showExitInfoDialog = true
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    if (isNfcExit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                                    .clip(CircleShape)
                                    .background(Acento.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = Acento,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = stringResource(R.string.zen_launcher_nfc_prompt),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Texto,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Acento.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = Acento,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Modo Zen por tiempo",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Texto
                                )
                                Text(
                                    text = "Finaliza al terminar el temporizador",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Secundario
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showExitInfoDialog) {
            val effectiveRemainingSec = remainingSeconds ?: (12 * 3600L - elapsedSeconds).coerceAtLeast(0L)
            val safeHours = effectiveRemainingSec / 3600
            val safeMinutes = (effectiveRemainingSec % 3600) / 60
            val safeSeconds = effectiveRemainingSec % 60
            val formattedSafetyTime = if (safeHours > 0) {
                "%02d:%02d:%02d".format(safeHours, safeMinutes, safeSeconds)
            } else {
                "%02d:%02d".format(safeMinutes, safeSeconds)
            }

            AlertDialog(
                onDismissRequest = { showExitInfoDialog = false },
                containerColor = Superficie,
                titleContentColor = Texto,
                textContentColor = Texto,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = null,
                        tint = Acento,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.zen_launcher_exit_dialog_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.zen_launcher_exit_dialog_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Texto.copy(alpha = 0.9f)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SuperficieSubtle,
                            border = androidx.compose.foundation.BorderStroke(0.75.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.zen_launcher_exit_dialog_remaining_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secundario
                                )
                                Text(
                                    text = formattedSafetyTime,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Acento
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showExitInfoDialog = false }
                    ) {
                        Text(
                            text = stringResource(R.string.zen_launcher_exit_dialog_btn),
                            color = Acento,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ZenAppRow(
    app: ZenLauncherAppItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Superficie,
        border = androidx.compose.foundation.BorderStroke(0.75.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SuperficieSubtle),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconDrawable != null) {
                    val bitmap = app.iconDrawable.toBitmap(width = 96, height = 96)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(28.dp)
                    )
                } else if (app.vectorIcon != null) {
                    Icon(
                        imageVector = app.vectorIcon,
                        contentDescription = app.label,
                        tint = Acento,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = app.label,
                        tint = Acento,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.01).sp
                ),
                color = Texto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
