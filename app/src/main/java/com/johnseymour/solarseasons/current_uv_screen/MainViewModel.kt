package com.johnseymour.solarseasons.current_uv_screen

import android.content.*
import androidx.lifecycle.ViewModel
import com.johnseymour.solarseasons.DiskRepository
import com.johnseymour.solarseasons.ErrorStatus
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

    fun isForecastCurrent(): Boolean
    {
        uvForecastData?.firstOrNull()?.time?.toLocalDate()?.let()
        {
            return it.isEqual(LocalDate.now())
        }
        return false
    }

    fun readForecastFromDisk(sharedPreferences: SharedPreferences)
    {
        uvForecastData = DiskRepository.readLatestForecast(sharedPreferences)
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