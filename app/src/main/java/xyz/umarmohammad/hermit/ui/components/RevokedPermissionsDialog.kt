package xyz.umarmohammad.hermit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.ui.theme.Acento
import xyz.umarmohammad.hermit.ui.theme.Aviso
import xyz.umarmohammad.hermit.ui.theme.BorderDark
import xyz.umarmohammad.hermit.ui.theme.Secundario
import xyz.umarmohammad.hermit.ui.theme.Superficie
import xyz.umarmohammad.hermit.ui.theme.SuperficieSubtle
import xyz.umarmohammad.hermit.ui.theme.Texto
import xyz.umarmohammad.hermit.util.RequiredPermissionManager

@Composable
fun RevokedPermissionsDialog(
    status: RequiredPermissionManager.RequiredPermissionsStatus
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = {}, // Persistent until required permissions are restored
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
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
                        .background(Aviso.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Aviso,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.perms_revoked_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).sp
                    ),
                    color = Texto,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.perms_revoked_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Secundario,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Permissions List
                AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        // 1. Accessibility Service
                        PermissionItemRow(
                            icon = Icons.Default.Security,
                            title = stringResource(R.string.perms_revoked_accessibility_title),
                            description = stringResource(R.string.perms_revoked_accessibility_desc),
                            isGranted = status.isAccessibilityGranted,
                            onGrant = { RequiredPermissionManager.requestAccessibility(context) }
                        )

                        AppleDivider()

                        // 2. Overlay
                        PermissionItemRow(
                            icon = Icons.Default.Layers,
                            title = stringResource(R.string.perms_revoked_overlay_title),
                            description = stringResource(R.string.perms_revoked_overlay_desc),
                            isGranted = status.isOverlayGranted,
                            onGrant = { RequiredPermissionManager.requestOverlay(context) }
                        )

                        AppleDivider()

                        // 3. Usage Stats
                        PermissionItemRow(
                            icon = Icons.Default.Timeline,
                            title = stringResource(R.string.perms_revoked_usage_title),
                            description = stringResource(R.string.perms_revoked_usage_desc),
                            isGranted = status.isUsageStatsGranted,
                            onGrant = { RequiredPermissionManager.requestUsageStats(context) }
                        )

                        AppleDivider()

                        // 4. Battery Exemption
                        PermissionItemRow(
                            icon = Icons.Default.BatteryAlert,
                            title = stringResource(R.string.perms_revoked_battery_title),
                            description = stringResource(R.string.perms_revoked_battery_desc),
                            isGranted = status.isBatteryExempt,
                            onGrant = { RequiredPermissionManager.requestBatteryExemption(context) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Acento else Aviso,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Texto
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Secundario,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Acento,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.perms_revoked_status_granted),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Acento
                )
            }
        } else {
            OutlinedButton(
                onClick = onGrant,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Acento),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Acento),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 4.dp
                )
            ) {
                Text(
                    text = stringResource(R.string.perms_revoked_btn_grant),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
