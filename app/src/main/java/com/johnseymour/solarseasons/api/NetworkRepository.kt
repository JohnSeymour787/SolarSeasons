package com.johnseymour.solarseasons.api

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.GsonBuilder
import com.johnseymour.solarseasons.SunInfo
import com.johnseymour.solarseasons.UVData
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.Promise
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

    private var test = 10F

    fun getRealTimeUV(latitude: Double = 51.50636369327448, longitude: Double = -0.15934363365078322, altitude: Double = 0.0): UVData?
    {
        val uvDataLive = MutableLiveData<UVData>()


        //       uvDataLive.postValue(UVData(3F,4F))
/*        GlobalScope.launch {
            delay(2000)
        }*/

        test++

     //   return null

        val response: Response<UVData> = openUVAPI.getRealTimeUV(latitude, longitude, altitude).execute()

        return if (response.isSuccessful)
        {
            response.body()
        }
        else
        {
            Log.d("UVAPI", response.message())
            null
        }


/*        openUVAPI.getRealTimeUV(latitude, longitude, altitude).enqueue(object: Callback<UVData>
        {
            override fun onResponse(call: Call<UVData>, response: Response<UVData>)
            {
                response.body()?.let()
                {
                    uvDataLive.postValue(it)
                }
            }

            override fun onFailure(call: Call<UVData>, t: Throwable)
            {
                TODO("Not yet implemented")
            }
        })*/
    }

    fun OLDgetRealTimeUV(latitude: Double = 51.50636369327448, longitude: Double = -0.15934363365078322, altitude: Double = 0.0): LiveData<UVData>
    {
        val uvDataLive = MutableLiveData<UVData>()


        //       uvDataLive.postValue(UVData(3F,4F))
/*        GlobalScope.launch {
            delay(2000)
        }*/


        openUVAPI.getRealTimeUV(latitude, longitude, altitude).enqueue(object: Callback<UVData>
        {
            override fun onResponse(call: Call<UVData>, response: Response<UVData>)
            {
                response.body()?.let()
                {
                    uvDataLive.postValue(it)
                }
            }

            override fun onFailure(call: Call<UVData>, t: Throwable)
            {
                val cake = 2
                TODO("Not yet implemented")
            }
        })

        return uvDataLive
    }

    fun Semi_OLDgetRealTimeUV(latitude: Double = 51.50636369327448, longitude: Double = -0.15934363365078322, altitude: Double = 0.0): Promise<UVData, String>
    {
        val result = deferred<UVData, String>()

        openUVAPI.getRealTimeUV(latitude, longitude, altitude).enqueue(object: Callback<UVData>
        {
            override fun onResponse(call: Call<UVData>, response: Response<UVData>)
            {
                response.body()?.let()
                {
                    result.resolve(it)
                }
            }

            override fun onFailure(call: Call<UVData>, t: Throwable)
            {
                val cake = 2
                TODO("Not yet implemented")
                result.reject("")
            }
        })

        return result.promise
    }
}