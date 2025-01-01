package com.johnseymour.solarseasons.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UVLocationData(val latitude: Double, val longitude: Double, val altitude: Double): Parcelable
{
    companion object
    {
        const val UV_LOCATION_KEY = "uv_location_key"
    }
}