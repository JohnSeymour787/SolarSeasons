package com.johnseymour.solarseasons.api

import com.google.android.gms.common.util.ArrayUtils
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
    private const val OPEN_UV_API_BASE_URL = "https://api.openuv.io/api/v1/"
    private const val WEATHER_API_BASE_URL = "https://api.weatherapi.com/v1/"

    private val openUVAPI by lazy()
    {
        val gsonBuilder = GsonBuilder().run()
        {
            registerTypeAdapter(SunInfo::class.java, SunInfoGsonAdapter)
            registerTypeAdapter(UVData::class.java, UVDataGsonAdapter)
            create()
        }

        val gsonConverter = GsonConverterFactory.create(gsonBuilder)

        // Add API key header to all outgoing requests
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

        val retrofit = Retrofit.Builder().baseUrl(OPEN_UV_API_BASE_URL)
            .addConverterFactory(gsonConverter)
            .client(httpClient)
            .build()

        retrofit.create(OPENUVAPI::class.java)
    }

    private val weatherAPI by lazy()
    {
        val gsonBuilder = GsonBuilder().run()
        {
            registerTypeAdapter(Double::class.javaObjectType, RealtimeCloudCoverDeserialiser)
            create()
        }

        val gsonConverter = GsonConverterFactory.create(gsonBuilder)

        // Add API key query parameter to all outgoing requests
        val httpClient = OkHttpClient.Builder().apply()
        {
            addInterceptor()
            {
                val newUrl = it.request().url().newBuilder()
                    .addQueryParameter("key", WEATHER_API_KEY)
                    .build()

                val request = it.request().newBuilder()
                    .url(newUrl)
                    .build()

                it.proceed(request)
            }.build()
        }.build()

        val retrofit = Retrofit.Builder().baseUrl(WEATHER_API_BASE_URL)
            .addConverterFactory(gsonConverter)
            .client(httpClient)
            .build()

        retrofit.create(WeatherAPI::class.java)
    }

    fun getRealTimeUV(latitude: Double, longitude: Double, altitude: Double): Promise<UVData, ErrorStatus>
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

                val errorText = response.errorBody()?.string() ?: run()
                {
                    result.reject(ErrorStatus.GeneralError)
                    return
                }

                when
                {
                    errorText.contains("Daily API quota exceeded") -> result.reject(ErrorStatus.APIQuotaExceeded)

                    errorText.contains("API Key not found") -> result.reject(ErrorStatus.APIKeyInvalid)

                    else -> result.reject(ErrorStatus.GeneralError)
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
                    result.reject(ErrorStatus.GeneralNoResponseError)
                }
            }
        })

        return result.promise
    }

    /**
     * Returns the current cloud cover at a location as a decimal percentage, with 1.0 meaning 100% cloudy
     *  and 0.0 meaning clear skies.
     */
    fun getCurrentCloudCover(latitude: Double, longitude: Double): Promise<Double, Unit>
    {
        val result = deferred<Double, Unit>()

        val queryString = "$latitude,$longitude"

        weatherAPI.getRealTimeCloudCover(queryString).enqueue(object: Callback<Double>
        {
            override fun onResponse(call: Call<Double>, response: Response<Double>)
            {
                response.body()?.let()
                {
                    result.resolve(it)
                }
            }

            override fun onFailure(call: Call<Double>, t: Throwable) { }
        })

        return result.promise
    }
}