package com.johnseymour.solarseasons

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.*
import com.google.android.gms.location.*
import com.johnseymour.solarseasons.api.NetworkRepository
import kotlinx.coroutines.*
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class UVDataCoWorker(context: Context, workerParameters: WorkerParameters): CoroutineWorker(context, workerParameters), Observer<UVData>
{
    private val locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest by lazy()
    {
        LocationRequest.create().apply()
        {
            interval = 10//TimeUnit.MINUTES.toMillis(30)
//                    fastestInterval = TimeUnit.MINUTES.toMillis(15)
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            numUpdates = 1
        }
    }

    private var uvLiveData: LiveData<UVData>? = null

    private fun startLocationService(): Promise<UVData, String>
    {
        val promise = deferred<UVData, String>()
        //TODO(
        // /*
        //    When getting old location data, check that it is within 15 minutes
        // */

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            //locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            //Check that the location is reasonably recent according to the locationRequest parameters
            locationClient.locationAvailability.addOnSuccessListener()
            {
                if (it.isLocationAvailable)
                {
                    locationClient.lastLocation.addOnSuccessListener()
                    { location: Location? ->
                        location?.let()
                        {
                            val formatter = SimpleDateFormat("HH:mm:ss")

/*                            val uvData = runBlocking()
                            {
                                withContext(Dispatchers.Default)
                                {
                                    NetworkRepository.getRealTimeUV(it.latitude, it.longitude, it.altitude)
                                }
                            }*/

                            val data = runBlocking(Dispatchers.Default) { NetworkRepository.getRealTimeUV(it.latitude, it.longitude, it.altitude) }
                            data?.let()
                            {
                                promise.resolve(it)
                                //val intent = Intent(applicationContext, SmallUVDisplay::class.java).apply { action = UVData.UV_DATA_CHANGED; putExtra(UVData.UV_DATA_KEY, it) }
                               // applicationContext.sendBroadcast(intent)
                            }
                            //uvLiveData?.observeForever(this)

//                            liveData.removeObserver(this)
                            //TODO() ^Need to remove the observer somehow ? or just switch to an alarm task or smt
                            //formatter.timeZone = TimeZone.getDefault()
                            Log.d("location", "Using older data")
                            Log.d(
                                "location",
                                "Coords (lat, long): ${it.latitude} ${it.longitude} Altitude: ${it.altitude}"
                            )
                            Log.d(
                                "location",
                                "Accuracy: ${it.accuracy} Time: ${formatter.format(Date(it.time))}"
                            )
                            Log.d(
                                "location",
                                "Elapsed time: ${formatter.format(Date(it.elapsedRealtimeNanos))}"
                            )
                            Log.d("location", "Provider: ${it.provider}")
                        } ?: run {
                            locationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.getMainLooper()
                            )
                        }
                    }
                }
                else
                {
                    locationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }
        }

        return promise.promise
    }

    private val locationCallback: LocationCallback =
        //   {
        object : LocationCallback()
        {
            override fun onLocationResult(locationResult: LocationResult?)
            {
                locationResult ?: return

                super.onLocationResult(locationResult)
                val formatter = SimpleDateFormat("HH:mm:ss")
                formatter.timeZone = TimeZone.getDefault()
                locationResult.lastLocation.let()
                {
                    Log.d("location", "Requested new data")
                    Log.d(
                        "location",
                        "Coords (lat, long): ${it.latitude} ${it.longitude} Altitude: ${it.altitude}"
                    )
                    Log.d(
                        "location",
                        "Accuracy: ${it.accuracy} Time: ${formatter.format(Date(it.time))}"
                    )
                    Log.d("location", "Provider: ${it.provider}")

/*                    val cake = runBlocking()
                    {
                        withContext(Dispatchers.Default) { NetworkRepository.getRealTimeUV() }

                    }*/
                    val lel = 2

                    /*                  NetworkRepository.getRealTimeUV(it.latitude, it.longitude, it.altitude)
                                          .observe(this@MainActivity)
                                          { lUVData ->
                                              viewModel.uvData = lUVData
                                              testDisplay.text = "UV Rating: ${lUVData.uv}"
                                          }*/
                }

                locationClient.removeLocationUpdates(this)
            }
        }

    override suspend fun doWork(): Result
    {
        val cake = 3


    //    val intent = Intent(UVData.UV_DATA_CHANGED).apply { putExtra(UVData.UV_DATA_KEY, UVData(1F, 2F)) }
   //     applicationContext.sendBroadcast(intent)
/*        withContext(Dispatchers.IO)
        {
            withContext(Dispatchers.Default) { startLocationService() }

            Result.success()
        }*/

       // withContext(Dispatchers.Default) { startLocationService() }


        val data = coroutineScope { startLocationService().get() }
        val cae = 2
        return Result.success(workDataOf(UVData.UV_DATA_KEY to deferred<UVData, String>().promise))
    }

    override fun onChanged(data: UVData?)
    {
        data?.let()
        {
            uvLiveData?.removeObserver(this)
            uvLiveData = null

            val cake = uvLiveData?.hasObservers()
            val de = 2

            val intent = Intent(UVData.UV_DATA_CHANGED).apply { putExtra(UVData.UV_DATA_KEY, it) }
            applicationContext.sendBroadcast(intent)
        }
    }
}











