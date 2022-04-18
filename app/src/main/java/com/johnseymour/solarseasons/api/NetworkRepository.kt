package com.johnseymour.solarseasons.api

import android.util.Log
import com.google.gson.GsonBuilder
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.models.SunInfo
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.ErrorStatus
import com.johnseymour.solarseasons.models.UVForecastData
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
            registerTypeAdapter(Array<UVForecastData>::class.java, UVForecastGsonAdapter)
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

    /**
     * Constrains the altitude parameter to be within the bounds required by the UV API.
     *  (Location services sometimes return altitude values out of this range)
     */
    private fun validateAltitude(altitude: Double): Double
    {
        return when
        {
            altitude > Constants.MAXIMUM_EARTH_ALTITUDE -> Constants.MAXIMUM_EARTH_ALTITUDE
            altitude < Constants.MINIMUM_API_ACCEPTED_ALTITUDE -> Constants.MINIMUM_API_ACCEPTED_ALTITUDE
            else -> altitude
        }
    }

    fun getRealTimeUV(latitude: Double, longitude: Double, altitude: Double): Promise<UVData, ErrorStatus>
    {
        val result = deferred<UVData, ErrorStatus>()

        openUVAPI.getRealTimeUV(latitude, longitude, validateAltitude(altitude)).enqueue(object: Callback<UVData>
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

                Log.d("Network", "NetworkRepository - Response error: $errorText")
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

    fun getUVForecast(latitude: Double, longitude: Double, altitude: Double): Promise<List<UVForecastData>, ErrorStatus>
    {
        val result = deferred<List<UVForecastData>, ErrorStatus>()

        openUVAPI.getUVForecast(latitude, longitude, validateAltitude(altitude)).enqueue(object: Callback<Array<UVForecastData>>
        {
            override fun onResponse(call: Call<Array<UVForecastData>>, response: Response<Array<UVForecastData>>)
            {
                response.body()?.let()
                {
                    result.resolve(it.toList())
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

                Log.d("Network", "NetworkRepository - Response error: $errorText")
            }

            override fun onFailure(call: Call<Array<UVForecastData>>, t: Throwable)
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
}