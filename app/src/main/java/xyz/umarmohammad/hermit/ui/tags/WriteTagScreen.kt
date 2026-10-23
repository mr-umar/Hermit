package xyz.umarmohammad.hermit.ui.tags

import android.nfc.Tag
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import xyz.umarmohammad.hermit.ui.components.GlassCard
import xyz.umarmohammad.hermit.ui.components.HermitTopBar
import xyz.umarmohammad.hermit.ui.components.TactileButton
import java.util.UUID

enum class WriteStage {
    CONFIGURE,
    WAITING_FOR_TAG,
    WRITING,
    SUCCESS,
    ERROR
}

@Composable
fun WriteTagScreen(
    tagRepository: TagRepository,
    profileRepository: ProfileRepository,
    activePhysicalTag: Tag?,
    onTagHandled: () -> Unit,
    onBackClick: () -> Unit
) {
    val profiles by profileRepository.allProfiles.collectAsState(initial = emptyList())

    var tagName by remember { mutableStateOf("Focus Tag") }
    var tagDescription by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf(TagAction.TOGGLE) }
    var selectedProfileId by remember { mutableStateOf<String?>(null) }

    var stage by remember { mutableStateOf(WriteStage.CONFIGURE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingTagToWrite by remember { mutableStateOf<Tag?>(null) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var existingTagSummary by remember { mutableStateOf("") }

    LaunchedEffect(profiles) {
        if (selectedProfileId == null && profiles.isNotEmpty()) {
            selectedProfileId = profiles.first().id
        }
    }

    // Function to perform real write
    val performWrite = { targetTag: Tag ->
        stage = WriteStage.WRITING
        val tagId = UUID.randomUUID().toString()
        val payload = HermitTagPayload(
            id = tagId,
            action = selectedAction.name,
            profileId = selectedProfileId,
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
                    description = tagDescription.trim(),
                    action = selectedAction,
                    targetProfileId = selectedProfileId,
                    lastScannedAt = System.currentTimeMillis()
                )
                tagRepository.saveTag(record)

                launch(Dispatchers.Main) {
                    stage = WriteStage.SUCCESS
                    onTagHandled()
                }
            } else {
                val err = result.exceptionOrNull()
                launch(Dispatchers.Main) {
                    stage = WriteStage.ERROR
                    errorMessage = when (err?.message) {
                        "READ_ONLY" -> "Tag is read-only or locked."
                        "VERIFICATION_FAILED" -> "Data verification failed after write. Please try again."
                        "UNSUPPORTED_TAG_TECHNOLOGY" -> "Tag format is unsupported. Standard NDEF tags are required."
                        else -> err?.localizedMessage ?: "Failed to write tag. Make sure it stays in range."
                    }
                    onTagHandled()
                }
            }
        }
    }

    // React when physical tag is presented
    LaunchedEffect(activePhysicalTag) {
        if (stage == WriteStage.WAITING_FOR_TAG && activePhysicalTag != null) {
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
    }

    Scaffold(
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
                            color = MaterialTheme.colorScheme.primary
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action selector
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = stringResource(R.string.tag_detail_action),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TagAction.entries.forEach { action ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedAction = action }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (selectedAction == action),
                                            onClick = { selectedAction = action }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(action.titleRes),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Target Profile selector
                        if (selectedAction != TagAction.DEACTIVATE) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.tag_detail_profile),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    profiles.forEach { profile ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedProfileId = profile.id }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (selectedProfileId == profile.id),
                                                onClick = { selectedProfileId = profile.id }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        TactileButton(
                            onClick = { stage = WriteStage.WAITING_FOR_TAG },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = tagName.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.Default.Nfc, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.write_tag_step2))
                        }
                    }
                }

                WriteStage.WAITING_FOR_TAG, WriteStage.WRITING -> {
                    val pulse = rememberInfiniteTransition(label = "nfc_pulse")
                    val pulseScale by pulse.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200),
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
                                .size(140.dp)
                                .scale(pulseScale)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        Text(
                            text = if (stage == WriteStage.WRITING) {
                                stringResource(R.string.write_tag_writing)
                            } else {
                                stringResource(R.string.write_tag_ready_prompt)
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Keep your device still until write and verification complete.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        TextButton(onClick = { stage = WriteStage.CONFIGURE }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }

                WriteStage.SUCCESS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.write_tag_success),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "\"$tagName\" is registered and verified. Tap it anytime to control your focus.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        TactileButton(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.write_tag_btn_finish))
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
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.write_tag_error_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = errorMessage ?: stringResource(R.string.write_tag_error_lost),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

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
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            }

            // Overwrite safety dialog
            if (showOverwriteDialog && pendingTagToWrite != null) {
                AlertDialog(
                    onDismissRequest = {
                        showOverwriteDialog = false
                        pendingTagToWrite = null
                        onTagHandled()
                    },
                    title = { Text("Overwrite Tag Data?") },
                    text = { Text(stringResource(R.string.write_tag_overwrite_warning, existingTagSummary)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showOverwriteDialog = false
                                pendingTagToWrite?.let { performWrite(it) }
                                pendingTagToWrite = null
                            }
                        ) {
                            Text(stringResource(R.string.write_tag_overwrite_confirm), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showOverwriteDialog = false
                                pendingTagToWrite = null
                                stage = WriteStage.CONFIGURE
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
