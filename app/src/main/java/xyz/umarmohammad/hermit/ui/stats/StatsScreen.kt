package xyz.umarmohammad.hermit.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.model.FocusSessionRecord
import xyz.umarmohammad.hermit.data.repository.SessionRepository
import xyz.umarmohammad.hermit.ui.components.EmptyStateView
import xyz.umarmohammad.hermit.ui.components.GlassCard
import xyz.umarmohammad.hermit.ui.components.HermitTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(sessionRepository: SessionRepository) {
    val allSessions by sessionRepository.allSessions.collectAsState(initial = emptyList())
    val completedCount by sessionRepository.completedSessionsCount.collectAsState(initial = 0)
    val totalBlocked by sessionRepository.totalBlockedAttempts.collectAsState(initial = 0)
    val totalSeconds by sessionRepository.totalFocusDurationSeconds.collectAsState(initial = 0L)

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            HermitTopBar(title = stringResource(R.string.stats_title))
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Summary Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Focus Time
                val sec = totalSeconds ?: 0L
                val hours = sec / 3600
                val mins = (sec % 3600) / 60
                val durationText = if (hours > 0) {
                    stringResource(R.string.stats_duration_format, hours, mins)
                } else {
                    stringResource(R.string.stats_duration_min, mins)
                }

                StatMetricBox(
                    label = stringResource(R.string.stats_total_time),
                    value = durationText,
                    modifier = Modifier.weight(1f)
                )

                StatMetricBox(
                    label = stringResource(R.string.stats_completed_sessions),
                    value = "$completedCount",
                    modifier = Modifier.weight(1f)
                )

                StatMetricBox(
                    label = stringResource(R.string.stats_blocks_prevented),
                    value = "${totalBlocked ?: 0}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.stats_recent_history),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (allSessions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Timeline,
                    title = stringResource(R.string.stats_title),
                    description = stringResource(R.string.stats_empty),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allSessions, key = { it.id }) { session ->
                        SessionHistoryCard(session = session, dateFormat = dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, contentPadding = 12.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionHistoryCard(session: FocusSessionRecord, dateFormat: SimpleDateFormat) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.profileName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = dateFormat.format(Date(session.startedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val durationMin = if (session.endedAt != null) {
                    ((session.endedAt - session.startedAt) / 60000).coerceAtLeast(1)
                } else {
                    0
                }
                Text(
                    text = "${durationMin}m",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${session.blockedAttemptsCount} blocked",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
