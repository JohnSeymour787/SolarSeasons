package com.johnseymour.solarseasons.current_uv_screen

import android.content.*
import androidx.lifecycle.ViewModel
import com.johnseymour.solarseasons.DiskRepository
import com.johnseymour.solarseasons.ErrorStatus
import com.johnseymour.solarseasons.isNotEqual
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import java.time.LocalDate

class MainViewModel: ViewModel()
{
    var uvData: UVData? = null
    var latestError: ErrorStatus? = null
    var uvForecastData: List<UVForecastData>? = null

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
     * Writes this object's uvData field into persistent storage
     */
    fun saveUVToDisk(context: Context)
    {
        uvData?.let()
        {
            DiskRepository.writeLatestUV(it, context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE))
        }
    }
}