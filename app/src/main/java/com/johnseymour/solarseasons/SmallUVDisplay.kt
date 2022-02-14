package com.johnseymour.solarseasons

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import java.io.FileNotFoundException

/**
 * Implementation of App Widget functionality.
 */
class SmallUVDisplay : AppWidgetProvider()
{
    companion object
    {
        private var uvData: UVData? = null
        private var latestError: ErrorStatus? = null
        private var observer: Observer<List<WorkInfo>>? = null
        private var lastObserving: LiveData<List<WorkInfo>>? = null
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds)
        {
            // Pending intent to launch the MainActivity when a Widget is selected
            val intent: PendingIntent = Intent(context, MainActivity::class.java).apply()
            {
                action = UVData.UV_DATA_UPDATED
                latestError?.let()
                {
                    this.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)
                } ?: run { putExtra(UVData.UV_DATA_KEY, uvData) }
            }.let { PendingIntent.getActivity(context, appWidgetId, it, PendingIntent.FLAG_UPDATE_CURRENT) }

            updateAppWidget(context, appWidgetManager, appWidgetId, intent)
        }
    }

    // First widget is created
    override fun onEnabled(context: Context)
    {
        // If not already observing something, start a new immediate request
        if (observer == null)
        {
            initiateEarliestRequest(context)
        }
    }

    private fun initiateEarliestRequest(context: Context)
    {
        observer = createObserver(context)

        lastObserving = UVDataWorker.initiateWorker(context, true, Constants.SHORTEST_REFRESH_TIME)

        observer?.let { lastObserving?.observeForever(it) }
    }

    private fun createObserver(context: Context): Observer<List<WorkInfo>>
    {
        return Observer<List<WorkInfo>>
        { workInfo ->
            if (workInfo.firstOrNull()?.state == WorkInfo.State.SUCCEEDED)
            {
                val widgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, SmallUVDisplay::class.java))

                // Update intents
                val widgetIntent = Intent(context, SmallUVDisplay::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)

                val activityIntent = Intent(context, MainActivity::class.java)
                    .setAction(UVData.UV_DATA_UPDATED)

                LocationService.uvDataPromise?.success()
                {
                    if (!UVDataWorker.ignoreWorkRequest)
                    {
                        uvData = it
                        latestError = null

                        DiskRepository.writeLatestUV(it, context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE))

                        widgetIntent.putExtra(UVData.UV_DATA_KEY, it)
                        activityIntent.putExtra(UVData.UV_DATA_KEY, it)

                        context.sendBroadcast(widgetIntent)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(activityIntent)
                    }
                }?.fail()
                {
                    latestError = it

                    activityIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)

                    context.sendBroadcast(widgetIntent)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(activityIntent)
                }
            }
        }
    }

    override fun onDisabled(context: Context)
    {
        UVDataWorker.cancelWorker(context)
    }

    override fun onReceive(context: Context?, intent: Intent?)
    {
        context ?: return

        // Send an immediate request on phone startup
        if ((intent?.action == Intent.ACTION_BOOT_COMPLETED) && (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED))
        {
            observer?.let { lastObserving?.removeObserver(it) }

            initiateEarliestRequest(context)
        }

        intent?.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
        { luvData ->
            uvData = luvData
            latestError = null

            // Don't initiate a background request if that permission isn't given
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                return
            }

            // Observer can be null at this stage if haven't added a new widget recently
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

        // Read from disk if memory is null
        uvData ?: try { uvData = DiskRepository.readLatestUV(context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)) } catch (e: FileNotFoundException) {}

        when
        {
            latestError != null -> // Error has occurred
            {
                views.setTextViewText(R.id.uvValue, context.getString(R.string.widget_error))
                views.setTextColor(R.id.uvValue, context.resources.getColor(R.color.primary_text, context.theme))

                views.setViewVisibility(R.id.widgetSunProgress, View.INVISIBLE)
                views.setViewVisibility(R.id.updatedTime, View.INVISIBLE)
                views.setInt(R.id.backgroundView, "setColorFilter", context.resources.getColor(R.color.uv_low, context.theme))
            }

            uvData != null -> // Valid data
            {
                val luvData = uvData ?: return

                val uvString = context.getString(R.string.uv_value, luvData.uv)
                val timeString = preferredTimeString(context, luvData.uvTime)

                views.setTextViewText(R.id.uvValue, uvString)
                views.setTextColor(R.id.uvValue, context.resources.getColor(luvData.textColorInt, context.theme))

                views.setTextViewText(R.id.updatedTime, timeString)
                views.setTextColor(R.id.updatedTime, context.resources.getColor(luvData.textColorInt, context.theme))
                views.setViewVisibility(R.id.updatedTime, View.VISIBLE)

                views.setInt(R.id.widgetSunProgress, "setProgress", luvData.sunProgressPercent)
                views.setViewVisibility(R.id.widgetSunProgress, View.VISIBLE)

                views.setInt(R.id.backgroundView, "setColorFilter", context.resources.getColor(luvData.backgroundColorInt, context.theme))
            }

            else -> // Default values on startup
            {
                views.setTextViewText(R.id.uvValue, "0.0")

                views.setTextViewText(R.id.updatedTime, "00:00")
                views.setViewVisibility(R.id.updatedTime, View.VISIBLE)

                views.setInt(R.id.widgetSunProgress, "setProgress", 0)
                views.setViewVisibility(R.id.widgetSunProgress, View.VISIBLE)
            }
        }

        views.setOnClickPendingIntent(R.id.layout, intent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}