package com.johnseymour.solarseasons.ui.currentuv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.DiskRepository
import com.johnseymour.solarseasons.ErrorStatus
import com.johnseymour.solarseasons.data.AppSettingsRepository
import com.johnseymour.solarseasons.models.SunInfo
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.models.UVLocationData
import com.johnseymour.solarseasons.parcelableArrayListCompat
import com.johnseymour.solarseasons.parcelableCompat
import com.johnseymour.solarseasons.serializableCompat
import com.johnseymour.solarseasons.services.LocationService
import com.johnseymour.solarseasons.services.UVDataUseCase
import com.johnseymour.solarseasons.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nl.komponents.kovenant.deferred
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.ZonedDateTime

data class ForecastDisplay(val data: List<UVForecastData>, val initialIndex: Int)

data class SunInfoDisplay(val data: List<SunInfo.SunTimeData>, val initialIndex: Int)

data class CurrentUvUiState(
    val uvData: UVData? = null,
    val forecast: List<UVForecastData> = emptyList(),
    val forecastInitialIndex: Int = 0,
    val sunInfoTimes: List<SunInfo.SunTimeData> = emptyList(),
    val sunInfoInitialIndex: Int = 0,
    val error: ErrorStatus? = null,
    val statusMessage: String? = null,
    val showAppDetailsButton: Boolean = false,
    val isRefreshing: Boolean = false,
)

class CurrentUvViewModel : ViewModel()
{
    private val _uiState = MutableStateFlow(CurrentUvUiState())
    val uiState: StateFlow<CurrentUvUiState> = _uiState.asStateFlow()

    private var uvData: UVData? = null
    private var latestError: ErrorStatus? = null
    private var uvForecastData: List<UVForecastData>? = null
    private var initialPermissionsRequested = false
    private var initialised = false
    private var receiverContext: Context? = null

    val uvDataChangedIntentFilter = IntentFilter(UVData.UV_DATA_UPDATED)

