package com.johnseymour.solarseasons

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
data class UVData(val uv: Float,
                 // val uvTime: ZonedDateTime,
                 @SerializedName("uv_max") val uvMax: Float): Parcelable
