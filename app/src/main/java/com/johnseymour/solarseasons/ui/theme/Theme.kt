package com.johnseymour.solarseasons.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.resolveColourAttr

/**
 * Colours that are specific to the SolarSeasons theme modes.
 *
 * The app has two theme modes:
 *  - Default: follows the device light/dark theme and only tints the UV value and progress bar with the UV colour.
 *  - Custom: the whole screen background is the UV colour while the section backgrounds are translucent white.
 */
@Immutable
data class SolarColors(
    val isCustomTheme: Boolean,
    val screenBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val uvValueColor: Color,
    val settingsIconTint: Color,
    val sectionBackground: Color,
    val progressTint: Color,
    val progressTrack: Color,
)

val LocalSolarColors = staticCompositionLocalOf<SolarColors> {
    SolarColors(
        isCustomTheme = false,
        screenBackground = Color.White,
        primaryText = Color.Black,
        secondaryText = Color.DarkGray,
        uvValueColor = Color.Green,
        settingsIconTint = Color.Black,
        sectionBackground = Color.LightGray,
        progressTint = Color.Green,
        progressTrack = Color.LightGray,
    )
}

@Composable
fun rememberSolarColors(useCustomTheme: Boolean, uvData: UVData? = null): SolarColors
{
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    return remember(useCustomTheme, uvData, isDarkTheme)
    {
        val windowBackground = Color(context.resolveColourAttr(android.R.attr.windowBackground))
        val themePrimaryText = Color(context.resolveColourAttr(android.R.attr.textColorPrimary))
        val themeSecondaryText = Color(context.resolveColourAttr(android.R.attr.textColorSecondary))

        val uvColor = Color(
            ContextCompat.getColor(
                context,
                uvData?.backgroundColorInt ?: R.color.uv_low
            )
        )

        if (useCustomTheme)
        {
            val uvTextColor = Color(
                ContextCompat.getColor(
                    context,
                    uvData?.textColorInt ?: R.color.dark_text
                )
            )

            SolarColors(
                isCustomTheme = true,
                screenBackground = uvColor,
                primaryText = uvTextColor,
                secondaryText = uvTextColor,
                uvValueColor = uvTextColor,
                settingsIconTint = uvTextColor,
                sectionBackground = Color.White.copy(alpha = 0.2F),
                progressTint = Color(ContextCompat.getColor(context, R.color.progress_bar_tint)),
                progressTrack = Color.Transparent,
            )
        }
        else
        {
            SolarColors(
                isCustomTheme = false,
                screenBackground = windowBackground,
                primaryText = themePrimaryText,
                secondaryText = themeSecondaryText,
                uvValueColor = uvColor,
                settingsIconTint = themePrimaryText,
                sectionBackground = Color(
                    ContextCompat.getColor(context, R.color.section_background_transparent_colour)
                ).copy(alpha = 0.2F),
                progressTint = uvColor,
                progressTrack = Color.Transparent,
            )
        }
    }
}

@Composable
fun SolarSeasonsTheme(
    useCustomTheme: Boolean = false,
    uvData: UVData? = null,
    content: @Composable () -> Unit
)
{
    val solarColors = rememberSolarColors(useCustomTheme, uvData)
    val isDarkTheme = isSystemInDarkTheme()

    val baseColorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

    val colorScheme = baseColorScheme.copy(
        primary = Color(ContextCompat.getColor(LocalContext.current, R.color.uv_low)),
        onPrimary = Color.White,
        background = solarColors.screenBackground,
        onBackground = solarColors.primaryText,
        surface = solarColors.screenBackground,
        onSurface = solarColors.primaryText,
        surfaceVariant = solarColors.sectionBackground,
        onSurfaceVariant = solarColors.secondaryText,
        secondary = Color(ContextCompat.getColor(LocalContext.current, R.color.uv_moderate)),
        error = Color(ContextCompat.getColor(LocalContext.current, R.color.uv_very_high)),
    )

    CompositionLocalProvider(LocalSolarColors provides solarColors)
    {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SolarTypography,
            content = content,
        )
    }
}
