package com.johnseymour.solarseasons

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.*
import java.time.ZonedDateTime

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
        sunInfoList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        sunInfoList.addItemDecoration(SunInfoHorizontalSpaceDecoration(resources.getDimensionPixelOffset(R.dimen.cell_sun_info_horizontal_spacing)))

        skinExposureList.layoutManager = GridLayoutManager(this, 2)
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
            LocationService.uvDataPromise?.success()
            { lUVData ->
                runOnUiThread()
                {
                    updateUIFields(lUVData)

                    if (swipeRefresh.isRefreshing)
                    {
                        swipeRefresh.isRefreshing = false
                    }

                    lastObserving?.removeObserver(this)

                    DiskRepository.writeLatestUV(lUVData, getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE))

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

        uvValue.text = resources.getString(R.string.uv_value, lUVData.uv)
        uvValue.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        uvText.text = resources.getText(lUVData.uvLevelTextInt)
        uvText.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        maxUV.text = resources.getString(R.string.max_uv, lUVData.uvMax)
        maxUV.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        maxUVTime.text = resources.getString(R.string.max_uv_time, preferredTimeString(this, lUVData.uvMaxTime))
        maxUVTime.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        lastUpdated.text = resources.getString(R.string.latest_update, preferredTimeString(this, lUVData.uvTime))
        lastUpdated.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        sunProgressLabel.visibility = View.VISIBLE
        sunProgressLabel.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        sunProgress.progress = lUVData.sunProgressPercent
        sunProgress.visibility = View.VISIBLE

        sunInfoListLabel.visibility = View.VISIBLE
        sunInfoListLabel.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        skinExposureLabel.setTextColor(resources.getColor(lUVData.textColorInt, theme))
        lUVData.safeExposure?.entries?.toList()?.let()
        {
            skinExposureList.adapter = SkinExposureAdapter(it, lUVData.textColorInt)
            skinExposureLabel.visibility = View.VISIBLE
            skinExposureList.visibility = View.VISIBLE
        } ?: run()
        {
            skinExposureLabel.visibility = View.GONE
            skinExposureList.visibility = View.GONE
        }

        val sortedSolarTimes = lUVData.sunInfo.timesArray.sortedWith { a, b -> a.second.compareTo(b.second) }
        // Calculate the index for the List of times that is closest to now, use this to set the default scroll position
        val timeNow = ZonedDateTime.now()
        var bestScrollPosition = 0
        while ((bestScrollPosition < sortedSolarTimes.size - 1) && (timeNow.isAfter(sortedSolarTimes[bestScrollPosition].second)))
        {
            bestScrollPosition++
        }

        sunInfoList.adapter = SunInfoAdapter(sortedSolarTimes, lUVData.textColorInt)
        sunInfoList.scrollToPosition(bestScrollPosition)
    }
}