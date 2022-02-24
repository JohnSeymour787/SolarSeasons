package com.johnseymour.solarseasons

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat()
{
    private lateinit var viewModel: SettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        savedInstanceState?.remove("cae")
        savedInstanceState?.containsKey("cae")

        savedInstanceState?.isEmpty
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        setFragmentResult("", viewModel.settingsBundle)
    }


    companion object
    {
        const val LAUNCH_SETTINGS_FRAGMENT_KEY = "launch_settings_fragment_key"
    }
}