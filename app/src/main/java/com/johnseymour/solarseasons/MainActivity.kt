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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.*
import com.google.android.gms.location.*
import nl.komponents.kovenant.ui.successUi
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener
{
    private val viewModel by lazy()
    {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted)
            {
                Toast.makeText(this, "Just given permission", Toast.LENGTH_SHORT).show()

                //updateUVData()
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

                //updateUVData()
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

                //startActivity(intent)
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

        swipeRefresh.setOnRefreshListener(this)
    }

    private fun updateUVData()
    {
        UVDataWorker.initiateWorker(this).observe(this)
        { workInfo ->
            if (workInfo.firstOrNull()?.state == WorkInfo.State.SUCCEEDED)
            {
                UVDataWorker.uvDataDeferred?.promise?.success()
                { lUVData ->
                    runOnUiThread()
                    {
                        uvValue.text = resources.getString(R.string.widget_uv_value, lUVData.uv)
                        maxUV.text = resources.getString(R.string.max_uv, lUVData.uvMax)
                        sunProgress.progress = lUVData.sunProgressPercent

                        //TODO() V Put into constants class
                        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

                        uvMaxTime.text = resources.getString(R.string.max_uv_time, formatter.format(lUVData.uvMaxTime))
                        sunset.text = resources.getString(R.string.sunset_time, formatter.format(lUVData.sunInfo.sunset))
                        sunrise.text = resources.getString(R.string.sunrise_time, formatter.format(lUVData.sunInfo.sunrise))
                        solarNoon.text = resources.getString(R.string.solar_noon_time, formatter.format(lUVData.sunInfo.solarNoon))

                        if (swipeRefresh.isRefreshing)
                        {
                            swipeRefresh.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    override fun onRefresh()
    {
        updateUVData()
    }
}