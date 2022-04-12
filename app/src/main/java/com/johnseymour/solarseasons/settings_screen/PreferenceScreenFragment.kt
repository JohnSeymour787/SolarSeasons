package com.johnseymour.solarseasons.settings_screen

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceFragmentCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.SmallUVDisplay
import com.johnseymour.solarseasons.api.OPENUV_API_KEY
import com.johnseymour.solarseasons.hasWidgets

class PreferenceScreenFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener
{
    private lateinit var backgroundWorkCategory: PreferenceCategory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        backgroundWorkCategory = findPreference("background_work_settings")!!

        findPreference<EditTextPreference>("manual_location_latitude")?.setOnBindEditTextListener()
        { editText ->
            bindEditText(editText, ::validateLatitude, R.string.preference_manual_location_latitude_error)
            editText.inputType = editText.inputType or InputType.TYPE_NUMBER_FLAG_SIGNED
        }

        findPreference<EditTextPreference>("manual_location_longitude")?.setOnBindEditTextListener()
        { editText ->
            bindEditText(editText, ::validateLongitude, R.string.preference_manual_location_longitude_error)
            editText.inputType = editText.inputType or InputType.TYPE_NUMBER_FLAG_SIGNED
        }

        findPreference<EditTextPreference>("manual_location_altitude")?.setOnBindEditTextListener()
        { editText ->
            bindEditText(editText, ::validateAltitude, R.string.preference_manual_location_altitude_error)
        }

        if (Constants.ENABLE_MANUAL_LOCATION_FEATURE)
        {
            findPreference<PreferenceCategory>("manual_location_settings")?.let()
            {
                it.isVisible = true
            }
        }

        if (Constants.ENABLE_API_KEY_ENTRY_FEATURE)
        {
            findPreference<PreferenceCategory>("api_key_category")?.let()
            {
                it.isVisible = true
            }

            findPreference<EditTextPreference>("stored_api_key")
                ?.summaryProvider = Preference.SummaryProvider<EditTextPreference>()
                {
                    "*".repeat(it.text.length)
                }
        }

        // Doing this here as well as onResume to ensure that the visibility is set quick enough to avoid a laggy appearance
        //  under most circumstances
        initialiseWidgetPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    private fun initialiseWidgetPreferences()
    {
        if (requireContext().hasWidgets())
        {
            backgroundWorkCategory.isEnabled = true
            backgroundWorkCategory.summary = ""
        }
        else
        {
            backgroundWorkCategory.isEnabled = false
            backgroundWorkCategory.setSummary(R.string.preferences_screen_no_widgets)
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

            Constants.SharedPreferences.CLOUD_COVER_FACTOR_KEY ->
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

            Constants.SharedPreferences.MANUAL_LOCATION_ENABLED_KEY ->
            {
                useManualLocation = sharedPreferences.getBoolean(key, false)
            }

            Constants.SharedPreferences.API_KEY ->
            {
                OPENUV_API_KEY = sharedPreferences.getString(key, null) ?: ""
            }
        }
    }

    private fun bindEditText(editText: EditText, validationMethod: (Double) -> Boolean, errorMessageResourceID: Int)
    {
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        editText.setSelection(editText.length())

        editText.doAfterTextChanged() // Validate field
        { editable ->
            editable ?: return@doAfterTextChanged

            val acceptButton = editText.rootView.findViewById<Button>(android.R.id.button1)

            if (validationMethod(editable.toString().toDoubleOrNull() ?: 0.0))
            {
                editText.error = null
                acceptButton?.isEnabled = true
            }
            else
            {
                editText.error = resources.getText(errorMessageResourceID)
                acceptButton?.isEnabled = false
            }
        }
    }

    private fun validateLatitude(latitude: Double): Boolean
    {
        return ((latitude >= -90.0) && (latitude <= 90.0))
    }

    private fun validateLongitude(longitude: Double): Boolean
    {
        return ((longitude >= -180.0) && (longitude <= 180.0))
    }

    private fun validateAltitude(altitude: Double): Boolean
    {
        return ((altitude >= Constants.MINIMUM_API_ACCEPTED_ALTITUDE) && (altitude <= Constants.MAXIMUM_EARTH_ALTITUDE))
    }

    companion object
    {
        const val WIDGET_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY = "widget_settings_updated_result_key"
        const val APP_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY = "app_settings_updated_result_key"
        var useCustomTheme = false
        var useManualLocation = false
    }
}