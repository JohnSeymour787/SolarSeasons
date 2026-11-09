package com.johnseymour.solarseasons

import android.app.Application
import androidx.preference.PreferenceManager

class SolarSeasonsApplication: Application()
{
    override fun onCreate()
    {
        super.onCreate()

        // Background updates are only needed while a widget is present
        if (!hasWidgets()) { return }

        val backgroundRefreshRate = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Constants.SharedPreferences.BACKGROUND_REFRESH_RATE_KEY, null)
            ?.toLongOrNull() ?: Constants.DEFAULT_REFRESH_TIME

        UVDataWorker.initiatePeriodicWorker(this, backgroundRefreshRate)
    }
}
