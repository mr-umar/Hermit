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
    version = 6,
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

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_profiles ADD COLUMN enableDnd INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_profiles ADD COLUMN enableGrayscale INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): HermitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HermitDatabase::class.java,
                    "hermit_database.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
