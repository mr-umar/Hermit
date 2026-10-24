package xyz.umarmohammad.hermit.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import java.util.Calendar

object ScreenTimeManager {

    data class ScreenTimeResult(
        val totalScreenTimeMillis: Long,
        val topPackages: List<Pair<String, Long>>
    )

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @Suppress("DEPRECATION")
    fun getTodayUsage(context: Context): ScreenTimeResult {
        if (!hasUsagePermission(context)) {
            return ScreenTimeResult(0L, emptyList())
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val now = System.currentTimeMillis()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ScreenTimeResult(0L, emptyList())

        val events = try {
            usm.queryEvents(startOfDay, now)
        } catch (_: Exception) {
            null
        }

        val pm = context.packageManager
        val allExcluded = getExcludedPackages(context)
        val appUsageMap = mutableMapOf<String, Long>()

        if (events != null && events.hasNextEvent()) {
            var currentForegroundApp: String? = null
            var currentForegroundStartTime: Long = 0L
            var isScreenOn: Boolean = true

            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName
                val timestamp = event.timeStamp

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (pkg.isNullOrBlank() || allExcluded.contains(pkg)) {
                            // User switched to launcher, home screen, or Hermit
                            if (isScreenOn && currentForegroundApp != null && currentForegroundStartTime > 0L && timestamp > currentForegroundStartTime) {
                                val duration = minOf(timestamp - currentForegroundStartTime, 4 * 3600 * 1000L)
                                if (duration > 0L) {
                                    appUsageMap[currentForegroundApp!!] = (appUsageMap[currentForegroundApp!!] ?: 0L) + duration
                                }
                            }
                            currentForegroundApp = null
                            currentForegroundStartTime = 0L
                            continue
                        }
                        if (isScreenOn && currentForegroundApp != null && currentForegroundStartTime > 0L && timestamp > currentForegroundStartTime) {
                            val duration = minOf(timestamp - currentForegroundStartTime, 4 * 3600 * 1000L)
                            if (duration > 0L) {
                                appUsageMap[currentForegroundApp!!] = (appUsageMap[currentForegroundApp!!] ?: 0L) + duration
                            }
                        }
                        currentForegroundApp = pkg
                        currentForegroundStartTime = maxOf(timestamp, startOfDay)
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (pkg.isNullOrBlank()) continue
                        if (currentForegroundApp == pkg) {
                            if (isScreenOn && currentForegroundStartTime > 0L && timestamp > currentForegroundStartTime) {
                                val duration = minOf(timestamp - currentForegroundStartTime, 4 * 3600 * 1000L)
                                if (duration > 0L) {
                                    appUsageMap[pkg] = (appUsageMap[pkg] ?: 0L) + duration
                                }
                            }
                            currentForegroundApp = null
                            currentForegroundStartTime = 0L
                        }
                    }

                    UsageEvents.Event.SCREEN_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_HIDDEN -> {
                        isScreenOn = true
                        currentForegroundStartTime = timestamp
                    }

                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_SHOWN,
                    26 /* DEVICE_SHUTDOWN */ -> {
                        if (isScreenOn && currentForegroundApp != null && currentForegroundStartTime > 0L && timestamp > currentForegroundStartTime) {
                            val duration = minOf(timestamp - currentForegroundStartTime, 4 * 3600 * 1000L)
                            if (duration > 0L) {
                                appUsageMap[currentForegroundApp!!] = (appUsageMap[currentForegroundApp!!] ?: 0L) + duration
                            }
                        }
                        isScreenOn = false
                        currentForegroundApp = null
                        currentForegroundStartTime = 0L
                    }
                }
            }

