package com.johnseymour.solarseasons

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.johnseymour.solarseasons.api.NetworkRepository
import kotlinx.android.synthetic.main.activity_main.*
import android.Manifest
import android.content.Intent
import android.location.Location
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Just given permission", Toast.LENGTH_SHORT).show()
/*                NetworkRepository.getRealTimeUV().observe(this)
                {
                    testDisplay.text = "UV Rating: ${it.uv}"
                }*/

                val locationClient = LocationServices.getFusedLocationProviderClient(this)

                locationClient.lastLocation.addOnSuccessListener()
                { location: Location? ->
                    location?.let()
                    {
                        coords.text = "Coords (lat, long): ${it.latitude} ${it.longitude} Altitude: ${it.altitude}"
                        accuracy.text = "Accuracy: ${it.accuracy} Time: ${it.time}"
                        provider.text = "Provider: ${it.provider}"
                    }?:run{ accuracy.text = "Location was null"}
                }

            } else {
                Toast.makeText(
                    this,
                    "Permission required for getting UV at current location.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        when
        {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ->
            {
                Toast.makeText(this, "Already have permission", Toast.LENGTH_SHORT).show()

                val locationClient = LocationServices.getFusedLocationProviderClient(this)

                val locationRequest = LocationRequest.create().apply()
                {
                    interval = 10//TimeUnit.MINUTES.toMillis(30)
//                    fastestInterval = TimeUnit.MINUTES.toMillis(15)
                    priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                    numUpdates = 1
                }

                val locationCallback: LocationCallback = object: LocationCallback()
                {
                    override fun onLocationResult(locationResult: LocationResult?)
                    {
                        locationResult ?: return

                        super.onLocationResult(locationResult)

                        locationResult.lastLocation.let()
                        {
                            coords.text = "Coords (lat, long): ${it.latitude} ${it.longitude} Altitude: ${it.altitude}"
                            accuracy.text = "Accuracy: ${it.accuracy} Time: ${it.time}"
                            provider.text = "Provider: ${it.provider}"

                           NetworkRepository.getRealTimeUV(it.latitude, it.longitude, it.altitude).observe(this@MainActivity)
                            {
                                testDisplay.text = "UV Rating: ${it.uv}"
                            }
                        }

                        locationClient.removeLocationUpdates(this)
                    }

                    override fun onLocationAvailability(p0: LocationAvailability)
                    {
                        super.onLocationAvailability(p0)
                    }
                }


                locationClient.lastLocation.addOnSuccessListener()
                { location: Location? ->
                    location?.let()
                    {
                        coords.text = "Coords (lat, long): ${it.latitude} ${it.longitude} Altitude: ${it.altitude}"
                        accuracy.text = "Accuracy: ${it.accuracy} Time: ${it.time}"
                        provider.text = "Provider: ${it.provider}"
                    } ?: run { locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper()) }
                }
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
            {
                Toast.makeText(this, "Permission required for getting UV at current location.", Toast.LENGTH_SHORT).show()
            }

            else ->
            {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
        {
            disablePermissionReset.setOnClickListener()
            {
                val intent = Intent().apply()
                {
                    action = Intent.ACTION_AUTO_REVOKE_PERMISSIONS
                }

                startActivity(intent)
            }
        }
        else
        {
            disablePermissionReset.visibility = View.GONE
        }
    }
}