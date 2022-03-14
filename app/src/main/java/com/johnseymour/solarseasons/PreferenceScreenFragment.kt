package com.johnseymour.solarseasons

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceFragmentCompat
import androidx.core.os.bundleOf
import androidx.preference.PreferenceCategory

class PreferenceScreenFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener
{
    private lateinit var backgroundWorkCategory: PreferenceCategory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        backgroundWorkCategory = findPreference("background_work_settings")!!

        // Doing this here as well as onResume to ensure that the visibility is set quick enough to avoid a laggy appearance
        //  under most circumstances
        initialiseWidgetPreferences()
    }

    private fun initialiseWidgetPreferences()
    {
        backgroundWorkCategory.isEnabled = requireContext().hasWidgets()
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
            Constants.SharedPreferences.APP_THEME_KEY ->
            {
                val updatedAppThemeValue = sharedPreferences.getString(key, null) ?: return
                useCustomTheme = updatedAppThemeValue == Constants.SharedPreferences.CUSTOM_APP_THEME_VALUE
                setFragmentResult(APP_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, bundleOf(key to updatedAppThemeValue))
            }

            Constants.SharedPreferences.APP_LAUNCH_AUTO_REQUEST_KEY ->
            {
                val updatedAutoRequestValue = sharedPreferences.getBoolean(key, true)
                setFragmentResult(APP_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY, bundleOf(key to updatedAutoRequestValue))
            }

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
        const val APP_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY = "app_settings_updated_result_key"
        var useCustomTheme = false
    }
}