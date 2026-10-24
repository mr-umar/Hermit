package xyz.umarmohammad.hermit.ui.tags

import android.nfc.Tag
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.model.NfcTagRecord
import xyz.umarmohammad.hermit.data.repository.TagRepository
import xyz.umarmohammad.hermit.nfc.NfcCapabilityInfo
import xyz.umarmohammad.hermit.nfc.NfcTagManager
import xyz.umarmohammad.hermit.ui.components.GlassCard
import xyz.umarmohammad.hermit.ui.components.HermitTopBar
import xyz.umarmohammad.hermit.ui.components.TactileButton

@Composable
fun ReadTagScreen(
    tagRepository: TagRepository,
    activePhysicalTag: Tag?,
    onTagHandled: () -> Unit,
    onBackClick: () -> Unit
) {
    var scannedInfo by remember { mutableStateOf<NfcCapabilityInfo?>(null) }
    var isImported by remember { mutableStateOf(false) }
    var customTagName by remember { mutableStateOf("") }

    LaunchedEffect(activePhysicalTag) {
        if (activePhysicalTag != null) {
            val info = NfcTagManager.inspectTag(activePhysicalTag)
            scannedInfo = info
            isImported = false
            customTagName = info.existingPayload?.name ?: "Etiqueta física"
            onTagHandled()
        }
    }

    Scaffold(
        topBar = {
            HermitTopBar(
                title = stringResource(R.string.read_tag_title),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val info = scannedInfo

            if (info == null) {
                // Waiting for physical scan
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = stringResource(R.string.read_tag_prompt),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                // Display Scanned Details
                Text(
                    text = stringResource(R.string.read_tag_scanned_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )

                // Status Banner: Hermit vs Generic
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (info.existingPayload != null) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (info.existingPayload != null) Icons.Default.Check else Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (info.existingPayload != null) {
                                stringResource(R.string.read_tag_hermit_detected)
                            } else if (info.hasExistingData) {
                                stringResource(R.string.read_tag_generic_detected)
                            } else {
                                stringResource(R.string.read_tag_empty)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Technical Parameters
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TagDetailRow(label = stringResource(R.string.read_tag_serial), value = info.tagUidHex)
                        TagDetailRow(label = stringResource(R.string.read_tag_type), value = info.chipType)
                        TagDetailRow(
                            label = stringResource(R.string.read_tag_writable),
                            value = if (info.isWritable) stringResource(R.string.read_tag_writable_yes) else stringResource(R.string.read_tag_writable_no)
                        )
                        TagDetailRow(
                            label = stringResource(R.string.read_tag_capacity),
                            value = "${info.maxNdefSize} bytes (${info.currentNdefSize} bytes used)"
                        )
                        TagDetailRow(label = stringResource(R.string.read_tag_tech), value = info.techList.joinToString(", "))
                    }
                }

                // Hermit payload contents if present
                info.existingPayload?.let { payload ->
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Hermit Configuration",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            TagDetailRow(label = "Tag ID", value = payload.id)
                            TagDetailRow(label = "Action", value = payload.action)
                            payload.profileId?.let { pid ->
                                TagDetailRow(label = "Profile ID", value = pid)
                            }
                            payload.name?.let { pName ->
                                TagDetailRow(label = "Label", value = pName)
                            }
                        }
                    }

                    if (!isImported) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = customTagName,
                            onValueChange = { customTagName = it },
                            label = { Text(stringResource(R.string.tag_detail_name)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TactileButton(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val record = NfcTagRecord(
                                        id = payload.id,
                                        tagPhysicalUid = info.tagUidHex,
                                        name = customTagName.trim().ifBlank { payload.name ?: "Etiqueta física" },
                                        action = payload.getResolvedAction(),
                                        targetProfileId = payload.profileId,
                                        lastScannedAt = System.currentTimeMillis()
                                    )
                                    tagRepository.saveTag(record)
                                    launch(Dispatchers.Main) {
                                        isImported = true
                                    }
                                }
                            },
                            enabled = customTagName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.read_tag_action_import))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TactileButton(
                    onClick = {
                        scannedInfo = null
                        isImported = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.read_tag_action_rescan))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TagDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1.5f, fill = false)
        )
    }
}
