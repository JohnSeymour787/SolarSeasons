package com.johnseymour.solarseasons.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.hasWidgets
import com.johnseymour.solarseasons.ui.theme.LocalSolarColors
import com.johnseymour.solarseasons.ui.theme.SolarSeasonsTheme
import com.johnseymour.solarseasons.widget.WidgetBackgroundManager

@Composable
fun SettingsScreen()
{
    val context = LocalContext.current
    var widgetsPresent by remember { mutableStateOf(context.hasWidgets()) }

    // The settings screen keeps the normal window theme even when the UV custom theme is selected.
    SolarSeasonsTheme(useCustomTheme = false)
    {
        val colors = LocalSolarColors.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val view = LocalView.current
        val isDarkTheme = isSystemInDarkTheme()

        DisposableEffect(lifecycleOwner)
        {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME)
                {
                    widgetsPresent = context.hasWidgets()
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        DisposableEffect(isDarkTheme)
        {
            val window = (view.context as? Activity)?.window
            if (window != null)
            {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }

            onDispose { }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.screenBackground)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        )
        {
            Text(
                text = stringResource(R.string.settings_fragment_title),
                color = colorResource(R.color.uv_low),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.sectionBackground)
                    .padding(horizontal = 12.dp),
            )
            {
                PreferenceFragmentHost(modifier = Modifier.fillMaxWidth())
            }

            if (widgetsPresent)
            {
                Text(
                    text = stringResource(R.string.preferences_screen_background_processing_explanation),
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.sectionBackground)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )

                Button(
                    onClick = {
                        WidgetBackgroundManager.stopBackgroundWork(context)
                        Toast.makeText(
                            context.applicationContext,
                            R.string.settings_fragment_stop_background_success,
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.uv_low),
                        contentColor = colorResource(R.color.dark_text),
                    ),
                )
                {
                    Text(
                        text = stringResource(R.string.settings_fragment_stop_background_button_text),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