    val uvDataBackgroundBroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            handleUvBroadcast(context, intent)
        }
    }

    private val uvDataForegroundBroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            handleUvBroadcast(context, intent)
        }
    }

    private fun handleUvBroadcast(context: Context, intent: Intent)
    {
        if (intent.action != UVData.UV_DATA_UPDATED) { return }

        intent.parcelableCompat<UVData>(UVData.UV_DATA_KEY)?.let()
        { data ->
            intent.parcelableArrayListCompat<UVForecastData>(UVForecastData.UV_FORECAST_LIST_KEY)
                ?.toList()
                ?.let { forecast -> uvForecastData = forecast }

            uvData = data
            latestError = null
            persistError(context, null)
            publishContent()
        } ?: intent.serializableCompat<ErrorStatus>(ErrorStatus.ERROR_STATUS_KEY)?.let()
        { error ->
            latestError = error
            persistError(context, error)
            publishError(error)
        }
    }

    fun initialise(context: Context, readPersistedError: Boolean = false)
    {
        if (initialised) { return }

        initialised = true

        try
        {
            val dataPreferences = context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)

            if (readPersistedError)
            {
                DiskRepository.readLatestError(dataPreferences)?.let()
                {
                    latestError = it
                    publishError(it)
                    return
                }
            }

            uvForecastData = DiskRepository.readLatestForecast(dataPreferences)
            DiskRepository.readLatestUV(dataPreferences)?.let()
            {
                uvData = it
                publishContent()
            }
        }
        catch (_: FileNotFoundException) { }
    }

    fun onStart(context: Context, readPersistedError: Boolean = false)
    {
        val appContext = context.applicationContext
        receiverContext = appContext

        unregisterSafely(appContext, uvDataBackgroundBroadcastReceiver)
        ContextCompat.registerReceiver(
            appContext,
            uvDataForegroundBroadcastReceiver,
            uvDataChangedIntentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        AppSettingsRepository.initialise(appContext)
        initialise(appContext, readPersistedError)

        val shouldRequestUVUpdateOnLaunch = PreferenceManager.getDefaultSharedPreferences(appContext)
            .getBoolean(Constants.SharedPreferences.APP_LAUNCH_AUTO_REQUEST_KEY, true)

        val currentError = latestError
        if (currentError != null)
        {
            publishError(currentError)
            return
        }

        uvData?.let { publishContent() }

        if (!shouldRequestUVUpdateOnLaunch) { return }

        if ((uvData?.minutesSinceDataRetrieved ?: 0) > Constants.MINIMUM_APP_FOREGROUND_REFRESH_TIME)
        {
            _uiState.update { it.copy(isRefreshing = true) }
            updateCurrentUV(appContext, false)
        }
    }

    fun onStop(context: Context)
    {
        val appContext = context.applicationContext

        unregisterSafely(appContext, uvDataForegroundBroadcastReceiver)
        ContextCompat.registerReceiver(
            appContext,
            uvDataBackgroundBroadcastReceiver,
            uvDataChangedIntentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun refresh(context: Context)
    {
        val appContext = context.applicationContext

        _uiState.update { it.copy(statusMessage = null, showAppDetailsButton = false, isRefreshing = true) }

        val forceLocationUpdate =
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        updateCurrentUV(appContext, forceLocationUpdate)
    }

    fun consumeInitialPermissionRequest(): Boolean
    {
        if (initialPermissionsRequested) { return false }

        initialPermissionsRequested = true
        return true
    }

    fun setStatusMessage(message: String)
    {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun setAppDetailsButtonVisible(visible: Boolean)
    {
        _uiState.update { it.copy(showAppDetailsButton = visible) }
    }

    fun clearStatus()
    {
        _uiState.update { it.copy(statusMessage = null, showAppDetailsButton = false) }
    }

    fun onCloudCoverSettingChanged(context: Context, enabled: Boolean)
    {
        if (enabled) { return }

        val data = uvData ?: return
        if (data.cloudCover == null) { return }

        data.cloudCover = null
        publishContent()
        saveUVToDisk(context.applicationContext)
        WidgetUpdater.requestUpdate(context.applicationContext)
    }

    fun updateCurrentUV(context: Context, forceLocationUpdate: Boolean = false)
    {
        val appContext = context.applicationContext
        val lastLocationData = DiskRepository.readLastLocation(PreferenceManager.getDefaultSharedPreferences(appContext))

        if (lastLocationData == null || forceLocationUpdate)
        {
            LocationService.locationDataDeferred = deferred()

            val locationServiceIntent = LocationService.createServiceIntent(appContext)
            appContext.startForegroundService(locationServiceIntent)

            LocationService.locationDataPromise?.success()
            {
                DiskRepository.writeLastLocation(it, PreferenceManager.getDefaultSharedPreferences(appContext))
                currentUVForLocationData(appContext, it, true)
                LocationService.locationDataDeferred = null
            }

            return
        }

        currentUVForLocationData(appContext, lastLocationData, false)
    }

    fun saveUVToDisk(context: Context)
    {
        uvData?.let()
        {
            DiskRepository.writeLatestUV(
                it,
                context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)
            )
        }
    }

    private fun currentUVForLocationData(context: Context, locationData: UVLocationData, updateCityData: Boolean)
    {
        val appContext = context.applicationContext
        val dataPreferences = appContext.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)

        UVDataUseCase(appContext).getUVData(locationData, isForecastNotCurrent(), updateCityData).success()
        {
            DiskRepository.writeLatestUV(it.uvData, dataPreferences)
            DiskRepository.writeLatestError(null, dataPreferences)

            it.forecast?.let()
            { forecastData ->
                DiskRepository.writeLatestForecastList(forecastData, dataPreferences)
                uvForecastData = forecastData
            }

            uvData = it.uvData
            latestError = null
            publishContent()
            WidgetUpdater.requestUpdate(appContext)
        }.fail()
        {
            DiskRepository.writeLatestError(it, dataPreferences)
            latestError = it
            publishError(it)
            WidgetUpdater.requestUpdate(appContext)
        }
    }

    private fun publishContent()
    {
        val data = uvData ?: return
        val forecastDisplay = buildForecastDisplay(data)
        val sunInfoDisplay = buildSunInfoDisplay(data)

        _uiState.update()
        {
            it.copy(
                uvData = data,
                forecast = forecastDisplay?.data ?: emptyList(),
                forecastInitialIndex = forecastDisplay?.initialIndex ?: 0,
                sunInfoTimes = sunInfoDisplay.data,
                sunInfoInitialIndex = sunInfoDisplay.initialIndex,
                error = null,
                isRefreshing = false,
            )
        }
    }

    private fun publishError(error: ErrorStatus)
    {
        _uiState.update { it.copy(error = error, isRefreshing = false) }
    }

    private fun persistError(context: Context, error: ErrorStatus?)
    {
        DiskRepository.writeLatestError(
            error,
            context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)
        )
    }

    private fun unregisterSafely(context: Context, receiver: BroadcastReceiver)
    {
        try
        {
            context.unregisterReceiver(receiver)
        }
        catch (_: IllegalArgumentException) { }
    }

    /**
     * Checks the date component of the UVForecastData to determine if it doesn't represent the forecast for today.
     */
    fun isForecastNotCurrent(): Boolean
    {
        uvForecastData?.firstOrNull()?.time?.toLocalDate()?.let()
        {
            return !it.isEqual(LocalDate.now())
        }
        return true
    }

    private fun buildForecastDisplay(data: UVData): ForecastDisplay?
    {
        val forecast = uvForecastData ?: return null

        // A new daily request can fail; don't show yesterday's forecast.
        if (forecast.isEmpty() || isForecastNotCurrent()) { return null }

        val timeNow = ZonedDateTime.now()

        var bestScrollPosition = 0
        while ((bestScrollPosition < forecast.size) && timeNow.isAfter(forecast[bestScrollPosition].time))
        {
            bestScrollPosition++
        }

        bestScrollPosition--

        val forecastList = forecast.toMutableList()
        val currentData = UVForecastData(data.uv, timeNow, isTimeNow = true)

        when (bestScrollPosition)
        {
            -1 ->
            {
                if (data.minutesSinceDataRetrieved > Constants.UV_FORECAST_ACCEPTABLE_RECENT_UV_TIME)
                {
                    forecastList.add(0, currentData.copy(uv = 0.0F))
                }
                else
                {
                    forecastList.add(0, currentData)
                }

                bestScrollPosition = 0
            }

            in 0 until forecast.size - 1 ->
            {
                if (data.minutesSinceDataRetrieved > Constants.UV_FORECAST_ACCEPTABLE_RECENT_UV_TIME)
                {
                    forecastList[bestScrollPosition] = forecastList[bestScrollPosition].copy(isTimeNow = true)
                }
                else
                {
                    forecastList[bestScrollPosition] = currentData
                }
            }

            forecast.size - 1 ->
            {
                if (data.minutesSinceDataRetrieved > Constants.UV_FORECAST_ACCEPTABLE_RECENT_UV_TIME)
                {
                    forecastList.add(currentData.copy(uv = 0.0F))
                }
                else
                {
                    forecastList.add(currentData)
                }

                bestScrollPosition++
            }
        }

        return ForecastDisplay(forecastList, bestScrollPosition)
    }

    private fun buildSunInfoDisplay(data: UVData): SunInfoDisplay
    {
        val sortedSolarTimes = data.sunInfo.timesArray.sortedWith { a, b -> a.time.compareTo(b.time) }
        val timeNow = ZonedDateTime.now()

        var bestScrollPosition = 0
        while ((bestScrollPosition < sortedSolarTimes.size - 1) && timeNow.isAfter(sortedSolarTimes[bestScrollPosition].time))
        {
            bestScrollPosition++
        }

        return SunInfoDisplay(sortedSolarTimes, bestScrollPosition)
    }

    override fun onCleared()
    {
        receiverContext?.let()
        {
            unregisterSafely(it, uvDataForegroundBroadcastReceiver)
            unregisterSafely(it, uvDataBackgroundBroadcastReceiver)
        }

        receiverContext = null
        super.onCleared()
    }
}
