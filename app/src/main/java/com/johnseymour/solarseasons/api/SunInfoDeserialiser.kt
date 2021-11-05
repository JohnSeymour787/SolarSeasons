package com.johnseymour.solarseasons.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.johnseymour.solarseasons.SunInfo
import java.lang.reflect.Type
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

object SunInfoDeserialiser: JsonDeserializer<SunInfo>
{
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): SunInfo
    {
        var solarNoon: ZonedDateTime? = null
        var nadir: ZonedDateTime? = null
        var sunrise: ZonedDateTime? = null
        var sunset: ZonedDateTime? = null
        var sunriseEnd: ZonedDateTime? = null
        var sunsetStart: ZonedDateTime? = null
        var dawn: ZonedDateTime? = null
        var dusk: ZonedDateTime? = null
        var nauticalDawn: ZonedDateTime? = null
        var nauticalDusk: ZonedDateTime? = null
        var nightEnd: ZonedDateTime? = null
        var night: ZonedDateTime? = null
        var goldenHourEnd: ZonedDateTime? = null
        var goldenHour: ZonedDateTime? = null
        var azimuth = 0.0
        var altitude = 0.0

        //Using system zone here rather than a global constant because app includes a widget and user can change timezone
        val systemZone = ZoneId.systemDefault()

        json?.asJsonObject?.getAsJsonObject("sun_times")?.let()
        {
            solarNoon = it.get("solarNoon")?.toZonedDateTime(systemZone)
            nadir = it.get("nadir")?.toZonedDateTime(systemZone)
            sunrise = it.get("sunrise")?.toZonedDateTime(systemZone)
            sunset = it.get("sunset")?.toZonedDateTime(systemZone)
            sunriseEnd = it.get("sunriseEnd")?.toZonedDateTime(systemZone)
            sunsetStart = it.get("sunsetStart")?.toZonedDateTime(systemZone)
            dawn = it.get("dawn")?.toZonedDateTime(systemZone)
            dusk = it.get("dusk")?.toZonedDateTime(systemZone)
            nauticalDawn = it.get("nauticalDawn")?.toZonedDateTime(systemZone)
            nauticalDusk = it.get("nauticalDusk")?.toZonedDateTime(systemZone)
            nightEnd = it.get("nightEnd")?.toZonedDateTime(systemZone)
            night = it.get("night")?.toZonedDateTime(systemZone)
            goldenHourEnd = it.get("goldenHourEnd")?.toZonedDateTime(systemZone)
            goldenHour = it.get("goldenHour")?.toZonedDateTime(systemZone)
        }

        json?.asJsonObject?.getAsJsonObject("sun_position")?.let()
        {
            azimuth = it.getAsJsonPrimitive("azimuth")?.asDouble ?: 0.0
            altitude = it.getAsJsonPrimitive("altitude")?.asDouble ?: 0.0
        }

        return SunInfo(solarNoon, nadir, sunrise, sunset, sunriseEnd, sunsetStart, dawn, dusk, nauticalDawn, nauticalDusk, nightEnd, night, goldenHourEnd, goldenHour, azimuth, altitude)
    }
}

//Also used in UVDataDeserialiser
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
            Instant.parse((this.asString ?: "")).atZone(zoneID)
        }
        catch (e: DateTimeParseException)
        {
            null
        }
    }
}