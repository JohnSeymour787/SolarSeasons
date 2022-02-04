package com.johnseymour.solarseasons

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.johnseymour.solarseasons.api.NetworkRepository
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.util.function.Consumer

class LocationService: Service(), Consumer<Location?>, LocationListener
{
    companion object
    {
        private const val TEST_MODE = true
        private var counter = 0F

        private const val NOTIFICATION_CHANNEL_ID = "Solar.seasons.id"
        private const val NOTIFICATION_CHANNEL_NAME = "Solar.seasons.foreground_location_channel"

        private var uvDataDeferred: Deferred<UVData, ErrorStatus>? = null
        val uvDataPromise: Promise<UVData, ErrorStatus>?
            get()
            {
                return uvDataDeferred?.promise
            }
    }

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

    private val notificationChannel by lazy()
    {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
        channel
    }

    private fun createNotification(): Notification
    {
        return NotificationCompat.Builder(applicationContext, notificationChannel.id)
            .setContentTitle(getString(R.string.service_notification_title))
            .setTicker(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_description))
            .setSmallIcon(R.mipmap.ic_launcher_legacy)
            .setOngoing(true)
            .build()
    }

    override fun onCreate()
    {
        super.onCreate()

        startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        uvDataDeferred = deferred()

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
                locationManager.getCurrentLocation(provider, this, applicationContext.mainExecutor, this@LocationService)
            }
        }
        else
        {
            locationManager.requestSingleUpdate(provider, this@LocationService, applicationContext.mainLooper)
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
                LocationServiceGooglePlay.test.uv = counter
                uvDataDeferred?.resolve(LocationServiceGooglePlay.test)
                stopSelf()
            }
            else
            {
                NetworkRepository.Semi_OLDgetRealTimeUV(it.latitude, it.longitude, it.altitude).success()
                { uvData ->
                    uvDataDeferred?.resolve(uvData)
                    stopSelf()
                }.fail()
                { errorStatus ->
                    uvDataDeferred?.reject(errorStatus)
                    stopSelf()
                }
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

                        else -> finalLocationFailure(ErrorStatus.LocationAnyPermissionError)
                    }
                }

                LocationRequestStatus.SecondActiveRequestNetwork -> // If 2 network requests fail then it is likely due to a lack of any location data on the device (ie, location has only just been enabled)
                {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        finalLocationFailure(ErrorStatus.FineLocationPermissionError)
                    }
                    else
                    {
                        finalLocationFailure(ErrorStatus.GeneralLocationError)
                    }
                }

                LocationRequestStatus.SecondActiveRequestGPS -> finalLocationFailure(ErrorStatus.GeneralLocationError)
            }
        }
    }

    /**
     * LocationListener callback required by some LocationManager methods, rather than a Consumer callback.
     *  Same behaviour is used.
     */
    override fun onLocationChanged(location: Location) = accept(location)

    /**
     * Called when it is determined that the location cannot be determined anymore. Rejects the current
     *  uvDataDeferred promise and stops this service.
     *
     *  @param errorStatus - ErrorStatus used to reject the uvDataDeferred promise with.
     */
    private fun finalLocationFailure(errorStatus: ErrorStatus)
    {
        uvDataDeferred?.reject(errorStatus)
        stopSelf()
    }

    override fun onDestroy()
    {
        super.onDestroy()

        locationCancellationSignal?.cancel()

        if (uvDataDeferred?.promise?.isDone() == false)
        {
            uvDataDeferred?.reject(ErrorStatus.LocationServiceTerminated)
        }
    }
}