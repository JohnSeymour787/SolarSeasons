package com.johnseymour.solarseasons.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.api.OPENUV_API_KEY
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Settings that affect the UI, exposed as a flow so Compose screens can react to changes. */
data class AppSettings(
    val useCustomTheme: Boolean = false,
    val autoRequestOnLaunch: Boolean = true,
    val cloudCoverEnabled: Boolean = false,
)

/**
 * Reads the app's shared preferences and republishes them whenever they change.
 *
 * This replaces the old fragment-result plumbing that used to carry preference updates
 * between the preference screen and the current UV screen.
 */
object AppSettingsRepository
{
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var preferences: SharedPreferences? = null

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    @Synchronized
    fun initialise(context: Context)
    {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

        if (preferences === prefs)
        {
            refresh()
            return
        }

        preferences?.unregisterOnSharedPreferenceChangeListener(listener)
        preferences = prefs
        prefs.registerOnSharedPreferenceChangeListener(listener)
        refresh()
    }

    fun refresh()
    {
        val prefs = preferences ?: return

        val useCustomTheme = prefs.getString(Constants.SharedPreferences.APP_THEME_KEY, null) ==
                Constants.SharedPreferences.CUSTOM_APP_THEME_VALUE

        PreferenceScreenFragment.useCustomTheme = useCustomTheme

        if (Constants.ENABLE_API_KEY_ENTRY_FEATURE)
        {
            prefs.getString(Constants.SharedPreferences.API_KEY, null)?.let { OPENUV_API_KEY = it }
        }

        _settings.value = AppSettings(
            useCustomTheme = useCustomTheme,
            autoRequestOnLaunch = prefs.getBoolean(Constants.SharedPreferences.APP_LAUNCH_AUTO_REQUEST_KEY, true),
            cloudCoverEnabled = prefs.getBoolean(Constants.SharedPreferences.CLOUD_COVER_FACTOR_KEY, false),
        )
    }

    fun useCustomTheme(): Boolean = _settings.value.useCustomTheme
}
