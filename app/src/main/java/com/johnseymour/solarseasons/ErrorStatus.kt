package com.johnseymour.solarseasons

import android.content.res.Resources

enum class ErrorStatus
{
    GeneralError,
    NetworkError,
    LocationServiceTerminated,
    GeneralLocationError,
    LocationDisabledError,
    LocationAnyPermissionError,
    FineLocationPermissionError;

    fun statusString(resources: Resources): String
    {
        return when (this)
        {
            GeneralError -> resources.getString(R.string.unknown_error)
            NetworkError -> resources.getString(R.string.no_network_connection)
            LocationServiceTerminated -> resources.getString(R.string.location_service_terminated)
            GeneralLocationError -> resources.getString(R.string.location_not_found)
            LocationDisabledError -> resources.getString(R.string.location_disabled_error)
            LocationAnyPermissionError -> resources.getString(R.string.location_permission_any_denied)
            FineLocationPermissionError -> resources.getString(R.string.location_permission_fine_error)
        }
    }

    companion object
    {
        const val ERROR_STATUS_KEY = "error_status_key"
    }
}