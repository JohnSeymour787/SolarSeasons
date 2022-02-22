package com.johnseymour.solarseasons

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.Manifest
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.res.ColorStateList
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.*
import java.io.FileNotFoundException
import java.time.ZonedDateTime
import android.os.Build

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, Observer<List<WorkInfo>>
{
    private val viewModel by lazy()
    {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    private val localBroadcastManager by lazy { LocalBroadcastManager.getInstance(this) }

    private val uvDataForegroundBroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            if (intent.action == UVData.UV_DATA_UPDATED)
            {
                intent.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
                {
                    newUVDataReceived(it)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION]?.let()
            { granted ->
                if (!granted)
                {
                    appStatusInformation.text = getString(R.string.location_permission_generic_rationale)
                    return@registerForActivityResult
                }
            }
        }

        when
        {
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) ->
            {
                appStatusInformation.text = getString(R.string.location_permission_generic_rationale)
            }

            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ->
            {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }

            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ->
            {
                if (getWidgetIDs().isNotEmpty())
                {
                    val backgroundOptionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    {
                        // Text for the option in the location permissions screen, eg, "Allow all the time"
                        packageManager.backgroundPermissionOptionLabel
                    }
                    else
                    {
                        getString(R.string.default_background_permission_option_label)
                    }

                    appStatusInformation.text = getString(R.string.location_permission_background_rationale, getString(R.string.activity_status_information_swipe_hint), backgroundOptionLabel)

                    launchAppDetailsButton.visibility = View.VISIBLE
                }
            }
        }

        // Coming from a clicked widget
        if (intent.action == UVData.UV_DATA_UPDATED)
        {
            (intent.getSerializableExtra(ErrorStatus.ERROR_STATUS_KEY) as? ErrorStatus)?.let()
            {
                displayError(it)
            } ?: intent.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
            {
                newUVDataReceived(it)
            } ?: updateUVDataFromDisk()
        }

        layout.setOnRefreshListener(this)
        layout.setProgressViewOffset(true, resources.getDimensionPixelOffset(R.dimen.activity_swipe_refresh_offset_start), resources.getDimensionPixelOffset(R.dimen.activity_swipe_refresh_offset_end))

        sunInfoList.addItemDecoration(SunInfoHorizontalSpaceDecoration(resources.getDimensionPixelOffset(R.dimen.list_view_cell_spacing)))

        skinExposureList.addItemDecoration(SkinExposureVerticalSpaceDecoration(resources.getDimensionPixelOffset(R.dimen.list_view_cell_spacing)))
    }

    override fun onResume()
    {
        super.onResume()

        localBroadcastManager.unregisterReceiver(viewModel.uvDataBackgroundBroadcastReceiver)

        viewModel.latestError?.let()
        {
            displayError(it)
        } ?: run()
        {
            viewModel.uvData?.let { displayNewUVData(it) }
        }

        // Actively update UI when background requests come in when activity is in foreground
        localBroadcastManager.registerReceiver(uvDataForegroundBroadcastReceiver, viewModel.uvDataChangedIntentFilter)
    }

    override fun onPause()
    {
        super.onPause()

        localBroadcastManager.unregisterReceiver(uvDataForegroundBroadcastReceiver)

        localBroadcastManager.registerReceiver(viewModel.uvDataBackgroundBroadcastReceiver, viewModel.uvDataChangedIntentFilter)
    }

    private fun newUVDataReceived(uvData: UVData)
    {
        viewModel.uvData = uvData
        viewModel.latestError = null
        displayNewUVData(uvData)
    }

    private fun updateUVDataFromDisk()
    {
        try
        {
            DiskRepository.readLatestUV(getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, MODE_PRIVATE))?.let()
            {
                viewModel.uvData = it
                displayNewUVData(it)
            }
        } catch (e: FileNotFoundException){ }
    }

    private fun prepareUVDataRequest()
    {
        viewModel.lastObserving = UVDataWorker.initiateOneTimeWorker(this)
        viewModel.lastObserving?.observe(this, this)
    }

    override fun onRefresh()
    {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            appStatusInformation.visibility = View.INVISIBLE
            launchAppDetailsButton.visibility = View.INVISIBLE
            prepareUVDataRequest()
        }
        else if (layout.isRefreshing)
        {
            layout.isRefreshing = false
        }
    }

    override fun onChanged(workInfo: List<WorkInfo>?)
    {
        if (workInfo?.firstOrNull()?.state == WorkInfo.State.SUCCEEDED)
        {
            LocationService.uvDataPromise?.success()
            { lUVData ->
                runOnUiThread()
                {
                    newUVDataReceived(lUVData)

                    if (layout.isRefreshing)
                    {
                        layout.isRefreshing = false
                    }

                    viewModel.lastObserving?.removeObserver(this)

                    DiskRepository.writeLatestUV(lUVData, getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE))

                    val ids = getWidgetIDs()
                    if (ids.isNotEmpty())
                    {
                        // Update all widgets
                        val intent = Intent(this, SmallUVDisplay::class.java).apply()
                        {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(UVData.UV_DATA_KEY, lUVData)
                            putExtra(SmallUVDisplay.START_BACKGROUND_WORK_KEY, true) // Will result in background updates if the relevant permission is granted
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }

                        sendBroadcast(intent)
                    }
                }
            }?.fail()
            { errorStatus ->
                runOnUiThread()
                {
                    if (layout.isRefreshing)
                    {
                        layout.isRefreshing = false
                    }

                    viewModel.latestError = errorStatus

                    displayError(errorStatus)
                }
            }
        }
    }

    private fun displayError(errorStatus: ErrorStatus)
    {
        appStatusInformation.text = errorStatus.statusString(resources)
        appStatusInformation.visibility = View.VISIBLE
        layout.setBackgroundColor(resources.getColor(R.color.uv_low, theme))

        uvValue.visibility = View.INVISIBLE
        uvText.visibility = View.INVISIBLE
        maxUV.visibility = View.INVISIBLE
        maxUVTime.visibility = View.INVISIBLE
        lastUpdated.visibility = View.INVISIBLE
        sunProgressLabel.visibility = View.INVISIBLE
        sunProgress.visibility = View.INVISIBLE
        sunProgressLabelBackground.visibility = View.INVISIBLE
        sunInfoListTitleLabel.visibility = View.INVISIBLE
        sunInfoListSubLabel.visibility = View.INVISIBLE
        sunInfoList.visibility = View.INVISIBLE
        sunInfoListBackground.visibility = View.INVISIBLE
        skinExposureLabel.visibility = View.INVISIBLE
        skinExposureList.visibility = View.INVISIBLE
        skinExposureBackground.visibility = View.INVISIBLE
    }

    private fun displayNewUVData(lUVData: UVData)
    {
        layout.setBackgroundColor(resources.getColor(lUVData.backgroundColorInt, theme))

        settingsIcon.imageTintList = ColorStateList.valueOf(resources.getColor(lUVData.textColorInt, theme))

        uvValue.visibility = View.VISIBLE
        uvValue.text = resources.getString(R.string.uv_value, lUVData.uv)
        uvValue.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        uvText.visibility = View.VISIBLE
        uvText.text = resources.getText(lUVData.uvLevelTextInt)
        uvText.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        maxUV.visibility = View.VISIBLE
        maxUV.text = resources.getString(R.string.max_uv, lUVData.uvMax)
        maxUV.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        maxUVTime.visibility = View.VISIBLE
        maxUVTime.text = resources.getString(R.string.max_uv_time, preferredTimeString(this, lUVData.uvMaxTime))
        maxUVTime.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        lastUpdated.visibility = View.VISIBLE
        lastUpdated.text = resources.getString(R.string.latest_update, preferredTimeString(this, lUVData.uvTime))
        lastUpdated.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        sunProgressLabel.visibility = View.VISIBLE
        sunProgressLabel.setTextColor(resources.getColor(lUVData.textColorInt, theme))
        sunProgressLabel.setText(R.string.sun_progress_label)
        if (sunProgressLabel.lineCount > 1)
        {
            sunProgressLabel.setText(R.string.sun_progress_label_shortened)
        }

        sunProgress.progress = lUVData.sunProgressPercent
        sunProgress.visibility = View.VISIBLE
        sunProgressLabelBackground.visibility = View.VISIBLE

        sunInfoListBackground.visibility = View.VISIBLE

        sunInfoListTitleLabel.visibility = View.VISIBLE
        sunInfoListTitleLabel.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        sunInfoListSubLabel.visibility = View.VISIBLE
        sunInfoListSubLabel.setTextColor(resources.getColor(lUVData.textColorInt, theme))

        skinExposureLabel.setTextColor(resources.getColor(lUVData.textColorInt, theme))
        lUVData.safeExposure?.entries?.toList()?.let()
        {
            skinExposureList.adapter = SkinExposureAdapter(it, lUVData.textColorInt)
            skinExposureList.layoutManager = GridLayoutManager(this, 2)
            skinExposureLabel.visibility = View.VISIBLE
            skinExposureList.visibility = View.VISIBLE
            skinExposureBackground.visibility = View.VISIBLE
        } ?: run()
        {
            skinExposureLabel.visibility = View.GONE
            skinExposureList.visibility = View.GONE
            skinExposureBackground.visibility = View.GONE
        }

        val sortedSolarTimes = lUVData.sunInfo.timesArray.sortedWith { a, b -> a.time.compareTo(b.time) }
        // Calculate the index for the List of times that is closest to now, use this to set the default scroll position
        val timeNow = ZonedDateTime.now()
        var bestScrollPosition = 0
        while ((bestScrollPosition < sortedSolarTimes.size - 1) && (timeNow.isAfter(sortedSolarTimes[bestScrollPosition].time)))
        {
            bestScrollPosition++
        }

        sunInfoList.visibility = View.VISIBLE
        sunInfoList.adapter = SunInfoAdapter(sortedSolarTimes, lUVData.textColorInt, ::sunTimeOnClick)
        sunInfoList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        sunInfoList.scrollToPosition(bestScrollPosition)

        appStatusInformation.visibility = View.INVISIBLE
        launchAppDetailsButton.visibility = View.INVISIBLE
    }

    private fun sunTimeOnClick(sunTimeData: SunInfo.SunTimeData)
    {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(sunTimeData.nameResourceInt)
        builder.setIcon(sunTimeData.imageResourceInt)
        builder.setMessage(SunInfo.sunTimeDescription(sunTimeData.nameResourceInt))
        builder.setPositiveButton(R.string.close_window) { _, _ -> }

        builder.create().show()
    }

    private fun getWidgetIDs(): IntArray = AppWidgetManager.getInstance(application).getAppWidgetIds(ComponentName(applicationContext, SmallUVDisplay::class.java))

    fun launchAppDetailsActivity(v: View)
    {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply()
        {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}