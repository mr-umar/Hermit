package xyz.umarmohammad.hermit.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.umarmohammad.hermit.data.model.ExitBehavior
import xyz.umarmohammad.hermit.data.model.TagAction

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (_: Exception) {
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return json.encodeToString(list ?: emptyList())
    }

    @TypeConverter
    fun fromExitBehavior(value: ExitBehavior?): String {
        return value?.name ?: ExitBehavior.ALLOW_EMERGENCY.name
    }

    @TypeConverter
    fun toExitBehavior(value: String?): ExitBehavior {
        return ExitBehavior.fromString(value)
    }

    @TypeConverter
    fun fromTagAction(value: TagAction?): String {
        return value?.name ?: TagAction.TOGGLE.name
    }

    @TypeConverter
    fun toTagAction(value: String?): TagAction {
        return TagAction.fromString(value)
    }
}
