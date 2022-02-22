package com.johnseymour.solarseasons

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object Constants
{
    object Formatters
    {
        val HOUR_12: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val HOUR_24: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    /** Default background refresh rate of calling the API in minutes **/
    const val DEFAULT_REFRESH_TIME = 30L

    /** Shortest time interval to call the API, in minutes **/
    const val SHORTEST_REFRESH_TIME = 1L

    const val MINUTES_PER_HOUR = 60
    const val MINUTES_PER_DAY = 1440

    const val USE_GOOGLE_PLAY_LOCATION = false

    const val USE_PERIODIC_WORK = true
}