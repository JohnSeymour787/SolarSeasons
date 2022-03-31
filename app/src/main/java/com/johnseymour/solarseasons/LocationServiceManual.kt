package com.johnseymour.solarseasons

import android.content.Intent
import androidx.preference.PreferenceManager

class LocationServiceManual: LocationService()
{
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val latitude = sharedPreferences.getString(Constants.SharedPreferences.MANUAL_LOCATION_LATITUDE_KEY, null)?.toDoubleOrNull()
        val longitude = sharedPreferences.getString(Constants.SharedPreferences.MANUAL_LOCATION_LONGITUDE_KEY, null)?.toDoubleOrNull()
        val altitude = sharedPreferences.getString(Constants.SharedPreferences.MANUAL_LOCATION_ALTITUDE_KEY, null)?.toDoubleOrNull()

        if ((latitude == null) || (longitude == null) || (altitude == null))
        {
            uvDataDeferred?.reject(ErrorStatus.ManualLocationError)
            stopSelf()
            return START_STICKY
        }

        super.locationSuccess(latitude, longitude, altitude)

        return START_STICKY
    }
}