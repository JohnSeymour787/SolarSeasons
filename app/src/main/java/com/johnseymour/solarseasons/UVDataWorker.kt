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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.*
import com.johnseymour.solarseasons.api.NetworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class UVDataWorker(context: Context, workerParameters: WorkerParameters): CoroutineWorker(context, workerParameters), Observer<UVData>
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

    private fun startLocationService()
    {
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
                                val intent = Intent(applicationContext, SmallUVDisplay::class.java).apply { action = UVData.UV_DATA_CHANGED; putExtra(UVData.UV_DATA_KEY, it) }
                                applicationContext.sendBroadcast(intent)
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
        startLocationService()
        return Result.success()
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