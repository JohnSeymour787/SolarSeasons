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

    /** Slight buffer time to allow for work to be executed and completed, as it is not an exact job **/
    const val WORK_EXECUTION_SLACK_TIME = 5L

    const val MINUTES_PER_HOUR = 60
    const val MINUTES_PER_DAY = 1440

    const val USE_GOOGLE_PLAY_LOCATION = false

    object SharedPreferences
    {
        const val SUBSCRIBE_SCREEN_UNLOCK_KEY = "subscribe_screen_unlock"
        const val WORK_TYPE_KEY = "work_type"
        const val DEFAULT_WORK_TYPE_VALUE = "periodic_work"
        const val BACKGROUND_REFRESH_RATE_KEY = "background_refresh_rate"
        const val APP_THEME_KEY = "app_theme"
        const val DEFAULT_APP_THEME_VALUE = "default_theme"
    }
}