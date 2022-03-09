package com.johnseymour.solarseasons

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceFragmentCompat
import androidx.core.os.bundleOf
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat

class PreferenceScreenFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener
{
    private lateinit var subscribeScreenUnlockPreference: SwitchPreferenceCompat
    private lateinit var workTypePreference: ListPreference
    private lateinit var backgroundRefreshRatePreference: ListPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        subscribeScreenUnlockPreference = findPreference(Constants.SharedPreferences.SUBSCRIBE_SCREEN_UNLOCK_KEY)!!
        workTypePreference = findPreference(Constants.SharedPreferences.WORK_TYPE_KEY)!!
        backgroundRefreshRatePreference = findPreference(Constants.SharedPreferences.BACKGROUND_REFRESH_RATE_KEY)!!

        // Doing this here as well as onResume to ensure that the visibility is set quick enough to avoid a laggy appearance
        //  under most circumstances
        initialiseWidgetPreferences()
    }

    private fun initialiseWidgetPreferences()
    {
        if (requireContext().hasWidgets())
        {
            subscribeScreenUnlockPreference.isVisible = true
            workTypePreference.isVisible = true
            backgroundRefreshRatePreference.isVisible = true
        }
        else
        {
            subscribeScreenUnlockPreference.isVisible = false
            workTypePreference.isVisible = false
            backgroundRefreshRatePreference.isVisible = false
        }
    }

    override fun onResume()
    {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        initialiseWidgetPreferences()
    }

    override fun onPause()
    {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?)
    {
        sharedPreferences ?: return
        key ?: return

        when (key)
        {
            Constants.SharedPreferences.SUBSCRIBE_SCREEN_UNLOCK_KEY ->
            {
                val updatedScreenUnlockSetting = sharedPreferences.getBoolean(key, false)
                setFragmentResult(WIDGET_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, bundleOf(SmallUVDisplay.SET_RECEIVING_SCREEN_UNLOCK_KEY to updatedScreenUnlockSetting))
            }
            Constants.SharedPreferences.WORK_TYPE_KEY ->
            {
                val updatedWorkTypeValue = sharedPreferences.getString(key, null) ?: return
                setFragmentResult(WIDGET_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, bundleOf(SmallUVDisplay.SET_USE_PERIODIC_WORK_KEY to (updatedWorkTypeValue == Constants.SharedPreferences.DEFAULT_WORK_TYPE_VALUE)))
            }
            Constants.SharedPreferences.BACKGROUND_REFRESH_RATE_KEY ->
            {
                val updatedRefreshRateValue = sharedPreferences.getString(key, null)?.toLongOrNull() ?: return
                setFragmentResult(WIDGET_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, bundleOf(SmallUVDisplay.SET_BACKGROUND_REFRESH_RATE_KEY to updatedRefreshRateValue))
            }
        }
    }

    companion object
    {
        const val WIDGET_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY = "widget_settings_updated_result_key"
    }
}