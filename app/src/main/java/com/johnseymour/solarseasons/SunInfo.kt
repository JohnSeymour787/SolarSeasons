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
     * Returns a list of all non-null times of this SunInfo paired with their name
     *
     * @return - List<Pair<String, ZonedDateTime>> with the string representing the name for that time (eg, sunset)
     */
    val timesArray: List<Pair<String, ZonedDateTime>>
        get()
        {
            val result = mutableListOf<Pair<String, ZonedDateTime>>()

            solarNoon?.let { result.add("solarNoon" to it) }
            nadir?.let { result.add("nadir" to it) }
            sunrise?.let { result.add("sunrise" to it) }
            sunset?.let { result.add("sunset" to it) }
            sunriseEnd?.let { result.add("sunriseEnd" to it) }
            sunsetStart?.let { result.add("sunsetStart" to it) }
            dawn?.let { result.add("dawn" to it) }
            dusk?.let { result.add("dusk" to it) }
            nauticalDawn?.let { result.add("nauticalDawn" to it) }
            nauticalDusk?.let { result.add("nauticalDusk" to it) }
            nightEnd?.let { result.add("nightEnd" to it) }
            night?.let { result.add("night" to it) }
            goldenHourEnd?.let { result.add("goldenHourEnd" to it) }
            goldenHour?.let { result.add("goldenHour" to it) }

            return result
        }
}