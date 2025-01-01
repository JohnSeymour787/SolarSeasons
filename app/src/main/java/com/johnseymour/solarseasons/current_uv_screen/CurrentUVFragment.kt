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
import com.johnseymour.solarseasons.*
import com.johnseymour.solarseasons.current_uv_screen.uv_forecast.UVForecastAdapter
import com.johnseymour.solarseasons.databinding.FragmentCurrentUvBinding
import com.johnseymour.solarseasons.models.*
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment
import com.johnseymour.solarseasons.settings_screen.SettingsFragment
import java.io.FileNotFoundException
import java.time.ZonedDateTime

class CurrentUVFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener
{
    private var _binding: FragmentCurrentUvBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        _binding = FragmentCurrentUvBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    private val viewModel by lazy()
    {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private val localBroadcastManager by lazy { LocalBroadcastManager.getInstance(requireContext()) }

    private val uvDataForegroundBroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            if (intent.action == UVData.UV_DATA_UPDATED)
            {
                if (binding.layout.isRefreshing)
                {
                    binding.layout.isRefreshing = false
                }

                intent.parcelableCompat<UVData>(UVData.UV_DATA_KEY)?.let()
                {
                    intent.parcelableArrayListCompat<UVForecastData>(UVForecastData.UV_FORECAST_LIST_KEY)?.toList()?.let()
                    { forecastData ->
                        viewModel.uvForecastData = forecastData
                    }

                    newUVDataReceived(it)
                }

                intent.serializableCompat<ErrorStatus>(ErrorStatus.ERROR_STATUS_KEY)?.let()
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

        val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION]?.let()
            { granted ->
                if (!granted)
                {
                    binding.appStatusInformation.text = getString(R.string.location_permission_generic_rationale)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                permissions[Manifest.permission.POST_NOTIFICATIONS]?.let()
                { granted ->
                    if (!granted)
                    {
                        defaultPreferences
                            .edit()
                            .putBoolean(Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_KEY, false)
                            .apply()
                    }
                }
            }
        }

        val notificationSettingsOn = defaultPreferences.getBoolean(Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_KEY, true)

        val permissionsToRequest = mutableListOf<String>()

        when
        {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationSettingsOn.not() -> { }

            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->
            {
                context?.showNotificationsRationaleDialogue()
            }

            requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
            {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        when
        {
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) ->
            {
                binding.appStatusInformation.text = getString(R.string.location_permission_generic_rationale)
            }

            requireContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ->
            {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
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

                    binding.appStatusInformation.text = getString(R.string.location_permission_background_rationale, getString(R.string.activity_status_information_swipe_hint), backgroundOptionLabel)

                    binding.launchAppDetailsButton.visibility = View.VISIBLE
                }
            }
        }

        if (permissionsToRequest.isNotEmpty())
        {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }

        initialiseMemoryPreferences()

        arguments?.serializableCompat<ErrorStatus>(ErrorStatus.ERROR_STATUS_KEY)?.let()
        {
            displayError(it)
        } ?: arguments?.parcelableCompat<UVData>(UVData.UV_DATA_KEY)?.let()
        {
            viewModel.readForecastFromDisk(requireContext().getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE))
            newUVDataReceived(it)
        } ?: updateUVDataFromDisk()

        binding.layout.setOnRefreshListener(this)
        binding.layout.setProgressViewOffset(true, resources.getDimensionPixelOffset(R.dimen.activity_swipe_refresh_offset_start), resources.getDimensionPixelOffset(R.dimen.activity_swipe_refresh_offset_end))

        binding.sunInfoList.addItemDecoration(SunInfoHorizontalSpaceDecoration(resources.getDimensionPixelOffset(R.dimen.list_view_cell_spacing)))

        binding.skinExposureList.addItemDecoration(SkinExposureVerticalSpaceDecoration(resources.getDimensionPixelOffset(R.dimen.list_view_cell_spacing)))

        binding.launchAppDetailsButton.setOnClickListener { launchAppDetailsActivity() }

        binding.settingsButton.setOnClickListener()
        {
            parentFragmentManager.setFragmentResult(SettingsFragment.LAUNCH_SETTINGS_FRAGMENT_KEY, bundleOf())
        }

