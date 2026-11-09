package com.johnseymour.solarseasons

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.api.OPENUV_API_KEY
import com.johnseymour.solarseasons.data.AppSettingsRepository
import com.johnseymour.solarseasons.ui.navigation.AppNavHost
import com.johnseymour.solarseasons.ui.navigation.Routes
import com.johnseymour.solarseasons.ui.theme.SolarSeasonsTheme
import com.johnseymour.solarseasons.widget.WidgetBackgroundManager

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        AppSettingsRepository.initialise(this)
        WidgetBackgroundManager.configure(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent()
        {
            val startDestination = remember { initialDestination() }
            val launchedFromWidget = remember()
            {
                intent?.getBooleanExtra(com.johnseymour.solarseasons.widget.LaunchFromWidgetKey.name, false) == true ||
                        intent?.action == com.johnseymour.solarseasons.models.UVData.UV_DATA_UPDATED
            }

            SolarSeasonsTheme()
            {
                AppNavHost(
                    startDestination = startDestination,
                    showPersistedError = launchedFromWidget,
                )
            }
        }
    }

    private fun initialDestination(): String
    {
        if (Constants.ENABLE_API_KEY_ENTRY_FEATURE)
        {
            val apiKey = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Constants.SharedPreferences.API_KEY, null)

            if (apiKey == null)
            {
                return Routes.API_KEY_ONBOARDING
            }

            OPENUV_API_KEY = apiKey
        }

        return Routes.CURRENT_UV
    }
}
