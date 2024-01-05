package com.johnseymour.solarseasons.api

import com.google.gson.*
import com.johnseymour.solarseasons.models.UVProtectionTimeData
import com.johnseymour.solarseasons.toZonedDateTime
import java.lang.reflect.Type
import java.time.ZoneId
import java.time.ZonedDateTime

object UVProtectionDataGsonAdapter: JsonDeserializer<UVProtectionTimeData>
{
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): UVProtectionTimeData?
    {
        context ?: return null

        json?.asJsonObject?.getAsJsonObject("result")?.let()
        {
            val systemZone = ZoneId.systemDefault()

            val fromTime = it.getAsJsonPrimitive("from_time")?.toZonedDateTime(systemZone) ?: ZonedDateTime.now(systemZone)
            val fromUV = it.getAsJsonPrimitive("from_uv")?.asFloat ?: 0F
            val toTime = it.getAsJsonPrimitive("to_time")?.toZonedDateTime(systemZone) ?: ZonedDateTime.now(systemZone)
            val toUV = it.getAsJsonPrimitive("to_uv")?.asFloat ?: 0F

            return UVProtectionTimeData(fromTime, fromUV, toTime, toUV)
        }

        return null
    }
}