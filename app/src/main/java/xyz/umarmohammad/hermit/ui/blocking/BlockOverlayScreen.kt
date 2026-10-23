package xyz.umarmohammad.hermit.ui.blocking

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.ui.components.TactileButton
import xyz.umarmohammad.hermit.ui.theme.Acento
import xyz.umarmohammad.hermit.ui.theme.Fondo
import xyz.umarmohammad.hermit.ui.theme.Secundario
import xyz.umarmohammad.hermit.ui.theme.Texto

@Composable
fun BlockOverlayScreen(
    blockedAppName: String,
    blockedAppIcon: Drawable?,
    profileName: String,
    remainingSeconds: Long?,
    strictCountdownRemaining: Int?,
    onGoHome: () -> Unit,
    onEmergencyExit: () -> Unit
) {
    val context = LocalContext.current

    // Subtle gentle breathing pulse on background sanctuary orb
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sanctuary_pulse"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Fondo
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Profile context
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Texto.copy(alpha = 0.08f),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Acento, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = profileName,
                        style = MaterialTheme.typography.labelLarge,
                        color = Texto
                    )
                }
            }

            // Central sanctuary core
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .background(Acento.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(Acento.copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (blockedAppIcon != null) {
                            val bitmap = blockedAppIcon.toBitmap(width = 128, height = 128)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = blockedAppName,
                                modifier = Modifier.size(54.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Texto,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.block_screen_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = Texto,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.block_screen_subtitle, blockedAppName, profileName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Secundario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.block_screen_calm_quote),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secundario.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (remainingSeconds != null && remainingSeconds > 0) {
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    val timeStr = "%02d:%02d".format(minutes, seconds)

                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Texto.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = stringResource(R.string.home_time_remaining, timeStr),
                            style = MaterialTheme.typography.labelLarge,
                            color = Acento,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom Actions & NFC Guidance
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // NFC Instruction Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Texto.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = Acento,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.block_screen_nfc_instruction),
                            style = MaterialTheme.typography.labelLarge,
                            color = Texto
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Go to Home Launcher
                TactileButton(
                    onClick = onGoHome,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Acento,
                        contentColor = Fondo
                    )
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.block_screen_btn_home))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Emergency Exit Button (Handles strict reflection delay)
                if (strictCountdownRemaining != null && strictCountdownRemaining > 0) {
                    Text(
                        text = stringResource(R.string.block_screen_emergency_countdown, strictCountdownRemaining),
                        style = MaterialTheme.typography.labelSmall,
                        color = Secundario.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    OutlinedButton(
                        onClick = onEmergencyExit,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Secundario
                        )
                    ) {
                        Text(stringResource(R.string.block_screen_btn_emergency))
                    }
                }
            }
        }
    }
}
