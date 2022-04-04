package com.johnseymour.solarseasons

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.api.OPENUV_API_KEY
import com.johnseymour.solarseasons.api_key_screen.APIKeyEntryFragment
import com.johnseymour.solarseasons.api_key_screen.APIKeyFragment
import com.johnseymour.solarseasons.current_uv_screen.CurrentUVFragment
import com.johnseymour.solarseasons.settings_screen.SettingsFragment

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.setFragmentResultListener(SettingsFragment.LAUNCH_SETTINGS_FRAGMENT_KEY, this)
        { _, _ ->
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) ?: return@setFragmentResultListener

            if (currentFragment::class.java != SettingsFragment::class.java)
            {
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, SettingsFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
        }

        if (savedInstanceState == null)
        {
            // In case coming from a clicked widget
            val uvFragment = CurrentUVFragment.newInstance(intent.extras)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, uvFragment)
                .commit()
        }

        if (Constants.ENABLE_API_KEY_ENTRY_FEATURE)
        {
            supportFragmentManager.setFragmentResultListener(APIKeyEntryFragment.LAUNCH_MAIN_APP_FRAGMENT_KEY, this)
            { _, _ ->
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, CurrentUVFragment.newInstance(intent.extras))
                    .commit()
            }

            val apiKey = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.SharedPreferences.API_KEY, null)
            if (apiKey == null)
            {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, APIKeyFragment.newInstance())
                    .commit()
            }
            else
            {
                OPENUV_API_KEY = apiKey
            }
        }
    }
}