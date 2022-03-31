package com.johnseymour.solarseasons.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherAPI
{
    @GET("current.json")
    fun getRealTimeCloudCover(
        @Query("q") query: String
    ): Call<Double>
}