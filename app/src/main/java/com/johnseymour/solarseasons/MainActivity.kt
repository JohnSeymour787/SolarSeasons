package com.johnseymour.solarseasons

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.johnseymour.solarseasons.api.NetworkRepository
import kotlinx.android.synthetic.main.activity_main.*
import android.Manifest
import android.content.Intent
import android.location.Location
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Just given permission", Toast.LENGTH_SHORT).show()
/*                NetworkRepository.getRealTimeUV().observe(this)
                {
                    testDisplay.text = "UV Rating: ${it.uv}"
                }*/



            } else {
                Toast.makeText(
                    this,
                    "Permission required for getting UV at current location.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        when
        {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ->
            {
                Toast.makeText(this, "Already have permission", Toast.LENGTH_SHORT).show()
/*                NetworkRepository.getRealTimeUV().observe(this)
                {
                    testDisplay.text = "UV Rating: ${it.uv}"
                }*/
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) ->
            {
                Toast.makeText(this, "Permission required for getting UV at current location.", Toast.LENGTH_SHORT).show()
            }

            else ->
            {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
        {
            disablePermissionReset.setOnClickListener()
            {
                val intent = Intent().apply()
                {
                    action = Intent.ACTION_AUTO_REVOKE_PERMISSIONS
                }

                startActivity(intent)
            }
        }
        else
        {
            disablePermissionReset.visibility = View.GONE
        }
    }
}