package com.johnseymour.solarseasons

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.johnseymour.solarseasons.api.NetworkRepository
import kotlinx.android.synthetic.main.activity_main.*
import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Intent
import android.location.Location
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.*
import com.google.android.gms.location.*
import nl.komponents.kovenant.ui.successUi
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, Observer<List<WorkInfo>>
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
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED ->
            {
                Toast.makeText(this, "Already have permission", Toast.LENGTH_SHORT).show()

                //TODO() Probably dont request new data here, rather need to read from a file the last data saved (probably by a widget)
                //updateUVData()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ->
            {
                Toast.makeText(this, "Permission required for getting UV at current location.", Toast.LENGTH_SHORT).show()
            }

            else ->
            {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
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

        if (intent.action == UVData.UV_DATA_CHANGED)
        {
            val date = intent.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)
            intent.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
            {
                updateUIFields(it)
            }
        }

        testButton.setOnClickListener()
        {
            // Update all widgets
            val intent = Intent(this, SmallUVDisplay::class.java).apply()
            {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(UVData.UV_DATA_KEY, "")
                val ids = AppWidgetManager.getInstance(application).getAppWidgetIds(ComponentName(applicationContext, SmallUVDisplay::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }

            sendBroadcast(intent)
        }

        swipeRefresh.setOnRefreshListener(this)
    }

    private var lastObserving: LiveData<List<WorkInfo>>? = null

    private fun updateUVData()
    {
        lastObserving = UVDataWorker.initiateWorker(this)
        lastObserving?.observe(this, this)
    }

    override fun onRefresh()
    {
        updateUVData()
    }

    override fun onChanged(workInfo: List<WorkInfo>?)
    {
        if (workInfo?.firstOrNull()?.state == WorkInfo.State.SUCCEEDED)
        {
            UVDataWorker.uvDataPromise?.success()
            { lUVData ->
                runOnUiThread()
                {
                    updateUIFields(lUVData)

                    if (swipeRefresh.isRefreshing)
                    {
                        swipeRefresh.isRefreshing = false
                    }

                    lastObserving?.removeObserver(this)
                    lastObserving = UVDataWorker.initiateWorker(this, true)     //TODO() Think about what happens when close the activity but need to keep the widgets updating
                    lastObserving?.observe(this, this)

                    // Update all widgets
                    val intent = Intent(this, SmallUVDisplay::class.java).apply()
                    {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(UVData.UV_DATA_KEY, lUVData)
                        val ids = AppWidgetManager.getInstance(application).getAppWidgetIds(ComponentName(applicationContext, SmallUVDisplay::class.java))
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }

                    sendBroadcast(intent)
                }
            }
        }
    }

    private fun updateUIFields(lUVData: UVData)
    {
        uvValue.text = resources.getString(R.string.widget_uv_value, lUVData.uv)
        maxUV.text = resources.getString(R.string.max_uv, lUVData.uvMax)
        sunProgress.progress = lUVData.sunProgressPercent
        uvMaxTime.text = resources.getString(R.string.max_uv_time, Constants.Formatters.hour12.format(lUVData.uvMaxTime))
        sunset.text = resources.getString(R.string.sunset_time, Constants.Formatters.hour12.format(lUVData.sunInfo.sunset))
        sunrise.text = resources.getString(R.string.sunrise_time, Constants.Formatters.hour12.format(lUVData.sunInfo.sunrise))
        solarNoon.text = resources.getString(R.string.solar_noon_time, Constants.Formatters.hour12.format(lUVData.sunInfo.solarNoon))
    }
}