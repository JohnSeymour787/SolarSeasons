package com.johnseymour.solarseasons.settings_screen

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.johnseymour.solarseasons.*
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

        childFragmentManager.setFragmentResultListener(PreferenceScreenFragment.WIDGET_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, this)
        { _, bundle ->
            parentFragmentManager.setFragmentResult(PreferenceScreenFragment.WIDGET_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, bundle)
        }

        childFragmentManager.setFragmentResultListener(PreferenceScreenFragment.APP_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, this)
        { _, bundle ->
            parentFragmentManager.setFragmentResult(PreferenceScreenFragment.APP_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, bundle)
        }

        stopBackgroundWorkButton.setOnClickListener()
        {
            val lApplicationContext = requireContext().applicationContext
            UVDataWorker.cancelWorker(lApplicationContext)
            UVDataWorker.stopLocationService(lApplicationContext)

            Toast.makeText(lApplicationContext, R.string.settings_fragment_stop_background_success, Toast.LENGTH_LONG).show()
        }

        if (PreferenceScreenFragment.useCustomTheme)
        {
            enableLightStatusBar(requireActivity().window.decorView, resources.configuration)
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (requireContext().hasWidgets())
        {
            backgroundIssuesTextBackground.visibility = View.VISIBLE
            backgroundIssuesText.visibility = View.VISIBLE
            stopBackgroundWorkButton.visibility = View.VISIBLE
        }
        else
        {
            backgroundIssuesTextBackground.visibility = View.GONE
            backgroundIssuesText.visibility = View.GONE
            stopBackgroundWorkButton.visibility = View.GONE
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()

        if (PreferenceScreenFragment.useCustomTheme)
        {
            // Disable the light-mode status bar when leaving this screen
            disableLightStatusBar(requireActivity().window.decorView)
        }
    }

    companion object
    {
        const val LAUNCH_SETTINGS_FRAGMENT_KEY = "launch_settings_fragment_key"
        fun newInstance() = SettingsFragment()
    }
}