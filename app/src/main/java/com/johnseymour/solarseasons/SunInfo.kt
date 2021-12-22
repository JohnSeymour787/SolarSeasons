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

            solarNoon?.let { result.add(SunTimeData(R.string.sun_info_solar_noon, it, R.mipmap.ic_launcher_legacy)) }
            nadir?.let { result.add(SunTimeData(R.string.sun_info_nadir, it, R.mipmap.ic_launcher_legacy)) }
            sunrise?.let { result.add(SunTimeData(R.string.sun_info_sunrise, it, R.mipmap.ic_launcher_legacy)) }
            sunset?.let { result.add(SunTimeData(R.string.sun_info_sunset, it, R.mipmap.ic_launcher_legacy)) }
            sunriseEnd?.let { result.add(SunTimeData(R.string.sun_info_sunrise_end, it, R.mipmap.ic_launcher_legacy)) }
            sunsetStart?.let { result.add(SunTimeData(R.string.sun_info_sunset_start, it, R.mipmap.ic_launcher_legacy)) }
            dawn?.let { result.add(SunTimeData(R.string.sun_info_dawn, it, R.mipmap.ic_launcher_legacy)) }
            dusk?.let { result.add(SunTimeData(R.string.sun_info_dusk, it, R.mipmap.ic_launcher_legacy)) }
            nauticalDawn?.let { result.add(SunTimeData(R.string.sun_info_nautical_dawn, it, R.mipmap.ic_launcher_legacy)) }
            nauticalDusk?.let { result.add(SunTimeData(R.string.sun_info_nautical_dusk, it, R.mipmap.ic_launcher_legacy)) }
            nightEnd?.let { result.add(SunTimeData(R.string.sun_info_night_end, it, R.mipmap.ic_launcher_legacy)) }
            night?.let { result.add(SunTimeData(R.string.sun_info_night, it, R.mipmap.ic_launcher_legacy)) }
            goldenHourEnd?.let { result.add(SunTimeData(R.string.sun_info_golden_hour_end, it, R.mipmap.ic_launcher_legacy)) }
            goldenHour?.let { result.add(SunTimeData(R.string.sun_info_golden_hour, it, R.mipmap.ic_launcher_legacy)) }

            return result
        }
}