package com.johnseymour.solarseasons.services

import com.johnseymour.solarseasons.models.UVLocationData

class LocationServiceTestNoAPI: LocationService()
{
    override fun serviceMain(): Int
    {
        locationDataDeferred?.resolve(UVLocationData(-37.9050904, 144.9330945, 10.0))

        stopSelf()

        return START_STICKY
    }
}