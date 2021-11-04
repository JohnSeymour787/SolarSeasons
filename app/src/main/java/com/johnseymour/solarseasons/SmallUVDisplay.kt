package com.johnseymour.solarseasons

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.*
import java.lang.ref.WeakReference
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Implementation of App Widget functionality.
 */
class SmallUVDisplay : AppWidgetProvider()
{
    companion object
    {
        var uvData: UVData? = null // todo() probably make private

        var test = 0.0
    }

  //  private var observing: LiveData<List<WorkInfo>>? = null

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

    private var observer: Observer<List<WorkInfo>>? = null
    private var lastObserving: LiveData<List<WorkInfo>>? = null

    // First widget is created
    override fun onEnabled(context: Context)
    {
        lastObserving = UVDataWorker.initiateWorker(context)

        if (observer == null)
        {
            observer = Observer<List<WorkInfo>>
            { workInfo ->
                if (workInfo.firstOrNull()?.state == WorkInfo.State.SUCCEEDED)
                {
                    UVDataWorker.uvDataPromise?.success()
                    {
                        uvData = it //TODO() Write to disk


                      //  updateUVData(context, it)


                        // Update all widgets
                        val intent = Intent(context, SmallUVDisplay::class.java).apply()
                        {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(UVData.UV_DATA_KEY, it)

                            //val ids = AppWidgetManager.getInstance(context.).getAppWidgetIds(ComponentName(applicationContext, SmallUVDisplay::class.java))
                          //  putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }

                        context.sendBroadcast(intent)

                    }
                }
            }

            observer?.let { lastObserving?.observeForever(it) }
        }

      //  context.registerReceiver(this, IntentFilter().apply { addAction(UVData.UV_DATA_CHANGED) })
    }

    override fun onDisabled(context: Context)
    {
        // Enter relevant functionality for when the last widget is disabled
    }

    private fun updateUVData(context: Context, uvData: UVData)
    {
        Handler(Looper.getMainLooper()).post()
        {

            val uv: Float = test.toFloat()

            //uvData.uv = test.toFloat()
            test++
/*
            return@post

            observer?.let { lastObserving?.removeObserver(it) }

            return@post //TODO() temporarily preventing recursive API calls
            lastObserving = UVDataWorker.initiateWorker(context, true)

            if (observer == null)
            {
                observer = Observer<List<WorkInfo>>
                { workInfo ->
                    if (workInfo.firstOrNull()?.state == WorkInfo.State.SUCCEEDED)
                    {
                        UVDataWorker.uvDataPromise?.success()
                        {
                            Companion.uvData = it
                            updateUVData(context, it)
                        }
                    }
                }
            }*/

            observer?.let { lastObserving?.observeForever(it) }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?)
    {
        val cake = 2

        context ?: return

        intent?.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
        { luvData ->
            uvData = luvData
            // TODO() Observer can be null at this stage if haven't added a new widget recently (ie, after phone restart)
            // Stop observing completed work
            observer?.let { lastObserving?.removeObserver(it) }
            // Override new work to observe
            lastObserving = UVDataWorker.initiateWorker(context, true)
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

