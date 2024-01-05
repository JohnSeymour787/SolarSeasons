package com.johnseymour.solarseasons

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.content.pm.PackageManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.api.OPENUV_API_KEY
import com.johnseymour.solarseasons.models.UVData
import java.io.FileNotFoundException
import java.time.LocalDate

/**
 * Implementation of App Widget functionality.
 */
class SmallUVDisplay : AppWidgetProvider()
{
    companion object
    {
        private var uvData: UVData? = null
        private var latestError: ErrorStatus? = null
        private var previousReceivingScreenOnBroadcastSetting = false
        private var backgroundRefreshRate = Constants.DEFAULT_REFRESH_TIME
        private var companionFieldsInitialised = false
        var usePeriodicWork = false
        const val SET_RECEIVING_SCREEN_UNLOCK_KEY = "set_receiving_screen_unlock_key"
        const val SET_USE_PERIODIC_WORK_KEY = "set_use_periodic_work_key"
        const val SET_BACKGROUND_REFRESH_RATE_KEY = "set_background_refresh_rate_key"
        const val START_BACKGROUND_WORK_KEY = "start_background_work_key"

        private val userPresentFilter = IntentFilter(Intent.ACTION_USER_PRESENT)
        private val userPresentReceiver = object: BroadcastReceiver()
        {
            override fun onReceive(context: Context?, intent: Intent?)
            {
                context ?: return

                val luvData = uvData ?: return

                if (!luvData.sunInSky()) { return }

                // Compare uvData time with current time, if difference is greater than the backgroundRefreshRate then make a new request
                if (luvData.minutesSinceDataRetrieved > (backgroundRefreshRate + Constants.WORK_EXECUTION_SLACK_TIME))
                {
                    prepareEarliestRequest(context)
                }
            }
        }

        private fun prepareEarliestRequest(context: Context)
        {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_DENIED)
            {
                return
            }

            UVDataWorker.initiateOneTimeWorker(context, firstDailyRequest = isFirstDailyRequest(context), false, Constants.SHORTEST_REFRESH_TIME)
        }

