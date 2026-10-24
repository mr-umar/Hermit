package xyz.umarmohammad.hermit.ui.stats

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.model.FocusSessionRecord
import xyz.umarmohammad.hermit.data.repository.SessionRepository
import xyz.umarmohammad.hermit.ui.components.EmptyStateView
import xyz.umarmohammad.hermit.ui.components.GlassCard
import xyz.umarmohammad.hermit.ui.components.HermitTopBar
import xyz.umarmohammad.hermit.ui.theme.Acento
import xyz.umarmohammad.hermit.util.ScreenTimeManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TopAppUsage(
    val packageName: String,
    val appName: String,
    val usageMillis: Long,
    val iconBitmap: ImageBitmap? = null
)

@Composable
fun StatsScreen(sessionRepository: SessionRepository) {
    val context = LocalContext.current
    val allSessions by sessionRepository.allSessions.collectAsState(initial = emptyList())
    val completedCount by sessionRepository.completedSessionsCount.collectAsState(initial = 0)
    val totalBlocked by sessionRepository.totalBlockedAttempts.collectAsState(initial = 0)
    val totalSeconds by sessionRepository.totalFocusDurationSeconds.collectAsState(initial = 0L)

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    var hasUsagePermission by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    var todayScreenTimeMillis by remember { mutableLongStateOf(0L) }
    var topApps by remember { mutableStateOf<List<TopAppUsage>>(emptyList()) }
    var selectedDay by remember { mutableStateOf<CalendarDayInfo?>(null) }

    // Load usage stats if permission is granted
    LaunchedEffect(hasUsagePermission) {
        if (hasUsagePermission) {
            withContext(Dispatchers.IO) {
                val usageResult = ScreenTimeManager.getTodayUsage(context)
                val pm = context.packageManager

                val topList = usageResult.topPackages.map { (pkg, time) ->
                    val label = try {
                        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getApplicationInfo(pkg, 0)
                        }
                        pm.getApplicationLabel(info).toString()
                    } catch (_: Exception) {
                        pkg.substringAfterLast(".")
                    }

                    val iconBmp = try {
                        pm.getApplicationIcon(pkg).toBitmap(64, 64).asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }

                    TopAppUsage(
                        packageName = pkg,
                        appName = label,
                        usageMillis = time,
                        iconBitmap = iconBmp
                    )
                }

                withContext(Dispatchers.Main) {
                    todayScreenTimeMillis = usageResult.totalScreenTimeMillis
                    topApps = topList
                }
            }
        }
    }

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Summary Metric Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                }

                // Digital Wellbeing Section
                item {
                    if (hasUsagePermission) {
                        DigitalWellbeingDashboardCard(
                            todayScreenTimeMillis = todayScreenTimeMillis,
                            allSessions = allSessions,
                            topApps = topApps
                        )
                    } else {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PhoneAndroid,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.stats_screen_time_title),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(R.string.stats_usage_perm_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.stats_usage_perm_btn))
                                }
                            }
                        }
                    }
                }

                // Interactive 3-Month Focus & Screen Time Calendar Heading
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.stats_recent_history),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    FocusCalendarCard(
                        allSessions = allSessions,
                        onDayClick = { dayInfo ->
                            selectedDay = dayInfo
                        }
                    )
                }

                if (allSessions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.stats_recent_sessions_header),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(allSessions.take(5), key = { it.id }) { session ->
                        SessionHistoryCard(session = session, dateFormat = dateFormat)
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassCard {
                            EmptyStateView(
                                icon = Icons.Default.HourglassBottom,
                                title = stringResource(R.string.stats_empty_sessions_title),
                                description = stringResource(R.string.stats_empty_sessions_desc)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        selectedDay?.let { day ->
            DayDetailDialog(
                dayInfo = day,
                onDismiss = { selectedDay = null }
            )
        }
    }
}

@Composable
private fun DigitalWellbeingDashboardCard(
    todayScreenTimeMillis: Long,
    allSessions: List<FocusSessionRecord>,
    topApps: List<TopAppUsage>
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = cal.timeInMillis

    val now = System.currentTimeMillis()
    // Calculate today's focus seconds based on actual elapsed overlap today
    val todayFocusMillis = allSessions.sumOf { s ->
        val sEnd = s.endedAt ?: now
        maxOf(0L, minOf(sEnd, now) - maxOf(s.startedAt, startOfDay))
    }

    val focusRatio = if (todayScreenTimeMillis > 0) {
        ((todayFocusMillis.toFloat() / todayScreenTimeMillis.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }

    val screenHours = todayScreenTimeMillis / 3600000L
    val screenMins = (todayScreenTimeMillis % 3600000L) / 60000L
    val screenTimeStr = if (screenHours > 0) {
        "${screenHours}h ${screenMins}m"
    } else if (screenMins > 0) {
        "${screenMins}m"
    } else if (todayScreenTimeMillis >= 10_000L) {
        "< 1m"
    } else {
        "0m"
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.stats_screen_time_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.stats_focus_ratio_desc, focusRatio),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Screen time display & Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.stats_screen_time_today),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = screenTimeStr,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.stats_focus_ratio),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$focusRatio%",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Acento
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { focusRatio / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Acento,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Top apps breakdown
            if (topApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.stats_top_used_apps),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                topApps.forEach { app ->
                    val appMins = (app.usageMillis / 60000L).coerceAtLeast(1)
                    val appHours = appMins / 60
                    val remMins = appMins % 60
                    val formattedAppTime = if (appHours > 0) "${appHours}h ${remMins}m" else "${remMins}m"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (app.iconBitmap != null) {
                                Image(
                                    bitmap = app.iconBitmap,
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = formattedAppTime,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, contentPadding = 12.dp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
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
                    text = stringResource(R.string.stats_session_blocked, session.blockedAttemptsCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    return ScreenTimeManager.hasUsagePermission(context)
}

data class CalendarDayInfo(
    val dayStartMillis: Long,
    val dayEndMillis: Long,
    val dayNum: Int,
    val month: Int,
    val year: Int,
    val dateFormatted: String,
    val sessions: List<FocusSessionRecord>
)

@Composable
private fun FocusCalendarCard(
    allSessions: List<FocusSessionRecord>,
    onDayClick: (CalendarDayInfo) -> Unit
) {
    var monthOffset by remember { mutableIntStateOf(0) } // 0 = current, -1 = 1 month ago, -2 = 2 months ago

    val currentCal = remember { Calendar.getInstance() }
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentDayOfYear = currentCal.get(Calendar.DAY_OF_YEAR)

    val targetCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val year = targetCal.get(Calendar.YEAR)
    val month = targetCal.get(Calendar.MONTH)
    val daysInMonth = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingEmptyCells = (targetCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0

    val monthTitle = remember(targetCal) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(targetCal.time).replaceFirstChar { it.uppercase() }
    }

    val weekDayLabels = remember {
        val loc = Locale.getDefault()
        if (loc.language == "es") {
            listOf("L", "M", "X", "J", "V", "S", "D")
        } else {
            listOf("M", "T", "W", "T", "F", "S", "S")
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with month title and navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (monthOffset > -2) monthOffset--
                        },
                        enabled = monthOffset > -2
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.stats_calendar_prev_month),
                            tint = if (monthOffset > -2) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (monthOffset < 0) monthOffset++
                        },
                        enabled = monthOffset < 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.stats_calendar_next_month),
                            tint = if (monthOffset < 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.stats_calendar_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Week days header (L, M, X, J, V, S, D)
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Grid
            val totalCells = leadingEmptyCells + daysInMonth
            val totalRows = (totalCells + 6) / 7

            for (rowIndex in 0 until totalRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (colIndex in 0 until 7) {
                        val cellIndex = rowIndex * 7 + colIndex
                        if (cellIndex < leadingEmptyCells || cellIndex >= totalCells) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val dayNum = cellIndex - leadingEmptyCells + 1
                            val dayCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayNum)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val dayStartMillis = dayCal.timeInMillis
                            val dayEndCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayNum)
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }
                            val dayEndMillis = dayEndCal.timeInMillis

                            val isToday = (dayCal.get(Calendar.YEAR) == currentYear &&
                                    dayCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear)
                            val isFuture = dayStartMillis > System.currentTimeMillis()

                            val daySessions = allSessions.filter { s ->
                                val sEnd = s.endedAt ?: System.currentTimeMillis()
                                maxOf(0L, minOf(sEnd, dayEndMillis) - maxOf(s.startedAt, dayStartMillis)) > 0L
                            }
                            val hasFocusSessions = daySessions.isNotEmpty()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isToday) {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(10.dp)
                                            )
                                        } else Modifier
                                    )
                                    .then(
                                        if (!isFuture) {
                                            Modifier.clickable {
                                                val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                                                val dateFormatted = sdf.format(dayCal.time).replaceFirstChar { it.uppercase() }
                                                onDayClick(
                                                    CalendarDayInfo(
                                                        dayStartMillis = dayStartMillis,
                                                        dayEndMillis = dayEndMillis,
                                                        dayNum = dayNum,
                                                        month = month,
                                                        year = year,
                                                        dateFormatted = dateFormatted,
                                                        sessions = daySessions
                                                    )
                                                )
                                            }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isToday || hasFocusSessions) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                            isToday -> MaterialTheme.colorScheme.primary
                                            hasFocusSessions -> MaterialTheme.colorScheme.onBackground
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )

                                    if (hasFocusSessions) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HourlySlotData(
    val hour: Int,
    val label: String,
    val sessions: List<FocusSessionRecord>,
    val focusMinutes: Int,
    val phoneMinutes: Int,
    val phoneMillis: Long = 0L,
    val isCurrentHour: Boolean = false,
    val hStart: Long,
    val hEnd: Long
)

@Composable
private fun DayDetailDialog(
    dayInfo: CalendarDayInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hourlyUsage by remember(dayInfo) { mutableStateOf<ScreenTimeManager.DayHourlyUsage?>(null) }
    var isLoadingUsage by remember(dayInfo) { mutableStateOf(true) }

    LaunchedEffect(dayInfo) {
        isLoadingUsage = true
        hourlyUsage = withContext(Dispatchers.IO) {
            ScreenTimeManager.getDayHourlyUsage(context, dayInfo.dayStartMillis, dayInfo.dayEndMillis)
        }
        isLoadingUsage = false
    }

    val totalFocusMillis = remember(dayInfo) {
        dayInfo.sessions.sumOf { s ->
            val sEnd = s.endedAt ?: System.currentTimeMillis()
            maxOf(0L, minOf(sEnd, dayInfo.dayEndMillis) - maxOf(s.startedAt, dayInfo.dayStartMillis))
        }
    }
    val totalFocusMin = (totalFocusMillis / 60_000L).toInt()

    val totalScreenTimeMillis = hourlyUsage?.totalScreenTimeMillis ?: 0L
    val totalScreenTimeMin = (totalScreenTimeMillis / 60_000L).toInt()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.stats_day_detail_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dayInfo.dateFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Row Cards (Hermit vs Screen time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Hermit Focus
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.stats_total_time),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val h = totalFocusMin / 60
                            val m = totalFocusMin % 60
                            Text(
                                text = if (h > 0) "${h}h ${m}m" else "${m}m",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.stats_day_detail_sessions_count, dayInfo.sessions.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mobile Screen Time
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.stats_screen_time_title),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val sh = totalScreenTimeMin / 60
                            val sm = totalScreenTimeMin % 60
                            val screenUsageStr = if (sh > 0) {
                                "${sh}h ${sm}m"
                            } else if (sm > 0) {
                                "${sm}m"
                            } else if (totalScreenTimeMillis >= 10_000L) {
                                "< 1m"
                            } else {
                                "0m"
                            }
                            Text(
                                text = screenUsageStr,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isLoadingUsage) "..." else stringResource(R.string.stats_screen_time_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timeline Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.stats_day_hourly_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoadingUsage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                } else {
                    val currentNow = System.currentTimeMillis()
                    val nowCal = Calendar.getInstance().apply { timeInMillis = currentNow }
                    val isToday = (dayInfo.year == nowCal.get(Calendar.YEAR) &&
                            dayInfo.month == nowCal.get(Calendar.MONTH) &&
                            dayInfo.dayNum == nowCal.get(Calendar.DAY_OF_MONTH))
                    val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)

                    val hourlyData = remember(hourlyUsage, dayInfo) {
                        val lastActiveScreenHour = hourlyUsage?.hourlyScreenTimeMillis?.filter { it.value > 0 }?.keys?.maxOrNull() ?: -1
                        val lastActiveSessionHour = dayInfo.sessions.maxOfOrNull { s ->
                            val sEnd = s.endedAt ?: s.startedAt
                            val c = Calendar.getInstance().apply { timeInMillis = sEnd }
                            c.get(Calendar.HOUR_OF_DAY)
                        } ?: -1
                        val lastActiveHour = maxOf(lastActiveScreenHour, lastActiveSessionHour)

                        // If today: strictly from 00:00 up to the current hour (0..currentHour). No future hours!
                        // If past day: from 00:00 up to the last hour with activity (or 0 if idle).
                        val endHour = if (isToday) {
                            currentHour
                        } else {
                            if (lastActiveHour >= 0) lastActiveHour else 0
                        }

                        val slotCal = Calendar.getInstance().apply {
                            timeInMillis = dayInfo.dayStartMillis
                        }

                        (0..endHour).map { hour ->
                            slotCal.set(Calendar.HOUR_OF_DAY, hour)
                            slotCal.set(Calendar.MINUTE, 0)
                            slotCal.set(Calendar.SECOND, 0)
                            slotCal.set(Calendar.MILLISECOND, 0)
                            val hStart = slotCal.timeInMillis
                            slotCal.add(Calendar.HOUR_OF_DAY, 1)
                            val hEnd = slotCal.timeInMillis

                            val sessionsInHour = dayInfo.sessions.filter { s ->
                                val sEnd = s.endedAt ?: currentNow
                                maxOf(0L, minOf(sEnd, hEnd) - maxOf(s.startedAt, hStart)) > 0L
                            }

                            val focusMin = sessionsInHour.sumOf { s ->
                                val sEnd = s.endedAt ?: currentNow
                                ((maxOf(0L, minOf(sEnd, hEnd) - maxOf(s.startedAt, hStart))) / 60_000L).toInt()
                            }.coerceIn(0, 60)

                            val phoneMillis = hourlyUsage?.hourlyScreenTimeMillis?.get(hour) ?: 0L
                            val phoneMin = (phoneMillis / 60_000L).toInt()

                            HourlySlotData(
                                hour = hour,
                                label = String.format(Locale.getDefault(), "%02d:00 - %02d:00", hour, (hour + 1) % 24),
                                sessions = sessionsInHour,
                                focusMinutes = focusMin,
                                phoneMinutes = phoneMin,
                                phoneMillis = phoneMillis,
                                isCurrentHour = isToday && (hour == currentHour),
                                hStart = hStart,
                                hEnd = hEnd
                            )
                        }
                    }

                    // Mini Day Activity Visual Bar Chart
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                hourlyData.forEach { slot ->
                                    val focusH = (slot.focusMinutes.toFloat() / 60f).coerceIn(0f, 1f)
                                    val phoneH = ((slot.phoneMillis.toFloat() / 60_000f) / 60f).coerceIn(0f, 1f)
                                    val totalH = (focusH + phoneH).coerceIn(0f, 1f)

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.Bottom,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (totalH > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(totalH)
                                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                                    .background(if (focusH > 0f) Acento else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.dp)
                                                    .background(
                                                        if (slot.isCurrentHour) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "00:00",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hourlyData.size > 2) {
                                    val midSlot = hourlyData[hourlyData.size / 2]
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:00", midSlot.hour),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val lastSlot = hourlyData.lastOrNull()
                                if (lastSlot != null) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:00", (lastSlot.hour + 1) % 24),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hourlyData.forEach { slot ->
                            HourlySlotCard(slot = slot)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlySlotCard(slot: HourlySlotData) {
    val hasActivity = slot.sessions.isNotEmpty() || slot.phoneMillis >= 10_000L
    val focusRatio = (slot.focusMinutes.toFloat() / 60f).coerceIn(0f, 1f)
    val phoneRatio = ((slot.phoneMillis.toFloat() / 60_000f) / 60f).coerceIn(0f, 1f - focusRatio)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (slot.isCurrentHour) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else if (hasActivity) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        },
        border = if (slot.isCurrentHour) {
            BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        } else null
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            // Header Row: Time range on left, tags on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = slot.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (slot.isCurrentHour) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (slot.isCurrentHour) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.stats_slot_in_progress),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (hasActivity) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (slot.focusMinutes > 0) {
                            Text(
                                text = "${slot.focusMinutes}m foco",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (slot.phoneMillis >= 10_000L) {
                            val screenMinStr = if (slot.phoneMinutes > 0) "${slot.phoneMinutes}m" else "< 1m"
                            Text(
                                text = "$screenMinStr pantalla",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual Segmented Timeline Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                if (focusRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(focusRatio, fill = true)
                            .background(Acento)
                    )
                }
                if (phoneRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(phoneRatio, fill = true)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f))
                    )
                }
                val emptyRatio = (1f - focusRatio - phoneRatio).coerceAtLeast(0f)
                if (emptyRatio > 0f) {
                    Spacer(modifier = Modifier.weight(emptyRatio, fill = true))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Activity details chips
            if (hasActivity) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    slot.sessions.forEach { session ->
                        val sEnd = session.endedAt ?: System.currentTimeMillis()
                        val durationMin = ((maxOf(0L, minOf(sEnd, slot.hEnd) - maxOf(session.startedAt, slot.hStart))) / 60_000L)
                            .toInt().coerceAtLeast(1)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = Acento,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${session.profileName} • ${durationMin} min",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (slot.phoneMillis >= 10_000L) {
                        val usageText = if (slot.phoneMinutes > 0) {
                            stringResource(R.string.stats_slot_screen_time, slot.phoneMinutes)
                        } else {
                            stringResource(R.string.stats_slot_screen_time_less_than_minute)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = usageText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = if (slot.isCurrentHour) {
                        stringResource(R.string.stats_slot_current_no_activity_yet)
                    } else {
                        stringResource(R.string.stats_slot_idle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
