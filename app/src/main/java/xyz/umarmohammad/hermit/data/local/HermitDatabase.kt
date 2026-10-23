package xyz.umarmohammad.hermit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.data.model.ExitBehavior
import xyz.umarmohammad.hermit.data.model.FocusProfile
import xyz.umarmohammad.hermit.data.model.FocusSessionRecord
import xyz.umarmohammad.hermit.data.model.NfcTagRecord

@Database(
    entities = [
        FocusProfile::class,
        NfcTagRecord::class,
        FocusSessionRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HermitDatabase : RoomDatabase() {
    abstract fun focusProfileDao(): FocusProfileDao
    abstract fun nfcTagDao(): NfcTagDao
    abstract fun sessionRecordDao(): SessionRecordDao

    companion object {
        @Volatile
        private var INSTANCE: HermitDatabase? = null

        fun getInstance(context: Context): HermitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HermitDatabase::class.java,
                    "hermit_database.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultProfiles(database.focusProfileDao())
                    }
                }
            }
        }

        suspend fun populateDefaultProfiles(dao: FocusProfileDao) {
            val defaults = listOf(
                FocusProfile(
                    id = "default_deep_work",
                    name = "Deep Work",
                    description = "Zero distractions for intense creative and analytical tasks.",
                    iconName = "work",
                    colorHex = "#0D9488",
                    blockedPackages = listOf(
                        "com.instagram.android",
                        "com.twitter.android",
                        "com.zhiliaoapp.musically", // TikTok
                        "com.facebook.katana",
                        "com.google.android.youtube",
                        "com.reddit.frontpage",
                        "com.snapchat.android"
                    ),
                    durationMinutes = 45,
                    exitBehavior = ExitBehavior.ALLOW_EMERGENCY,
                    strictMode = false,
                    isDefault = true
                ),
                FocusProfile(
                    id = "default_study",
                    name = "Study",
                    description = "Block social media, streaming, and games while learning.",
                    iconName = "school",
                    colorHex = "#2563EB",
                    blockedPackages = listOf(
                        "com.instagram.android",
                        "com.twitter.android",
                        "com.zhiliaoapp.musically",
                        "com.netflix.mediaclient",
                        "com.google.android.youtube"
                    ),
                    durationMinutes = 60,
                    exitBehavior = ExitBehavior.ALLOW_EMERGENCY,
                    strictMode = false,
                    isDefault = false
                ),
                FocusProfile(
                    id = "default_rest",
                    name = "Rest & Unwind",
                    description = "Keep work communications away during personal hours.",
                    iconName = "spa",
                    colorHex = "#059669",
                    blockedPackages = listOf(
                        "com.slack",
                        "com.microsoft.teams",
                        "com.google.android.gm"
                    ),
                    durationMinutes = null,
                    exitBehavior = ExitBehavior.NFC_ONLY,
                    strictMode = true,
                    isDefault = false
                ),
                FocusProfile(
                    id = "default_evening",
                    name = "Mindful Evening",
                    description = "Wind down before bed without screen addiction triggers.",
                    iconName = "bedtime",
                    colorHex = "#7C3AED",
                    blockedPackages = listOf(
                        "com.instagram.android",
                        "com.twitter.android",
                        "com.zhiliaoapp.musically",
                        "com.reddit.frontpage",
                        "com.google.android.youtube"
                    ),
                    durationMinutes = 90,
                    exitBehavior = ExitBehavior.ALLOW_EMERGENCY,
                    strictMode = false,
                    isDefault = false
                )
            )
            dao.insertProfiles(defaults)
        }
    }
}