class UVDataWorker(applicationContext: Context, workerParameters: WorkerParameters): Worker(applicationContext, workerParameters)
{
    companion object
    {
        private const val WORK_NAME = "UV_DATA_WORK"

        private val workConstraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()
        private var uvDataRequest: OneTimeWorkRequest? = null
        private var previousSetting = false // Previous setting of initialDelay for the work or not

        private var uvDataDeferred: Deferred<UVData, String>? = null
        val uvDataPromise: Promise<UVData, String>?
            get()
            {
                return uvDataDeferred?.promise
            }
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

        fun initiateWorker(context: Context, delayedStart: Boolean = false, delayTime: Long = 30): LiveData<List<WorkInfo>>
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
    }


    val test = UVData(uv=0.0399F, uvTime= ZonedDateTime.parse("2021-09-25T00:00:30.826+10:00[Australia/Sydney]"),
        uvMax=3.0005F, uvMaxTime= ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"),
        ozone=332.5F, ozoneTime= ZonedDateTime.parse("2021-09-25T16:04:07.137+10:00[Australia/Sydney]"),
        safeExposure= mapOf("st1" to 4180, "st2" to 5016, "st3" to 6688, "st4" to 8360, "st5" to 13376, "st6" to 25079),
        sunInfo=SunInfo(solarNoon= ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"), nadir=ZonedDateTime.parse("2021-09-25T09:53:36.274+10:00[Australia/Sydney]"),
            sunrise=ZonedDateTime.parse("2021-09-25T15:52:48.317+10:00[Australia/Sydney]"),
            sunset=ZonedDateTime.parse("2021-09-26T03:54:24.230+10:00[Australia/Sydney]"),
            sunriseEnd=ZonedDateTime.parse("2021-09-25T15:56:13.870+10:00[Australia/Sydney]"),
            sunsetStart=ZonedDateTime.parse("2021-09-26T03:50:58.677+10:00[Australia/Sydney]"),
            dawn=ZonedDateTime.parse("2021-09-25T15:19:32.279+10:00[Australia/Sydney]"),
            dusk=ZonedDateTime.parse("2021-09-26T04:27:40.269+10:00[Australia/Sydney]"),
            nauticalDawn=ZonedDateTime.parse("2021-09-25T14:40:20.870+10:00[Australia/Sydney]"),
            nauticalDusk=ZonedDateTime.parse("2021-09-26T05:06:51.678+10:00[Australia/Sydney]"),
            nightEnd=ZonedDateTime.parse("2021-09-25T13:59:43.486+10:00[Australia/Sydney]"),
            night=ZonedDateTime.parse("2021-09-26T05:47:29.061+10:00[Australia/Sydney]"),
            goldenHourEnd=ZonedDateTime.parse("2021-09-25T16:36:54.771+10:00[Australia/Sydney]"),
            goldenHour=ZonedDateTime.parse("2021-09-26T03:10:17.776+10:00[Australia/Sydney]"),
            azimuth=-1.48815118586359, altitude=0.04749226792696052))

    override fun doWork(): Result
    {
        startLocationService()

        return Result.success()
    }

    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)

    private val locationRequest by lazy()
    {
        LocationRequest.create().apply()
        {
            interval = 10//TimeUnit.MINUTES.toMillis(30)
//                    fastestInterval = TimeUnit.MINUTES.toMillis(15)
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            numUpdates = 1
        }
    }


    private fun startLocationService()
    {
        //TODO(
        // /*
        //    When getting old location data, check that it is within 15 minutes
        // */

        uvDataDeferred = deferred()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            //locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            // Check that the location is reasonably recent according to the locationRequest parameters
            locationClient.locationAvailability.addOnSuccessListener()
            {
                if (it.isLocationAvailable)
                {
                    locationClient.lastLocation.addOnSuccessListener()
                    { location: Location? ->
                        location?.let()
                        {

          //                  uvDataDeferred?.resolve(test)

                            NetworkRepository.Semi_OLDgetRealTimeUV(it.latitude, it.longitude, it.altitude).success()
                    //        NetworkRepository.Semi_OLDgetRealTimeUV().success()
                            { data ->
                                uvDataDeferred?.resolve(data)
                            }
                        } ?: run { locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper()) }
                    }
                }
                else
                {
                    locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                }
            }
        }
    }

    private val locationCallback: LocationCallback =
        object : LocationCallback()
        {
            override fun onLocationResult(locationResult: LocationResult?)
            {
                locationResult ?: return
                super.onLocationResult(locationResult)

                locationResult.lastLocation.let()
                {
         //           uvDataDeferred?.resolve(test)

                    NetworkRepository.Semi_OLDgetRealTimeUV(it.latitude, it.longitude, it.altitude).success()
                    //NetworkRepository.Semi_OLDgetRealTimeUV().success()
                    { data ->
                        uvDataDeferred?.resolve(data)
                    }
                }

                locationClient.removeLocationUpdates(this)
            }
        }
}