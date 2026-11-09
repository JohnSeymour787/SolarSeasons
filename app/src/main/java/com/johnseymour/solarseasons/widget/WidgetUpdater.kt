package com.johnseymour.solarseasons.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Small helper so callers from non-suspend/traditional Android code can refresh all widgets. */
object WidgetUpdater
{
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun requestUpdate(context: Context)
    {
        val appContext = context.applicationContext
        scope.launch()
        {
            runCatching { SmallUvWidget().updateAll(appContext) }
        }
    }
}
