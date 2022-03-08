package com.johnseymour.solarseasons

import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.setFragmentResultListener(PreferenceScreenFragment.PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, this)
        { _, bundle ->
            parentFragmentManager.setFragmentResult(PreferenceScreenFragment.PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, bundle)
        }

        stopBackgroundWorkButton.setOnClickListener()
        {
            val lApplicationContext = requireContext().applicationContext
            UVDataWorker.cancelWorker(lApplicationContext)
            UVDataWorker.stopLocationService(lApplicationContext)

            Toast.makeText(lApplicationContext, R.string.settings_fragment_stop_background_success, Toast.LENGTH_LONG).show()
        }

        // If not in dark mode, enable the light-mode status bar for this screen only
        val lDecorView = requireActivity().window.decorView
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
        {
            if (!resources.configuration.isNightModeActive)
            {
                lDecorView.windowInsetsController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            }
        }
        else
        {
            if (resources.configuration.uiMode == Configuration.UI_MODE_NIGHT_NO)
            {
                @Suppress("DEPRECATION")
                lDecorView.systemUiVisibility = lDecorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()

        // If wasn't in dark mode, disable the light-mode status bar when leave this screen
        val lDecorView = requireActivity().window.decorView
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
        {
            if (!resources.configuration.isNightModeActive)
            {
                lDecorView.windowInsetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            }
        }
        else
        {
            if (resources.configuration.uiMode != Configuration.UI_MODE_NIGHT_YES)
            {
                @Suppress("DEPRECATION")
                lDecorView.systemUiVisibility = lDecorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    companion object
    {
        const val LAUNCH_SETTINGS_FRAGMENT_KEY = "launch_settings_fragment_key"
        fun newInstance() = SettingsFragment()
    }
}