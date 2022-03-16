package com.johnseymour.solarseasons

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.work.*
import nl.komponents.kovenant.deferred
import java.util.concurrent.TimeUnit

class UVDataWorker(applicationContext: Context, workerParameters: WorkerParameters): Worker(applicationContext, workerParameters)
{
    companion object
    {
        private const val WORK_NAME = "UV_DATA_WORK"
        private val MIN_PERIODIC_INTERVAL_MINUTES = TimeUnit.MILLISECONDS.toMinutes(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
        private val MAX_BACKOFF_MINUTES = TimeUnit.MILLISECONDS.toMinutes(PeriodicWorkRequest.MAX_BACKOFF_MILLIS)

        private val workConstraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()
        private var uvDataRequest: WorkRequest? = null
        private var previousDelayStartSetting = false

        var ignoreWorkRequest: Boolean = false
            private set

        private fun createWorkRequest(delayedStart: Boolean, delayTime: Long): OneTimeWorkRequest
        {
            return OneTimeWorkRequestBuilder<UVDataWorker>().run()
            {
                ignoreWorkRequest = if (delayedStart)
                {
                    setInitialDelay(delayTime, TimeUnit.MINUTES)
                    setConstraints(workConstraints) // Only set the battery constraint when doing a delayed (background) start
                    false
                }
                else
                {
                    true // For making an immediate request, don't want the widget listener to trigger when the
                         //  request is done due to a limitation of Kovenant (cannot cancel the previous promise, uses the
                         //  same object). Otherwise 2 broadcasts are sent.
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
                ignoreWorkRequest = false

                setInitialDelay(startDelay, TimeUnit.MINUTES)
                setConstraints(workConstraints)
                setBackoffCriteria(BackoffPolicy.LINEAR, lBackoffDelay, TimeUnit.MINUTES)
                build()
            }
        }


        fun initiatePeriodicWorker(context: Context, startDelay: Long? = null, timeInterval: Long): LiveData<List<WorkInfo>>
        {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(WORK_NAME)

            val delay = startDelay ?: timeInterval

            uvDataRequest = createPeriodicRequest(timeInterval, delay)

            // Need to initialise this here before the work is enqueued as some clients will immediately subscribe to it
            LocationService.uvDataDeferred = deferred()

            // Start a unique work, but if one is already going, then replace that one (shouldn't need to occur because removed the work before)
            (uvDataRequest as? PeriodicWorkRequest)?.let { workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, it) }

            return workManager.getWorkInfosForUniqueWorkLiveData(WORK_NAME)
        }

        fun initiateOneTimeWorker(context: Context, delayedStart: Boolean = false, delayTime: Long = Constants.DEFAULT_REFRESH_TIME): LiveData<List<WorkInfo>>
        {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(WORK_NAME)

            // First time creating, to avoid making the same thing
            if (uvDataRequest == null)
            {
                uvDataRequest = createWorkRequest(delayedStart, delayTime)
            }
            // However, if the setting is different from last time, need to make a new request and update the remembered setting
            else if (delayedStart != previousDelayStartSetting)
            {
                uvDataRequest = createWorkRequest(delayedStart, delayTime)

                previousDelayStartSetting = delayedStart
            }

            // Need to initialise this here before the work is enqueued as some clients will immediately subscribe to it
            LocationService.uvDataDeferred = deferred()

            // Start a unique work, but if one is already going, then replace that one (shouldn't need to occur because removed the work before)
            (uvDataRequest as? OneTimeWorkRequest)?.let { workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, it) }

            return workManager.getWorkInfosForUniqueWorkLiveData(WORK_NAME)
        }

        fun locationServiceIntent(applicationContext: Context): Intent
        {
            return when
            {
                Constants.ENABLE_MANUAL_LOCATION_FEATURE && PreferenceScreenFragment.useManualLocation ->
                {
                    Intent(applicationContext, LocationServiceManual::class.java)
                }

                Constants.USE_GOOGLE_PLAY_LOCATION -> Intent(applicationContext, LocationServiceGooglePlay::class.java)

                else -> Intent(applicationContext, LocationServiceNonGoogle::class.java)
            }
        }

        fun cancelWorker(context: Context)
        {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
        }

        fun stopLocationService(applicationContext: Context)
        {
            applicationContext.stopService(locationServiceIntent(applicationContext))
        }
    }

    override fun doWork(): Result
    {
        applicationContext.startForegroundService(locationServiceIntent(applicationContext))

        return Result.success()
    }
}