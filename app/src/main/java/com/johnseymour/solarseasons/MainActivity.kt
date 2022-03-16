package com.johnseymour.solarseasons

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

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
    }
}