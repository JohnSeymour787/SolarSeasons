package com.johnseymour.solarseasons.api

import com.google.gson.*
import java.lang.reflect.Type

object RealtimeCloudCoverDeserialiser: JsonDeserializer<Double>
{
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Double?
    {
        json?.asJsonObject?.getAsJsonObject("current")?.let()
        {
            return (it.getAsJsonPrimitive("cloud")?.asInt ?: 100) / 100.0
        }

        return null
    }
}