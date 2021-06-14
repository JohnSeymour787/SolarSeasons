package com.johnseymour.solarseasons

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 */
class SmallUVDisplay : AppWidgetProvider()
{
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds)
        {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context)
    {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context)
    {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int)
{
    val uv: Double = 3.1332

    val widgetText = context.getString(R.string.widget_uv_value, uv)

    val bgColor = if (uv < 3.0)
    {
        R.color.uv_low
    }
    else if (uv >= 3.0 && uv < 6.0)
    {
        R.color.uv_moderate
    }
    else if (uv >= 6.0 && uv < 8.0)
    {
        R.color.uv_high
    }
    else if (uv >= 8.0 && uv < 11.0)
    {
        R.color.uv_very_high
    }
    else
    {
        R.color.uv_extreme
    }

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.small_u_v_display)
    views.setTextViewText(R.id.uvValue, widgetText)
    views.setInt(R.id.layout, "setBackgroundColor", context.resources.getColor(bgColor, context.theme))
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}