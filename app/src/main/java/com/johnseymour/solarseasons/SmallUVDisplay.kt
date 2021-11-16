package com.johnseymour.solarseasons

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.*

/**
 * Implementation of App Widget functionality.
 */
class SmallUVDisplay : AppWidgetProvider()
{
    companion object
    {
        private var uvData: UVData? = null
        private var observer: Observer<List<WorkInfo>>? = null
        private var lastObserving: LiveData<List<WorkInfo>>? = null
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        // Pending intent to launch the MainActivity when a Widget is selected
        val intent: PendingIntent = Intent(context, MainActivity::class.java).apply()
        {
            action = UVData.UV_DATA_CHANGED
            putExtra(UVData.UV_DATA_KEY, uvData)
        }.let { PendingIntent.getActivity(context, 0, it, 0) }

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds)
        {
            updateAppWidget(context, appWidgetManager, appWidgetId, intent)
        }
    }

    // First widget is created
    override fun onEnabled(context: Context)
    {
        // If not already observing something, start a new immediate request
        if (observer == null)
        {
            observer = createObserver(context)

            lastObserving = UVDataWorker.initiateWorker(context)
            observer?.let { lastObserving?.observeForever(it) }
        }
    }

    private fun createObserver(context: Context): Observer<List<WorkInfo>>
    {
        return Observer<List<WorkInfo>>
        { workInfo ->
            if (workInfo.firstOrNull()?.state == WorkInfo.State.SUCCEEDED)
            {
                UVDataWorker.uvDataPromise?.success()
                {
                    uvData = it //TODO() Write to disk

                    // Update all widgets
                    val intent = Intent(context, SmallUVDisplay::class.java).apply()
                    {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(UVData.UV_DATA_KEY, it)
                    }

                    context.sendBroadcast(intent)
                }
            }
        }
    }

    override fun onDisabled(context: Context)
    {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?)
    {
        context ?: return

        intent?.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
        { luvData ->
            uvData = luvData
            // Observer can be null at this stage if haven't added a new widget recently (ie, after phone restart)
            if (observer == null)
            {
                observer = createObserver(context)
            }

            // Stop observing completed work
            observer?.let { lastObserving?.removeObserver(it) }

            // Prepare a new worker to observe
            lastObserving = if (luvData.sunInSky())
            {
                // Override new work to observe
                UVDataWorker.initiateWorker(context, true)
            }
            else
            {
                // Delay the next automatic worker until the sunrise of the next day
                UVDataWorker.initiateWorker(context, true, luvData.minutesUntilSunrise)
            }

            // Begin observing new work
            observer?.let { lastObserving?.observeForever(it) }
        }

        // Will call onUpdate
        super.onReceive(context, intent)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, intent: PendingIntent)
    {
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.small_u_v_display)
        uvData?.let()
        {
            val uvString = context.getString(R.string.widget_uv_value, it.uv)
            val timeString = if (DateFormat.is24HourFormat(context)) { Constants.Formatters.hour24.format(it.uvTime) } else { Constants.Formatters.hour12.format(it.uvTime) }

            views.setTextViewText(R.id.uvValue, uvString)
            views.setTextColor(R.id.uvValue, context.resources.getColor(it.textColorInt, context.theme))

            views.setTextViewText(R.id.updatedTime, timeString)
            views.setTextColor(R.id.updatedTime, context.resources.getColor(it.textColorInt, context.theme))

            views.setInt(R.id.widgetSunProgress, "setProgress", it.sunProgressPercent)

            views.setInt(R.id.backgroundView, "setColorFilter", context.resources.getColor(it.backgroundColorInt, context.theme))

        } ?: run()
        {
            views.setTextViewText(R.id.uvValue, "0.0")
            views.setTextViewText(R.id.updatedTime, "00:00")
            views.setInt(R.id.widgetSunProgress, "setProgress", 0)
        }

        views.setOnClickPendingIntent(R.id.layout, intent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}