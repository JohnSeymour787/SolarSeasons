package com.johnseymour.solarseasons

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    companion object
    {
        const val LAUNCH_SETTINGS_FRAGMENT_KEY = "launch_settings_fragment_key"
        fun newInstance() = SettingsFragment()
    }
}