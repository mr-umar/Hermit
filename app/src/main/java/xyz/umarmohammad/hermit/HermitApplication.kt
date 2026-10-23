package xyz.umarmohammad.hermit

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.umarmohammad.hermit.data.local.HermitDatabase
import xyz.umarmohammad.hermit.data.preference.PreferenceManager
import xyz.umarmohammad.hermit.data.repository.ProfileRepository
import xyz.umarmohammad.hermit.session.SessionManager
import xyz.umarmohammad.hermit.updater.UpdateManager

class HermitApplication : Application() {

    lateinit var database: HermitDatabase private set
    lateinit var sessionManager: SessionManager private set
    lateinit var preferenceManager: PreferenceManager private set
    val updateManager: UpdateManager by lazy { UpdateManager(this, preferenceManager) }

    override fun onCreate() {
        super.onCreate()
        database = HermitDatabase.getInstance(this)
        preferenceManager = PreferenceManager(this)
        sessionManager = SessionManager.getInstance(this)

        CoroutineScope(Dispatchers.IO).launch {
            val profileRepo = ProfileRepository(database.focusProfileDao())
            profileRepo.ensureDefaultProfiles()

            // Apply saved locale preference if manually specified
            val savedLocale = preferenceManager.appLocale.first()
            if (savedLocale == "en" || savedLocale == "es") {
                val appLocale = LocaleListCompat.forLanguageTags(savedLocale)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        }
    }
}
