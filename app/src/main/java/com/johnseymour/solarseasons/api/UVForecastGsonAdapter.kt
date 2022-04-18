package com.johnseymour.solarseasons.api

import com.google.gson.*
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.toZonedDateTime
import java.lang.reflect.Type
import java.time.ZoneId

object UVForecastGsonAdapter: JsonDeserializer<Array<UVForecastData>>
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

            result.add(UVForecastData(uv, uvTime))
        }

        return result.toTypedArray()
    }
}