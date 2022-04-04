package com.johnseymour.solarseasons.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.johnseymour.solarseasons.ErrorStatus
import java.lang.Exception

class LocationServiceGooglePlay: LocationService(), OnSuccessListener<Location>, OnFailureListener
{
    private var activeLocationRequestMade = false
    private var highAccuracyLocationRequestMade = false

    private var locationCancellationSource: CancellationTokenSource? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        // Widget is responsible for additionally checking the background permission
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationClient.lastLocation
                .addOnSuccessListener(this)
                .addOnFailureListener(this)
        }

        return START_STICKY
    }

    // applicationContext not ready until after super.onCreate()
    private val locationClient: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(applicationContext) }

    override fun onSuccess(location: Location?)
    {
        location?.let()
        {
            if (TEST_MODE)
            {
                counter++
                test.uv = counter
                uvDataDeferred?.resolve(test)
                stopSelf()
            }
            else
            {
                super.locationSuccess(it.latitude, it.longitude, it.altitude)
            }

        } ?: run() // Can be null if location was turned off or device restarted
        {
            if (activeLocationRequestMade) // If locationClient.getCurrentLocation initially returns a null location, try again with PRIORITY_HIGH_ACCURACY
            {
                if (highAccuracyLocationRequestMade)
                {
                    super.finalLocationFailure(ErrorStatus.GeneralLocationError)
                    return
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    locationCancellationSource = CancellationTokenSource().apply()
                    {
                        locationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, this.token) // Forcing the GPS to turn on, if available
                            .addOnSuccessListener(this@LocationServiceGooglePlay)
                            .addOnFailureListener()
                            {
                                super.finalLocationFailure(ErrorStatus.GeneralLocationError)
                            }

                        activeLocationRequestMade = true
                        highAccuracyLocationRequestMade = true
                    }
                }
                else
                {
                    super.finalLocationFailure(ErrorStatus.FineLocationPermissionError)
                }
            }
            else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                locationCancellationSource = CancellationTokenSource().apply()
                {
                    locationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, this.token)
                        .addOnSuccessListener(this@LocationServiceGooglePlay)
                        .addOnFailureListener(this@LocationServiceGooglePlay)

                    activeLocationRequestMade = true
                }
            }
        }
    }

    override fun onFailure(e: Exception)
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationCancellationSource = CancellationTokenSource().apply()
            {
                locationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, this.token)
                    .addOnSuccessListener(this@LocationServiceGooglePlay)
                    .addOnFailureListener() // Don't want to recursively retry
                    {
                        super.finalLocationFailure(ErrorStatus.GeneralLocationError)
                    }

                activeLocationRequestMade = true
            }
        }
    }

    override fun onDestroy()
    {
        locationCancellationSource?.cancel()
        super.onDestroy()
    }
}