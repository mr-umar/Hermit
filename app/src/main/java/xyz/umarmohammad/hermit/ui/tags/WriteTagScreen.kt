package xyz.umarmohammad.hermit.ui.tags

import android.nfc.Tag
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.model.NfcTagRecord
import xyz.umarmohammad.hermit.data.model.TagAction
import xyz.umarmohammad.hermit.data.repository.ProfileRepository
import xyz.umarmohammad.hermit.data.repository.TagRepository
import xyz.umarmohammad.hermit.nfc.HermitTagPayload
import xyz.umarmohammad.hermit.nfc.NfcTagManager
import xyz.umarmohammad.hermit.ui.components.AppleDivider
import xyz.umarmohammad.hermit.ui.components.AppleGroupedCard
import xyz.umarmohammad.hermit.ui.components.AppleListRow
import xyz.umarmohammad.hermit.ui.components.GlassCard
import xyz.umarmohammad.hermit.ui.components.HermitTopBar
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

enum class WriteStage {
    CONFIGURE,
    WAITING_FOR_TAG,
    WRITING,
    TEST_PROMPT,
    TEST_SUCCESS,
    ERROR
}

@Composable
fun WriteTagScreen(
    tagRepository: TagRepository,
    activePhysicalTag: Tag?,
    onTagHandled: () -> Unit,
    onBackClick: () -> Unit
) {
    var tagName by remember { mutableStateOf("Etiqueta física") }

    var stage by remember { mutableStateOf(WriteStage.CONFIGURE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingTagToWrite by remember { mutableStateOf<Tag?>(null) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var existingTagSummary by remember { mutableStateOf("") }

    var writtenTagId by remember { mutableStateOf<String?>(null) }
    var writtenTagUid by remember { mutableStateOf<String?>(null) }
    var testWarningMessage by remember { mutableStateOf<String?>(null) }

    // Real write operation with pre-flight check and readback
    val performWrite = { targetTag: Tag ->
        stage = WriteStage.WRITING
        val tagId = UUID.randomUUID().toString()
        val payload = HermitTagPayload(
            id = tagId,
            action = TagAction.TOGGLE.name,
            profileId = null,
            name = tagName.trim()
        )

        CoroutineScope(Dispatchers.IO).launch {
            val result = NfcTagManager.writeTag(targetTag, payload)
            if (result.isSuccess) {
                val physicalUid = NfcTagManager.bytesToHex(targetTag.id)
                val record = NfcTagRecord(
                    id = tagId,
                    tagPhysicalUid = physicalUid,
                    name = tagName.trim(),
                    description = "",
                    action = TagAction.TOGGLE,
                    targetProfileId = null,
                    lastScannedAt = System.currentTimeMillis()
                )
                tagRepository.saveTag(record)

                launch(Dispatchers.Main) {
                    writtenTagId = tagId
                    writtenTagUid = physicalUid
                    testWarningMessage = null
                    // Move to safe test prompt without triggering sessions
                    stage = WriteStage.TEST_PROMPT
                    onTagHandled()
                }
            } else {
                val err = result.exceptionOrNull()
                launch(Dispatchers.Main) {
                    stage = WriteStage.ERROR
                    errorMessage = when (err?.message) {
                        "READ_ONLY" -> "This tag is locked or read-only."
                        "VERIFICATION_FAILED" -> "Data verification failed after write. Please try again."
                        "UNSUPPORTED_TAG_TECHNOLOGY" -> "Tag format is unsupported. Standard NDEF tags are required."
                        else -> err?.localizedMessage ?: "Failed to write tag. Make sure it stays in range."
                    }
                    onTagHandled()
                }
            }
        }
    }

    // Physical tag detection dispatcher
    LaunchedEffect(activePhysicalTag) {
        if (activePhysicalTag == null) return@LaunchedEffect

        when (stage) {
            WriteStage.WAITING_FOR_TAG -> {
                val info = NfcTagManager.inspectTag(activePhysicalTag)
                if (!info.isWritable) {
                    stage = WriteStage.ERROR
                    errorMessage = "This tag is locked or read-only."
                    onTagHandled()
                    return@LaunchedEffect
                }
                if (info.hasExistingData) {
                    existingTagSummary = info.existingSummary ?: "Existing NDEF records"
                    pendingTagToWrite = activePhysicalTag
                    showOverwriteDialog = true
                } else {
                    performWrite(activePhysicalTag)
                }
            }
            WriteStage.TEST_PROMPT -> {
                // Safe check without starting or stopping sessions
                val payload = NfcTagManager.readHermitPayload(activePhysicalTag)
                val physicalUid = NfcTagManager.bytesToHex(activePhysicalTag.id)

                if (payload != null && (writtenTagId == null || payload.id == writtenTagId || payload.name == tagName.trim())) {
                    writtenTagUid = physicalUid
                    testWarningMessage = null
                    stage = WriteStage.TEST_SUCCESS
                } else {
                    val otherSummary = payload?.name ?: physicalUid
                    testWarningMessage = "Se detectó otra etiqueta ($otherSummary). Acerca la que acabas de configurar."
                }
                onTagHandled()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Fondo,
        topBar = {
            HermitTopBar(
                title = stringResource(R.string.write_tag_title),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (stage) {
                WriteStage.CONFIGURE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = stringResource(R.string.write_tag_step1),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Acento
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = tagName,
                            onValueChange = { tagName = it },
                            label = { Text(stringResource(R.string.tag_detail_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Universal toggle explanation card
                        AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = Acento,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.write_tag_universal_title),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Texto
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.write_tag_universal_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Secundario
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        TactileButton(
                            onClick = { stage = WriteStage.WAITING_FOR_TAG },
                            enabled = tagName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.write_tag_step2))
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                WriteStage.WAITING_FOR_TAG -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_write")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.94f,
                        targetValue = 1.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1100, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                                .clip(CircleShape)
                                .background(Acento.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Superficie),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = Acento,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = stringResource(R.string.write_tag_step2),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Texto,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.write_tag_ready_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Secundario,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedButton(
                            onClick = { stage = WriteStage.CONFIGURE },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.action_cancel), color = Secundario)
                        }
                    }
                }

                WriteStage.WRITING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Acento,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.write_tag_writing),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Texto,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.write_tag_verifying),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Secundario,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                WriteStage.TEST_PROMPT -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_test")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.94f,
                        targetValue = 1.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1100, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                                    .clip(CircleShape)
                                    .background(Acento.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(Superficie),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Nfc,
                                        contentDescription = null,
                                        tint = Acento,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = stringResource(R.string.write_tag_test_prompt_title),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Texto,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = stringResource(R.string.write_tag_test_prompt_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secundario,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Safe note callout: does not start/stop session
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SuperficieSubtle)
                                    .border(0.75.dp, Acento.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Acento,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.write_tag_test_prompt_safe_note),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                        color = Texto
                                    )
                                }
                            }

                            if (testWarningMessage != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Superficie)
                                        .border(0.75.dp, Aviso.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Aviso,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = testWarningMessage!!,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                            color = Aviso
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp, bottom = 16.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBackClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.write_tag_btn_skip_test), color = Secundario)
                            }
                        }
                    }
                }

                WriteStage.TEST_SUCCESS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Acento,
                                modifier = Modifier.size(68.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = stringResource(R.string.write_tag_test_success_title),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Texto,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.write_tag_test_success_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secundario,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Apple-style verified configuration summary
                            AppleGroupedCard(modifier = Modifier.fillMaxWidth()) {
                                AppleListRow(
                                    title = stringResource(R.string.tag_detail_name),
                                    value = tagName,
                                    icon = Icons.Default.Nfc,
                                    iconTint = Acento
                                )

                                AppleDivider(startIndent = true)

                                AppleListRow(
                                    title = stringResource(R.string.tag_detail_action),
                                    value = stringResource(R.string.write_tag_toggle_action),
                                    icon = Icons.Default.PlayArrow,
                                    iconTint = Acento
                                )

                                if (writtenTagUid != null) {
                                    AppleDivider(startIndent = true)

                                    AppleListRow(
                                        title = "UID Físico",
                                        value = writtenTagUid,
                                        icon = Icons.Default.Fingerprint,
                                        iconTint = Secundario
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Background opening info callout
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SuperficieSubtle)
                                    .border(0.75.dp, BorderDark, RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Acento,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.write_tag_test_background_info),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                                        color = Texto
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp, bottom = 16.dp)
                        ) {
                            TactileButton(
                                onClick = onBackClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.write_tag_btn_finish))
                            }
                        }
                    }
                }

                WriteStage.ERROR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Aviso,
                            modifier = Modifier.size(68.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = stringResource(R.string.write_tag_error_title),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Aviso,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = errorMessage ?: stringResource(R.string.write_tag_error_lost),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Secundario,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        TactileButton(
                            onClick = {
                                stage = WriteStage.WAITING_FOR_TAG
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.write_tag_retry))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = onBackClick) {
                            Text(stringResource(R.string.action_cancel), color = Secundario)
                        }
                    }
                }
            }

            // Overwrite confirmation dialog
            if (showOverwriteDialog && pendingTagToWrite != null) {
                AlertDialog(
                    onDismissRequest = {
                        showOverwriteDialog = false
                        pendingTagToWrite = null
                        onTagHandled()
                    },
                    title = { Text(stringResource(R.string.dialog_error_title)) },
                    text = { Text(stringResource(R.string.write_tag_overwrite_warning, existingTagSummary)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showOverwriteDialog = false
                                pendingTagToWrite?.let { performWrite(it) }
                                pendingTagToWrite = null
                            }
                        ) {
                            Text(stringResource(R.string.write_tag_overwrite_confirm), color = Aviso)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showOverwriteDialog = false
                                pendingTagToWrite = null
                                onTagHandled()
                            }
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
        }
    }
}
