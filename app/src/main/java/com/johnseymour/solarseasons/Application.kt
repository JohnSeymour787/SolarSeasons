package com.johnseymour.solarseasons

import android.app.Application
import com.johnseymour.solarseasons.data.AppSettingsRepository
import com.johnseymour.solarseasons.widget.WidgetBackgroundManager

class SolarSeasonsApplication: Application()
{
    override fun onCreate()
    {
        super.onCreate()

        AppSettingsRepository.initialise(this)
        WidgetBackgroundManager.configure(this)

        // Background updates are only needed while a widget is present
        if (!hasWidgets()) { return }

        WidgetBackgroundManager.scheduleWorker(this)
    }
}
