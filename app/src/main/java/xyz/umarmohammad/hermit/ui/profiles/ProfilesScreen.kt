package xyz.umarmohammad.hermit.ui.profiles

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.model.FocusProfile
import xyz.umarmohammad.hermit.data.repository.ProfileRepository
import xyz.umarmohammad.hermit.data.repository.TagRepository
import xyz.umarmohammad.hermit.ui.components.EmptyStateView
import xyz.umarmohammad.hermit.ui.components.GlassCard
import xyz.umarmohammad.hermit.ui.components.HermitTopBar

fun getProfileIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "work" -> Icons.Default.Work
        "school" -> Icons.Default.School
        "spa" -> Icons.Default.Spa
        "bedtime" -> Icons.Default.Bedtime
        "code" -> Icons.Default.Code
        "book" -> Icons.Default.Book
        else -> Icons.Default.AutoAwesome
    }
}

@Composable
fun ProfilesScreen(
    profileRepository: ProfileRepository,
    tagRepository: TagRepository? = null,
    onEditProfile: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onAddTagClick: () -> Unit = {}
) {
    val profiles by profileRepository.allProfiles.collectAsState(initial = emptyList())
    val tags by tagRepository?.allTags?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val hasTags = tags.isNotEmpty()
    val coroutineScope = rememberCoroutineScope()
    var profileToDelete by remember { mutableStateOf<FocusProfile?>(null) }
    var showFrictionDialog by remember { mutableStateOf(false) }

    val handleCreateClick = {
        if (hasTags) {
            onCreateProfile()
        } else {
            showFrictionDialog = true
        }
    }

    Scaffold(
        topBar = {
            HermitTopBar(title = stringResource(R.string.profiles_title))
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = handleCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.profiles_add))
            }
        }
    ) { innerPadding ->
        if (profiles.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(R.string.home_no_profiles_title),
                description = stringResource(R.string.home_no_profiles_desc),
                actionButtonText = stringResource(R.string.home_btn_create_first_profile),
                onActionClick = handleCreateClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCardItem(
                        profile = profile,
                        onEdit = { onEditProfile(profile.id) },
                        onDelete = { profileToDelete = profile }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Physical Friction Guidance Dialog before creating profile (only shown if no tags exist)
        if (showFrictionDialog) {
            AlertDialog(
                onDismissRequest = { showFrictionDialog = false },
                title = { Text(stringResource(R.string.profile_friction_dialog_title)) },
                text = { Text(stringResource(R.string.profile_friction_dialog_desc)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showFrictionDialog = false
                            onAddTagClick()
                        }
                    ) {
                        Text(stringResource(R.string.profile_friction_dialog_setup_tag))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showFrictionDialog = false
                            onCreateProfile()
                        }
                    ) {
                        Text(stringResource(R.string.profile_friction_dialog_continue))
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        profileToDelete?.let { targetProfile ->
            AlertDialog(
                onDismissRequest = { profileToDelete = null },
                title = { Text(stringResource(R.string.profiles_delete_confirm_title)) },
                text = { Text(stringResource(R.string.profiles_delete_confirm_desc, targetProfile.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                profileRepository.deleteProfile(targetProfile)
                            }
                            profileToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { profileToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileCardItem(
    profile: FocusProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = getProfileIcon(profile.iconName)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.strictMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.profile_strict_mode),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (profile.description.isNotBlank()) {
                    Text(
                        text = profile.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    val durationText = if (profile.activationType == "NFC") {
                        "NFC (12h)"
                    } else if (profile.durationMinutes != null) {
                        val hours = profile.durationMinutes / 60
                        val mins = profile.durationMinutes % 60
                        if (hours > 0 && mins > 0) "${hours}h ${mins}m"
                        else if (hours > 0) "${hours}h"
                        else "${mins}m"
                    } else {
                        stringResource(R.string.profile_duration_unlimited_badge)
                    }

                    val appsCountText = if (profile.isMonkMode || profile.name.equals("Zen", ignoreCase = true)) {
                        stringResource(R.string.profiles_zen_badge)
                    } else {
                        "${profile.blockedPackages.size} apps"
                    }

                    Text(
                        text = "$durationText • $appsCountText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "${stringResource(R.string.action_edit)}: ${profile.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "${stringResource(R.string.action_delete)}: ${profile.name}",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
