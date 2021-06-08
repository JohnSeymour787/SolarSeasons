package com.johnseymour.solarseasons

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.johnseymour.solarseasons.api.NetworkRepository
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NetworkRepository.getRealTimeUV().observe(this)
        {
            testDisplay.text = "UV Rating: ${it.uv}"
        }
    }
}