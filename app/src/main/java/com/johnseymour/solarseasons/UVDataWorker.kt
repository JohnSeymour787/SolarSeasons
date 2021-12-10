package com.johnseymour.solarseasons

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.work.*
import java.util.concurrent.TimeUnit

class UVDataWorker(applicationContext: Context, workerParameters: WorkerParameters): Worker(applicationContext, workerParameters)
{
    companion object
    {
        private const val WORK_NAME = "UV_DATA_WORK"

        private val workConstraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()
        private var uvDataRequest: OneTimeWorkRequest? = null
        private var previousSetting = false // Previous setting of initialDelay for the work or not

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

        fun initiateWorker(context: Context, delayedStart: Boolean = false, delayTime: Long = Constants.DEFAULT_REFRESH_TIME): LiveData<List<WorkInfo>>
        {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)

            // First time creating, to avoid making the same thing
            if (uvDataRequest == null)
            {
                uvDataRequest = createWorkRequest(delayedStart, delayTime)
            }
            // However, if the setting is different from last time, need to make a new request and update the remembered setting
            else if (delayedStart != previousSetting)
            {
                uvDataRequest = createWorkRequest(delayedStart, delayTime)

                previousSetting = delayedStart
            }

            // Start a unique work, but if one is already going, then keep that one (shouldn't need to occur because removed the work before)
            uvDataRequest?.let { workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, it) }

            return workManager.getWorkInfosForUniqueWorkLiveData(WORK_NAME)
        }

        fun cancelWorker(context: Context)
        {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }

    override fun doWork(): Result
    {
       val intent = Intent(applicationContext, LocationService::class.java)

        applicationContext.startForegroundService(intent)

        return Result.success()
    }
}