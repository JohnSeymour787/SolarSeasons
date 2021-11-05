package com.johnseymour.solarseasons

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
data class UVData(
    val uv: Float,
    val uvTime: ZonedDateTime? = null,
    val uvMax: Float,
    val uvMaxTime: ZonedDateTime? = null,
    val ozone: Float,
    val ozoneTime: ZonedDateTime? = null,
    val safeExposure: Map<String, Int>? = null,
    val sunInfo: SunInfo): Parcelable
{
    val backgroundColorInt: Int
    get() =
        if (uv < UV_LOW)
        {
            R.color.uv_low
        }
        else if (uv >= UV_LOW && uv < UV_MODERATE)
        {
            R.color.uv_moderate
        }
        else if (uv >= UV_MODERATE && uv < UV_HIGH)
        {
            R.color.uv_high
        }
        else if (uv >= UV_HIGH && uv < UV_VERY_HIGH)
        {
            R.color.uv_very_high
        }
        else
        {
            R.color.uv_extreme
        }

    val textColorInt: Int
        get() =
            if (uv < UV_HIGH)
            {
                R.color.primary_text
            }
            else
            {
                R.color.white
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
            val endTimeEpoch = (sunInfo.sunset ?: sunInfo.dusk ?: sunInfo.sunsetStart)?.toEpochSecond() ?: return 1 // Default to 1 to avoid div 0

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

        private const val UV_LOW = 3.0
        private const val UV_MODERATE = 6.0
        private const val UV_HIGH = 8.0
        private const val UV_VERY_HIGH = 11.0
    }
}
