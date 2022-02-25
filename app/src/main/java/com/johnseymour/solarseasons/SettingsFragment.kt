package com.johnseymour.solarseasons

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceFragmentCompat
import androidx.core.os.bundleOf

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener
{
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        view?.setBackgroundColor(resources.getColor(R.color.uv_low, requireContext().theme))

        return view
    }

    override fun onResume()
    {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
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
                setFragmentResult(SETTINGS_UPDATED_FRAGMENT_RESULT_KEY, bundleOf(SmallUVDisplay.SET_RECEIVING_SCREEN_UNLOCK_KEY to updatedScreenUnlockSetting))
            }
            Constants.SharedPreferences.WORK_TYPE_KEY ->
            {
                val updatedWorkTypeValue = sharedPreferences.getString(key, null) ?: return
                setFragmentResult(SETTINGS_UPDATED_FRAGMENT_RESULT_KEY, bundleOf(SmallUVDisplay.SET_USE_PERIODIC_WORK_KEY to (updatedWorkTypeValue == Constants.SharedPreferences.DEFAULT_WORK_TYPE_VALUE)))
            }
            Constants.SharedPreferences.BACKGROUND_REFRESH_RATE_KEY ->
            {
                val updatedRefreshRateValue = sharedPreferences.getString(key, null)?.toLongOrNull() ?: return
                setFragmentResult(SETTINGS_UPDATED_FRAGMENT_RESULT_KEY, bundleOf(SmallUVDisplay.SET_BACKGROUND_REFRESH_RATE_KEY to updatedRefreshRateValue))
            }
        }
    }

    companion object
    {
        const val SETTINGS_UPDATED_FRAGMENT_RESULT_KEY = "settings_updated_result_key"
        const val LAUNCH_SETTINGS_FRAGMENT_KEY = "launch_settings_fragment_key"
    }
}