package com.johnseymour.solarseasons.api

import android.util.Log
import com.google.gson.GsonBuilder
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.models.SunInfo
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.ErrorStatus
import com.johnseymour.solarseasons.asNegative
import com.johnseymour.solarseasons.asPositive
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.models.UVProtectionTimeData
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.reject
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

object NetworkRepository
{
    private const val OPEN_UV_API_BASE_URL = "https://api.openuv.io/api/v1/"
    private const val WEATHER_API_BASE_URL = "https://api.weatherapi.com/v1/"
    private const val REVERSE_GEOCODER_API_BASE_URL = "https://api.bigdatacloud.net/data/"
    private const val UV_FORECAST_ROUND_TO_MINUTE = 10L

    private val openUVAPI by lazy()
    {
        val gsonBuilder = GsonBuilder().run()
        {
            registerTypeAdapter(SunInfo::class.java, SunInfoGsonAdapter)
            registerTypeAdapter(UVData::class.java, UVDataGsonAdapter)
            registerTypeAdapter(Array<UVForecastData>::class.java, UVForecastGsonAdapter)
            registerTypeAdapter(UVProtectionTimeData::class.java, UVProtectionDataGsonAdapter)
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

    private val reverseGeoCoderAPI by lazy()
    {
        val gsonBuilder = GsonBuilder().run()
        {
            registerTypeAdapter(String::class.javaObjectType, ReverseGeoCoderDeserialiser)
            create()
        }

        val gsonConverter = GsonConverterFactory.create(gsonBuilder)

        val retrofit = Retrofit.Builder().baseUrl(REVERSE_GEOCODER_API_BASE_URL)
            .addConverterFactory(gsonConverter)
            .build()

        retrofit.create(ReverseGeoCodingAPI::class.java)
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

    private fun handleOpenUVAPIResponseError(errorText: String): ErrorStatus
    {
        return when
        {
            errorText.contains("Daily API quota exceeded") -> ErrorStatus.APIQuotaExceeded

            errorText.contains("API Key not found") -> ErrorStatus.APIKeyInvalid

            else ->
            {
                Log.d("Network", "NetworkRepository - Response error: $errorText")

                ErrorStatus.GeneralError
            }
        }
    }

    fun getRealTimeUV(latitude: Double, longitude: Double, altitude: Double): Promise<UVData, ErrorStatus>
    {
        val result = deferred<UVData, ErrorStatus>()

        if (Constants.TEST_MODE_NO_API)
        {
            result.resolve(UVData.generateTestData())
            return result.promise
        }

        openUVAPI.getRealTimeUV(latitude, longitude, validateAltitude(altitude)).enqueue(object: Callback<UVData>
        {
            override fun onResponse(call: Call<UVData>, response: Response<UVData>)
            {
                response.body()?.let()
                {
                    result.resolve(it)
                    return
                }

                response.errorBody()?.string()?.let()
                {
                    result.reject(handleOpenUVAPIResponseError(it))
                    return
                }

                result.reject(ErrorStatus.GeneralError)
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

            override fun onFailure(call: Call<Double>, t: Throwable) { result.reject() }
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
                    // Forecast times from the API are not always on the hour, or a nice looking time
                    // If so, round them each to the nearest UV_FORECAST_ROUND_TO_MINUTE multiple
                    it.firstOrNull()?.time?.minute?.let()
                    { forecastMinute ->
                        val differenceToNearestMins = ((forecastMinute / UV_FORECAST_ROUND_TO_MINUTE.toFloat()).roundToInt() * UV_FORECAST_ROUND_TO_MINUTE) - forecastMinute

                        if (differenceToNearestMins != 0L)
                        {
                            it.forEachIndexed()
                            { index, uvForecastData ->
                                it[index] = uvForecastData.copy(time = uvForecastData.time.plusMinutes(differenceToNearestMins))
                            }
                        }
                    }

                    result.resolve(it.toList())
                    return
                }

                response.errorBody()?.string()?.let()
                {
                    result.reject(handleOpenUVAPIResponseError(it))
                    return
                }

                result.reject(ErrorStatus.GeneralError)
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

    fun getUVProtectionTimes(latitude: Double, longitude: Double, altitude: Double, fromUV: Float, toUV: Float): Promise<UVProtectionTimeData, ErrorStatus>
    {
        val result = deferred<UVProtectionTimeData, ErrorStatus>()

        openUVAPI.getUVProtectionTimes(latitude, longitude, validateAltitude(altitude), fromUV, toUV).enqueue(object: Callback<UVProtectionTimeData>
        {
            override fun onResponse(call: Call<UVProtectionTimeData>, response: Response<UVProtectionTimeData>)
            {
                response.body()?.let()
                {
                    result.resolve(it)
                    return
                }

                response.errorBody()?.string()?.let()
                {
                    result.reject(handleOpenUVAPIResponseError(it))
                    return
                }

                result.reject(ErrorStatus.GeneralError)
            }

            override fun onFailure(call: Call<UVProtectionTimeData>, t: Throwable)
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
     * Returns the city name at a coordinate location
     */
    fun getGeoCodedCityName(latitude: Double, longitude: Double): Promise<String, Unit>
    {
        val result = deferred<String, Unit>()

        reverseGeoCoderAPI.getCityName(obfuscateCoordinate(latitude, Constants.MAXIMUM_LATITUDE_DEGREE), obfuscateCoordinate(longitude, Constants.MAXIMUM_LONGITUDE_DEGREE), Locale.getDefault().language).enqueue(object: Callback<String>
        {
            override fun onResponse(call: Call<String>, response: Response<String>)
            {
                response.body()?.let()
                {
                    result.resolve(it)
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) { result.reject() }
        })

        return result.promise
    }

    /**
     * Adds some inaccuracy to a location coordinate for some privacy before sending location to external APIs.
     * Validates the obfuscated coordinate to the range of >= -maxAbsoluteValue and <= +maxAbsoluteValue
     */
    private fun obfuscateCoordinate(coordinate: Double, maxAbsoluteValue: Double): Double
    {
        // Randomly add or subtract the obfuscation amount
        val result = if (Random.nextBoolean())
        {
            coordinate + Constants.LOCATION_OBFUSCATION_COORDINATE_DECIMAL
        }
        else
        {
            coordinate - Constants.LOCATION_OBFUSCATION_COORDINATE_DECIMAL
        }

        return when
        {
            result > maxAbsoluteValue.asPositive() -> maxAbsoluteValue.asPositive()

            result < maxAbsoluteValue.asNegative() -> maxAbsoluteValue.asNegative()

            else -> result
        }
    }
}