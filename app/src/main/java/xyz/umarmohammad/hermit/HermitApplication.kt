package xyz.umarmohammad.hermit

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    var isAppInForeground: Boolean = false
        private set
    private var startedActivitiesCount = 0

    override fun onCreate() {
        super.onCreate()
        database = HermitDatabase.getInstance(this)
        preferenceManager = PreferenceManager(this)
        sessionManager = SessionManager.getInstance(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivitiesCount++
                isAppInForeground = true
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivitiesCount--
                isAppInForeground = (startedActivitiesCount > 0)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        CoroutineScope(Dispatchers.IO).launch {
            database.focusProfileDao().deleteDefaultProfiles()

            // If user has zero NFC tags registered, ensure no profile remains configured for NFC_ONLY
            val tags = database.nfcTagDao().getAllTags().first()
            if (tags.isEmpty()) {
                val profiles = database.focusProfileDao().getAllProfiles().first()
                for (p in profiles) {
                    if (p.exitBehavior == xyz.umarmohammad.hermit.data.model.ExitBehavior.NFC_ONLY || p.activationType == "NFC") {
                        database.focusProfileDao().insertProfile(
                            p.copy(
                                exitBehavior = xyz.umarmohammad.hermit.data.model.ExitBehavior.ALLOW_EMERGENCY,
                                activationType = "TIME",
                                durationMinutes = if (p.durationMinutes == null || p.durationMinutes >= 720) 60 else p.durationMinutes
                            )
                        )
                    }
                }
            }

            // Apply saved locale preference if manually specified
            val savedLocale = preferenceManager.appLocale.first()
            if (savedLocale == "en" || savedLocale == "es") {
                withContext(Dispatchers.Main) {
                    try {
                        val appLocale = LocaleListCompat.forLanguageTags(savedLocale)
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    } catch (_: Throwable) {
                        // Safe fallback if AppCompat not present
                    }
                }
            }
        }
    }
}
