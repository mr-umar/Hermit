package xyz.umarmohammad.hermit.data.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Representation of an application installed on the user device.
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val icon: Drawable? = null,
    val iconBitmap: ImageBitmap? = null
)
