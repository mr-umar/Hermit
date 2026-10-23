package xyz.umarmohammad.hermit.data.model

import androidx.annotation.StringRes
import xyz.umarmohammad.hermit.R

/**
 * Defines the intended focus action when a physical NFC tag is scanned.
 */
enum class TagAction(@StringRes val titleRes: Int) {
    TOGGLE(R.string.tag_action_toggle),
    ACTIVATE(R.string.tag_action_activate),
    DEACTIVATE(R.string.tag_action_deactivate),
    SWITCH(R.string.tag_action_switch);

    companion object {
        fun fromString(value: String?): TagAction =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TOGGLE
    }
}
