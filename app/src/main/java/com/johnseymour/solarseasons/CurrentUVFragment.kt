package com.johnseymour.solarseasons

import android.Manifest
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.WorkInfo
import kotlinx.android.synthetic.main.fragment_current_u_v.*
import java.io.FileNotFoundException
import java.time.ZonedDateTime

class CurrentUVFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, Observer<List<WorkInfo>>
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_current_u_v, container, false)
    }

    private val viewModel by lazy()
    {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    private val localBroadcastManager by lazy { LocalBroadcastManager.getInstance(requireContext()) }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

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

            requireContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ->
            {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }

            requireContext().checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ->
            {
                if (getWidgetIDs().isNotEmpty())
                {
                    val backgroundOptionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    {
                        // Text for the option in the location permissions screen, eg, "Allow all the time"
                        requireContext().packageManager.backgroundPermissionOptionLabel
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

        (arguments?.getSerializable(ErrorStatus.ERROR_STATUS_KEY) as? ErrorStatus)?.let()
        {
            displayError(it)
        } ?: arguments?.getParcelable<UVData>(UVData.UV_DATA_KEY)?.let()
        {
            newUVDataReceived(it)
        } ?: updateUVDataFromDisk()

        layout.setOnRefreshListener(this)
        layout.setProgressViewOffset(true, resources.getDimensionPixelOffset(R.dimen.activity_swipe_refresh_offset_start), resources.getDimensionPixelOffset(R.dimen.activity_swipe_refresh_offset_end))

        sunInfoList.addItemDecoration(SunInfoHorizontalSpaceDecoration(resources.getDimensionPixelOffset(R.dimen.list_view_cell_spacing)))

        skinExposureList.addItemDecoration(SkinExposureVerticalSpaceDecoration(resources.getDimensionPixelOffset(R.dimen.list_view_cell_spacing)))

        launchAppDetailsButton.setOnClickListener { launchAppDetailsActivity() }

        settingsButton.setOnClickListener()
        {
            parentFragmentManager.setFragmentResult(SettingsFragment.LAUNCH_SETTINGS_FRAGMENT_KEY, bundleOf())
        }

        parentFragmentManager.setFragmentResultListener(SettingsFragment.SETTINGS_UPDATED_FRAGMENT_RESULT_KEY, this)
        { _, bundle ->
            if (!bundle.isEmpty)
            {
                // Update widgets with new settings
                val intent = Intent(requireContext(), SmallUVDisplay::class.java).apply()
                {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getWidgetIDs())
                    putExtras(bundle)
                }

                requireContext().sendBroadcast(intent)
            }
        }
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

        if (getWidgetIDs().isEmpty())
        {
            settingsButton.visibility = View.INVISIBLE
        }
        else
        {
            settingsButton.visibility = View.VISIBLE
        }
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
            DiskRepository.readLatestUV(requireContext().getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE))?.let()
            {
                viewModel.uvData = it
                displayNewUVData(it)
            }
        } catch (e: FileNotFoundException){ }
    }

    private fun prepareUVDataRequest()
    {
        viewModel.lastObserving = UVDataWorker.initiateOneTimeWorker(requireContext())
        viewModel.lastObserving?.observe(this, this)
    }

    override fun onRefresh()
    {
        if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
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
                requireActivity().runOnUiThread()
                {
                    newUVDataReceived(lUVData)

                    if (layout.isRefreshing)
                    {
                        layout.isRefreshing = false
                    }

                    viewModel.lastObserving?.removeObserver(this)

                    DiskRepository.writeLatestUV(lUVData, requireContext().getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE))

                    val ids = getWidgetIDs()
                    if (ids.isNotEmpty())
                    {
                        // Update all widgets
                        val intent = Intent(requireContext(), SmallUVDisplay::class.java).apply()
                        {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(UVData.UV_DATA_KEY, lUVData)
                            putExtra(SmallUVDisplay.START_BACKGROUND_WORK_KEY, true) // Will result in background updates if the relevant permission is granted
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                            putExtra(SmallUVDisplay.SET_RECEIVING_SCREEN_UNLOCK_KEY, true) //TODO() To be sent from an intent from a settings screen
                            putExtra(SmallUVDisplay.SET_USE_PERIODIC_WORK_KEY, false) //TODO() To be sent from an intent from a settings screen
                        }

                        requireContext().sendBroadcast(intent)
                    }
                }
            }?.fail()
            { errorStatus ->
                requireActivity().runOnUiThread()
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
        layout.setBackgroundColor(resources.getColor(R.color.uv_low, requireContext().theme))

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
        layout.setBackgroundColor(resources.getColor(lUVData.backgroundColorInt, requireContext().theme))

        settingsButton.imageTintList = ColorStateList.valueOf(resources.getColor(lUVData.textColorInt, requireContext().theme))

        uvValue.visibility = View.VISIBLE
        uvValue.text = resources.getString(R.string.uv_value, lUVData.uv)
        uvValue.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))

        uvText.visibility = View.VISIBLE
        uvText.text = resources.getText(lUVData.uvLevelTextInt)
        uvText.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))

        maxUV.visibility = View.VISIBLE
        maxUV.text = resources.getString(R.string.max_uv, lUVData.uvMax)
        maxUV.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))

        maxUVTime.visibility = View.VISIBLE
        maxUVTime.text = resources.getString(R.string.max_uv_time, preferredTimeString(requireContext(), lUVData.uvMaxTime))
        maxUVTime.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))

        lastUpdated.visibility = View.VISIBLE
        lastUpdated.text = resources.getString(R.string.latest_update, preferredTimeString(requireContext(), lUVData.uvTime))
        lastUpdated.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))

        sunProgressLabel.visibility = View.VISIBLE
        sunProgressLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
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
        sunInfoListTitleLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))

        sunInfoListSubLabel.visibility = View.VISIBLE
        sunInfoListSubLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))

        skinExposureLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        lUVData.safeExposure?.entries?.toList()?.let()
        {
            skinExposureList.adapter = SkinExposureAdapter(it, lUVData.textColorInt)
            skinExposureList.layoutManager = GridLayoutManager(requireContext(), 2)
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
        sunInfoList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        sunInfoList.scrollToPosition(bestScrollPosition)

        appStatusInformation.visibility = View.INVISIBLE
        launchAppDetailsButton.visibility = View.INVISIBLE
    }

    private fun sunTimeOnClick(sunTimeData: SunInfo.SunTimeData)
    {
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle(sunTimeData.nameResourceInt)
        builder.setIcon(sunTimeData.imageResourceInt)
        builder.setMessage(SunInfo.sunTimeDescription(sunTimeData.nameResourceInt))
        builder.setPositiveButton(R.string.close_window) { _, _ -> }

        builder.create().show()
    }

    private fun getWidgetIDs(): IntArray = AppWidgetManager.getInstance(requireContext()).getAppWidgetIds(ComponentName(requireContext(), SmallUVDisplay::class.java))

    private fun launchAppDetailsActivity()
    {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply()
        {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    companion object
    {
        fun newInstance(fragmentArguments: Bundle?) = CurrentUVFragment().apply { arguments = fragmentArguments }
    }
}