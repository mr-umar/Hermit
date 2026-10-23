package xyz.umarmohammad.hermit.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.receiver.HermitDeviceAdminReceiver

object DeviceProtectionManager {

    fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(context, HermitDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        return dpm?.isAdminActive(getAdminComponent(context)) == true
    }

    fun createEnableAdminIntent(context: Context): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponent(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.device_admin_explanation)
            )
        }
    }

    fun removeAdmin(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        dpm?.removeActiveAdmin(getAdminComponent(context))
    }
}
