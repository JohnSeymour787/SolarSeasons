package com.johnseymour.solarseasons.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.DiskRepository
import com.johnseymour.solarseasons.UVDataWorker
import com.johnseymour.solarseasons.hasWidgets

/**
 * Owns the background behaviour that used to live in the widget's companion object:
 *  - the screen-unlock broadcast receiver
 *  - the configured background refresh rate
 *  - scheduling/cancelling the periodic worker
 */
object WidgetBackgroundManager
{
    private var userPresentReceiver: BroadcastReceiver? = null
    private var screenUnlockRegistered = false
    private var backgroundRefreshRate = Constants.DEFAULT_REFRESH_TIME

    fun configure(context: Context)
    {
        val appContext = context.applicationContext
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)

        backgroundRefreshRate = prefs
            .getString(Constants.SharedPreferences.BACKGROUND_REFRESH_RATE_KEY, null)
            ?.toLongOrNull() ?: Constants.DEFAULT_REFRESH_TIME

        // Background updates are only needed while a widget is present.
        val enabled = appContext.hasWidgets() &&
                prefs.getBoolean(Constants.SharedPreferences.SUBSCRIBE_SCREEN_UNLOCK_KEY, false)

        setReceiveScreenUnlock(appContext, enabled, force = true)
    }

    fun setReceiveScreenUnlock(context: Context, enabled: Boolean, force: Boolean = false)
    {
        if (!force && enabled == screenUnlockRegistered) { return }

        val appContext = context.applicationContext

        if (enabled)
        {
            if (userPresentReceiver == null)
            {
                userPresentReceiver = object : BroadcastReceiver()
                {
                    override fun onReceive(context: Context?, intent: Intent?)
                    {
                        context ?: return

                        if (intent?.action != Intent.ACTION_USER_PRESENT) { return }

                        val uvData = try
                        {
                            DiskRepository.readLatestUV(
                                context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)
                            )
                        }
                        catch (_: Exception)
                        {
                            null
                        } ?: return

                        if (!uvData.sunInSky()) { return }

                        if (uvData.minutesSinceDataRetrieved > (backgroundRefreshRate + Constants.WORK_EXECUTION_SLACK_TIME))
                        {
                            scheduleWorker(context)
                        }
                    }
                }
            }

            userPresentReceiver?.let()
            {
                runCatching { appContext.unregisterReceiver(it) }
                runCatching { appContext.registerReceiver(it, IntentFilter(Intent.ACTION_USER_PRESENT)) }
            }
        }
        else
        {
            userPresentReceiver?.let()
            {
                runCatching { appContext.unregisterReceiver(it) }
            }
            userPresentReceiver = null
        }

        screenUnlockRegistered = enabled
    }

    fun setBackgroundRefreshRate(context: Context, minutes: Long)
    {
        backgroundRefreshRate = minutes
        scheduleWorker(context, ExistingPeriodicWorkPolicy.UPDATE)
    }

    fun scheduleWorker(context: Context, existingWorkPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP)
    {
        val appContext = context.applicationContext
        if (!appContext.hasWidgets()) { return }

        UVDataWorker.initiatePeriodicWorker(appContext, backgroundRefreshRate, existingWorkPolicy)
    }

    fun stopBackgroundWork(context: Context)
    {
        val appContext = context.applicationContext
        UVDataWorker.cancelWorker(appContext)
        UVDataWorker.stopLocationService(appContext)
    }
}
