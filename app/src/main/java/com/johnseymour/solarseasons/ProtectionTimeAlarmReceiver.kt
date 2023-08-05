package com.johnseymour.solarseasons

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.johnseymour.solarseasons.models.UVProtectionTimeData

class ProtectionTimeAlarmReceiver : BroadcastReceiver()
{
    companion object
    {
        private const val NOTIFICATION_CHANNEL_ID = "Solar.seasons.protection.times.id"
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

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_notification_service)
            .setOngoing(false)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOnlyAlertOnce(true)

        notificationManager.notify(notificationId, builder.build())
    }
}