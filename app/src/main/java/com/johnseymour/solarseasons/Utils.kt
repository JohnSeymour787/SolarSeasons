package com.johnseymour.solarseasons

import android.content.Context
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

fun preferredTimeString(context: Context, time: ZonedDateTime?): String
{
    time ?: return ""
    return if (DateFormat.is24HourFormat(context)) { Constants.Formatters.HOUR_24.format(time) } else { Constants.Formatters.HOUR_12.format(time) }
}