        private fun isFirstDailyRequest(context: Context): Boolean
        {
            try
            {
                val forecast = DiskRepository.readLatestForecast(context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE))?.firstOrNull() ?: return true

                return forecast.time.toLocalDate().isNotEqual(LocalDate.now())
            }
            catch (_: FileNotFoundException) {}
            return true
        }
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
            }.let { PendingIntent.getActivity(context, appWidgetId, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }

            updateAppWidget(context, appWidgetManager, appWidgetId, intent)
        }
    }

    // First widget is created
    override fun onEnabled(context: Context)
    {
        if (!companionFieldsInitialised)
        {
            configureWidgetCompanion(context)
        }
    }

    override fun onDisabled(context: Context)
    {
        UVDataWorker.cancelWorker(context)
    }

    /**
     * Initialises companion object lateinit vars as well as reading the shared preferences for the widget and making
     *  necessary configuration based on their values.
     */
    private fun configureWidgetCompanion(context: Context)
    {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext).apply()
        {
            if (Constants.ENABLE_API_KEY_ENTRY_FEATURE)
            {
                OPENUV_API_KEY = getString(Constants.SharedPreferences.API_KEY, null) ?: ""
            }

            previousReceivingScreenOnBroadcastSetting = getBoolean(Constants.SharedPreferences.SUBSCRIBE_SCREEN_UNLOCK_KEY, previousReceivingScreenOnBroadcastSetting)

            if (previousReceivingScreenOnBroadcastSetting)
            {
                context.applicationContext.registerReceiver(userPresentReceiver, userPresentFilter)
            }

            usePeriodicWork = getString(Constants.SharedPreferences.WORK_TYPE_KEY, Constants.SharedPreferences.DEFAULT_WORK_TYPE_VALUE) == Constants.SharedPreferences.DEFAULT_WORK_TYPE_VALUE
            backgroundRefreshRate = getString(Constants.SharedPreferences.BACKGROUND_REFRESH_RATE_KEY, null)?.toLongOrNull() ?: backgroundRefreshRate
        }

        // Initialise memory
        uvData ?: try { uvData = DiskRepository.readLatestUV(context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)) } catch (_: FileNotFoundException) {}

        companionFieldsInitialised = true
    }

    override fun onReceive(context: Context?, intent: Intent?)
    {
        context ?: return

        if (!companionFieldsInitialised)
        {
            configureWidgetCompanion(context)
        }

        // Send an immediate request on phone startup
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED)
        {
            prepareEarliestRequest(context)
            return
        }

        (intent?.getSerializableExtra(SET_RECEIVING_SCREEN_UNLOCK_KEY) as? Boolean)?.let()
        { receiveScreenUnlockSetting ->
            if (receiveScreenUnlockSetting != previousReceivingScreenOnBroadcastSetting)
            {
                if (receiveScreenUnlockSetting)
                {
                    context.applicationContext.registerReceiver(userPresentReceiver, userPresentFilter)
                }
                else
                {
                    context.applicationContext.unregisterReceiver(userPresentReceiver)
                }
                previousReceivingScreenOnBroadcastSetting = receiveScreenUnlockSetting
            }
        }

        (intent?.getSerializableExtra(SET_USE_PERIODIC_WORK_KEY) as? Boolean)?.let()
        {
            if (it != usePeriodicWork)
            {
                usePeriodicWork = it
                prepareEarliestRequest(context)
            }
        }

        (intent?.getSerializableExtra(SET_BACKGROUND_REFRESH_RATE_KEY) as? Long)?.let()
        {
            if (it != backgroundRefreshRate)
            {
                backgroundRefreshRate = it
                prepareEarliestRequest(context)
            }
        }

        intent?.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
        { luvData ->
            uvData = luvData
            latestError = null

            // Don't initiate a background request if that permission isn't given
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                return@let
            }

            if ((usePeriodicWork) && (!luvData.sunInSky()))
            {
                // Delay the next automatic worker until the sunrise of the next day
                UVDataWorker.initiatePeriodicWorker(context, timeInterval = backgroundRefreshRate, startDelay = luvData.minutesUntilSunrise)
                return@let
            }

            if (intent.getBooleanExtra(START_BACKGROUND_WORK_KEY, false))
            {
                when
                {
                    usePeriodicWork -> UVDataWorker.initiatePeriodicWorker(context, timeInterval = backgroundRefreshRate)

                    luvData.sunInSky() -> UVDataWorker.initiateOneTimeWorker(context, firstDailyRequest = isFirstDailyRequest(context),true, backgroundRefreshRate)

                    // Delay the next automatic worker until the sunrise of the next day
                    else -> UVDataWorker.initiateOneTimeWorker(context, firstDailyRequest = true,true, luvData.minutesUntilSunrise)
                }
            }
        }

        (intent?.getSerializableExtra(ErrorStatus.ERROR_STATUS_KEY) as? ErrorStatus)?.let()
        {
            latestError = it
        }

        // Will call onUpdate
        super.onReceive(context, intent)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, intent: PendingIntent)
    {
        val lUseCustomTheme = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SharedPreferences.APP_THEME_KEY, null) == Constants.SharedPreferences.CUSTOM_APP_THEME_VALUE

        // Construct the RemoteViews object, custom theme set determines the layout used due mainly to the progressBar colour
        val views = if (lUseCustomTheme)
        {
            RemoteViews(context.packageName, R.layout.small_u_v_display)
        }
        else
        {
            RemoteViews(context.packageName, R.layout.small_u_v_display_default_theme)
        }

        when
        {
            // Errors such as battery saver and general location error don't need to display the error message (ie errors that are not the result of direct user action)
            latestError != null && latestError != ErrorStatus.LocationBatterySaverError && latestError != ErrorStatus.GeneralLocationError -> // Error has occurred
            {
                views.setTextViewText(R.id.uvValue, context.getString(R.string.widget_error))

                views.setViewVisibility(R.id.widgetSunProgress, View.INVISIBLE)
                views.setViewVisibility(R.id.updatedTime, View.INVISIBLE)

                if (lUseCustomTheme)
                {
                    views.setTextColor(R.id.uvValue, context.resources.getColor(R.color.dark_text, context.theme))
                    views.setInt(R.id.backgroundView, "setColorFilter", context.resources.getColor(R.color.uv_low, context.theme))
                }
                else
                {
                    views.setTextColor(R.id.uvValue, context.resources.getColor(R.color.appWidgetTextColor, context.theme))
                    views.setInt(R.id.backgroundView, "setColorFilter", context.resources.getColor(R.color.appWidgetBackgroundColor, context.theme))
                }
            }

            uvData != null -> // Valid data
            {
                val luvData = uvData ?: return

                val uvString = context.getString(R.string.uv_value, luvData.uv)
                val timeString = preferredTimeString(context, luvData.uvTime)

                views.setTextViewText(R.id.uvValue, uvString)

                views.setTextViewText(R.id.updatedTime, timeString)
                views.setViewVisibility(R.id.updatedTime, View.VISIBLE)

                views.setInt(R.id.widgetSunProgress, "setProgress", luvData.sunProgressPercent)
                views.setViewVisibility(R.id.widgetSunProgress, View.VISIBLE)

                if (lUseCustomTheme)
                {
                    views.setTextColor(R.id.uvValue, context.resources.getColor(luvData.textColorInt, context.theme))
                    views.setTextColor(R.id.updatedTime, context.resources.getColor(luvData.textColorInt, context.theme))
                    views.setInt(R.id.backgroundView, "setColorFilter", context.resources.getColor(luvData.backgroundColorInt, context.theme))
                }
                else
                {
                    views.setTextColor(R.id.uvValue, context.resources.getColor(luvData.backgroundColorInt, context.theme))
                }
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