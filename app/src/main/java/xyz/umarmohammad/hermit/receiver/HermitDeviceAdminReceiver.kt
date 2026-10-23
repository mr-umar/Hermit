package xyz.umarmohammad.hermit.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import xyz.umarmohammad.hermit.R

/**
 * DeviceAdminReceiver used solely to prevent casual, impulsive uninstallation
 * of Hermit. Activating this disables the system "Uninstall" button in Android Settings.
 */
class HermitDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            context.getString(R.string.device_admin_enabled_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.device_admin_warning)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(
            context,
            context.getString(R.string.device_admin_disabled_toast),
            Toast.LENGTH_SHORT
        ).show()
    }
}
