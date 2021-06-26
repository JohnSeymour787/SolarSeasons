package com.johnseymour.solarseasons

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
data class UVData(val uv: Float,
                  val uvTime: ZonedDateTime? = null,
                  val uvMax: Float,
                  val uvMaxTime: ZonedDateTime? = null,
                  val ozone: Float,
                  val ozoneTime: ZonedDateTime? = null,
                  val safeExposure: Map<String, Int>? = null,
                  val sunInfo: SunInfo): Parcelable
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

    /**
     * Calculates the progress percentage of the uvTime of this object in the sun-up range from sunrise to sunset.
     * @return - An Int between 0 and 100, representing the completion of the sun through the sky during the day.
     *            100% represents the evening time and the sun has now set.
     *            0% represents a time before the sun has risen.
     */
    val sunProgressPercent: Int
    get()
        {
            val startTimeEpoch = (sunInfo.sunrise ?: sunInfo.dawn ?: sunInfo.sunriseEnd)?.toEpochSecond() ?: return 0
            val endTimeEpoch = (sunInfo.sunset ?: sunInfo.dusk ?: sunInfo.sunsetStart)?.toEpochSecond() ?: return 0

            //Calculation is a linear mapping from epoch time to a 0-100 range
            val numerator = ((uvTime ?: ZonedDateTime.now()).toEpochSecond()) - startTimeEpoch
            val denominator = endTimeEpoch - startTimeEpoch

            val percentage = (numerator.toDouble() / denominator) * 100

            return when
            {
                percentage > 100 -> 100
                percentage < 0 -> 0
                else -> percentage.toInt()
            }
        }

    companion object
    {
        const val UV_DATA_CHANGED = "com.johnseymour.solarseasons.UVDATA_CHANGED"
        const val UV_DATA_KEY = "uv_data_key"
    }
}