            // Count duration of app active right now
            if (isScreenOn && currentForegroundApp != null && currentForegroundStartTime > 0L && now > currentForegroundStartTime) {
                val duration = minOf(now - currentForegroundStartTime, 4 * 3600 * 1000L)
                if (duration > 0L) {
                    appUsageMap[currentForegroundApp!!] = (appUsageMap[currentForegroundApp!!] ?: 0L) + duration
                }
            }
        }

        val totalScreenTime = appUsageMap.values.sum()

        val topApps = appUsageMap.filter { (pkg, time) ->
            time > 30_000L && !allExcluded.contains(pkg)
        }.entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key to it.value }

        return ScreenTimeResult(
            totalScreenTimeMillis = totalScreenTime,
            topPackages = topApps
        )
    }

    private val knownLaunchers = setOf(
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.sec.android.app.launcher",
        "com.sec.android.app.twlauncher",
        "com.miui.home",
        "com.mi.android.globallauncher",
        "com.miui.mihome2",
        "net.oneplus.launcher",
        "com.oppo.launcher",
        "com.coloros.launcher",
        "com.coloros.recents",
        "com.realme.launcher",
        "com.bbk.launcher2",
        "com.vivo.upslide",
        "com.vivo.launcher",
        "com.huawei.android.launcher",
        "com.honor.qclauncher",
        "com.hihonor.android.launcher",
        "com.motorola.launcher3",
        "com.lenovo.launcher",
        "com.sonyericsson.home",
        "com.sonymobile.home",
        "com.asus.launcher",
        "com.lge.launcher2",
        "com.lge.launcher3",
        "com.transsion.hilauncher",
        "com.transsion.XOSLauncher",
        "com.transsion.itel.launcher",
        "com.nothing.launcher",
        "com.teslacoilsw.launcher",
        "ch.deletescape.lawnchair",
        "ch.deletescape.lawnchair.plah",
        "app.lawnchair",
        "app.lawnchair.play",
        "com.microsoft.launcher",
        "ginlemon.flowerfree",
        "ginlemon.flowerpro",
        "com.actionlauncher.playstore",
        "com.niagara.launcher",
        "bitpit.launcher",
        "com.indistractable.launcher",
        "com.befocused.launcher",
        "com.appdevgenie.minimalistlauncher",
        "com.qqlabs.minimalistlauncher"
    )

    private fun getExcludedPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val discoveredLaunchers = try {
            pm.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }

        val defaultLauncher = try {
            pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }

        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        val imePackages = try {
            imm?.enabledInputMethodList?.mapNotNull { it.packageName }?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }

        val systemExempt = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.android.settings"
        )

        return systemExempt +
                imePackages +
                discoveredLaunchers +
                (defaultLauncher?.let { setOf(it) } ?: emptySet()) +
                knownLaunchers +
                setOf(context.packageName, "xyz.umarmohammad.hermit")
    }

    data class DayHourlyUsage(
        val hourlyScreenTimeMillis: Map<Int, Long>,
        val totalScreenTimeMillis: Long
    )

    fun getDayHourlyUsage(context: Context, dayStartMillis: Long, dayEndMillis: Long): DayHourlyUsage {
        if (!hasUsagePermission(context)) {
            return DayHourlyUsage(emptyMap(), 0L)
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return DayHourlyUsage(emptyMap(), 0L)

        val queryEnd = minOf(dayEndMillis, System.currentTimeMillis())
        if (queryEnd <= dayStartMillis) {
            return DayHourlyUsage(emptyMap(), 0L)
        }

        val events = try {
            usm.queryEvents(dayStartMillis, queryEnd)
        } catch (_: Exception) {
            null
        }

        val allExcluded = getExcludedPackages(context)
        val hourlyMap = mutableMapOf<Int, Long>()

        val hourBounds = (0..23).map { hour ->
            val c = Calendar.getInstance().apply {
                timeInMillis = dayStartMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val hStart = c.timeInMillis
            c.add(Calendar.HOUR_OF_DAY, 1)
            val hEnd = c.timeInMillis
            Triple(hour, hStart, hEnd)
        }

        fun addUsageInterval(pkg: String, start: Long, end: Long) {
            if (pkg.isBlank() || allExcluded.contains(pkg)) return
            val validStart = maxOf(start, dayStartMillis)
            val validEnd = minOf(end, queryEnd)
            if (validEnd <= validStart) return

            for ((hour, hourStart, hourEnd) in hourBounds) {
                val overlap = maxOf(0L, minOf(validEnd, hourEnd) - maxOf(validStart, hourStart))
                if (overlap > 0) {
                    hourlyMap[hour] = (hourlyMap[hour] ?: 0L) + overlap
                }
            }
        }

        if (events != null && events.hasNextEvent()) {
            var currentForegroundApp: String? = null
            var currentForegroundStartTime: Long = 0L
            var isScreenOn: Boolean = true

            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName
                val timestamp = event.timeStamp

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (pkg.isNullOrBlank() || allExcluded.contains(pkg)) {
                            if (isScreenOn && currentForegroundApp != null && currentForegroundStartTime > 0L && timestamp > currentForegroundStartTime) {
                                addUsageInterval(currentForegroundApp!!, currentForegroundStartTime, timestamp)
                            }
                            currentForegroundApp = null
                            currentForegroundStartTime = 0L
                            continue
                        }
                        if (isScreenOn && currentForegroundApp != null && currentForegroundStartTime > 0L && timestamp > currentForegroundStartTime) {
                            addUsageInterval(currentForegroundApp!!, currentForegroundStartTime, timestamp)
                        }
                        currentForegroundApp = pkg
                        currentForegroundStartTime = maxOf(timestamp, dayStartMillis)
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (pkg.isNullOrBlank()) continue
                        if (currentForegroundApp == pkg) {
                            if (isScreenOn && currentForegroundStartTime > 0L && timestamp > currentForegroundStartTime) {
                                addUsageInterval(pkg, currentForegroundStartTime, timestamp)
                            }
                            currentForegroundApp = null
                            currentForegroundStartTime = 0L
                        }
                    }

                    UsageEvents.Event.SCREEN_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_HIDDEN -> {
                        isScreenOn = true
                        currentForegroundStartTime = timestamp
                    }

                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_SHOWN,
                    26 /* DEVICE_SHUTDOWN */ -> {
                        if (isScreenOn && currentForegroundApp != null && currentForegroundStartTime > 0L && timestamp > currentForegroundStartTime) {
                            addUsageInterval(currentForegroundApp!!, currentForegroundStartTime, timestamp)
                        }
                        isScreenOn = false
                        currentForegroundApp = null
                        currentForegroundStartTime = 0L
                    }
                }
            }

            if (isScreenOn && currentForegroundApp != null && currentForegroundStartTime > 0L && queryEnd > currentForegroundStartTime) {
                addUsageInterval(currentForegroundApp!!, currentForegroundStartTime, queryEnd)
            }
        }

        val totalScreenTime = hourlyMap.values.sum()
        return DayHourlyUsage(hourlyMap, totalScreenTime)
    }

    data class SessionStatsAnalysis(
        val unlockCount: Int,
        val topAppViews: List<Pair<String, Int>>
    )

    fun resolveAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }

    fun getSessionStatsAnalysis(context: Context, startTime: Long, endTime: Long): SessionStatsAnalysis {
        if (!hasUsagePermission(context)) {
            return SessionStatsAnalysis(0, emptyList())
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return SessionStatsAnalysis(0, emptyList())

        val queryEnd = minOf(endTime, System.currentTimeMillis())
        if (queryEnd <= startTime) {
            return SessionStatsAnalysis(0, emptyList())
        }

        val events = try {
            usm.queryEvents(startTime, queryEnd)
        } catch (_: Exception) {
            null
        } ?: return SessionStatsAnalysis(0, emptyList())

        val appOpenCounts = mutableMapOf<String, Int>()
        var unlocks = 0

        val systemExempt = setOf(
            "android",
            "com.android.systemui",
            "xyz.umarmohammad.hermit",
            context.packageName
        )

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN,
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    unlocks++
                }
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val pkg = event.packageName
                    if (!pkg.isNullOrBlank() && !systemExempt.contains(pkg)) {
                        appOpenCounts[pkg] = (appOpenCounts[pkg] ?: 0) + 1
                    }
                }
            }
        }

        val topApps = appOpenCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        return SessionStatsAnalysis(unlocks, topApps)
    }
}

