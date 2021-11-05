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

}