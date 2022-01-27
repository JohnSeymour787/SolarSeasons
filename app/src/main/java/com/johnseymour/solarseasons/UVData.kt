package com.johnseymour.solarseasons

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

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

    val uvLevelTextInt: Int
        get() =
            if (uv < UV_LOW)
            {
                R.string.uv_level_low
            }
            else if (uv >= UV_LOW && uv < UV_MODERATE)
            {
                R.string.uv_level_moderate
            }
            else if (uv >= UV_MODERATE && uv < UV_HIGH)
            {
                R.string.uv_level_high
            }
            else if (uv >= UV_HIGH && uv < UV_VERY_HIGH)
            {
                R.string.uv_level_very_high
            }
            else
            {
                R.string.uv_level_extreme
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

    /**
     * Calculates the minutes remaining from the uvTime until the next sunrise. Ignores date rollover.
     */
    val minutesUntilSunrise: Long
        get()
        {
            val currentTime = (uvTime ?: ZonedDateTime.now()).toLocalTime() ?: return 0

            val startTime = (sunInfo.sunrise ?: sunInfo.dawn ?: sunInfo.sunriseEnd)?.toLocalTime() ?: return 0

            // If on a new day, calculate the time difference directly with the sunrise time
            return if (currentTime.hour <= 12)
            {
                ChronoUnit.MINUTES.between(currentTime, startTime).absoluteValue
            }
            // Otherwise, need to manually work out the minutes until midnight MAX and sum this with the minutes
            //  from midnight MIN to the sunrise time
            else
            {
                val timeUntilMidnight = ChronoUnit.MINUTES.between(currentTime, LocalTime.MAX).absoluteValue
                val midnightToStart = ChronoUnit.MINUTES.between(LocalTime.MIN, startTime).absoluteValue

                timeUntilMidnight + midnightToStart
            }
        }

    /**
     * Computes whether the uvTime is within the sunrise and sunset of this object's SunInfo field.
     *  Only checks the time values only, ignores dates.
     *
     * @return - true if the uvTime is between sunrise and sunset, thus indicating that the sun is
     *                 above the horizon.
     *         - false if the uvTime is either before the sunrise or after the sunset time.
     */
    fun sunInSky(): Boolean
    {
        val startTime = (sunInfo.sunrise ?: sunInfo.dawn ?: sunInfo.sunriseEnd)?.toLocalTime() ?: return false
        val endTime = (sunInfo.sunset ?: sunInfo.dusk ?: sunInfo.sunsetStart)?.toLocalTime() ?: return false
        val timeNow = uvTime?.toLocalTime() ?: LocalTime.now()
        // Sun is in the sky only if not before sunrise and not after sunset
        return !timeNow.isBefore(startTime) && !timeNow.isAfter(endTime)
    }

    companion object
    {
        const val UV_DATA_UPDATED = "com.johnseymour.solarseasons.UVDATA_CHANGED"
        const val UV_DATA_KEY = "uv_data_key"

        private const val UV_LOW = 3.0
        private const val UV_MODERATE = 6.0
        private const val UV_HIGH = 8.0
        private const val UV_VERY_HIGH = 11.0

        fun skinTypeColorInt(skinTypeString: String): Int
        {
            return when (skinTypeString)
            {
                "st1" -> R.color.skin_type_1
                "st2" -> R.color.skin_type_2
                "st3" -> R.color.skin_type_3
                "st4" -> R.color.skin_type_4
                "st5" -> R.color.skin_type_5
                "st6" -> R.color.skin_type_6
                else -> R.color.skin_type_1
            }
        }

        fun skinTypeNameInt(skinTypeString: String): Int
        {
            return when (skinTypeString)
            {
                "st1" -> R.string.skin_type_1
                "st2" -> R.string.skin_type_2
                "st3" -> R.string.skin_type_3
                "st4" -> R.string.skin_type_4
                "st5" -> R.string.skin_type_5
                "st6" -> R.string.skin_type_6
                else -> R.string.skin_type_1
            }
        }
    }
}