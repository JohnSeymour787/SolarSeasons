package com.johnseymour.solarseasons

import android.content.res.Resources

enum class ErrorStatus
{
    GeneralError,
    NetworkError,
    LocationServiceTerminated,
    GeneralLocationError,
    LocationPermissionError;

    fun statusString(resources: Resources): String
    {
        return when (this)
        {
            GeneralError -> resources.getString(R.string.unknown_error)
            NetworkError -> resources.getString(R.string.no_network_connection)
            LocationServiceTerminated -> resources.getString(R.string.location_service_terminated)
            GeneralLocationError -> resources.getString(R.string.location_not_found)
            LocationPermissionError -> resources.getString(R.string.location_no_permission)
        }
    }
}