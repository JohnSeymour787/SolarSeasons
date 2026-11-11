package com.johnseymour.solarseasons.widget

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.DiskRepository
import com.johnseymour.solarseasons.ErrorStatus
import com.johnseymour.solarseasons.MainActivity
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.preferredTimeString
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

internal val LaunchFromWidgetKey = ActionParameters.Key<Boolean>("launch_from_widget")

/** Glance implementation of the home-screen UV widget. */
class SmallUvWidget : GlanceAppWidget()
{
    override suspend fun provideGlance(context: Context, id: GlanceId)
    {
        val appContext = context.applicationContext
        val initialState = readWidgetState(appContext)

        provideContent {
            val state by remember { widgetStateFlow(appContext) }.collectAsState(initialState)
            SmallUvWidgetContent(state)
        }
    }

    /**
     * Glance only calls [provideGlance] once per session, so [updateAll] updates re-render the
     * composition with whatever data was captured at session start. Observing the persisted data
     * inside the composition keeps the widget in sync as soon as the app writes new data.
     */
    private fun widgetStateFlow(context: Context): Flow<SmallUvWidgetState> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }

        val dataPreferences = context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        dataPreferences.registerOnSharedPreferenceChangeListener(listener)
        defaultPreferences.registerOnSharedPreferenceChangeListener(listener)

        trySend(Unit)

        awaitClose {
            dataPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            defaultPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.map { readWidgetState(context) }

    private fun readWidgetState(context: Context): SmallUvWidgetState
    {
        val res = context.resources
        val useCustomTheme = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.SharedPreferences.APP_THEME_KEY, null) == Constants.SharedPreferences.CUSTOM_APP_THEME_VALUE

        val dataPreferences = context.getSharedPreferences(DiskRepository.DATA_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val uvData = try { DiskRepository.readLatestUV(dataPreferences) } catch (_: Exception) { null }
        val latestError = try { DiskRepository.readLatestError(dataPreferences) } catch (_: Exception) { null }

        val appWidgetBackground = ContextCompat.getColor(context, R.color.appWidgetBackgroundColor)
        val appWidgetText = ContextCompat.getColor(context, R.color.appWidgetTextColor)

        val isNightMode = (res.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val deviceProgressColor = ContextCompat.getColor(
            context,
            if (isNightMode) R.color.progress_bar_tint else R.color.progress_bar_tint_light_theme
        )

        val errorToDisplay = latestError?.takeIf()
        {
            it != ErrorStatus.LocationBatterySaverError && it != ErrorStatus.GeneralLocationError
        }

        if (errorToDisplay != null)
        {
            return if (useCustomTheme)
            {
                SmallUvWidgetState(
                    uvText = context.getString(R.string.widget_error),
                    timeText = "",
                    progress = 0,
                    background = Color(ContextCompat.getColor(context, R.color.uv_low)),
                    uvColor = Color(ContextCompat.getColor(context, R.color.dark_text)),
                    timeColor = Color(ContextCompat.getColor(context, R.color.dark_text)),
                    progressColor = Color(ContextCompat.getColor(context, R.color.progress_bar_tint)),
                    showTime = false,
                    showProgress = false,
                )
            }
            else
            {
                SmallUvWidgetState(
                    uvText = context.getString(R.string.widget_error),
                    timeText = "",
                    progress = 0,
                    background = Color(appWidgetBackground),
                    uvColor = Color(appWidgetText),
                    timeColor = Color(appWidgetText),
                    progressColor = Color(deviceProgressColor),
                    showTime = false,
                    showProgress = false,
                )
            }
        }

        if (uvData != null)
        {
            val uvText = context.getString(R.string.uv_value, uvData.uv)
            val timeText = preferredTimeString(context, uvData.uvTime)

            return if (useCustomTheme)
            {
                SmallUvWidgetState(
                    uvText = uvText,
                    timeText = timeText,
                    progress = uvData.sunProgressPercent,
                    background = Color(ContextCompat.getColor(context, uvData.backgroundColorInt)),
                    uvColor = Color(ContextCompat.getColor(context, uvData.textColorInt)),
                    timeColor = Color(ContextCompat.getColor(context, uvData.textColorInt)),
                    progressColor = Color(ContextCompat.getColor(context, R.color.progress_bar_tint)),
                    showTime = true,
                    showProgress = true,
                )
            }
            else
            {
                SmallUvWidgetState(
                    uvText = uvText,
                    timeText = timeText,
                    progress = uvData.sunProgressPercent,
                    background = Color(appWidgetBackground),
                    uvColor = Color(ContextCompat.getColor(context, uvData.backgroundColorInt)),
                    timeColor = Color(appWidgetText),
                    progressColor = Color(deviceProgressColor),
                    showTime = true,
                    showProgress = true,
                )
            }
        }

        return SmallUvWidgetState(
            uvText = context.getString(R.string.appwidget_uv_text_default),
            timeText = context.getString(R.string.app_widget_time_text_default),
            progress = 0,
            background = if (useCustomTheme) Color(ContextCompat.getColor(context, R.color.uv_low)) else Color(appWidgetBackground),
            uvColor = if (useCustomTheme) Color(ContextCompat.getColor(context, R.color.dark_text)) else Color(appWidgetText),
            timeColor = if (useCustomTheme) Color(ContextCompat.getColor(context, R.color.dark_text)) else Color(appWidgetText),
            progressColor = if (useCustomTheme) Color(ContextCompat.getColor(context, R.color.progress_bar_tint)) else Color(deviceProgressColor),
            showTime = true,
            showProgress = true,
        )
    }
}

private data class SmallUvWidgetState(
    val uvText: String,
    val timeText: String,
    val progress: Int,
    val background: Color,
    val uvColor: Color,
    val timeColor: Color,
    val progressColor: Color,
    val showTime: Boolean,
    val showProgress: Boolean,
)

@Composable
private fun SmallUvWidgetContent(state: SmallUvWidgetState)
{
    val size: DpSize = LocalSize.current
    val uvTextSize = when
    {
        size.height < 100.dp -> 26.sp
        size.height < 170.dp -> 48.sp
        else -> 72.sp
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(state.background))
            .cornerRadius(7.dp)
            .clickable(
                actionStartActivity<MainActivity>(
                    actionParametersOf(LaunchFromWidgetKey to true)
                )
            ),
        contentAlignment = Alignment.Center,
    )
    {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        )
        {
            if (state.showTime)
            {
                Text(
                    text = state.timeText,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = ColorProvider(state.timeColor),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }

            Text(
                text = state.uvText,
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(
                    color = ColorProvider(state.uvColor),
                    fontSize = uvTextSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            )

            if (state.showProgress)
            {
                LinearProgressIndicator(
                    progress = state.progress / 100F,
                    modifier = GlanceModifier.fillMaxWidth(),
                    color = ColorProvider(state.progressColor),
                )
            }
        }
    }
}
