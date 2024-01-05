package com.johnseymour.solarseasons

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.johnseymour.solarseasons.models.UVProtectionTimeData
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ProtectionTimeAlarmReceiver : BroadcastReceiver()
{
    companion object
    {
        private const val NOTIFICATION_CHANNEL_ID = "Solar.seasons.protection.times.id"
        private const val PERSIST_NOTIFICATION_KEY = "persist_notification_key"
    }

    override fun onReceive(context: Context, intent: Intent)
    {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.areNotificationsEnabled().not()) { return }

        val name = context.getString(R.string.uv_protection_notification_channel_name)
        val descriptionText = context.getString(R.string.uv_protection_notification_channel_description)

        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT).apply()
        {
            description = descriptionText
        }

        notificationManager.createNotificationChannel(channel)

        val notificationId = intent.getIntExtra(UVProtectionTimeData.PROTECTION_NOTIFICATION_ID_KEY, 0)
        val title = intent.getStringExtra(UVProtectionTimeData.PROTECTION_NOTIFICATION_TITLE_KEY)
        val body = intent.getStringExtra(UVProtectionTimeData.PROTECTION_NOTIFICATION_BODY_KEY)
        val persistSameNotificationIntent = intent.getParcelableExtra<Intent>(UVProtectionTimeData.PROTECTION_NOTIFICATION_PERSIST_NOTIFICATION_KEY)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_notification_uv_protection)
            .setColor(context.getColor(R.color.uv_moderate))
            .setOngoing(false)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOnlyAlertOnce(true)

        persistSameNotificationIntent?.let()
        {
            it.putExtra(PERSIST_NOTIFICATION_KEY, true)

            builder.addAction(android.R.drawable.ic_lock_lock, context.getString(R.string.uv_protection_notification_persist_notification_message), PendingIntent.getBroadcast(context, notificationId, it, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE))
        }

        if (intent.getBooleanExtra(PERSIST_NOTIFICATION_KEY, false))
        {
            builder.priority = NotificationCompat.PRIORITY_MIN

            (intent.getSerializableExtra(UVProtectionTimeData.PROTECTION_NOTIFICATION_PERSIST_NOTIFICATION_TIMEOUT_KEY) as? ZonedDateTime)?.let()
            { timeOutTime ->
                val timeOutAfter = ZonedDateTime.now().until(timeOutTime, ChronoUnit.MILLIS)

                if (timeOutAfter > 0)
                {
                    builder.setOngoing(true)
                    builder.setTimeoutAfter(timeOutAfter)
                }
            }
        }

        notificationManager.notify(notificationId, builder.build())
    }
}