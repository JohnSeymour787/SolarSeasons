package com.johnseymour.solarseasons.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.johnseymour.solarseasons.*
import com.johnseymour.solarseasons.models.UVLocationData
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise

/**
 * Abstract class to implement custom location implementations using different location APIs.
 *
 * @note As this is an Android service, and thus all inheriting classes are, child classes
 *        must also be registered as a service in the AndroidManifest.xml file or they will
 *        not be started.
 */
abstract class LocationService: Service()
{
    companion object
    {
        private const val NOTIFICATION_CHANNEL_ID = "Solar.seasons.id"
        private const val NOTIFICATION_CHANNEL_NAME = "Solar.seasons.foreground_location_channel"

        const val FIRST_DAILY_REQUEST_KEY = "first_daily_request_key"

        var locationDataDeferred: Deferred<UVLocationData, ErrorStatus>? = null
        val locationDataPromise: Promise<UVLocationData, ErrorStatus>?
            get()
            {
                return locationDataDeferred?.promise
            }

        /**
         * Factory method that checks application-level settings to determine which implementation of the
         *  LocationService class to use.
         *
         * @return Intent to the relevant LocationService class, ready to start as a service
         */
        fun createServiceIntent(applicationContext: Context): Intent
        {
            return when
            {
                Constants.TEST_MODE_NO_API -> Intent(applicationContext, LocationServiceTestNoAPI::class.java)

                Constants.ENABLE_MANUAL_LOCATION_FEATURE && PreferenceScreenFragment.useManualLocation ->
                {
                    Intent(applicationContext, LocationServiceManual::class.java)
                }

                Constants.USE_GOOGLE_PLAY_LOCATION -> Intent(applicationContext, LocationServiceGooglePlay::class.java)

                else -> Intent(applicationContext, LocationServiceNonGoogle::class.java)
            }
        }
    }

    private val notificationChannel by lazy()
    {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel =
            NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
        channel
    }

    private fun createNotification(): Notification
    {
        return NotificationCompat.Builder(applicationContext, notificationChannel.id)
            .setContentTitle(getString(R.string.service_notification_title))
            .setTicker(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_description))
            .setSmallIcon(R.drawable.app_notification_service)
            .setOngoing(true)
            .build()
    }

    override fun onCreate()
    {
        super.onCreate()

        startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    private var firstRequestOfDay = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        firstRequestOfDay = intent?.getBooleanExtra(FIRST_DAILY_REQUEST_KEY, false) ?: firstRequestOfDay

        return serviceMain()
    }

    /** Main method in which the service code will run
     *  @return - Must return one of the Service.START_* flags, such as START_STICKY
     */
    abstract fun serviceMain(): Int

    override fun onBind(intent: Intent): IBinder? = null

    fun locationSuccess(latitude: Double, longitude: Double, altitude: Double)
    {
        locationDataDeferred?.resolve(UVLocationData(latitude, longitude, altitude))
        stopSelf()
    }

    /**
     * Called when it is determined that the location cannot be determined anymore. Rejects the current
     *  locationDataDeferred promise and stops this service.
     *
     *  @param errorStatus - ErrorStatus used to reject the locationDataDeferred promise with.
     */
    fun finalLocationFailure(errorStatus: ErrorStatus)
    {
        locationDataDeferred?.reject(errorStatus)
        stopSelf()
    }

    override fun onDestroy()
    {
        super.onDestroy()

        if (locationDataDeferred?.promise?.isDone() == false)
        {
            locationDataDeferred?.reject(ErrorStatus.LocationServiceTerminated)
        }
    }
}