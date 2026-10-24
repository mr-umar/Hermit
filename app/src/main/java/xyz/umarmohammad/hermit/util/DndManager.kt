package xyz.umarmohammad.hermit.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Utility to manage Do Not Disturb (DND) / notification policy
 * during Zen mode sessions.
 */
object DndManager {

    private var previousFilter: Int? = null

    fun hasDndPermission(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return nm?.isNotificationPolicyAccessGranted == true
    }

    fun requestDndPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun enableDnd(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (!nm.isNotificationPolicyAccessGranted) return

            if (previousFilter == null) {
                previousFilter = nm.currentInterruptionFilter
            }
            // Use PRIORITY filter so emergency/dialer can still break through if configured in OS
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        } catch (_: Exception) {}
    }

    fun restoreDnd(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (!nm.isNotificationPolicyAccessGranted) return

            val targetFilter = previousFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
            nm.setInterruptionFilter(targetFilter)
            previousFilter = null
        } catch (_: Exception) {}
    }
}
