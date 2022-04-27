package com.johnseymour.solarseasons

import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.johnseymour.solarseasons.api.SunInfoGsonAdapter
import com.johnseymour.solarseasons.api.UVDataGsonAdapter
import com.johnseymour.solarseasons.api.UVForecastGsonAdapter
import com.johnseymour.solarseasons.models.SunInfo
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData

object DiskRepository
{
    const val DATA_PREFERENCES_NAME = "uv_data_preferences.json"

    private val gson by lazy()
    {
        GsonBuilder()
            .registerTypeAdapter(SunInfo::class.java, SunInfoGsonAdapter)
            .registerTypeAdapter(UVData::class.java, UVDataGsonAdapter)
            .registerTypeAdapter(Array<UVForecastData>::class.java, UVForecastGsonAdapter)
            .create()
    }

    fun writeLatestUV(uvData: UVData, preferences: SharedPreferences)
    {
        preferences.edit()
            .putString(UVData.UV_DATA_KEY, gson.toJson(uvData))
            .apply()
    }

    fun readLatestUV(preferences: SharedPreferences): UVData?
    {
        return gson.fromJson(preferences.getString(UVData.UV_DATA_KEY, ""), UVData::class.java) ?: return null
    }

    fun writeLatestForecastList(forecast: List<UVForecastData>, preferences: SharedPreferences)
    {
        preferences.edit()
            .putString(UVForecastData.UV_FORECAST_LIST_KEY, gson.toJson(forecast.toTypedArray()))
            .apply()
    }

    fun readLatestForecast(preferences: SharedPreferences): List<UVForecastData>?
    {
        return gson.fromJson(preferences.getString(UVForecastData.UV_FORECAST_LIST_KEY, ""), Array<UVForecastData>::class.java)?.toList()
    }
}