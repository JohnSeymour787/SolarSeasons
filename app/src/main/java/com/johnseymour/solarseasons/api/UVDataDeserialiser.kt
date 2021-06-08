package com.johnseymour.solarseasons.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.johnseymour.solarseasons.UVData
import java.lang.reflect.Type

object UVDataDeserialiser: JsonDeserializer<UVData>
{
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): UVData
    {
        json?.asJsonObject?.getAsJsonObject("result")?.let()
        {
            val uv = it.getAsJsonPrimitive("uv")?.asFloat ?: 0F
            val maxUV = it.getAsJsonPrimitive("uv_max")?.asFloat ?: 0F

            return UVData(uv, maxUV)
        }

        return UVData(40F, 40F)
    }
}