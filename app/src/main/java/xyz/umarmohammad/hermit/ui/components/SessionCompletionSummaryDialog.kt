package xyz.umarmohammad.hermit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.session.SessionCompletionSummary
import xyz.umarmohammad.hermit.ui.theme.Acento
import xyz.umarmohammad.hermit.ui.theme.Aviso
import xyz.umarmohammad.hermit.ui.theme.BorderDark
import xyz.umarmohammad.hermit.ui.theme.Fondo
import xyz.umarmohammad.hermit.ui.theme.Secundario
import xyz.umarmohammad.hermit.ui.theme.Superficie
import xyz.umarmohammad.hermit.ui.theme.SuperficieSubtle
import xyz.umarmohammad.hermit.ui.theme.Texto

@Composable
fun SessionCompletionSummaryDialog(
    summary: SessionCompletionSummary,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val formattedDuration = remember(summary.durationSeconds) {
        val totalSec = summary.durationSeconds
        if (totalSec < 60) {
            context.getString(R.string.session_summary_sec_format, totalSec)
        } else {
            val minutes = totalSec / 60
            if (minutes < 60) {
                context.getString(R.string.session_summary_min_format, minutes)
            } else {
                val hours = minutes / 60
                val remMin = minutes % 60
                if (remMin == 0L) {
                    context.getString(R.string.session_summary_hours_format, hours)
                } else {
                    context.getString(R.string.session_summary_hours_min_format, hours, remMin)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(26.dp),
            color = Superficie,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Acento.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Acento,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.session_summary_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).sp
                    ),
                    color = Texto,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.session_summary_nfc_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Secundario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Duration Banner
                AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.session_summary_duration_prefix),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Secundario
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formattedDuration,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.02).sp
                            ),
                            color = Acento,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.session_summary_profile_label, summary.profileName),
                            style = MaterialTheme.typography.labelMedium,
                            color = Texto.copy(alpha = 0.75f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Overview
                AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        // 1. Phone unlocks
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SuperficieSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (summary.unlockCount == 0) Acento else Aviso,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.session_summary_unlocks_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secundario
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (summary.unlockCount == 0) {
                                        stringResource(R.string.session_summary_unlocks_zero)
                                    } else {
                                        stringResource(R.string.session_summary_unlocks_desc, summary.unlockCount)
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Texto
                                )
                            }
                        }

                        AppleDivider()

                        // 2. Blocked distractions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SuperficieSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Acento,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.session_summary_blocked_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secundario
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (summary.totalBlockedCount == 0) {
                                        stringResource(R.string.session_summary_blocked_zero)
                                    } else {
                                        stringResource(R.string.session_summary_blocked_desc, summary.totalBlockedCount)
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Texto
                                )
                            }
                        }
                    }
                }

                // Top Apps Checked or Blocked
                if (summary.topApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = Secundario,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.session_summary_apps_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Texto
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            summary.topApps.forEachIndexed { index, app ->
                                if (index > 0) AppleDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconBitmap = remember(app.packageName) {
                                        try {
                                            context.packageManager
                                                .getApplicationIcon(app.packageName)
                                                .toBitmap(64, 64)
                                                .asImageBitmap()
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }

                                    if (iconBitmap != null) {
                                        Image(
                                            bitmap = iconBitmap,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Apps,
                                            contentDescription = null,
                                            tint = Secundario,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = Texto,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (app.count == 1) {
                                                stringResource(R.string.session_summary_app_viewed_once)
                                            } else {
                                                stringResource(R.string.session_summary_app_viewed, app.count)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Secundario
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Acento.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${app.count}x",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Acento
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                TactileButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.session_summary_btn_dismiss))
                }
            }
        }
    }
}
