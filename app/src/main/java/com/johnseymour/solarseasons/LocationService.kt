package com.johnseymour.solarseasons

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.johnseymour.solarseasons.api.NetworkRepository
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.lang.Exception
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class LocationService: Service(), OnSuccessListener<Location>, OnFailureListener
{
    companion object
    {
        private const val TEST_MODE = false
        private var counter = 0F

        private const val NOTIFICATION_CHANNEL_ID = "Solar.seasons.id"
        private const val NOTIFICATION_CHANNEL_NAME = "Solar.seasons.foreground_location_channel"

        private var locationCancellationSource: CancellationTokenSource? = null

        private var uvDataDeferred: Deferred<UVData, ErrorStatus>? = null
        val uvDataPromise: Promise<UVData, ErrorStatus>?
            get()
            {
                return uvDataDeferred?.promise
            }


        val test = UVData(
            uv = 0.0399F, uvTime = ZonedDateTime.parse("2021-09-25T00:00:30.826+10:00[Australia/Sydney]"),
            uvMax = 3.0005F, uvMaxTime = ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"),
            ozone = 332.5F, ozoneTime = ZonedDateTime.parse("2021-09-25T16:04:07.137+10:00[Australia/Sydney]"),
            safeExposure = mapOf("st1" to 4180, "st2" to 5016, "st3" to 6688, "st4" to 8360, "st5" to 13376, "st6" to 25079),
            sunInfo = SunInfo(
                solarNoon = ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"), nadir = ZonedDateTime.parse("2021-09-25T09:53:36.274+10:00[Australia/Sydney]"),
                sunrise = ZonedDateTime.parse("2021-09-25T15:52:48.317+10:00[Australia/Sydney]"),
                sunset = ZonedDateTime.parse("2021-09-26T03:54:24.230+10:00[Australia/Sydney]"),
                sunriseEnd = ZonedDateTime.parse("2021-09-25T15:56:13.870+10:00[Australia/Sydney]"),
                sunsetStart = ZonedDateTime.parse("2021-09-26T03:50:58.677+10:00[Australia/Sydney]"),
                dawn = ZonedDateTime.parse("2021-09-25T15:19:32.279+10:00[Australia/Sydney]"),
                dusk = ZonedDateTime.parse("2021-09-26T04:27:40.269+10:00[Australia/Sydney]"),
                nauticalDawn = ZonedDateTime.parse("2021-09-25T14:40:20.870+10:00[Australia/Sydney]"),
                nauticalDusk = ZonedDateTime.parse("2021-09-26T05:06:51.678+10:00[Australia/Sydney]"),
                nightEnd = ZonedDateTime.parse("2021-09-25T13:59:43.486+10:00[Australia/Sydney]"),
                night = ZonedDateTime.parse("2021-09-26T05:47:29.061+10:00[Australia/Sydney]"),
                goldenHourEnd = ZonedDateTime.parse("2021-09-25T16:36:54.771+10:00[Australia/Sydney]"),
                goldenHour = ZonedDateTime.parse("2021-09-26T03:10:17.776+10:00[Australia/Sydney]"),
                azimuth = -1.48815118586359, altitude = 0.04749226792696052
            )
        )
    }

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

    private var activeLocationRequestMade = false

    override fun onCreate()
    {
        super.onCreate()

        startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        uvDataDeferred = deferred()

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

    private val locationRequest by lazy()
    {
        LocationRequest.create().apply()
        {
            interval = TimeUnit.MINUTES.toMillis(Constants.DEFAULT_REFRESH_TIME)
            fastestInterval = TimeUnit.SECONDS.toMillis(30)
            // fastestInterval = TimeUnit.MINUTES.toMillis(15)
            expirationTime = TimeUnit.MINUTES.toMillis(Constants.DEFAULT_REFRESH_TIME*2)
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            numUpdates = 1
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
                    if (TEST_MODE)
                    {
                        counter++
                        uvDataDeferred?.resolve(test)
                    }
                    else
                    {
                        NetworkRepository.Semi_OLDgetRealTimeUV(it.latitude, it.longitude, it.altitude)
                            .success()
                            //NetworkRepository.Semi_OLDgetRealTimeUV().success()
                            { data ->
                                uvDataDeferred?.resolve(data)
                            }

                    }

                    stopSelf()
                }

                locationClient.removeLocationUpdates(this)
            }
        }

    override fun onSuccess(location: Location?)
    {
        location?.let()
        {
            if (TEST_MODE)
            {
                counter++
                uvDataDeferred?.resolve(test)
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

        } ?: run() // Can be null if location was turned off or device restarted
        {
            if (activeLocationRequestMade) // If locationClient.getCurrentLocation was already called, don't call again
            {
                finalLocationFailure()
                return
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                locationCancellationSource = CancellationTokenSource().apply()
                {
                    locationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, this.token)
                        .addOnSuccessListener(this@LocationService)
                        .addOnFailureListener(this@LocationService)

                    activeLocationRequestMade = true
                }
            }
        }
    }

    private fun finalLocationFailure()
    {
        activeLocationRequestMade = false
        uvDataDeferred?.reject(ErrorStatus.GeneralLocationError)
        stopSelf()
    }

    override fun onFailure(e: Exception)
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationCancellationSource = CancellationTokenSource().apply()
            {
                locationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, this.token)
                    .addOnSuccessListener(this@LocationService)
                    .addOnFailureListener() // Don't want to recursively retry
                    {
                        finalLocationFailure()
                    }

                activeLocationRequestMade = true
            }
        } // TODO reject promise with location permission error
    }

    override fun onDestroy()
    {
        super.onDestroy()

        locationCancellationSource?.cancel()

        if (uvDataDeferred?.promise?.isDone() == false)
        {
            uvDataDeferred?.reject(ErrorStatus.LocationServiceTerminated)
        }
    }
}