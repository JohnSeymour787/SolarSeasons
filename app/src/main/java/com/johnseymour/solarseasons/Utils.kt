package com.johnseymour.solarseasons

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import com.google.gson.JsonElement
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * If this JsonElement is a String representing a date, this method will convert it to a ZonedDateTime.
 * @param zoneID - ZoneID of the ZonedDateTime to convert to
 * @return - If this element is a string and can be parsed, returns a ZonedDateTime instance using the passed zoneID parameter.
 *           Otherwise, returns null
 */
fun JsonElement.toZonedDateTime(zoneID: ZoneId): ZonedDateTime?
{
    return if (this.isJsonNull)
    {
        null
    }
    else
    {
        try
        {
            // When parsing the API's time strings
            Instant.parse((this.asString ?: "")).atZone(zoneID)
        }
        catch (e: DateTimeParseException)
        {
            // When reading from ZonedDateTimes that were saved to disk
            ZonedDateTime.parse(this.asString ?: "")
        }
        catch (e: DateTimeException)
        {
            null
        }
    }
}

/**
 * Uses the device's time format setting to identify the appropriate 12hour or 24hour formatter to use. Then formats the
 *  time parameter.
 */
fun preferredTimeString(context: Context, time: ZonedDateTime?): String
{
    time ?: return ""
    return if (DateFormat.is24HourFormat(context)) { Constants.Formatters.HOUR_24.format(time) } else { Constants.Formatters.HOUR_12.format(time) }
}

/**
 * Returns a user-friendly time string for a time duration in minutes >= 0. If longer than 1 hour then calculates
 *  hours and adds this in the string. If longer than 1 day, returns R.string.exposure_time_max_hours string resource.
 */
fun exposureDurationString(resources: Resources, minutes: Int): String
{
    return when (minutes)
    {
        in 0 until Constants.MINUTES_PER_HOUR -> resources.getQuantityString(R.plurals.exposure_time_minutes, minutes, minutes)

        in Constants.MINUTES_PER_HOUR until Constants.MINUTES_PER_DAY ->
        {
            val hours = minutes / Constants.MINUTES_PER_HOUR
            val remainingMinutes = minutes % Constants.MINUTES_PER_HOUR

            resources.getString(R.string.exposure_time_hours_and_minutes, resources.getString(R.string.exposure_time_hours, hours), resources.getString(R.string.exposure_time_short_minutes, remainingMinutes))
        }

        else -> resources.getString(R.string.exposure_time_max_hours)
    }
}