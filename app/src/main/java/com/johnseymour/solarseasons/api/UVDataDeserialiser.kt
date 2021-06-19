package com.johnseymour.solarseasons.api

import com.google.gson.JsonDeserializer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.johnseymour.solarseasons.UVData
import java.lang.reflect.Type
import java.time.Instant
import java.time.ZoneId

object UVDataDeserialiser: JsonDeserializer<UVData>
{
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): UVData
    {
        json?.asJsonObject?.getAsJsonObject("result")?.let()
        {
            val uv = it.getAsJsonPrimitive("uv")?.asFloat ?: 0F
            val uvMax = it.getAsJsonPrimitive("uv_max")?.asFloat ?: 0F
            val ozone = it.getAsJsonPrimitive("ozone")?.asFloat ?: 0F

            val uvTimeString = it.getAsJsonPrimitive("uv_time")?.asString ?: ""
            val uvMaxTimeString = it.getAsJsonPrimitive("uv_max_time")?.asString ?: ""
            val ozoneTimeString = it.getAsJsonPrimitive("ozone_time")?.asString ?: ""

            val uvTime = Instant.parse(uvTimeString).atZone(ZoneId.systemDefault())
            val uvMaxTime = Instant.parse(uvMaxTimeString).atZone(ZoneId.systemDefault())
            val ozoneTime = Instant.parse(ozoneTimeString).atZone(ZoneId.systemDefault())

            val safeExposure = it.getAsJsonObject("safe_exposure_time")?.let()
            { safeExposureObject ->
                getSafeExposureTimes(safeExposureObject)
            }

            return UVData(uv, uvTime, uvMax, uvMaxTime, ozone, ozoneTime, safeExposure)
        }

        return UVData(40F)
    }

    /**
     * Converts a JSONObject to a Kotlin Map for the safe exposure times data.
     * @param exposuresJSON - A GSON JsonObject with string keys and all values as integers
     * @return - Kotlin Map<String, Int> with matching string keys and Kotlin Int type values. If
     *            the JSON value is null, it is not added to the map. Additionally, if all values
     *            in the JSON are null, a null Kotlin Map is returned.
     */
    private fun getSafeExposureTimes(exposuresJSON: JsonObject): Map<String, Int>?
    {
        val result = mutableMapOf<String, Int>()

        for (key in exposuresJSON.keySet())
        {
            //Can be JSON null
            val exposureValue = exposuresJSON.get(key)

            if (!exposureValue.isJsonNull)
            {
                exposureValue.asInt.let { result[key] = it }
            }
        }

        return if (result.isNotEmpty()) { result } else { null }
    }
}