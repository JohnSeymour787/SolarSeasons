package com.johnseymour.solarseasons.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.app.ActivityCompat
import com.johnseymour.solarseasons.ErrorStatus
import java.util.function.Consumer

class LocationServiceNonGoogle: LocationService(), Consumer<Location?>, LocationListener
{
    private enum class LocationRequestStatus
    {
        FirstActiveRequest,
        SecondActiveRequestNetwork,
        SecondActiveRequestGPS
    }

    // applicationContext not ready until after super.onCreate()
    private val locationManager: LocationManager by lazy { applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager }

    private var locationRequestStatus = LocationRequestStatus.FirstActiveRequest

    private var locationCancellationSignal: CancellationSignal? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        if (!locationManager.isLocationEnabled)
        {
            uvDataDeferred?.reject(ErrorStatus.LocationDisabledError)
            stopSelf()

            return START_STICKY
        }

        // Widget is responsible for additionally checking the background permission
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let()
            {
               accept(it)
            } ?: run()
            {
                initiateLocationRequest(LocationManager.NETWORK_PROVIDER)

                locationRequestStatus = LocationRequestStatus.FirstActiveRequest
            }
        }
        else
        {
            uvDataDeferred?.reject(ErrorStatus.LocationAnyPermissionError)
            stopSelf()
        }

        return START_STICKY
    }

    /**
     * Initiates an active location request using the locationManager. LocationManager method called
     *  depends on build version. Assumes appropriate permission already granted for given provider parameter.
     *
     *  @param provider - Location provider source, must have values of either LocationManager.GPS_PROVIDER,
     *   LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, or LocationManager.FUSED_PROVIDER.
     */
    @SuppressLint("MissingPermission")
    private fun initiateLocationRequest(provider: String)
    {
        if (!locationManager.isProviderEnabled(provider)) { return }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            locationCancellationSignal = CancellationSignal().apply()
            {
                locationManager.getCurrentLocation(provider, this, applicationContext.mainExecutor, this@LocationServiceNonGoogle)
            }
        }
        else
        {
            @Suppress("DEPRECATION")
            locationManager.requestSingleUpdate(provider, this@LocationServiceNonGoogle, applicationContext.mainLooper)
        }
    }

    /**
     * Consumer#accept override callback for LocationManager location requests.
     *
     * @param location - Location retrieved from the LocationManager, can be null for various reasons.
     */
    override fun accept(location: Location?)
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
        } ?: run()
        {
            when (locationRequestStatus)
            {
                LocationRequestStatus.FirstActiveRequest ->
                {
                    when (PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ->
                        {
                            initiateLocationRequest(LocationManager.GPS_PROVIDER)

                            locationRequestStatus = LocationRequestStatus.SecondActiveRequestGPS
                        }

                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ->
                        {
                            initiateLocationRequest(LocationManager.NETWORK_PROVIDER)

                            locationRequestStatus = LocationRequestStatus.SecondActiveRequestNetwork
                        }

                        else -> super.finalLocationFailure(ErrorStatus.LocationAnyPermissionError)
                    }
                }

                LocationRequestStatus.SecondActiveRequestNetwork -> // If 2 network requests fail then it is likely due to a lack of any location data on the device (ie, location has only just been enabled)
                {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        super.finalLocationFailure(ErrorStatus.FineLocationPermissionError)
                    }
                    else
                    {
                        super.finalLocationFailure(ErrorStatus.GeneralLocationError)
                    }
                }

                LocationRequestStatus.SecondActiveRequestGPS -> super.finalLocationFailure(ErrorStatus.GeneralLocationError)
            }
        }
    }

    /**
     * LocationListener callback required by some LocationManager methods, rather than a Consumer callback.
     *  Same behaviour is used.
     */
    override fun onLocationChanged(location: Location) = accept(location)

    override fun onDestroy()
    {
        locationCancellationSignal?.cancel()
        super.onDestroy()
    }
}