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
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.*
import com.johnseymour.solarseasons.api.NetworkRepository
import com.johnseymour.solarseasons.models.UVCombinedForecastData
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import java.util.concurrent.atomic.AtomicInteger

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

        var uvDataDeferred: Deferred<UVCombinedForecastData, ErrorStatus>? = null
        val uvDataPromise: Promise<UVCombinedForecastData, ErrorStatus>?
            get()
            {
                return uvDataDeferred?.promise
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

    private var uvData: UVData? = null
    private var cloudCover: Double? = null
    private var uvForecast: List<UVForecastData>? = null

    private fun calculateNumberOfRequests(cloudCoverEnabled: Boolean): Int
    {
        var result = 1

        if (cloudCoverEnabled) { result++ }
        if (firstRequestOfDay) { result++ }

        return result
    }

    private fun networkRequestsComplete()
    {
        uvData?.let()
        {
            it.cloudCover = cloudCover
            uvDataDeferred?.resolve(UVCombinedForecastData(it, uvForecast))
        }

        stopSelf()
    }

    private var requestsMade = AtomicInteger(0)
    private var canRetryRequest = true // Single retry of realtime UV data allowed

    fun locationSuccess(latitude: Double, longitude: Double, altitude: Double)
    {
        val isCloudCoverEnabled = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .getBoolean(Constants.SharedPreferences.CLOUD_COVER_FACTOR_KEY, false)

        val networkRequestsToMake = calculateNumberOfRequests(isCloudCoverEnabled)

        if (isCloudCoverEnabled)
        {
            NetworkRepository.getCurrentCloudCover(latitude, longitude).success()
            { lCloudCover ->
                cloudCover = lCloudCover

                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete()
                }
            }.fail() // Failure of cloud cover data is non-critical
            {
                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete()
                }
            }
        }

        if (firstRequestOfDay)
        {
            NetworkRepository.getUVForecast(latitude, longitude, altitude).success()
            { lUVForecast ->

                uvForecast = lUVForecast

                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete()
                }
            }.fail() // Failure of forecast data is also non-critical
            {
                if (requestsMade.incrementAndGet() == networkRequestsToMake)
                {
                    networkRequestsComplete()
                }
            }
        }

        makeRealTimeUVRequest(latitude, longitude, altitude, networkRequestsToMake)
    }

    private fun makeRealTimeUVRequest(latitude: Double, longitude: Double, altitude: Double, networkRequestsToMake: Int)
    {
        NetworkRepository.getRealTimeUV(latitude, longitude, altitude).success()
        { luvData ->
            uvData = luvData

            if (requestsMade.incrementAndGet() == networkRequestsToMake)
            {
                networkRequestsComplete()
            }
        }.fail()
        { errorStatus ->
            if ((canRetryRequest) && (errorStatus != ErrorStatus.NetworkError))
            {
                canRetryRequest = false
                makeRealTimeUVRequest(latitude, longitude, altitude, networkRequestsToMake)
            }
            else
            {
                uvDataDeferred?.reject(errorStatus)
                stopSelf()
            }
        }
    }

    /**
     * Called when it is determined that the location cannot be determined anymore. Rejects the current
     *  uvDataDeferred promise and stops this service.
     *
     *  @param errorStatus - ErrorStatus used to reject the uvDataDeferred promise with.
     */
    fun finalLocationFailure(errorStatus: ErrorStatus)
    {
        uvDataDeferred?.reject(errorStatus)
        stopSelf()
    }

    override fun onDestroy()
    {
        super.onDestroy()

        if (uvDataDeferred?.promise?.isDone() == false)
        {
            uvDataDeferred?.reject(ErrorStatus.LocationServiceTerminated)
        }
    }
}