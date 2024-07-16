package com.johnseymour.solarseasons.current_uv_screen

import android.appwidget.AppWidgetManager
import android.content.*
import android.text.format.DateFormat
import android.util.DisplayMetrics
import androidx.lifecycle.ViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.DiskRepository
import com.johnseymour.solarseasons.ErrorStatus
import com.johnseymour.solarseasons.MainActivity
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.SmallUVDisplay
import com.johnseymour.solarseasons.getWidgetIDs
import com.johnseymour.solarseasons.isNotEqual
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.models.UVLocationData
import com.johnseymour.solarseasons.services.LocationService
import com.johnseymour.solarseasons.services.UVDataUseCase
import java.time.LocalDate

class MainViewModel: ViewModel()
{
    private var localBroadcastManager: LocalBroadcastManager? = null

    var uvData: UVData? = null
    var latestError: ErrorStatus? = null
    var uvForecastData: List<UVForecastData>? = null

    private val locationUpdatedIntentFilter = IntentFilter(LocationService.LOCATION_UPDATE_RECEIVED)

    private val locationUpdatedBroadcastReceiver by lazy()
    {
        object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                if (intent.action == LocationService.LOCATION_UPDATE_RECEIVED)
                {
                    intent.getParcelableExtra<UVLocationData>(UVLocationData.UV_LOCATION_KEY)?.let()
                    {
                        DiskRepository.writeLastLocation(it, PreferenceManager.getDefaultSharedPreferences(context))
                        // Always update city data for a new location
                        currentUVForLocationData(context, it, true)
                    }
                }

                localBroadcastManager?.unregisterReceiver(this)
                localBroadcastManager = null
            }
        }
    }

    val uvDataBackgroundBroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            if (intent.action == UVData.UV_DATA_UPDATED)
            {
                (intent.getSerializableExtra(ErrorStatus.ERROR_STATUS_KEY) as? ErrorStatus)?.let()
                {
                    latestError = it
                } ?: intent.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
                {
                    intent.getParcelableArrayListExtra<UVForecastData>(UVForecastData.UV_FORECAST_LIST_KEY)?.toList()?.let()
                    { forecastData ->
                        uvForecastData = forecastData
                    }
                    uvData = it
                }
            }
        }
    }

    val uvDataChangedIntentFilter = IntentFilter(UVData.UV_DATA_UPDATED)

    var shouldRequestUVUpdateOnLaunch = true

    /**
     * Checks the date component of the UVForecastData to determine if it doesn't represent the forecast for today
     *
     * @return - true if forecast is either not for today or is null and requires updating
     *         - false if currently stored forecast is for today and doesn't need updating
     */
    fun isForecastNotCurrent(): Boolean
    {
        uvForecastData?.firstOrNull()?.time?.toLocalDate()?.let()
        {
            return it.isNotEqual(LocalDate.now())
        }
        return true
    }

    fun readForecastFromDisk(sharedPreferences: SharedPreferences)
    {
        uvForecastData = DiskRepository.readLatestForecast(sharedPreferences)
    }

    fun readUVFromDisk(sharedPreferences: SharedPreferences)
    {
        DiskRepository.readLatestUV(sharedPreferences)?.let { uvData = it }
    }

    /**
     * Calculates the best width to use for the UV Forecast list cells depending on screen
     *  density and if the user has 24 hour time format on (uses less space)
     */
    fun calculateUVForecastCellWidth(context: Context): Int
    {
        val resources = context.resources
        return if ((resources.displayMetrics.densityDpi < DisplayMetrics.DENSITY_XXXHIGH) && (DateFormat.is24HourFormat(context)))
        {
            resources.getDimensionPixelSize(R.dimen.uv_forecast_cell_width)
        }
        else
        {
            resources.getDimensionPixelSize(R.dimen.uv_forecast_cell_width_larger)
        }
    }

    /**
     * Calculates the best width to use for the Sun Times list cells depending on screen
     *  density and if the user has 24 hour time format on (uses less space)
     */
    fun calculateSunTimesCellWidth(context: Context): Int
    {
        val resources = context.resources
        return if ((resources.displayMetrics.densityDpi < DisplayMetrics.DENSITY_XXXHIGH) && (DateFormat.is24HourFormat(context)))
        {
            resources.getDimensionPixelSize(R.dimen.cell_sun_info_width)
        }
        else
        {
            resources.getDimensionPixelSize(R.dimen.cell_sun_info_width_larger)
        }
    }

    /**
     * Writes this object's uvData field into persistent storage
     */
    fun saveUVToDisk(context: Context)
    {
        uvData?.let()
        {
            DiskRepository.writeLatestUV(it, context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE))
        }
    }

    fun updateCurrentUV(context: Context, forceLocationUpdate: Boolean = false)
    {
        val lastLocationData = DiskRepository.readLastLocation(PreferenceManager.getDefaultSharedPreferences(context))

        if (lastLocationData == null || forceLocationUpdate)
        {
            localBroadcastManager = LocalBroadcastManager.getInstance(context)
            localBroadcastManager?.registerReceiver(locationUpdatedBroadcastReceiver, locationUpdatedIntentFilter)

            val locationServiceIntent = LocationService.createServiceIntent(context)

            context.startForegroundService(locationServiceIntent)
            return
        }

        currentUVForLocationData(context, lastLocationData, false)
    }

    private fun currentUVForLocationData(context: Context, locationData: UVLocationData, updateCityData: Boolean)
    {
        val widgetIds = context.getWidgetIDs()

        val widgetIntent = Intent(context, SmallUVDisplay::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            .putExtra(SmallUVDisplay.START_BACKGROUND_WORK_KEY, true)

        val activityIntent = Intent(context, MainActivity::class.java)
            .setAction(UVData.UV_DATA_UPDATED)

        UVDataUseCase(context).getUVData(locationData, isForecastNotCurrent(), updateCityData).success()
        {
            val dataSharedPreferences = context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)
            DiskRepository.writeLatestUV(it.uvData, dataSharedPreferences)

            it.forecast?.let()
            { forecastData ->
                DiskRepository.writeLatestForecastList(forecastData, dataSharedPreferences)
                activityIntent.putParcelableArrayListExtra(UVForecastData.UV_FORECAST_LIST_KEY, ArrayList(forecastData))
            }

            widgetIntent.putExtra(UVData.UV_DATA_KEY, it.uvData)
            activityIntent.putExtra(UVData.UV_DATA_KEY, it.uvData)

            context.sendBroadcast(widgetIntent)
            LocalBroadcastManager.getInstance(context).sendBroadcast(activityIntent)
        }.fail()
        {
            widgetIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)
            activityIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)

            context.sendBroadcast(widgetIntent)
            LocalBroadcastManager.getInstance(context).sendBroadcast(activityIntent)
        }
    }
}