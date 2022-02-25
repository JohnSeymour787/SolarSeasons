package com.johnseymour.solarseasons

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.setFragmentResultListener(PreferenceScreenFragment.LAUNCH_SETTINGS_FRAGMENT_KEY, this)
        { _, _ ->
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, PreferenceScreenFragment())
                .addToBackStack(null)
                .commit()
        }

        // In case coming from a clicked widget
        val uvFragment = CurrentUVFragment.newInstance(intent.extras)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, uvFragment)
            .commit()
    }
}