package com.johnseymour.solarseasons.api

import com.google.gson.*
import com.johnseymour.solarseasons.SunInfo
import java.lang.reflect.Type
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

object SunInfoGsonAdapter: JsonDeserializer<SunInfo>, JsonSerializer<SunInfo>
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

    /**
     * Serialisation method to convert the SunInfo object to a JSON format that is similar to that returned by the API,
     *  thus enabling the same deserialiser to be used for writing to disk.
     */
    override fun serialize(sunInfo: SunInfo?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement
    {
        val result = JsonObject()

        context ?: return result
        sunInfo ?: return result

        JsonObject().apply()
        {
            addProperty("azimuth", sunInfo.azimuth)
            addProperty("altitude", sunInfo.altitude)

            result.add("sun_position", this)
        }

        JsonObject().apply()
        {
            addProperty("solarNoon", sunInfo.solarNoon?.toString() ?: "")
            addProperty("nadir", sunInfo.nadir?.toString() ?: "")
            addProperty("sunrise", sunInfo.sunrise?.toString() ?: "")
            addProperty("sunset", sunInfo.sunset?.toString() ?: "")
            addProperty("sunriseEnd", sunInfo.sunriseEnd?.toString() ?: "")
            addProperty("sunsetStart", sunInfo.sunsetStart?.toString() ?: "")
            addProperty("dawn", sunInfo.dawn?.toString() ?: "")
            addProperty("dusk", sunInfo.dusk?.toString() ?: "")
            addProperty("nauticalDawn", sunInfo.nauticalDawn?.toString() ?: "")
            addProperty("nauticalDusk", sunInfo.nauticalDusk?.toString() ?: "")
            addProperty("nightEnd", sunInfo.nightEnd?.toString() ?: "")
            addProperty("night", sunInfo.night?.toString() ?: "")
            addProperty("goldenHourEnd", sunInfo.goldenHourEnd?.toString() ?: "")
            addProperty("goldenHour", sunInfo.goldenHour?.toString() ?: "")

            result.add("sun_times", this)
        }

        return result
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