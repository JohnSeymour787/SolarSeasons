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

    const val OPEN_UV_WEBSITE_BASE_URL = "https://www.openuv.io"
    const val OPEN_UV_WEBSITE_CONSOLE_URL = "https://www.openuv.io/console"

    /** Default background refresh rate of calling the API in minutes **/
    const val DEFAULT_REFRESH_TIME = 60L

    /** Shortest time interval to call the API, in minutes **/
    const val SHORTEST_REFRESH_TIME = 1L

    /** Slight buffer time to allow for work to be executed and completed, as it is not an exact job **/
    const val WORK_EXECUTION_SLACK_TIME = 5L

    /** Shortest time for app to auto refresh when it is launched into the foreground **/
    const val MINIMUM_APP_FOREGROUND_REFRESH_TIME = 10L

    /** Time in minutes for which a current UV data is considered "recent" for displaying on the UV forecast graph **/
    const val UV_FORECAST_ACCEPTABLE_RECENT_UV_TIME = 40L

    const val UV_PROTECTION_TIME_DEFAULT_FROM_UV = 3.5F
    const val UV_PROTECTION_TIME_DEFAULT_TO_UV = 3.5F

    const val MINUTES_PER_HOUR = 60
    const val MINUTES_PER_DAY = 1440
    /** Maximum altitude on earth to get UV data for **/
    const val MAXIMUM_EARTH_ALTITUDE = 10000.0

    const val MAXIMUM_LATITUDE_DEGREE = 90.0
    const val MAXIMUM_LONGITUDE_DEGREE = 180.0

    const val MINIMUM_API_ACCEPTED_ALTITUDE = 0.0

    private const val LOCATION_OBFUSCATION_METERS = 1000
    private const val LOCATION_OBFUSCATION_METERS_TO_COORDINATE_DECIMAL = 0.00001
    /**
     * Decimal points of a coordinate degree that corresponds to meters in distance
     * @sample 0.00001 is around 1.11 meters
     * @see (https://gis.stackexchange.com/questions/8650/measuring-accuracy-of-latitude-and-longitude)
     */
    const val LOCATION_OBFUSCATION_COORDINATE_DECIMAL = LOCATION_OBFUSCATION_METERS * LOCATION_OBFUSCATION_METERS_TO_COORDINATE_DECIMAL

    /** Rough maximum UV expected at sea level, theoretically can be higher **/
    const val GENERAL_MAXIMUM_UV = 14F

    const val USE_GOOGLE_PLAY_LOCATION = BuildConfig.USE_GOOGLE_PLAY_LOCATION
    const val ENABLE_MANUAL_LOCATION_FEATURE = BuildConfig.ENABLE_MANUAL_LOCATION
    const val ENABLE_API_KEY_ENTRY_FEATURE = BuildConfig.ENABLE_API_KEY_ENTRY
    /** Skips location retrieval and API calls to save on request quota. Service generates simple UV data **/
    const val TEST_MODE_NO_API = false

    object SharedPreferences
    {
        const val SUBSCRIBE_SCREEN_UNLOCK_KEY = "subscribe_screen_unlock"
        const val WORK_TYPE_KEY = "work_type"
        const val DEFAULT_WORK_TYPE_VALUE = "periodic_work"
        const val BACKGROUND_REFRESH_RATE_KEY = "background_refresh_rate"
        const val APP_THEME_KEY = "app_theme"
        const val CUSTOM_APP_THEME_VALUE = "custom_theme"
        const val APP_LAUNCH_AUTO_REQUEST_KEY = "app_launch_auto_request"

        const val UV_PROTECTION_NOTIFICATION_KEY = "uv_protection_notification"
        const val UV_PROTECTION_NOTIFICATION_TIME_KEY = "uv_protection_notification_time"
        const val UV_PROTECTION_NOTIFICATION_CUSTOM_TIME_KEY = "uv_protection_notification_custom_time"
        const val UV_PROTECTION_END_NOTIFICATION_KEY = "uv_protection_end_notification"

        const val MANUAL_LOCATION_ENABLED_KEY = "enable_manual_location"
        const val MANUAL_LOCATION_LATITUDE_KEY = "manual_location_latitude"
        const val MANUAL_LOCATION_LONGITUDE_KEY = "manual_location_longitude"
        const val MANUAL_LOCATION_ALTITUDE_KEY = "manual_location_altitude"

        const val API_KEY = "stored_api_key"

        const val CLOUD_COVER_FACTOR_KEY = "cloud_cover_factor"
        const val LATEST_CITY_NAME_KEY = "latest_city_name"
    }
}