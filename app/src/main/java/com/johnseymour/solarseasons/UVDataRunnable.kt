package com.johnseymour.solarseasons

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.johnseymour.solarseasons.api.NetworkRepository
import java.text.SimpleDateFormat
import java.util.*

class UVDataRunnable(val context: Context, val callback: (Context, UVData) -> Unit): Runnable, Observer<UVData>
{
    private lateinit var locationClient: FusedLocationProviderClient

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

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
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
                   //          val liveData = NetworkRepository.getRealTimeUV(it.latitude, it.longitude, it.altitude)
 //                           liveData.observeForever(this)
//                            liveData.removeObserver(this)
                            //TODO() ^Need to remove the observer somehow ? or just switch to an alarm task or smt
                            //formatter.timeZone = TimeZone.getDefault()
                            Log.d("location","Using older data")
                            Log.d("location", "Coords (lat, long): ${it.latitude} ${it.longitude} Altitude: ${it.altitude}")
                            Log.d("location","Accuracy: ${it.accuracy} Time: ${formatter.format(Date(it.time))}")
                            Log.d("location", "Elapsed time: ${formatter.format(Date(it.elapsedRealtimeNanos))}")
                            Log.d("location","Provider: ${it.provider}")
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
 //   {
        object: LocationCallback()
        {
            override fun onLocationResult(locationResult: LocationResult?)
            {
                locationResult ?: return

                super.onLocationResult(locationResult)
                val formatter = SimpleDateFormat("HH:mm:ss")
                formatter.timeZone = TimeZone.getDefault()
                locationResult.lastLocation.let()
                {
                    Log.d("location","Requested new data")
                    Log.d("location","Coords (lat, long): ${it.latitude} ${it.longitude} Altitude: ${it.altitude}")
                    Log.d("location","Accuracy: ${it.accuracy} Time: ${formatter.format(Date(it.time))}")
                    Log.d("location", "Provider: ${it.provider}")

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
  //  }

    override fun run()
    {
        locationClient = LocationServices.getFusedLocationProviderClient(context)
        startLocationService()
    }

    override fun onChanged(data: UVData?)
    {
        val cake = 2
        data?.let()
        {
            callback(context, data)
        }
    }
}