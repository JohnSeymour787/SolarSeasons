package com.johnseymour.solarseasons.api

import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.models.UVProtectionTimeData
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

    @GET("forecast")
    fun getUVForecast(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("alt") altitude: Double
    ) : Call<Array<UVForecastData>>

    @GET("protection")
    fun getUVProtectionTimes(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("alt") altitude: Double,
        @Query("from") fromUv: Float,
        @Query("to") toUv: Float
    ) : Call<UVProtectionTimeData>
}