package com.johnseymour.solarseasons.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
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
            solarNoon = (it.getAsJsonPrimitive("solarNoon").asString ?: "").toZonedDateTime(systemZone)
            nadir = (it.getAsJsonPrimitive("nadir").asString ?: "").toZonedDateTime(systemZone)
            sunrise = (it.getAsJsonPrimitive("sunrise").asString ?: "").toZonedDateTime(systemZone)
            sunset = (it.getAsJsonPrimitive("sunset").asString ?: "").toZonedDateTime(systemZone)
            sunriseEnd = (it.getAsJsonPrimitive("sunriseEnd").asString ?: "").toZonedDateTime(systemZone)
            sunsetStart = (it.getAsJsonPrimitive("sunsetStart").asString ?: "").toZonedDateTime(systemZone)
            dawn = (it.getAsJsonPrimitive("dawn").asString ?: "").toZonedDateTime(systemZone)
            dusk = (it.getAsJsonPrimitive("dusk").asString ?: "").toZonedDateTime(systemZone)
            nauticalDawn = (it.getAsJsonPrimitive("nauticalDawn").asString ?: "").toZonedDateTime(systemZone)
            nauticalDusk = (it.getAsJsonPrimitive("nauticalDusk").asString ?: "").toZonedDateTime(systemZone)
            nightEnd = (it.getAsJsonPrimitive("nightEnd").asString ?: "").toZonedDateTime(systemZone)
            night = (it.getAsJsonPrimitive("night").asString ?: "").toZonedDateTime(systemZone)
            goldenHourEnd = (it.getAsJsonPrimitive("goldenHourEnd").asString ?: "").toZonedDateTime(systemZone)
            goldenHour = (it.getAsJsonPrimitive("goldenHour").asString ?: "").toZonedDateTime(systemZone)
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
fun String.toZonedDateTime(zoneID: ZoneId): ZonedDateTime?
{
    return try
    {
        Instant.parse(this).atZone(zoneID)
    }
    catch (e: DateTimeParseException)
    {
        null
    }
}