        parentFragmentManager.setFragmentResultListener(PreferenceScreenFragment.APP_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, this)
        { _, bundle ->

            if (bundle.containsKey(Constants.SharedPreferences.APP_THEME_KEY)) // Indicates that the theme was changed
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

            bundle.serializableCompat<Boolean>(Constants.SharedPreferences.APP_LAUNCH_AUTO_REQUEST_KEY)?.let()
            { autoRequestValue ->
                viewModel.shouldRequestUVUpdateOnLaunch = autoRequestValue
            }

            bundle.serializableCompat<Boolean>(Constants.SharedPreferences.CLOUD_COVER_FACTOR_KEY)?.let()
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
                // Manually simulate swiping down to start a new request
                binding.layout.isRefreshing = true
                viewModel.updateCurrentUV(requireContext(), false)
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

        } catch (_: FileNotFoundException){ }
    }

    override fun onRefresh()
    {
        if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            binding.appStatusInformation.visibility = View.INVISIBLE
            binding.launchAppDetailsButton.visibility = View.INVISIBLE
            viewModel.updateCurrentUV(requireContext(), true)
        }
        else
        {
            binding.appStatusInformation.visibility = View.INVISIBLE
            binding.launchAppDetailsButton.visibility = View.INVISIBLE
            viewModel.updateCurrentUV(requireContext(), false)
        }
    }

    private fun displayError(errorStatus: ErrorStatus)
    {
        binding.appStatusInformation.text = errorStatus.statusString(resources)
        binding.appStatusInformation.visibility = View.VISIBLE

        if (PreferenceScreenFragment.useCustomTheme)
        {
            binding.layout.setBackgroundColor(resources.getColor(R.color.uv_low, requireContext().theme))
        }

        binding.uvValue.visibility = View.GONE
        binding.uvText.visibility = View.GONE
        binding.maxUV.visibility = View.GONE
        binding.maxUVTime.visibility = View.GONE
        binding.cloudFactoredUVText.visibility = View.GONE
        binding.cloudCoverLevelText.visibility = View.GONE
        binding.cityName.visibility = View.GONE
        binding.lastUpdated.visibility = View.GONE
        binding.sunProgressLabel.visibility = View.GONE
        binding.sunProgress.visibility = View.GONE
        binding.sunProgressLabelBackground.visibility = View.GONE
        binding.sunInfoListTitleLabel.visibility = View.GONE
        binding.sunInfoListSubLabel.visibility = View.GONE
        binding.sunInfoList.visibility = View.GONE
        binding.sunInfoListBackground.visibility = View.GONE
        binding.skinExposureLabel.visibility = View.GONE
        binding.skinExposureList.visibility = View.GONE
        binding.skinExposureBackground.visibility = View.GONE
        binding.uvForecastLabel.visibility = View.GONE
        binding.uvForecastList.visibility = View.GONE
        binding.uvForecastBackground.visibility = View.GONE
    }

    private fun displayNewUVData(lUVData: UVData)
    {
        binding.uvValue.visibility = View.VISIBLE
        binding.uvValue.text = resources.getString(R.string.uv_value, lUVData.uv)

        binding.uvText.visibility = View.VISIBLE
        binding.uvText.text = resources.getText(lUVData.uvLevelTextInt)

        lUVData.cloudFactoredUV?.let()
        {
            binding.cloudFactoredUVText.visibility = View.VISIBLE
            binding.cloudFactoredUVText.text = resources.getString(R.string.estimated_uv_value, it)
        } ?: run { binding.cloudFactoredUVText.visibility = View.GONE }

        lUVData.cloudCoverTextInt?.let()
        {
            binding.cloudCoverLevelText.visibility = View.VISIBLE
            binding.cloudCoverLevelText.text = resources.getString(it)
        } ?: run { binding.cloudCoverLevelText.visibility = View.GONE }

        lUVData.cityName?.let()
        {
            binding.cityName.visibility = View.VISIBLE
            binding.cityName.text = lUVData.cityName
        } ?: run { binding.cityName.visibility = View.GONE }

        binding.maxUV.visibility = View.VISIBLE
        binding.maxUV.text = resources.getString(R.string.max_uv, lUVData.uvMax)

        binding.maxUVTime.visibility = View.VISIBLE
        binding.maxUVTime.text = resources.getString(R.string.max_uv_time, preferredTimeString(requireContext(), lUVData.uvMaxTime))

        binding.lastUpdated.visibility = View.VISIBLE
        binding.lastUpdated.text = resources.getString(R.string.latest_update, preferredTimeString(requireContext(), lUVData.uvTime))

        binding.sunProgressLabel.visibility = View.VISIBLE
        binding.sunProgressLabel.setText(R.string.sun_progress_label)
        if (binding.sunProgressLabel.lineCount > 1)
        {
            binding.sunProgressLabel.setText(R.string.sun_progress_label_shortened)
        }

        binding.sunProgress.progress = lUVData.sunProgressPercent
        binding.sunProgress.visibility = View.VISIBLE
        binding.sunProgressLabelBackground.visibility = View.VISIBLE

        binding.sunInfoListBackground.visibility = View.VISIBLE

        binding.sunInfoListTitleLabel.visibility = View.VISIBLE

        binding.sunInfoListSubLabel.visibility = View.VISIBLE

        lUVData.safeExposure?.entries?.toList()?.let()
        {
            binding.skinExposureList.adapter = SkinExposureAdapter(it, lUVData.textColorInt)
            binding.skinExposureList.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.skinExposureLabel.visibility = View.VISIBLE
            binding.skinExposureList.visibility = View.VISIBLE
            binding.skinExposureBackground.visibility = View.VISIBLE
        } ?: run()
        {
            binding.skinExposureLabel.visibility = View.GONE
            binding.skinExposureList.visibility = View.GONE
            binding.skinExposureBackground.visibility = View.GONE
        }

        val sortedSolarTimes = lUVData.sunInfo.timesArray.sortedWith { a, b -> a.time.compareTo(b.time) }
        // Calculate the index for the List of times that is closest to now, use this to set the default scroll position
        val timeNow = ZonedDateTime.now()
        var sunInfoBestScrollPosition = 0
        while ((sunInfoBestScrollPosition < sortedSolarTimes.size - 1) && (timeNow.isAfter(sortedSolarTimes[sunInfoBestScrollPosition].time)))
        {
            sunInfoBestScrollPosition++
        }

        binding.sunInfoList.visibility = View.VISIBLE
        binding.sunInfoList.adapter = SunInfoAdapter(sortedSolarTimes, lUVData.textColorInt, ::sunTimeOnClick, viewModel.calculateSunTimesCellWidth(requireContext()))
        binding.sunInfoList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.sunInfoList.scrollToPosition(sunInfoBestScrollPosition)

        viewModel.uvForecastData?.let()
        {
            // New daily request can fail, don't want to use yesterday's forecast
            if ((it.isEmpty()) || (viewModel.isForecastNotCurrent()))
            {
                binding.uvForecastLabel.visibility = View.GONE
                binding.uvForecastList.visibility = View.GONE
                binding.uvForecastBackground.visibility = View.GONE
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

                    forecastBestScrollPosition = 0
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

            binding.uvForecastBackground.visibility = View.VISIBLE
            binding.uvForecastLabel.visibility = View.VISIBLE
            binding.uvForecastList.visibility = View.VISIBLE

            binding.uvForecastList.adapter = UVForecastAdapter(lForecastList, lUVData.textColorInt, viewModel.calculateUVForecastCellWidth(requireContext()))
            binding.uvForecastList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.uvForecastList.scrollToPosition(forecastBestScrollPosition)
        } ?: run()
        {
            binding.uvForecastLabel.visibility = View.GONE
            binding.uvForecastList.visibility = View.GONE
            binding.uvForecastBackground.visibility = View.GONE
        }

        binding.appStatusInformation.visibility = View.INVISIBLE
        binding.launchAppDetailsButton.visibility = View.INVISIBLE

        if (PreferenceScreenFragment.useCustomTheme)
        {
            updateDynamicColours(lUVData)
        }
        else
        {
            // If using the default app theme, only apply colours to the UV value and the progress bar
            binding.uvValue.setTextColor(resources.getColor(lUVData.backgroundColorInt, requireContext().theme))
            binding.sunProgress.progressDrawable.setTint(resources.getColor(lUVData.backgroundColorInt, requireContext().theme))
        }
    }

    /**
     * One-time setting of text and background colours for when the non UV-based colour option is selected
     */
    private fun setStaticThemeColours()
    {
        binding.layout.setBackgroundColor(requireContext().resolveColourAttr(android.R.attr.windowBackground))

        val primaryTextColourInt = requireContext().resolveColourAttr(android.R.attr.textColorPrimary)
        binding.settingsButton.imageTintList = ColorStateList.valueOf(primaryTextColourInt)

        binding.uvText.setTextColor(primaryTextColourInt)
        binding.cloudFactoredUVText.setTextColor(primaryTextColourInt)
        binding.cloudCoverLevelText.setTextColor(primaryTextColourInt)
        binding.cityName.setTextColor(primaryTextColourInt)
        binding.maxUV.setTextColor(primaryTextColourInt)
        binding.maxUVTime.setTextColor(primaryTextColourInt)
        binding.lastUpdated.setTextColor(primaryTextColourInt)
        binding.sunProgressLabel.setTextColor(primaryTextColourInt)
        binding.sunInfoListTitleLabel.setTextColor(primaryTextColourInt)
        binding.sunInfoListSubLabel.setTextColor(primaryTextColourInt)
        binding.skinExposureLabel.setTextColor(primaryTextColourInt)
        binding.uvForecastLabel.setTextColor(primaryTextColourInt)
        binding.appStatusInformation.setTextColor(primaryTextColourInt)

        enableLightStatusBar(requireActivity().window.decorView, resources.configuration)

        binding.skinExposureBackground.background.setTint(resources.getColor(R.color.section_background_transparent_colour, requireContext().theme))
        binding.sunInfoListBackground.background.setTint(resources.getColor(R.color.section_background_transparent_colour, requireContext().theme))
        binding.sunProgressLabelBackground.background.setTint(resources.getColor(R.color.section_background_transparent_colour, requireContext().theme))
        binding.uvForecastBackground.background.setTint(resources.getColor(R.color.section_background_transparent_colour, requireContext().theme))
    }

    /**
     * Sets all one-time colours for the dynamic theme (all others are dynamically updated in #updateDynamicColours())
     */
    private fun setDynamicThemeColours()
    {
        binding.sunProgress.progressDrawable.setTint(resources.getColor(R.color.progress_bar_tint, requireContext().theme))

        binding.skinExposureBackground.background.setTint(resources.getColor(R.color.white, requireContext().theme))
        binding.sunInfoListBackground.background.setTint(resources.getColor(R.color.white, requireContext().theme))
        binding.sunProgressLabelBackground.background.setTint(resources.getColor(R.color.white, requireContext().theme))
        binding.uvForecastBackground.background.setTint(resources.getColor(R.color.white, requireContext().theme))

        binding.appStatusInformation.setTextColor(resources.getColor(R.color.dark_text, requireContext().theme))
        binding.settingsButton.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.uv_low, requireContext().theme))
    }

    private fun updateDynamicColours(lUVData: UVData)
    {
        binding.layout.setBackgroundColor(resources.getColor(lUVData.backgroundColorInt, requireContext().theme))

        binding.settingsButton.imageTintList = ColorStateList.valueOf(resources.getColor(lUVData.textColorInt, requireContext().theme))

        binding.uvValue.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))

        binding.uvText.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.maxUV.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.cloudFactoredUVText.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.cloudCoverLevelText.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.cityName.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.maxUVTime.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.lastUpdated.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.sunProgressLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.sunInfoListTitleLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.sunInfoListSubLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.skinExposureLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
        binding.uvForecastLabel.setTextColor(resources.getColor(lUVData.textColorInt, requireContext().theme))
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

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }

    companion object
    {
        fun newInstance(fragmentArguments: Bundle?) = CurrentUVFragment().apply { arguments = fragmentArguments }
    }
}