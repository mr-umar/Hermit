package xyz.umarmohammad.hermit.util

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import xyz.umarmohammad.hermit.service.HermitAccessibilityService

object RequiredPermissionManager {

    data class RequiredPermissionsStatus(
        val isBatteryExempt: Boolean = false,
        val isAccessibilityGranted: Boolean = false,
        val isOverlayGranted: Boolean = false,
        val isUsageStatsGranted: Boolean = false
    ) {
        val allGranted: Boolean
            get() = isBatteryExempt && isAccessibilityGranted && isOverlayGranted && isUsageStatsGranted

        val hasRevokedPermissions: Boolean
            get() = !allGranted

        val revokedCount: Int
            get() {
                var count = 0
                if (!isBatteryExempt) count++
                if (!isAccessibilityGranted) count++
                if (!isOverlayGranted) count++
                if (!isUsageStatsGranted) count++
                return count
            }
    }

    fun isBatteryExempt(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun isAccessibilityGranted(context: Context): Boolean {
        if (HermitAccessibilityService.isServiceRunning.value) return true
        val expectedComponent = ComponentName(context, HermitAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponent, ignoreCase = true) ||
                (componentName.contains(context.packageName) && componentName.contains("HermitAccessibilityService"))
            ) {
                return true
            }
        }
        return false
    }

    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isUsageStatsGranted(context: Context): Boolean {
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

    fun checkPermissions(context: Context): RequiredPermissionsStatus {
        return RequiredPermissionsStatus(
            isBatteryExempt = isBatteryExempt(context),
            isAccessibilityGranted = isAccessibilityGranted(context),
            isOverlayGranted = isOverlayGranted(context),
            isUsageStatsGranted = isUsageStatsGranted(context)
        )
    }

    fun requestBatteryExemption(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    fun requestAccessibility(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    }

    fun requestOverlay(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }

    fun requestUsageStats(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }
}
