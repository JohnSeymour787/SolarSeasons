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
    /**
     * Returns a list of all non-null times of this SunInfo paired with their string name ID
     *
     * @return - List<Pair<Int, ZonedDateTime>> with the strings.xml ID integer representing the name for that time (eg, Sunset)
     */
    val timesArray: List<Pair<Int, ZonedDateTime>>
        get()
        {
            val result = mutableListOf<Pair<Int, ZonedDateTime>>()

            solarNoon?.let { result.add(R.string.sun_info_solar_noon to it) }
            nadir?.let { result.add(R.string.sun_info_nadir to it) }
            sunrise?.let { result.add(R.string.sun_info_sunrise to it) }
            sunset?.let { result.add(R.string.sun_info_sunset to it) }
            sunriseEnd?.let { result.add(R.string.sun_info_sunrise_end to it) }
            sunsetStart?.let { result.add(R.string.sun_info_sunset_start to it) }
            dawn?.let { result.add(R.string.sun_info_dawn to it) }
            dusk?.let { result.add(R.string.sun_info_dusk to it) }
            nauticalDawn?.let { result.add(R.string.sun_info_nautical_dawn to it) }
            nauticalDusk?.let { result.add(R.string.sun_info_nautical_dusk to it) }
            nightEnd?.let { result.add(R.string.sun_info_night_end to it) }
            night?.let { result.add(R.string.sun_info_night to it) }
            goldenHourEnd?.let { result.add(R.string.sun_info_golden_hour_end to it) }
            goldenHour?.let { result.add(R.string.sun_info_golden_hour to it) }

            return result
        }
}