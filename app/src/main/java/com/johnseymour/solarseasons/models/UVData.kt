package com.johnseymour.solarseasons.models

import android.os.Parcelable
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment
import kotlinx.android.parcel.Parcelize
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Parcelize
data class UVData(
    var uv: Float,
    val uvTime: ZonedDateTime? = null,
    val uvMax: Float,
    val uvMaxTime: ZonedDateTime? = null,
    val ozone: Float,
    val ozoneTime: ZonedDateTime? = null,
    val safeExposure: Map<String, Int>? = null,
    val sunInfo: SunInfo,
    var cloudCover: Double? = null): Parcelable
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
        else if (PreferenceScreenFragment.useCustomTheme)
        {
            R.color.uv_extreme
        }
        else
        {
            R.color.uv_extreme_alternate
        }

    val textColorInt: Int
        get() =
            if (uv < UV_HIGH)
            {
                R.color.dark_text
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
     * Calculates the minutes from this object's uvTime and ZonedDateTime.now()
     *
     * @return absolute Long value of time between the UV time and current time
     */
    val minutesSinceDataRetrieved: Long
        get() = ChronoUnit.MINUTES.between(uvTime ?: ZonedDateTime.now(), ZonedDateTime.now()).absoluteValue

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

    val cloudFactoredUV: Float?
        get()
        {
            // Cloud cover is a decimal percentage, with 1.0 meaning 100% cloudy and 0.0 meaning clear skies
            val lCloudCover = cloudCover ?: return null

            return if (lCloudCover < LIGHT_CLOUD_COVER)
            {
                uv
            }
            else if ((lCloudCover >= LIGHT_CLOUD_COVER) && (lCloudCover < MODERATE_CLOUD_COVER))
            {
                uv * LIGHT_CLOUD_COVER_UV_REDUCTION_FACTOR
            }
            else if ((lCloudCover >= MODERATE_CLOUD_COVER) && (lCloudCover < HEAVY_CLOUD_COVER))
            {
                uv * MODERATE_CLOUD_COVER_UV_REDUCTION_FACTOR
            }
            else if (lCloudCover >= HEAVY_CLOUD_COVER)
            {
                uv * HEAVY_CLOUD_COVER_UV_REDUCTION_FACTOR
            }
            else
            {
                uv
            }
        }

    val cloudCoverTextInt: Int?
        get()
        {
            val lCloudCover = cloudCover ?: return null

            return if (lCloudCover < MINIMAL_CLOUD_COVER)
            {
                R.string.cloud_cover_none
            }
            else if ((lCloudCover >= MINIMAL_CLOUD_COVER) && (lCloudCover < MODERATE_CLOUD_COVER))
            {
                R.string.cloud_cover_light
            }
            else if ((lCloudCover >= MODERATE_CLOUD_COVER) && (lCloudCover < HEAVY_CLOUD_COVER))
            {
                R.string.cloud_cover_moderate
            }
            else if (lCloudCover >= HEAVY_CLOUD_COVER)
            {
                R.string.cloud_cover_heavy
            }
            else
            {
                null
            }
        }

    companion object
    {
        const val UV_DATA_UPDATED = "com.johnseymour.solarseasons.UVDATA_CHANGED"
        const val UV_DATA_KEY = "uv_data_key"

        private const val UV_LOW = 3.0F
        private const val UV_MODERATE = 6.0F
        private const val UV_HIGH = 8.0F
        private const val UV_VERY_HIGH = 11.0F

        private const val LIGHT_CLOUD_COVER_UV_REDUCTION_FACTOR = 0.89F
        private const val MODERATE_CLOUD_COVER_UV_REDUCTION_FACTOR = 0.73F
        private const val HEAVY_CLOUD_COVER_UV_REDUCTION_FACTOR = 0.31F

        private const val MINIMAL_CLOUD_COVER = 0.1
        private const val LIGHT_CLOUD_COVER = 0.2
        private const val MODERATE_CLOUD_COVER = 0.7
        private const val HEAVY_CLOUD_COVER = 0.9

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