package com.johnseymour.solarseasons.api

import com.google.gson.*
import java.lang.reflect.Type

object ReverseGeoCoderDeserialiser: JsonDeserializer<String>
{
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): String?
    {
        return json?.asJsonObject?.getAsJsonPrimitive("city")?.asString?.ifEmpty { null }
    }
}