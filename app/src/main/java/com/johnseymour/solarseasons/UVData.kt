package com.johnseymour.solarseasons

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
data class UVData(val uv: Float,
                  val uvTime: ZonedDateTime? = null,
                  val uvMax: Float? = null,
                  val uvMaxTime: ZonedDateTime? = null,
                  val ozone: Float? = null,
                  val ozoneTime: ZonedDateTime? = null,
                  val safeExposure: Map<String, Int>? = null): Parcelable
{
    val colorInt: Int
    get() =
        if (uv < 3.0)
        {
            R.color.uv_low
        }
        else if (uv >= 3.0 && uv < 6.0)
        {
            R.color.uv_moderate
        }
        else if (uv >= 6.0 && uv < 8.0)
        {
            R.color.uv_high
        }
        else if (uv >= 8.0 && uv < 11.0)
        {
            R.color.uv_very_high
        }
        else
        {
            R.color.uv_extreme
        }

    companion object
    {
        const val UV_DATA_CHANGED = "com.johnseymour.solarseasons.UVDATA_CHANGED"
        const val UV_DATA_KEY = "uv_data_key"
    }
}
