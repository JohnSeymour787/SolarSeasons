package com.johnseymour.solarseasons

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
data class SunInfo(val solarNoon: ZonedDateTime?,
                   val nadir: ZonedDateTime?, //Sun in lowest position, darkest moment in night
                   val sunrise: ZonedDateTime?, //Top edge of sun on horizon
                   val sunset: ZonedDateTime?, //Bottom edge of sun touches horizon (a little more down than sunsetStart), maybe actually sun goes below horizon
                   val sunriseEnd: ZonedDateTime?, //Bottom edge of sun touches horizon
                   val sunsetStart: ZonedDateTime?, //Bottom edge of sun touches horizon
                   val dawn: ZonedDateTime?, //Aka sunrise start
                   val dusk: ZonedDateTime?, //Aka sunset end
                   val nauticalDawn: ZonedDateTime?,
                   val nauticalDusk: ZonedDateTime?,
                   val nightEnd: ZonedDateTime?,
                   val night: ZonedDateTime?, //Night starts
                   val goldenHourEnd: ZonedDateTime?, //Morning golden hour (best time for photography)
                   val goldenHour: ZonedDateTime?, //Evening
                   val azimuth: Double,
                   val altitude: Double): Parcelable
{

    data class SunTimeData(val nameResourceInt: Int, val time: ZonedDateTime, val imageResourceInt: Int)
    /**
     * Returns a list of all non-null times of this SunInfo with their respective name and image resource IDs
     *
     * @return - List<SunTimeData> with the strings.xml ID integer representing the name for that time (eg, Sunset)
     */
    val timesArray: List<SunTimeData>
        get()
        {
            val result = mutableListOf<SunTimeData>()

            solarNoon?.let { result.add(SunTimeData(R.string.sun_info_solar_noon, it, R.drawable.solar_noon)) }
            nadir?.let { result.add(SunTimeData(R.string.sun_info_nadir, it, R.drawable.nadir)) }
            sunrise?.let { result.add(SunTimeData(R.string.sun_info_sunrise, it, R.drawable.sunrise)) }
            sunset?.let { result.add(SunTimeData(R.string.sun_info_sunset, it, R.drawable.sunset)) }
            sunriseEnd?.let { result.add(SunTimeData(R.string.sun_info_sunrise_end, it, R.drawable.sunrise_end)) }
            sunsetStart?.let { result.add(SunTimeData(R.string.sun_info_sunset_start, it, R.drawable.sunset_start)) }
            dawn?.let { result.add(SunTimeData(R.string.sun_info_dawn, it, R.drawable.dawn)) }
            dusk?.let { result.add(SunTimeData(R.string.sun_info_dusk, it, R.drawable.dusk)) }
            nauticalDawn?.let { result.add(SunTimeData(R.string.sun_info_nautical_dawn, it, R.drawable.nautical_dawn)) }
            nauticalDusk?.let { result.add(SunTimeData(R.string.sun_info_nautical_dusk, it, R.drawable.nautical_dusk)) }
            nightEnd?.let { result.add(SunTimeData(R.string.sun_info_night_end, it, R.drawable.night_end)) }
            night?.let { result.add(SunTimeData(R.string.sun_info_night, it, R.drawable.night)) }
            goldenHourEnd?.let { result.add(SunTimeData(R.string.sun_info_golden_hour_end, it, R.drawable.golden_hour_end)) }
            goldenHour?.let { result.add(SunTimeData(R.string.sun_info_golden_hour, it, R.drawable.golden_hour)) }

            return result
        }

    companion object
    {
        fun sunTimeDescription(timeNameResourceInt: Int): Int
        {
            return when(timeNameResourceInt)
            {
                R.string.sun_info_solar_noon -> R.string.sun_info_solar_noon_description
                R.string.sun_info_nadir -> R.string.sun_info_nadir_description
                R.string.sun_info_sunrise -> R.string.sun_info_sunrise_description
                R.string.sun_info_sunset -> R.string.sun_info_sunset_description
                R.string.sun_info_sunrise_end -> R.string.sun_info_sunrise_end_description
                R.string.sun_info_sunset_start -> R.string.sun_info_sunset_start_description
                R.string.sun_info_dawn -> R.string.sun_info_dawn_description
                R.string.sun_info_dusk -> R.string.sun_info_dusk_description
                R.string.sun_info_nautical_dawn -> R.string.sun_info_nautical_dawn_description
                R.string.sun_info_nautical_dusk -> R.string.sun_info_nautical_dusk_description
                R.string.sun_info_night_end -> R.string.sun_info_night_end_description
                R.string.sun_info_night -> R.string.sun_info_night_description
                R.string.sun_info_golden_hour_end -> R.string.sun_info_golden_hour_end_description
                R.string.sun_info_golden_hour -> R.string.sun_info_golden_hour_description

                else -> R.string.sun_info_description_not_found
            }
        }
    }
}