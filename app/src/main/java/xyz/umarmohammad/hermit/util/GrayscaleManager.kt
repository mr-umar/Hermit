package xyz.umarmohammad.hermit.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.provider.Settings
import android.view.View
import android.view.Window

/**
 * Manages grayscale / monochrome display mode both system-wide (via Settings.Secure
 * if granted WRITE_SECURE_SETTINGS) and per-window (via hardware layer color filter).
 */
object GrayscaleManager {

    private var previousDaltonizerEnabled: String? = null
    private var previousDaltonizerMode: String? = null

    fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Toggles system-wide grayscale via Android's Daltonizer engine if WRITE_SECURE_SETTINGS is granted.
     */
    fun enableSystemGrayscale(context: Context): Boolean {
        if (!hasWriteSecureSettings(context)) return false
        return try {
            val cr = context.contentResolver
            if (previousDaltonizerEnabled == null) {
                previousDaltonizerEnabled = Settings.Secure.getString(cr, "accessibility_display_daltonizer_enabled") ?: "0"
                previousDaltonizerMode = Settings.Secure.getString(cr, "accessibility_display_daltonizer") ?: "-1"
            }
            Settings.Secure.putString(cr, "accessibility_display_daltonizer_enabled", "1")
            Settings.Secure.putString(cr, "accessibility_display_daltonizer", "0")
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Restores system-wide color correction to its previous state.
     */
    fun restoreSystemGrayscale(context: Context) {
        if (!hasWriteSecureSettings(context)) return
        try {
            val cr = context.contentResolver
            val targetEnabled = previousDaltonizerEnabled ?: "0"
            val targetMode = previousDaltonizerMode ?: "-1"
            Settings.Secure.putString(cr, "accessibility_display_daltonizer_enabled", targetEnabled)
            Settings.Secure.putString(cr, "accessibility_display_daltonizer", targetMode)
            previousDaltonizerEnabled = null
            previousDaltonizerMode = null
        } catch (_: Exception) {}
    }

    /**
     * Applies a monochromatic grayscale color filter directly to an Activity's Window decorView.
     * This requires zero system permissions and works natively on all Android devices.
     */
    fun applyWindowGrayscale(window: Window, enable: Boolean) {
        try {
            if (enable) {
                val matrix = ColorMatrix().apply {
                    setSaturation(0f)
                }
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(matrix)
                }
                window.decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            } else {
                window.decorView.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        } catch (_: Exception) {}
    }
}
