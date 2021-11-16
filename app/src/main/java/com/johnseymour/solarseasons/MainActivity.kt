package com.johnseymour.solarseasons

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.*

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

        // Coming from a clicked widget
        if (intent.action == UVData.UV_DATA_CHANGED)
        {
            intent.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
            {
                updateUIFields(it)
            }
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
            }//?.fail()
//            {
//                runOnUiThread()
//                {
//                    if (swipeRefresh.isRefreshing)
//                    {
//                        swipeRefresh.isRefreshing = false
//                    }
//                }
//            }
        }
    }

    private fun updateUIFields(lUVData: UVData)
    {
        layout.setBackgroundColor(resources.getColor(lUVData.backgroundColorInt, theme))

        uvValue.text = resources.getString(R.string.widget_uv_value, lUVData.uv)
        uvValue.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        maxUV.text = resources.getString(R.string.max_uv, lUVData.uvMax)
        maxUV.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        uvMaxTime.text = resources.getString(R.string.max_uv_time, Constants.Formatters.hour12.format(lUVData.uvMaxTime))
        uvMaxTime.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        sunset.text = resources.getString(R.string.sunset_time, Constants.Formatters.hour12.format(lUVData.sunInfo.sunset))
        sunset.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        sunrise.text = resources.getString(R.string.sunrise_time, Constants.Formatters.hour12.format(lUVData.sunInfo.sunrise))
        sunrise.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        solarNoon.text = resources.getString(R.string.solar_noon_time, Constants.Formatters.hour12.format(lUVData.sunInfo.solarNoon))
        solarNoon.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        sunProgress.progress = lUVData.sunProgressPercent
    }
}