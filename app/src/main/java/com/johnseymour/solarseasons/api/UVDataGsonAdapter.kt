package com.johnseymour.solarseasons.api

import com.google.gson.*
import com.johnseymour.solarseasons.SunInfo
import com.johnseymour.solarseasons.UVData
import java.lang.reflect.Type
import java.time.ZoneId

object UVDataGsonAdapter: JsonDeserializer<UVData>, JsonSerializer<UVData>
{
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): UVData?
    {
        context ?: return null

        json?.asJsonObject?.getAsJsonObject("result")?.let()
        {
            val uv = it.getAsJsonPrimitive("uv")?.asFloat ?: 0F
            val uvMax = it.getAsJsonPrimitive("uv_max")?.asFloat ?: 0F
            val ozone = it.getAsJsonPrimitive("ozone")?.asFloat ?: 0F

            //Using system zone here rather than a global constant because app includes a widget and user can change timezone
            val systemZone = ZoneId.systemDefault()
            val uvTime = it.get("uv_time")?.toZonedDateTime(systemZone)
            val uvMaxTime = it.get("uv_max_time")?.toZonedDateTime(systemZone)
            val ozoneTime = it.get("ozone_time")?.toZonedDateTime(systemZone)

            val safeExposure = it.getAsJsonObject("safe_exposure_time")?.let()
            { safeExposureObject ->
                getSafeExposureTimes(safeExposureObject)
            }

            val sunInfo = context.deserialize<SunInfo>(it.getAsJsonObject("sun_info"), SunInfo::class.java)

            return UVData(uv, uvTime, uvMax, uvMaxTime, ozone, ozoneTime, safeExposure, sunInfo)
        }

        return null
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

    /**
     * Serialisation method to convert the UVData object to a JSON format that is similar to that returned by the API,
     *  thus enabling the same deserialiser to be used for writing to disk.
     */
    override fun serialize(uvData: UVData?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement
    {
        val result = JsonObject()

        context ?: return result
        uvData ?: return result

        val uvJSON = JsonObject()

        uvJSON.addProperty("uv", uvData.uv)
        uvJSON.addProperty("uv_max", uvData.uvMax)
        uvJSON.addProperty("ozone", uvData.ozone)
        uvJSON.addProperty("uv_time", uvData.uvTime?.toString() ?: "")
        uvJSON.addProperty("uv_max_time", uvData.uvMaxTime?.toString() ?: "")
        uvJSON.addProperty("ozone_time", uvData.ozoneTime?.toString() ?: "")

        uvJSON.add("safe_exposure_time", context.serialize(uvData.safeExposure) ?: JsonObject())

        uvJSON.add("sun_info", context.serialize(uvData.sunInfo))

        result.add("result", uvJSON)

        return result
    }
}