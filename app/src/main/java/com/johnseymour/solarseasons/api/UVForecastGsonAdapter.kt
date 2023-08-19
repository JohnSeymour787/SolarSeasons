package com.johnseymour.solarseasons.api

import com.google.gson.*
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.toZonedDateTime
import java.lang.reflect.Type
import java.time.ZoneId

object UVForecastGsonAdapter: JsonDeserializer<Array<UVForecastData>>, JsonSerializer<Array<UVForecastData>>
{
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Array<UVForecastData>
    {
        val result = mutableListOf<UVForecastData>()

        context ?: return result.toTypedArray()

        val systemZone = ZoneId.systemDefault()
        json?.asJsonObject?.getAsJsonArray("result")?.forEach()
        {
            val uv = it.asJsonObject?.get("uv")?.asFloat ?: return@forEach
            val uvTime = it.asJsonObject?.get("uv_time")?.toZonedDateTime(systemZone) ?: return@forEach
            val isProtectionBoundary = it.asJsonObject?.get("protection_boundary")?.asBoolean ?: false

            result.add(UVForecastData(uv, uvTime, isProtectionTimeBoundary = isProtectionBoundary))
        }

        return result.toTypedArray()
    }

    override fun serialize(forecastArray: Array<UVForecastData>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement
    {
        val result = JsonObject()

        forecastArray ?: return result

        val forecastJsonArray = JsonArray()

        forecastArray.forEach()
        {
            val arrayElement = JsonObject()
            arrayElement.addProperty("uv", it.uv)
            arrayElement.addProperty("uv_time", it.time.toString())
            arrayElement.addProperty("protection_boundary", it.isProtectionTimeBoundary)

            forecastJsonArray.add(arrayElement)
        }

        result.add("result", forecastJsonArray)

        return result
    }
}