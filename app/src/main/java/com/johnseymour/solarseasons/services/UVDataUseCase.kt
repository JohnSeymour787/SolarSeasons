package com.johnseymour.solarseasons.services

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.DiskRepository
import com.johnseymour.solarseasons.ErrorStatus
import com.johnseymour.solarseasons.api.NetworkRepository
import com.johnseymour.solarseasons.models.UVCombinedForecastData
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.models.UVLocationData
import com.johnseymour.solarseasons.models.UVProtectionTimeData
import com.johnseymour.solarseasons.toEpochMilli
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue

class UVDataUseCase(val context: Context)
{
    private var canRetryRequest = true // Single retry of realtime UV data allowed

    private var uvData: UVData? = null
    private var cloudCover: Double? = null
    private var cityName: String? = null
    private var uvForecast: List<UVForecastData>? = null
    private var uvProtection: UVProtectionTimeData? = null

    private fun calculateNumberOfRequests(cloudCoverEnabled: Boolean, isFirstDailyRequest: Boolean, updateCityData: Boolean): Int
    {
        var result = 1 // UV data always fetched

        if (updateCityData) { result++ }
        if (cloudCoverEnabled) { result++ }
        if (isFirstDailyRequest) { result += 2 } // Protection times and UV forecast

        return result
    }

    /** Modifies a UVForecastData list to find and set elements whose time is the closest match to the passed UVProtectionTimeData. */
    private fun getUVForecastWithProtectionTimeBoundaries(): List<UVForecastData>?
    {
        val forecastData = uvForecast?.toMutableList() ?: return null
        val protectionTimeData = uvProtection ?: return forecastData

        if (protectionTimeData.isProtectionNeeded.not()) { return forecastData }

        var fromTimeForecastIndex = 0
        while ((fromTimeForecastIndex < forecastData.size - 1) && ((forecastData[fromTimeForecastIndex].time).isBefore(protectionTimeData.fromTime)))
        {
            fromTimeForecastIndex++
        }
        fromTimeForecastIndex = if (fromTimeForecastIndex > 0) fromTimeForecastIndex - 1 else fromTimeForecastIndex

        val fromUvAtIndex = forecastData[fromTimeForecastIndex].uv
        val fromUvAtNext = forecastData[if (fromTimeForecastIndex < forecastData.size - 1) fromTimeForecastIndex + 1 else fromTimeForecastIndex].uv

        // If the next UV is closer to the protection UV then use that, increase the index
        if ((fromUvAtIndex - protectionTimeData.fromUV).absoluteValue > (fromUvAtNext - protectionTimeData.fromUV).absoluteValue)
        {
            fromTimeForecastIndex++
        }

        var toTimeForecastIndex = 0
        while ((toTimeForecastIndex < forecastData.size - 1) && (forecastData[toTimeForecastIndex].time).isBefore(protectionTimeData.toTime))
        {
            toTimeForecastIndex++
        }
        toTimeForecastIndex = if (toTimeForecastIndex > 0) toTimeForecastIndex - 1 else toTimeForecastIndex

        val toUvAtIndex = forecastData[toTimeForecastIndex].uv
        val toUvAtNext = forecastData[if (toTimeForecastIndex < forecastData.size - 1) toTimeForecastIndex + 1 else toTimeForecastIndex].uv

        // If the next UV is closer to the protection UV then use that, increase the index
        if ((toUvAtIndex - protectionTimeData.toUV).absoluteValue > (toUvAtNext - protectionTimeData.toUV).absoluteValue)
        {
            toTimeForecastIndex++
        }

        forecastData[fromTimeForecastIndex] = forecastData[fromTimeForecastIndex].copy(isProtectionTimeBoundary = true)
        forecastData[toTimeForecastIndex] = forecastData[toTimeForecastIndex].copy(isProtectionTimeBoundary = true)

        return forecastData
    }

    private fun networkRequestsComplete(deferredResult: Deferred<UVCombinedForecastData, ErrorStatus>)
    {
        uvData?.let()
        {
            it.cloudCover = cloudCover
            it.cityName = cityName
            deferredResult.resolve(UVCombinedForecastData(it, getUVForecastWithProtectionTimeBoundaries(), uvProtection))
        }
    }

