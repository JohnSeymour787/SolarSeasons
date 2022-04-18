package com.johnseymour.solarseasons.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
data class UVForecastData(val uv: Float, val time: ZonedDateTime): Parcelable
{
    companion object
    {
        const val UV_FORECAST_LIST_KEY = "uv_forecast_list_key"
    }
}