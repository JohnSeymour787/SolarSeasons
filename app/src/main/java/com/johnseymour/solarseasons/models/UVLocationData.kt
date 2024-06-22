package com.johnseymour.solarseasons.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UVLocationData(val latitude: Double, val longitude: Double, val altitude: Double): Parcelable
{
    companion object
    {
        const val UV_LOCATION_KEY = "uv_location_key"
    }

    override fun describeContents(): Int
    {
        //TODO("Not yet implemented")
        return 0
    }

    override fun writeToParcel(p0: Parcel?, p1: Int)
    {
        //TODO("Not yet implemented")
    }
}