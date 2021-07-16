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
        var uvDataDeferred: Deferred<UVData, String>? = null

        fun initiateWorker(context: Context, delayedStart: Boolean = false): LiveData<List<WorkInfo>>
        {
            val constraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()
            val uvDataRequest = OneTimeWorkRequestBuilder<UVDataWorker>().setConstraints(constraints).run()
            {
                if (delayedStart)
                {
                    setInitialDelay(10, TimeUnit.SECONDS)
                }
                build()
            }

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, uvDataRequest)

            return workManager.getWorkInfosForUniqueWorkLiveData(WORK_NAME)
        }
    }

    override fun doWork(): Result
    {
        startLocationService()

        return Result.success()
    }

    private val locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)

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

            //Check that the location is reasonably recent according to the locationRequest parameters
            locationClient.locationAvailability.addOnSuccessListener()
            {
                if (it.isLocationAvailable)
                {
                    locationClient.lastLocation.addOnSuccessListener()
                    { location: Location? ->
                        location?.let()
                        {

                            NetworkRepository.Semi_OLDgetRealTimeUV(it.latitude, it.longitude, it.altitude).success()
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
                    val data = NetworkRepository.getRealTimeUV(it.latitude, it.longitude, it.altitude)
                    data?.let()
                    {
                        uvDataDeferred?.resolve(data)
                    }
                }

                locationClient.removeLocationUpdates(this)
            }
        }
}