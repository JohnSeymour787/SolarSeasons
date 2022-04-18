package com.johnseymour.solarseasons.models

import java.time.ZonedDateTime

data class UVForecastData(val uv: Float, val time: ZonedDateTime)
{
    companion object
    {
        const val UV_FORECAST_LIST_KEY = "uv_forecast_list_key"
    }
}