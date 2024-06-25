package com.johnseymour.solarseasons.current_uv_screen

import android.Manifest
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
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
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.ListenableWorker.Result
import com.johnseymour.solarseasons.*
import com.johnseymour.solarseasons.UVDataWorker.Companion.INITIATE_BACKGROUND_WORK
import com.johnseymour.solarseasons.current_uv_screen.uv_forecast.UVForecastAdapter
import com.johnseymour.solarseasons.models.*
import com.johnseymour.solarseasons.services.LocationService
import com.johnseymour.solarseasons.services.UVDataUseCase
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment
import com.johnseymour.solarseasons.settings_screen.SettingsFragment
import kotlinx.android.synthetic.main.fragment_current_uv.*
import nl.komponents.kovenant.deferred
import java.io.FileNotFoundException
import java.time.ZonedDateTime

class CurrentUVFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_current_uv, container, false)
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
                if (layout.isRefreshing)
                {
                    layout.isRefreshing = false
                }

                intent.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
                {
                    intent.getParcelableArrayListExtra<UVForecastData>(UVForecastData.UV_FORECAST_LIST_KEY)?.toList()?.let()
                    { forecastData ->
                        viewModel.uvForecastData = forecastData
                    }

                    newUVDataReceived(it)
                }

                (intent.getSerializableExtra(ErrorStatus.ERROR_STATUS_KEY) as? ErrorStatus)?.let()
                { errorStatus ->
                    viewModel.latestError = errorStatus

                    displayError(errorStatus)
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
                if (requireContext().hasWidgets())
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

        initialiseMemoryPreferences()

        (arguments?.getSerializable(ErrorStatus.ERROR_STATUS_KEY) as? ErrorStatus)?.let()
        {
            displayError(it)
        } ?: arguments?.getParcelable<UVData>(UVData.UV_DATA_KEY)?.let()
        {
            viewModel.readForecastFromDisk(requireContext().getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE))
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

        parentFragmentManager.setFragmentResultListener(PreferenceScreenFragment.APP_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, this)
        { _, bundle ->

            if (bundle[Constants.SharedPreferences.APP_THEME_KEY] != null) // Indicates that the theme was changed
            {
                if (PreferenceScreenFragment.useCustomTheme)
                {
                    setDynamicThemeColours()
                }
                else
                {
                    setStaticThemeColours()
                }

                viewModel.uvData?.let { displayNewUVData(it) }

                // Update widgets to redraw
                broadcastBundleToWidgets(bundleOf())
            }

            (bundle[Constants.SharedPreferences.APP_LAUNCH_AUTO_REQUEST_KEY] as? Boolean)?.let()
            { autoRequestValue ->
                viewModel.shouldRequestUVUpdateOnLaunch = autoRequestValue
            }

            (bundle[Constants.SharedPreferences.CLOUD_COVER_FACTOR_KEY] as? Boolean)?.let()
            { cloudCoverEnabled ->
                if (!cloudCoverEnabled) // Remove the cloudCover data and refresh the UI
                {
                    viewModel.uvData?.cloudCover = null
                    viewModel.uvData?.let()
                    {
                        displayNewUVData(it)
                        viewModel.saveUVToDisk(requireContext())
                    }
                }
            }
        }

        parentFragmentManager.setFragmentResultListener(PreferenceScreenFragment.WIDGET_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, this)
        { _, bundle ->
            if (!bundle.isEmpty)
            {
                broadcastBundleToWidgets(bundle)
            }
        }

        if (PreferenceScreenFragment.useCustomTheme)
        {
            disableLightStatusBar(requireActivity().window.decorView)
        }
        else
        {
            enableLightStatusBar(requireActivity().window.decorView, resources.configuration)
        }
    }
    /**
     * Reads the shared preferences for settings used by this fragment and updates the relevant
     *  memory variables with these values for quicker access later
     */
    private fun initialiseMemoryPreferences()
    {
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())

        viewModel.shouldRequestUVUpdateOnLaunch = preferenceManager.getBoolean(Constants.SharedPreferences.APP_LAUNCH_AUTO_REQUEST_KEY, true)

        val themePreferenceString = preferenceManager.getString(Constants.SharedPreferences.APP_THEME_KEY, null)
        if (themePreferenceString == Constants.SharedPreferences.CUSTOM_APP_THEME_VALUE)
        {
            PreferenceScreenFragment.useCustomTheme = true
            setDynamicThemeColours()
        }

        PreferenceScreenFragment.useManualLocation = preferenceManager.getBoolean(Constants.SharedPreferences.MANUAL_LOCATION_ENABLED_KEY, false)
    }

    private fun broadcastBundleToWidgets(bundle: Bundle)
    {
        // Update widgets with new settings
        val intent = Intent(requireContext(), SmallUVDisplay::class.java).apply()
        {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, requireContext().getWidgetIDs())
            putExtras(bundle)
        }

        requireContext().sendBroadcast(intent)
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

            if (!viewModel.shouldRequestUVUpdateOnLaunch) { return@run }

            if ((viewModel.uvData?.minutesSinceDataRetrieved ?: 0) > Constants.MINIMUM_APP_FOREGROUND_REFRESH_TIME)
            {
                onRefresh() // Manually simulate swiping down to start a new request
                layout.isRefreshing = true
            }
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

    /**
     * Attempts to read the last saved UVData from persistent storage and updates the display if successful
     */
    private fun updateUVDataFromDisk()
    {
        try
        {
            val dataSharedPreferences = requireContext().getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE)

            viewModel.readForecastFromDisk(dataSharedPreferences)
            viewModel.readUVFromDisk(dataSharedPreferences)

        } catch (e: FileNotFoundException){ }
    }

    override fun onRefresh()
    {
        if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            appStatusInformation.visibility = View.INVISIBLE
            launchAppDetailsButton.visibility = View.INVISIBLE
            UVDataWorker.initiateOneTimeWorker(requireContext(), viewModel.isForecastNotCurrent())
        }
        else if (layout.isRefreshing)
        {
            layout.isRefreshing = false
        }
    }

    private fun getCurrentUVData()
    {
        // Need to initialise this here because the service is created asynchronously
   //     LocationService.uvDataDeferred = deferred()

        val locationServiceIntent = LocationService.createServiceIntent(requireContext())
        if (inputData.getBoolean(LocationService.FIRST_DAILY_REQUEST_KEY, false))
        {
            locationServiceIntent.putExtra(LocationService.FIRST_DAILY_REQUEST_KEY, true)
        }

        applicationContext.startForegroundService(locationServiceIntent)

        val widgetIds = applicationContext.getWidgetIDs()

        val widgetIntent = Intent(applicationContext, SmallUVDisplay::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)

        if (inputData.getBoolean(INITIATE_BACKGROUND_WORK, false))
        {
            // For coming from immediate request (no delay), will result in background updates if the relevant permission is granted
            widgetIntent.putExtra(SmallUVDisplay.START_BACKGROUND_WORK_KEY, true)
        }
        else
        {
            // The widget is not responsible for starting periodic work except for special situations
            widgetIntent.putExtra(SmallUVDisplay.START_BACKGROUND_WORK_KEY, !SmallUVDisplay.usePeriodicWork)
        }

        val activityIntent = Intent(requireContext(), MainActivity::class.java)
            .setAction(UVData.UV_DATA_UPDATED)

        UVDataUseCase(requireContext()).getUVData(UVDataUseCase.UVLocationData(1,1,1), false, viewModel.isForecastNotCurrent(), false).success()
        {
            val context = context ?: return@success

            val dataSharedPreferences = context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)
            DiskRepository.writeLatestUV(it.uvData, dataSharedPreferences)

            it.forecast?.let()
            { forecastData ->
                DiskRepository.writeLatestForecastList(forecastData, dataSharedPreferences)
                activityIntent.putParcelableArrayListExtra(UVForecastData.UV_FORECAST_LIST_KEY, ArrayList(forecastData))
            }

            widgetIntent.putExtra(UVData.UV_DATA_KEY, it.uvData)
            activityIntent.putExtra(UVData.UV_DATA_KEY, it.uvData)

            context.sendBroadcast(widgetIntent)
            LocalBroadcastManager.getInstance(context).sendBroadcast(activityIntent)
        }

        LocationService.uvDataPromise?.success()
        {


            result.set(Result.success())
        }?.fail()
        {
            widgetIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)
            activityIntent.putExtra(ErrorStatus.ERROR_STATUS_KEY, it)

            if (isFailureError(it))
            {
                // Only if not going to retry later show the error message
                context?.sendBroadcast(widgetIntent)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(activityIntent)
            }
        }
    }

    private fun displayError(errorStatus: ErrorStatus)
    {
        appStatusInformation.text = errorStatus.statusString(resources)
        appStatusInformation.visibility = View.VISIBLE

        if (PreferenceScreenFragment.useCustomTheme)
        {
            layout.setBackgroundColor(resources.getColor(R.color.uv_low, requireContext().theme))
        }

        uvValue.visibility = View.GONE
        uvText.visibility = View.GONE
        maxUV.visibility = View.GONE
        maxUVTime.visibility = View.GONE
        cloudFactoredUVText.visibility = View.GONE
        cloudCoverLevelText.visibility = View.GONE
        cityName.visibility = View.GONE
        lastUpdated.visibility = View.GONE
        sunProgressLabel.visibility = View.GONE
        sunProgress.visibility = View.GONE
        sunProgressLabelBackground.visibility = View.GONE
        sunInfoListTitleLabel.visibility = View.GONE
        sunInfoListSubLabel.visibility = View.GONE
        sunInfoList.visibility = View.GONE
        sunInfoListBackground.visibility = View.GONE
        skinExposureLabel.visibility = View.GONE
        skinExposureList.visibility = View.GONE
        skinExposureBackground.visibility = View.GONE
        uvForecastLabel.visibility = View.GONE
        uvForecastList.visibility = View.GONE
        uvForecastBackground.visibility = View.GONE
    }

    private fun displayNewUVData(lUVData: UVData)
    {
        uvValue.visibility = View.VISIBLE
        uvValue.text = resources.getString(R.string.uv_value, lUVData.uv)

        uvText.visibility = View.VISIBLE
        uvText.text = resources.getText(lUVData.uvLevelTextInt)

        lUVData.cloudFactoredUV?.let()
        {
            cloudFactoredUVText.visibility = View.VISIBLE
            cloudFactoredUVText.text = resources.getString(R.string.estimated_uv_value, it)
        } ?: run { cloudFactoredUVText.visibility = View.GONE }

        lUVData.cloudCoverTextInt?.let()
        {
            cloudCoverLevelText.visibility = View.VISIBLE
            cloudCoverLevelText.text = resources.getString(it)
        } ?: run { cloudCoverLevelText.visibility = View.GONE }

        lUVData.cityName?.let()
        {
            cityName.visibility = View.VISIBLE
            cityName.text = lUVData.cityName
        } ?: run { cityName.visibility = View.GONE }

        maxUV.visibility = View.VISIBLE
        maxUV.text = resources.getString(R.string.max_uv, lUVData.uvMax)

        maxUVTime.visibility = View.VISIBLE
        maxUVTime.text = resources.getString(R.string.max_uv_time, preferredTimeString(requireContext(), lUVData.uvMaxTime))

        lastUpdated.visibility = View.VISIBLE
        lastUpdated.text = resources.getString(R.string.latest_update, preferredTimeString(requireContext(), lUVData.uvTime))

        sunProgressLabel.visibility = View.VISIBLE
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

        sunInfoListSubLabel.visibility = View.VISIBLE

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
        var sunInfoBestScrollPosition = 0
        while ((sunInfoBestScrollPosition < sortedSolarTimes.size - 1) && (timeNow.isAfter(sortedSolarTimes[sunInfoBestScrollPosition].time)))
        {
            sunInfoBestScrollPosition++
        }

        sunInfoList.visibility = View.VISIBLE
        sunInfoList.adapter = SunInfoAdapter(sortedSolarTimes, lUVData.textColorInt, ::sunTimeOnClick, viewModel.calculateSunTimesCellWidth(requireContext()))
        sunInfoList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        sunInfoList.scrollToPosition(sunInfoBestScrollPosition)

        viewModel.uvForecastData?.let()
        {
            // New daily request can fail, don't want to use yesterday's forecast
            if ((it.isEmpty()) || (viewModel.isForecastNotCurrent()))
            {
                uvForecastLabel.visibility = View.GONE
                uvForecastList.visibility = View.GONE
                uvForecastBackground.visibility = View.GONE
                return@let
            }

            var forecastBestScrollPosition = 0
            while ((forecastBestScrollPosition < it.size) && (timeNow.isAfter(it[forecastBestScrollPosition].time)))
            {
                forecastBestScrollPosition++
            }

            forecastBestScrollPosition--

            val lForecastList = it.toMutableList()
            val currentData = UVForecastData(lUVData.uv, timeNow, isTimeNow = true)

            // Only add the forecast data if at either list end (to keep symmetrical UV curve)
            when (forecastBestScrollPosition)
            {
                -1 ->
                {
                    // If latest UV data is too old for the forecast, then just add 0 to the start of the forecast
                    if (lUVData.minutesSinceDataRetrieved > Constants.UV_FORECAST_ACCEPTABLE_RECENT_UV_TIME)
                    {
                        lForecastList.add(0, currentData.copy(uv = 0.0F))
                    }
                    else
                    {
                        lForecastList.add(0, currentData)
                    }

                    forecastBestScrollPosition++
                }

                in 0 until it.size-1 -> // Replace the nearest time with the current lUVData.uv
                {
                    // If latest UV data is too old for the forecast, then just change the existing forecast to be "now"
                    if (lUVData.minutesSinceDataRetrieved > Constants.UV_FORECAST_ACCEPTABLE_RECENT_UV_TIME)
                    {
                        lForecastList[forecastBestScrollPosition] = lForecastList[forecastBestScrollPosition].copy(isTimeNow = true)
                    }
                    else
                    {
                        lForecastList[forecastBestScrollPosition] = currentData
                    }
                }

                it.size-1 ->
                {
                    if (lUVData.minutesSinceDataRetrieved > Constants.UV_FORECAST_ACCEPTABLE_RECENT_UV_TIME)
                    {
                        lForecastList.add(currentData.copy(uv = 0.0F))
                    }
                    else
                    {
                        lForecastList.add(currentData)
                    }

                    forecastBestScrollPosition++
                }
            }

            uvForecastBackground.visibility = View.VISIBLE
            uvForecastLabel.visibility = View.VISIBLE
            uvForecastList.visibility = View.VISIBLE

            uvForecastList.adapter = UVForecastAdapter(lForecastList, lUVData.textColorInt, viewModel.calculateUVForecastCellWidth(requireContext()))
            uvForecastList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            uvForecastList.scrollToPosition(forecastBestScrollPosition)
        } ?: run()
        {
            uvForecastLabel.visibility = View.GONE
            uvForecastList.visibility = View.GONE
            uvForecastBackground.visibility = View.GONE
        }

        appStatusInformation.visibility = View.INVISIBLE
        launchAppDetailsButton.visibility = View.INVISIBLE

        if (PreferenceScreenFragment.useCustomTheme)
        {
            updateDynamicColours(lUVData)
        }
        else
        {
            // If using the default app theme, only apply colours to the UV value and the progress bar
            uvValue.setTextColor(resources.getColor(lUVData.backgroundColorInt, requireContext().theme))
            sunProgress.progressDrawable.setTint(resources.getColor(lUVData.backgroundColorInt, requireContext().theme))
        }
    }

    /**
     * One-time setting of text and background colours for when the non UV-based colour option is selected
     */
    private fun setStaticThemeColours()
    {
        layout.setBackgroundColor(requireContext().resolveColourAttr(android.R.attr.windowBackground))

        val primaryTextColourInt = requireContext().resolveColourAttr(android.R.attr.textColorPrimary)
        settingsButton.imageTintList = ColorStateList.valueOf(primaryTextColourInt)

        uvText.setTextColor(primaryTextColourInt)
        cloudFactoredUVText.setTextColor(primaryTextColourInt)
        cloudCoverLevelText.setTextColor(primaryTextColourInt)
        cityName.setTextColor(primaryTextColourInt)
        maxUV.setTextColor(primaryTextColourInt)
        maxUVTime.setTextColor(primaryTextColourInt)
        lastUpdated.setTextColor(primaryTextColourInt)
        sunProgressLabel.setTextColor(primaryTextColourInt)
        sunInfoListTitleLabel.setTextColor(primaryTextColourInt)
        sunInfoListSubLabel.setTextColor(primaryTextColourInt)
        skinExposureLabel.setTextColor(primaryTextColourInt)
        uvForecastLabel.setTextColor(primaryTextColourInt)
        appStatusInformation.setTextColor(primaryTextColourInt)

        enableLightStatusBar(requireActivity().window.decorView, resources.configuration)

        skinExposureBackground.background.setTint(resources.getColor(R.color.section_background_transparent_colour, requireContext().theme))
        sunInfoListBackground.background.setTint(resources.getColor(R.color.section_background_transparent_colour, requireContext().theme))
        sunProgressLabelBackground.background.setTint(resources.getColor(R.color.section_background_transparent_colour, requireContext().theme))
        uvForecastBackground.background.setTint(resources.getColor(R.color.section_background_transparent_colour, requireContext().theme))
    }

    /**
     * Sets all one-time colours for the dynamic theme (all others are dynamically updated in #updateDynamicColours())
     */
    private fun setDynamicThemeColours()
    {
        sunProgress.progressDrawable.setTint(resources.getColor(R.color.progress_bar_tint, requireContext().theme))

        skinExposureBackground.background.setTint(resources.getColor(R.color.white, requireContext().theme))
        sunInfoListBackground.background.setTint(resources.getColor(R.color.white, requireContext().theme))
        sunProgressLabelBackground.background.setTint(resources.getColor(R.color.white, requireContext().theme))
        uvForecastBackground.background.setTint(resources.getColor(R.color.white, requireContext().theme))

        appStatusInformation.setTextColor(resources.getColor(R.color.dark_text, requireContext().theme))
        settingsButton.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.uv_low, requireContext().theme))
    }

    private fun updateDynamicColours(lUVData: UVData)
    {
        layout.setBackgroundColor(resources.getColor(lUVData.backgroundColorInt, requireContext().theme))

        settingsButton.imageTintList = ColorStateList.valueOf(resources.getColor(lUVData.textColorInt, requireContext().theme))

        uvValue.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))

        uvText.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        maxUV.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        cloudFactoredUVText.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        cloudCoverLevelText.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        cityName.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        maxUVTime.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        lastUpdated.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        sunProgressLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        sunInfoListTitleLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        sunInfoListSubLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        skinExposureLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        uvForecastLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
   }

    private fun sunTimeOnClick(sunTimeData: SunInfo.SunTimeData)
    {
        val builder = AlertDialog.Builder(requireContext(), requireContext().getThemeForDeviceDefaultDialogAlert())

        builder.setTitle(sunTimeData.nameResourceInt)
        builder.setIcon(sunTimeData.imageResourceInt)
        builder.setMessage(SunInfo.sunTimeDescription(sunTimeData.nameResourceInt))
        builder.setPositiveButton(R.string.close_window) { _, _ -> }

        val alert = builder.create()

        // Force set text colour because some OS's don't properly theme the button for dark mode
        alert.setOnShowListener()
        {
            alert.getButton(AlertDialog.BUTTON_POSITIVE)?.apply()
            {
                setTextColor(requireContext().theme.textColorPrimary())
            }
        }

        alert.show()
    }

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