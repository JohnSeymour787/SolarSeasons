package com.johnseymour.solarseasons.api

import com.johnseymour.solarseasons.models.UVData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface OPENUVAPI
{
    @GET("uv")
    fun getRealTimeUV(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("alt") altitude: Double
    ): Call<UVData>
}