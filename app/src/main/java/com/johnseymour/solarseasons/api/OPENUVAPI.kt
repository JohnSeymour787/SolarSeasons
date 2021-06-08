package com.johnseymour.solarseasons.api

import com.johnseymour.solarseasons.UVData
import retrofit2.Call
import retrofit2.http.GET

interface OPENUVAPI
{
    @GET("uv?lat=51.50636369327448&lng=-0.15934363365078322&alt=0")
    fun getRealTimeUV(): Call<UVData>
}