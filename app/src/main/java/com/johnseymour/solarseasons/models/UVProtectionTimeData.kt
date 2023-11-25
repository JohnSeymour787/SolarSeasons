package com.johnseymour.solarseasons.models

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.johnseymour.solarseasons.ProtectionTimeAlarmReceiver
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.preferredTimeString
import java.time.ZonedDateTime

data class UVProtectionTimeData(val fromTime: ZonedDateTime, val fromUV: Float, val toTime: ZonedDateTime, val toUV: Float)
{
    // API returns 0 values if UV never reaches requested from/to times. Protection not needed after end time (toTime)
    val isProtectionNeeded: Boolean
        get() = fromUV != 0F && toUV != 0F && ZonedDateTime.now().isBefore(toTime)

    fun protectionStartPendingIntent(context: Context, protectionTimeStartedMessage: Boolean): PendingIntent
    {
        val intent = Intent(context, ProtectionTimeAlarmReceiver::class.java).apply()
        {
            putExtra(PROTECTION_NOTIFICATION_ID_KEY, PROTECTION_START_REQUEST_CODE)
            putExtra(PROTECTION_NOTIFICATION_TITLE_KEY, context.getString(R.string.uv_protection_notification_start_protection_title))
            if (protectionTimeStartedMessage)
            {
                putExtra(PROTECTION_NOTIFICATION_BODY_KEY, context.getString(R.string.uv_protection_notification_start_protection_body_immediate, fromUV, preferredTimeString(context, toTime)))
            }
            else
            {
                putExtra(PROTECTION_NOTIFICATION_BODY_KEY, context.getString(R.string.uv_protection_notification_start_protection_body_scheduled, preferredTimeString(context, fromTime), preferredTimeString(context, toTime), fromUV))
            }
        }

        val persistNotificationIntent = (intent.clone() as Intent).apply { putExtra(PROTECTION_NOTIFICATION_PERSIST_NOTIFICATION_TIMEOUT_KEY, toTime) }
        intent.putExtra(PROTECTION_NOTIFICATION_PERSIST_NOTIFICATION_KEY, persistNotificationIntent)

        return PendingIntent.getBroadcast(context, PROTECTION_START_REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun protectionEndPendingIntent(context: Context): PendingIntent
    {
        val intent = Intent(context, ProtectionTimeAlarmReceiver::class.java).apply()
        {
            putExtra(PROTECTION_NOTIFICATION_ID_KEY, PROTECTION_END_REQUEST_CODE)
            putExtra(PROTECTION_NOTIFICATION_TITLE_KEY, context.getString(R.string.uv_protection_notification_end_protection_title))
            putExtra(PROTECTION_NOTIFICATION_BODY_KEY, context.getString(R.string.uv_protection_notification_end_protection_body, toUV))
        }
        return PendingIntent.getBroadcast(context, PROTECTION_END_REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object
    {
        private const val PROTECTION_START_REQUEST_CODE = 100
        private const val PROTECTION_END_REQUEST_CODE = 101
        const val PROTECTION_NOTIFICATION_ID_KEY = "protection_notification_id_key"
        const val PROTECTION_NOTIFICATION_TITLE_KEY = "protection_notification_title_key"
        const val PROTECTION_NOTIFICATION_BODY_KEY = "protection_notification_body_key"
        const val PROTECTION_NOTIFICATION_PERSIST_NOTIFICATION_KEY = "protection_notification_persist_notification_key"
        const val PROTECTION_NOTIFICATION_PERSIST_NOTIFICATION_TIMEOUT_KEY = "protection_notification_persist_notification_timeout_key"
    }
}