    fun getUVData(locationData: UVLocationData, withCloudCover: Boolean, isFirstDailyRequest: Boolean, updateCityData: Boolean): Promise<UVCombinedForecastData, ErrorStatus>
    {
        val result = deferred<UVCombinedForecastData, ErrorStatus>()

        val networkRequestsToMake = calculateNumberOfRequests(withCloudCover, isFirstDailyRequest, updateCityData)
        val requestsMade = AtomicInteger(0)

        if (withCloudCover)
        {
            NetworkRepository.getCurrentCloudCover(locationData.latitude, locationData.longitude).success()
            { lCloudCover ->
                cloudCover = lCloudCover

                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete(result)
                }
            }.fail() // Failure of cloud cover data is non-critical
            {
                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete(result)
                }
            }
        }

        if (isFirstDailyRequest)
        {
            NetworkRepository.getUVForecast(locationData.latitude, locationData.longitude, locationData.altitude).success()
            { lUVForecast ->

                uvForecast = lUVForecast

                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete(result)
                }
            }.fail() // Failure of forecast data is also non-critical
            {
                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete(result)
                }
            }

            NetworkRepository.getUVProtectionTimes(locationData.latitude, locationData.longitude, locationData.altitude, Constants.UV_PROTECTION_TIME_DEFAULT_FROM_UV, Constants.UV_PROTECTION_TIME_DEFAULT_TO_UV).success()
            { lUVProtection ->
                uvProtection = lUVProtection
                scheduleProtectionTimeNotification(lUVProtection)
                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete(result)
                }
            }.fail() // Failure of protection data is also non-critical
            {
                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete(result)
                }
            }
        }

        if (updateCityData)
        {
            // Todo, if location has not changed then also dont need to make this call
            NetworkRepository.getGeoCodedCityName(locationData.latitude, locationData.longitude).success()
            { lCityName ->
                cityName = lCityName
                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete(result)
                }
            }.fail() // Failure of city name data is non-critical
            {
                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete(result)
                }
            }
        }
        // TODO() else needs to be handled outside this, use shared preferences to get last city name

        makeRealTimeUVRequest(locationData, networkRequestsToMake, result, requestsMade)

        return result.promise
    }

    private fun makeRealTimeUVRequest(
        locationData: UVLocationData,
        networkRequestsToMake: Int,
        deferredResult: Deferred<UVCombinedForecastData, ErrorStatus>,
        requestsMade: AtomicInteger
    )
    {
        NetworkRepository.getRealTimeUV(locationData.latitude, locationData.longitude, locationData.altitude).success()
        { luvData ->
            uvData = luvData

            if (requestsMade.incrementAndGet() == networkRequestsToMake)
            {
                networkRequestsComplete(deferredResult)
            }
        }.fail()
        { errorStatus ->
            if ((canRetryRequest) && (errorStatus != ErrorStatus.NetworkError))
            {
                canRetryRequest = false
                makeRealTimeUVRequest(locationData, networkRequestsToMake, deferredResult, requestsMade)
            }
            else
            {
                deferredResult.reject(errorStatus)
            }
        }
    }

    private fun scheduleProtectionTimeNotification(protectionTimeData: UVProtectionTimeData)
    {
        if (protectionTimeData.isProtectionNeeded.not()) { return }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.areNotificationsEnabled().not()) { return }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms().not()) { return }

        val timeNow = ZonedDateTime.now()

        var protectionTimeAlreadyStarted = false

        val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        if (defaultPreferences.getBoolean(Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_KEY, true).not()) { return }

        val protectionStartScheduleTime = when (DiskRepository.uvNotificationTimeType(defaultPreferences))
        {
            DiskRepository.NotificationTimeType.DayStart ->
            {
                if (timeNow.isAfter(protectionTimeData.fromTime))
                {
                    protectionTimeAlreadyStarted = true
                }
                timeNow
            }

            DiskRepository.NotificationTimeType.Custom ->
            {
                val uvNotificationCustomTime = DiskRepository.uvNotificationCustomTime(defaultPreferences) ?: timeNow
                if (timeNow.isAfter(uvNotificationCustomTime))
                {
                    timeNow
                }
                else
                {
                    uvNotificationCustomTime
                }
            }

            DiskRepository.NotificationTimeType.WhenNeeded ->
            {
                protectionTimeAlreadyStarted = true
                if (timeNow.isAfter(protectionTimeData.fromTime))
                {
                    timeNow
                }
                else
                {
                    protectionTimeData.fromTime
                }
            }
        }

        alarmManager.set(AlarmManager.RTC, protectionStartScheduleTime.toEpochMilli(), protectionTimeData.protectionStartPendingIntent(context, protectionTimeAlreadyStarted))

        val uvEndNotificationsEnabled = defaultPreferences.getBoolean(Constants.SharedPreferences.UV_PROTECTION_END_NOTIFICATION_KEY, true)
        if (uvEndNotificationsEnabled)
        {
            alarmManager.set(AlarmManager.RTC, protectionTimeData.toTime.toEpochMilli(), protectionTimeData.protectionEndPendingIntent(context))
        }
    }
}