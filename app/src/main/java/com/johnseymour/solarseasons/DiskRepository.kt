package com.johnseymour.solarseasons

import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.johnseymour.solarseasons.api.SunInfoGsonAdapter
import com.johnseymour.solarseasons.api.UVDataGsonAdapter
import com.johnseymour.solarseasons.api.UVForecastGsonAdapter
import com.johnseymour.solarseasons.models.SunInfo
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.models.UVLocationData
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

    fun uvNotificationCustomTime(preferences: SharedPreferences): ZonedDateTime?
    {
        try
        {
            val localTime = LocalTime.parse(preferences.getString(Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_CUSTOM_TIME_KEY, null)?.replace('.', ':'), DateTimeFormatter.ISO_TIME)
            return localTime.atDate(LocalDate.now()).atZone(ZoneId.systemDefault())
        }
        catch (ignored: Exception) {}

        return null
    }

    fun uvNotificationTimeType(preferences: SharedPreferences): NotificationTimeType
    {
        return NotificationTimeType.from(preferences.getString(Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_TIME_KEY, null) ?: "") ?: NotificationTimeType.DayStart
    }

    fun writeLastLocation(location: UVLocationData, preferences: SharedPreferences)
    {
        preferences.edit()
            .putString(Constants.SharedPreferences.MANUAL_LOCATION_LATITUDE_KEY, location.latitude.toString())
            .putString(Constants.SharedPreferences.MANUAL_LOCATION_LONGITUDE_KEY, location.longitude.toString())
            .putString(Constants.SharedPreferences.MANUAL_LOCATION_ALTITUDE_KEY, location.altitude.toString())
            .apply()
    }

    fun readLastLocation(preferences: SharedPreferences): UVLocationData?
    {
        val latitude = preferences.getString(Constants.SharedPreferences.MANUAL_LOCATION_LATITUDE_KEY, null)?.toDoubleOrNull() ?: return null
        val longitude = preferences.getString(Constants.SharedPreferences.MANUAL_LOCATION_LONGITUDE_KEY, null)?.toDoubleOrNull() ?: return null
        val altitude = preferences.getString(Constants.SharedPreferences.MANUAL_LOCATION_ALTITUDE_KEY, null)?.toDoubleOrNull() ?: return null

        return UVLocationData(latitude, longitude, altitude)
    }

    fun writeLastCityName(cityName: String, preferences: SharedPreferences)
    {
        preferences.edit()
            .putString(Constants.SharedPreferences.LATEST_CITY_NAME_KEY, cityName)
            .apply()
    }

    fun readLastCityName(preferences: SharedPreferences): String?
    {
        return preferences.getString(Constants.SharedPreferences.LATEST_CITY_NAME_KEY, null)
    }

    enum class NotificationTimeType(val valueString: String)
    {
        /** When firstRequestOfDay occurs. **/
        DayStart("first_request"),
        /** At fromTime. **/
        WhenNeeded("uv_from_time"),
        /** User selected time. **/
        Custom("custom_time");

        companion object
        {
            private val map = NotificationTimeType.values().associateBy { it.valueString }
            infix fun from(value: String) = map[value]
        }
    }
}