package com.johnseymour.solarseasons.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ReverseGeoCodingAPI
{
    @GET("reverse-geocode-client")
    fun getCityName(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("localityLanguage") language: String = "default"
    ): Call<String>
}