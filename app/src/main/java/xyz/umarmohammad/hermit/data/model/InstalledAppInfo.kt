package xyz.umarmohammad.hermit.data.model

import android.graphics.drawable.Drawable

/**
 * Representation of an application installed on the user device.
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val icon: Drawable? = null
)
