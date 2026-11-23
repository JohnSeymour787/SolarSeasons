package com.johnseymour.solarseasons.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.johnseymour.solarseasons.UVDataWorker

class SmallUvWidgetReceiver : GlanceAppWidgetReceiver()
{
    override val glanceAppWidget: GlanceAppWidget = SmallUvWidget()

    override fun onReceive(context: Context, intent: Intent)
    {
        WidgetBackgroundManager.configure(context)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED)
        {
            WidgetBackgroundManager.scheduleWorker(context)
            return
        }

        super.onReceive(context, intent)
    }

    override fun onDisabled(context: Context)
    {
        super.onDisabled(context)

        UVDataWorker.cancelWorker(context)
        WidgetBackgroundManager.setReceiveScreenUnlock(context, false, force = true)
    }
}
