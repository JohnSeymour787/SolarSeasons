package com.johnseymour.solarseasons

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.google.common.util.concurrent.ListenableFuture
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.services.LocationService
import nl.komponents.kovenant.deferred
import java.util.concurrent.TimeUnit

class UVDataWorker(applicationContext: Context, workerParameters: WorkerParameters): ListenableWorker(applicationContext, workerParameters)
{
    companion object
    {
        private const val WORK_NAME = "UV_DATA_WORK"
        private const val INITIATE_BACKGROUND_WORK = "restart_background_work"
        private val MIN_PERIODIC_INTERVAL_MINUTES = TimeUnit.MILLISECONDS.toMinutes(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
        private val MAX_BACKOFF_MINUTES = TimeUnit.MILLISECONDS.toMinutes(PeriodicWorkRequest.MAX_BACKOFF_MILLIS)

        private val workConstraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()
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
            (uvDataRequest as? PeriodicWorkRequest)?.let { workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, it) }
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

    override fun startWork(): ListenableFuture<Result>
    {
        return CallbackToFutureAdapter.getFuture()
        { result ->
            // Need to initialise this here because the service is created asynchronously
            LocationService.uvDataDeferred = deferred()

            val locationServiceIntent = LocationService.createServiceIntent(applicationContext)
            if (inputData.getBoolean(LocationService.FIRST_DAILY_REQUEST_KEY, false))
            {
                locationServiceIntent.putExtra(LocationService.FIRST_DAILY_REQUEST_KEY, true)
            }

            applicationContext.startForegroundService(locationServiceIntent)

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

            LocationService.uvDataPromise?.success()
            {
                val dataSharedPreferences = applicationContext.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)
                DiskRepository.writeLatestUV(it.uvData, dataSharedPreferences)

                it.forecast?.let()
                { forecastData ->
                    DiskRepository.writeLatestForecastList(forecastData, dataSharedPreferences)
                    activityIntent.putParcelableArrayListExtra(UVForecastData.UV_FORECAST_LIST_KEY, ArrayList(forecastData))
                }

                widgetIntent.putExtra(UVData.UV_DATA_KEY, it.uvData)
                activityIntent.putExtra(UVData.UV_DATA_KEY, it.uvData)

                applicationContext.sendBroadcast(widgetIntent)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(activityIntent)

                result.set(Result.success())
            }?.fail()
            {
                widgetIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)
                activityIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)

                applicationContext.sendBroadcast(widgetIntent)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(activityIntent)

                result.set(Result.failure())
            }
        }
    }
}