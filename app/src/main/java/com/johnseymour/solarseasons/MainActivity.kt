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
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity()
{
    private val viewModel by lazy()
    {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    private lateinit var locationClient: FusedLocationProviderClient

    private val locationRequest by lazy()
    {
        LocationRequest.create().apply()
        {
            interval = 10//TimeUnit.MINUTES.toMillis(30)
//                    fastestInterval = TimeUnit.MINUTES.toMillis(15)
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            numUpdates = 1
        }
    }

    private fun startLocationService()
    {
        //TODO(
        // /*
        //    When getting old location data, check that it is within 15 minutes
        // */

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            //locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            //Check that the location is reasonably recent according to the locationRequest parameters
            locationClient.locationAvailability.addOnSuccessListener()
            {
                if (it.isLocationAvailable)
                {
                    locationClient.lastLocation.addOnSuccessListener()
                    { location: Location? ->
                        location?.let()
                        {
                            val formatter = SimpleDateFormat("HH:mm:ss")
                            NetworkRepository.getRealTimeUV(it.latitude, it.longitude, it.altitude)
                                .observe(this@MainActivity)
                                { lUVData ->
                                    viewModel.uvData = lUVData
                                    testDisplay.text = "UV Rating: ${lUVData.uv}"
                                }
                            //   formatter.timeZone = TimeZone.getDefault()
                            Log.d("location","Using older data")
                            Log.d("location", "Coords (lat, long): ${it.latitude} ${it.longitude} Altitude: ${it.altitude}")
                            Log.d("location","Accuracy: ${it.accuracy} Time: ${formatter.format(Date(it.time))}")
                            Log.d("location", "Elapsed time: ${formatter.format(Date(it.elapsedRealtimeNanos))}")
                            Log.d("location","Provider: ${it.provider}")
                        } ?: run { locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper()) }
                    }
                }
                else
                {
                    locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                }
            }
        }
    }

    private val locationCallback by lazy()
    {
        object: LocationCallback()
        {
            override fun onLocationResult(locationResult: LocationResult?)
            {
                locationResult ?: return

                super.onLocationResult(locationResult)
                val formatter = SimpleDateFormat("HH:mm:ss")
                formatter.timeZone = TimeZone.getDefault()
                locationResult.lastLocation.let()
                {
                    Log.d("location","Requested new data")
                    Log.d("location","Coords (lat, long): ${it.latitude} ${it.longitude} Altitude: ${it.altitude}")
                    Log.d("location","Accuracy: ${it.accuracy} Time: ${formatter.format(Date(it.time))}")
                    Log.d("location", "Provider: ${it.provider}")

                    NetworkRepository.getRealTimeUV(it.latitude, it.longitude, it.altitude)
                        .observe(this@MainActivity)
                        { lUVData ->
                            viewModel.uvData = lUVData
                            testDisplay.text = "UV Rating: ${lUVData.uv}"
                        }
                }

                locationClient.removeLocationUpdates(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted)
            {
                Toast.makeText(this, "Just given permission", Toast.LENGTH_SHORT).show()

                startLocationService()

            }
            else
            {
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

                startLocationService()
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

        //If on Android 12 or more, show a button to take the user to the Android settings page of this app
        // to disable auto-revoking of permissions if the app isn't used for a long time period.
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