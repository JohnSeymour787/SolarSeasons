package com.johnseymour.solarseasons

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.work.*
import com.google.common.util.concurrent.ListenableFuture
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.models.UVLocationData
import com.johnseymour.solarseasons.models.UVProtectionTimeData
import com.johnseymour.solarseasons.services.LocationService
import com.johnseymour.solarseasons.services.UVDataUseCase
import nl.komponents.kovenant.deferred
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class UVDataWorker(applicationContext: Context, workerParameters: WorkerParameters): ListenableWorker(applicationContext, workerParameters)
{
    companion object
    {
        private const val WORK_NAME = "UV_DATA_WORK"
        private const val INITIATE_BACKGROUND_WORK = "restart_background_work"
        private val MIN_PERIODIC_INTERVAL_MINUTES = TimeUnit.MILLISECONDS.toMinutes(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
        private val MAX_BACKOFF_MINUTES = TimeUnit.MILLISECONDS.toMinutes(WorkRequest.MAX_BACKOFF_MILLIS)

        private val workConstraints = Constraints.Builder().setRequiresBatteryNotLow(true).setRequiredNetworkType(NetworkType.CONNECTED).build()
        private var uvDataRequest: WorkRequest? = null
        private var previousDelayStartSetting = false

        private fun createWorkRequest(delayedStart: Boolean, delayTime: Long, firstDailyRequest: Boolean): OneTimeWorkRequest
        {
            return OneTimeWorkRequestBuilder<UVDataWorker>().run()
            {
                if (delayedStart)
                {
                    setInitialDelay(delayTime, TimeUnit.MINUTES)
                    setConstraints(workConstraints) // Only set the battery constraint when doing a delayed (background) start
                    setBackoffCriteria(BackoffPolicy.LINEAR, delayTime, TimeUnit.MINUTES)
                }
                else
                {
                    setInputData(workDataOf(INITIATE_BACKGROUND_WORK to true))
                }

                if (firstDailyRequest)
                {
                    setInputData(workDataOf(LocationService.FIRST_DAILY_REQUEST_KEY to true))
                }

                build()
            }
        }

        private fun createPeriodicRequest(timeInterval: Long, startDelay: Long): PeriodicWorkRequest
        {
            val lTimeInterval = if (timeInterval < MIN_PERIODIC_INTERVAL_MINUTES)
            {
                MIN_PERIODIC_INTERVAL_MINUTES
            }
            else
            {
                timeInterval
            }

            val lBackoffDelay = if (startDelay > MAX_BACKOFF_MINUTES)
            {
                MAX_BACKOFF_MINUTES
            }
            else
            {
                startDelay
            }

            return PeriodicWorkRequestBuilder<UVDataWorker>(lTimeInterval, TimeUnit.MINUTES).run()
            {
                setInitialDelay(startDelay, TimeUnit.MINUTES)
                setConstraints(workConstraints)
                setBackoffCriteria(BackoffPolicy.LINEAR, lBackoffDelay, TimeUnit.MINUTES)
                build()
            }
        }

        fun initiatePeriodicWorker(context: Context, startDelay: Long? = null, timeInterval: Long)
        {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(WORK_NAME)

            val delay = startDelay ?: timeInterval

            uvDataRequest = createPeriodicRequest(timeInterval, delay)

            // Start a unique work, but if one is already going, then replace that one (shouldn't need to occur because removed the work before)
            (uvDataRequest as? PeriodicWorkRequest)?.let { workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, it) }
        }

        fun initiateOneTimeWorker(context: Context, firstDailyRequest: Boolean, delayedStart: Boolean = false, delayTime: Long = Constants.DEFAULT_REFRESH_TIME)
        {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(WORK_NAME)

            // First time creating, to avoid making the same thing
            if (uvDataRequest == null)
            {
                uvDataRequest = createWorkRequest(delayedStart, delayTime, firstDailyRequest)
            }
            // However, if the setting is different from last time, need to make a new request and update the remembered setting
            else if (delayedStart != previousDelayStartSetting)
            {
                uvDataRequest = createWorkRequest(delayedStart, delayTime, firstDailyRequest)

                previousDelayStartSetting = delayedStart
            }

            // Start a unique work, but if one is already going, then replace that one (shouldn't need to occur because removed the work before)
            (uvDataRequest as? OneTimeWorkRequest)?.let { workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, it) }
        }

        fun cancelWorker(context: Context)
        {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
        }

        fun stopLocationService(applicationContext: Context)
        {
            applicationContext.stopService(LocationService.createServiceIntent(applicationContext))
        }
    }

    private var firstDailyRequest: Boolean = false

    override fun startWork(): ListenableFuture<Result>
    {
        firstDailyRequest = inputData.getBoolean(LocationService.FIRST_DAILY_REQUEST_KEY, false)

        val widgetIds = applicationContext.getWidgetIDs()

        val widgetIntent = Intent(applicationContext, SmallUVDisplay::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)

        if (inputData.getBoolean(INITIATE_BACKGROUND_WORK, false))
        {
            // For coming from immediate request (no delay), will result in background updates if the relevant permission is granted
            widgetIntent.putExtra(SmallUVDisplay.START_BACKGROUND_WORK_KEY, true)
        }
        else
        {
            // The widget is not responsible for starting periodic work except for special situations
            widgetIntent.putExtra(SmallUVDisplay.START_BACKGROUND_WORK_KEY, !SmallUVDisplay.usePeriodicWork)
        }

        val activityIntent = Intent(applicationContext, MainActivity::class.java)
            .setAction(UVData.UV_DATA_UPDATED)

        return CallbackToFutureAdapter.getFuture()
        { result ->

            val lastLocationData = DiskRepository.readLastLocation(PreferenceManager.getDefaultSharedPreferences(applicationContext))

            if (lastLocationData == null)
            {
                if (applicationContext.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    widgetIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, ErrorStatus.FineLocationPermissionError)
                    activityIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, ErrorStatus.FineLocationPermissionError)

                    applicationContext.sendBroadcast(widgetIntent)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(activityIntent)
                    result.set(Result.failure())
                }
                else
                {
                    // Need to initialise this here because the service is created asynchronously
                    LocationService.locationDataDeferred = deferred()

                    val locationServiceIntent = LocationService.createServiceIntent(applicationContext)
                    applicationContext.startForegroundService(locationServiceIntent)

                    LocationService.locationDataPromise?.success()
                    {
                        DiskRepository.writeLastLocation(it, PreferenceManager.getDefaultSharedPreferences(applicationContext))
                        currentUVForLocationData(it, updateCityData = true, result, widgetIntent, activityIntent)
                        LocationService.locationDataDeferred = null
                    }
                }
            }
            else
            {
                currentUVForLocationData(lastLocationData, updateCityData = false, result, widgetIntent, activityIntent)
            }
        }
    }

    private fun currentUVForLocationData(locationData: UVLocationData, updateCityData: Boolean, workResult: CallbackToFutureAdapter.Completer<Result>, widgetIntent: Intent, activityIntent: Intent)
    {
        UVDataUseCase(applicationContext).getUVData(locationData, firstDailyRequest, updateCityData).success()
        {
            val dataSharedPreferences = applicationContext.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)
            DiskRepository.writeLatestUV(it.uvData, dataSharedPreferences)

            it.forecast?.let()
            { forecastData ->
                val updatedForecast = it.protectionTime?.let { protectionData -> setUVForecastProtectionTimeBoundaries(forecastData.toMutableList(), protectionData) } ?: forecastData
                DiskRepository.writeLatestForecastList(updatedForecast, dataSharedPreferences)
                activityIntent.putParcelableArrayListExtra(UVForecastData.UV_FORECAST_LIST_KEY, ArrayList(updatedForecast))
            }

            it.protectionTime?.let()
            { protectionTimeData ->
                scheduleProtectionTimeNotification(protectionTimeData)
            }

            widgetIntent.putExtra(UVData.UV_DATA_KEY, it.uvData)
            activityIntent.putExtra(UVData.UV_DATA_KEY, it.uvData)

            applicationContext.sendBroadcast(widgetIntent)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(activityIntent)

            workResult.set(Result.success())
        }.fail()
        {
            widgetIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)
            activityIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)

            if (isFailureError(it))
            {
                // Only if not going to retry later show the error message
                applicationContext.sendBroadcast(widgetIntent)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(activityIntent)
                workResult.set(Result.failure())
            }
            else
            {
                workResult.set(Result.retry())
            }
        }
    }

    /** Modifies a UVForecastData list to find and set elements whose time is the closest match to the passed UVProtectionTimeData. */
    private fun setUVForecastProtectionTimeBoundaries(forecastData: MutableList<UVForecastData>, protectionTimeData: UVProtectionTimeData): List<UVForecastData>
    {
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

    private fun scheduleProtectionTimeNotification(protectionTimeData: UVProtectionTimeData)
    {
        if (protectionTimeData.isProtectionNeeded.not()) { return }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.areNotificationsEnabled().not()) { return }

        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms().not()) { return }

        val timeNow = ZonedDateTime.now()

        var protectionTimeAlreadyStarted = false

        val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

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

        alarmManager.set(AlarmManager.RTC, protectionStartScheduleTime.toEpochMilli(), protectionTimeData.protectionStartPendingIntent(applicationContext, protectionTimeAlreadyStarted))

        val uvEndNotificationsEnabled = defaultPreferences.getBoolean(Constants.SharedPreferences.UV_PROTECTION_END_NOTIFICATION_KEY, true)
        if (uvEndNotificationsEnabled)
        {
            alarmManager.set(AlarmManager.RTC, protectionTimeData.toTime.toEpochMilli(), protectionTimeData.protectionEndPendingIntent(applicationContext))
        }
    }

    private fun isFailureError(errorStatus: ErrorStatus): Boolean
    {
        return errorStatus == ErrorStatus.APIKeyInvalid
                || errorStatus == ErrorStatus.APIQuotaExceeded
                || errorStatus == ErrorStatus.LocationDisabledError
                || errorStatus == ErrorStatus.LocationAnyPermissionError
                || errorStatus == ErrorStatus.FineLocationPermissionError
    }
}