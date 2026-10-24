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

        // 1. Discover all Home Launchers installed on device across any OEM/brand
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

        // 2. Comprehensive OEM & custom launcher safety net
        val knownLaunchers = setOf(
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
            "com.qqlabs.minimalistlauncher",
            "xyz.umarmohammad.hermit",
            context.packageName
        )

        // 3. Input Method Editors (Keyboards)
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        val imePackages = try {
            imm?.enabledInputMethodList?.mapNotNull { it.packageName }?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }

        // 4. System frameworks and UI
        val systemExempt = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.android.settings"
        )

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
                        if (pkg.isNullOrBlank()) continue
                        if (isScreenOn && currentForegroundApp != null && timestamp > currentForegroundStartTime) {
                            val duration = timestamp - currentForegroundStartTime
                            if (duration in 1 until 24 * 3600 * 1000L) {
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
                            if (isScreenOn && timestamp > currentForegroundStartTime) {
                                val duration = timestamp - currentForegroundStartTime
                                if (duration in 1 until 24 * 3600 * 1000L) {
                                    appUsageMap[pkg] = (appUsageMap[pkg] ?: 0L) + duration
                                }
                            }
                            currentForegroundApp = null
                            currentForegroundStartTime = 0L
                        } else if (currentForegroundApp == null && timestamp > startOfDay) {
                            val duration = timestamp - startOfDay
                            if (duration in 1 until 24 * 3600 * 1000L) {
                                appUsageMap[pkg] = (appUsageMap[pkg] ?: 0L) + duration
                            }
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
                        if (isScreenOn && currentForegroundApp != null && timestamp > currentForegroundStartTime) {
                            val duration = timestamp - currentForegroundStartTime
                            if (duration in 1 until 24 * 3600 * 1000L) {
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
            if (isScreenOn && currentForegroundApp != null && now > currentForegroundStartTime) {
                val duration = now - currentForegroundStartTime
                if (duration in 1 until 24 * 3600 * 1000L) {
                    appUsageMap[currentForegroundApp!!] = (appUsageMap[currentForegroundApp!!] ?: 0L) + duration
                }
            }
        }

        // Calculate totalScreenTime: all active apps except system frameworks and keyboards
        val nonSystemExempt = systemExempt + imePackages
        val totalScreenTime = appUsageMap.filter { !nonSystemExempt.contains(it.key) }.values.sum()

        // Calculate top distractors: exclude launchers, keyboards, system frameworks, and Hermit itself
        val allExcludedForTopApps = discoveredLaunchers +
                (defaultLauncher?.let { setOf(it) } ?: emptySet()) +
                knownLaunchers +
                imePackages +
                systemExempt

        val topApps = appUsageMap.filter { (pkg, time) ->
            time > 30_000L && !allExcludedForTopApps.contains(pkg)
        }.entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key to it.value }

        return ScreenTimeResult(
            totalScreenTimeMillis = totalScreenTime,
            topPackages = topApps
        )
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
        val nonSystemExempt = systemExempt + imePackages

        val hourlyMap = mutableMapOf<Int, Long>()

        fun addUsageInterval(pkg: String, start: Long, end: Long) {
            if (pkg.isBlank() || nonSystemExempt.contains(pkg)) return
            val validStart = maxOf(start, dayStartMillis)
            val validEnd = minOf(end, queryEnd)
            if (validEnd <= validStart) return

            for (hour in 0..23) {
                val hourStart = dayStartMillis + hour * 3600_000L
                val hourEnd = hourStart + 3600_000L
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
                        if (pkg.isNullOrBlank()) continue
                        if (isScreenOn && currentForegroundApp != null && timestamp > currentForegroundStartTime) {
                            addUsageInterval(currentForegroundApp!!, currentForegroundStartTime, timestamp)
                        }
                        currentForegroundApp = pkg
                        currentForegroundStartTime = maxOf(timestamp, dayStartMillis)
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (pkg.isNullOrBlank()) continue
                        if (currentForegroundApp == pkg) {
                            if (isScreenOn && timestamp > currentForegroundStartTime) {
                                addUsageInterval(pkg, currentForegroundStartTime, timestamp)
                            }
                            currentForegroundApp = null
                            currentForegroundStartTime = 0L
                        } else if (currentForegroundApp == null && timestamp > dayStartMillis) {
                            addUsageInterval(pkg, dayStartMillis, timestamp)
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
                        if (isScreenOn && currentForegroundApp != null && timestamp > currentForegroundStartTime) {
                            addUsageInterval(currentForegroundApp!!, currentForegroundStartTime, timestamp)
                        }
                        isScreenOn = false
                        currentForegroundApp = null
                        currentForegroundStartTime = 0L
                    }
                }
            }

            if (isScreenOn && currentForegroundApp != null && queryEnd > currentForegroundStartTime) {
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

