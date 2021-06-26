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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
  //                        NetworkRepository.OLDgetRealTimeUV(it.latitude, it.longitude, it.altitude)
                            NetworkRepository.OLDgetRealTimeUV()
                                .observe(this@MainActivity)
                                { lUVData ->
                                    viewModel.uvData = lUVData
                                    uvValue.text = resources.getString(R.string.widget_uv_value, lUVData.uv)
                                    maxUV.text = resources.getString(R.string.max_uv, lUVData.uvMax)

                                    sunProgress.progress = lUVData.sunProgressPercent

                                    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

                                    uvMaxTime.text = resources.getString(R.string.max_uv_time, formatter.format(lUVData.uvMaxTime))
                                    sunset.text = resources.getString(R.string.sunset_time, formatter.format(lUVData.sunInfo.sunset))
                                    sunrise.text = resources.getString(R.string.sunrise_time, formatter.format(lUVData.sunInfo.sunrise))
                                    solarNoon.text = resources.getString(R.string.solar_noon_time, formatter.format(lUVData.sunInfo.solarNoon))
                                }
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

                locationResult.lastLocation.let()
                {
  /*                  NetworkRepository.getRealTimeUV(it.latitude, it.longitude, it.altitude)
                        .observe(this@MainActivity)
                        { lUVData ->
                            viewModel.uvData = lUVData
                            testDisplay.text = "UV Rating: ${lUVData.uv}"
                        }*/
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

        testButton.setOnClickListener()
        {
//  val intent = Intent(UVData.UV_DATA_CHANGED).apply { putExtra(UVData.UV_DATA_KEY, UVData(10F, 29F)) }
       //     val intent = Intent(applicationContext, SmallUVDisplay::class.java).apply { putExtra(UVData.UV_DATA_KEY, null) }
            baseContext.sendBroadcast(intent)
        }
    }
}