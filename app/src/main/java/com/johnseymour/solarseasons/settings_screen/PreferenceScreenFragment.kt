package com.johnseymour.solarseasons.settings_screen

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.SmallUVDisplay
import com.johnseymour.solarseasons.api.OPENUV_API_KEY
import com.johnseymour.solarseasons.hasWidgets
import com.johnseymour.solarseasons.showNotificationsRationaleDialogue
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PreferenceScreenFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener
{
    private lateinit var backgroundWorkCategory: PreferenceCategory
    private lateinit var uvProtectionTimePreference: ListPreference
    private lateinit var customTimePreference: EditTextPreference
    private lateinit var uvProtectionEndTimePreference: SwitchPreferenceCompat

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        backgroundWorkCategory = findPreference("background_work_settings")!!
        uvProtectionTimePreference = findPreference("uv_protection_notification_time")!!
        customTimePreference = findPreference("uv_protection_notification_custom_time")!!
        uvProtectionEndTimePreference = findPreference("uv_protection_end_notification")!!

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

        findPreference<EditTextPreference>("uv_protection_notification_custom_time")?.setOnBindEditTextListener()
        { editText ->
            bindEditText(editText, ::validateTime, R.string.preference_uv_notification_custom_time_error)
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

        setUVProtectionSettingsVisibility()
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

            Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_KEY ->
            {
                setUVProtectionSettingsVisibility()

                val notificationsOn = sharedPreferences.getBoolean(key, true)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationsOn.not())
                {
                    return
                }

                if (requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                {
                    context?.showNotificationsRationaleDialogue()
                }
            }

            Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_TIME_KEY ->
            {
                val isCustomTime = isUVProtectionCustomTime(sharedPreferences)

                customTimePreference.isVisible = isCustomTime

                // If not custom time then clear the custom time preference
                if (isCustomTime.not())
                {
                    // This causes this method to be called again but this specific preference change is not directly handled here
                    sharedPreferences.edit().remove(Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_CUSTOM_TIME_KEY).apply()
                }
            }
        }
    }

    private fun isUVProtectionCustomTime(prefs: SharedPreferences): Boolean = prefs.getString(Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_TIME_KEY, null)?.equals("custom_time") ?: false

    private fun setUVProtectionSettingsVisibility()
    {
        val uvNotificationsEnabled = preferenceManager.sharedPreferences.getBoolean(Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_KEY, true)

        if (uvNotificationsEnabled)
        {
            uvProtectionTimePreference.isVisible = true
            uvProtectionEndTimePreference.isVisible = true
            customTimePreference.isVisible = isUVProtectionCustomTime(preferenceManager.sharedPreferences)
        }
        else
        {
            uvProtectionTimePreference.isVisible = false
            uvProtectionEndTimePreference.isVisible = false
            customTimePreference.isVisible = false
        }
    }

    private fun bindEditText(editText: EditText, validationMethod: (String) -> Boolean, errorMessageResourceID: Int)
    {
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        editText.setSelection(editText.length())

        editText.doAfterTextChanged() // Validate field
        { editable ->
            editable ?: return@doAfterTextChanged

            val acceptButton = editText.rootView.findViewById<Button>(android.R.id.button1)

            if (validationMethod(editable.toString()))
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

    private fun validateLatitude(latitude: String): Boolean
    {
        val latitudeDouble = latitude.toDoubleOrNull() ?: 0.0
        return ((latitudeDouble >= -90.0) && (latitudeDouble <= 90.0))
    }

    private fun validateLongitude(longitude: String): Boolean
    {
        val longitudeDouble = longitude.toDoubleOrNull() ?: 0.0
        return ((longitudeDouble >= -180.0) && (longitudeDouble <= 180.0))
    }

    private fun validateAltitude(altitude: String): Boolean
    {
        val altitudeDouble = altitude.toDoubleOrNull() ?: 0.0
        return ((altitudeDouble >= Constants.MINIMUM_API_ACCEPTED_ALTITUDE) && (altitudeDouble <= Constants.MAXIMUM_EARTH_ALTITUDE))
    }

    private fun validateTime(timeString: String): Boolean
    {
        try
        {
            LocalTime.parse(timeString.replace('.', ':'), DateTimeFormatter.ISO_TIME)
            return true
        }
        catch (ignored: Exception) {}
        return false
    }

    companion object
    {
        const val WIDGET_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY = "widget_settings_updated_result_key"
        const val APP_PREFERENCES_UPDATED_FRAGMENT_RESULT_KEY = "app_settings_updated_result_key"
        var useCustomTheme = false
        var useManualLocation = false
    }
}