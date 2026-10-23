package xyz.umarmohammad.hermit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import xyz.umarmohammad.hermit.session.SessionManager

/**
 * Ensures active focus sessions survive device reboots without losing state.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Initializing SessionManager triggers automatic session restoration from DataStore
            SessionManager.getInstance(context)
        }
    }
}
