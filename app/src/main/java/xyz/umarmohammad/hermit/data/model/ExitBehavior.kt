package xyz.umarmohammad.hermit.data.model

import androidx.annotation.StringRes
import xyz.umarmohammad.hermit.R

/**
 * Defines the permissible exit criteria for an active focus session.
 */
enum class ExitBehavior(@StringRes val titleRes: Int) {
    NFC_ONLY(R.string.profile_exit_nfc_only),
    ALLOW_EMERGENCY(R.string.profile_exit_allow_emergency),
    TIMED_ONLY(R.string.profile_exit_timed_only);

    companion object {
        fun fromString(value: String?): ExitBehavior =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ALLOW_EMERGENCY
    }
}
