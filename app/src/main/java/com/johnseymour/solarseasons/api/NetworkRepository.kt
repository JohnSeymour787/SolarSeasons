package com.johnseymour.solarseasons.api

import com.google.gson.GsonBuilder
import com.johnseymour.solarseasons.SunInfo
import com.johnseymour.solarseasons.UVData
import com.johnseymour.solarseasons.ErrorStatus
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.Promise
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object NetworkRepository
{
    private const val API_BASE_URL = "https://api.openuv.io/api/v1/"

    private val openUVAPI by lazy()
    {
        val gsonBuilder = GsonBuilder().run()
        {
            registerTypeAdapter(SunInfo::class.java, SunInfoGsonAdapter)
            registerTypeAdapter(UVData::class.java, UVDataGsonAdapter)
            create()
        }

        val gsonConverter = GsonConverterFactory.create(gsonBuilder)

        //Add API key header to all outgoing requests
        val httpClient = OkHttpClient.Builder().apply()
        {
            addInterceptor()
            {
                val request = it.request().newBuilder().apply()
                {
                    header("x-access-token", OPENUV_API_KEY)
                    method(it.request().method(), it.request().body())
                }.build()
                it.proceed(request)
            }.build()
        }.build()

        val retrofit = Retrofit.Builder().baseUrl(API_BASE_URL)
            .addConverterFactory(gsonConverter)
            .client(httpClient)
            .build()

        retrofit.create(OPENUVAPI::class.java)
    }

    fun Semi_OLDgetRealTimeUV(latitude: Double, longitude: Double, altitude: Double): Promise<UVData, ErrorStatus>
    {
        val result = deferred<UVData, ErrorStatus>()

        openUVAPI.getRealTimeUV(latitude, longitude, altitude).enqueue(object: Callback<UVData>
        {
            override fun onResponse(call: Call<UVData>, response: Response<UVData>)
            {
                response.body()?.let()
                {
                    result.resolve(it)
                    return
                }

                if (response.errorBody()?.string()?.contains("Daily API quota exceeded") == true)
                {
                    result.reject(ErrorStatus.APIQuotaExceeded)
                }
                else
                {
                    result.reject(ErrorStatus.GeneralError)
                }
            }

            override fun onFailure(call: Call<UVData>, t: Throwable)
            {
                if (t is IOException) // If network error
                {
                    result.reject(ErrorStatus.NetworkError)
                }
                else
                {
                    result.reject(ErrorStatus.GeneralError)
                }
            }
        })

        return result.promise
    }
}