package com.johnseymour.solarseasons.ui.currentuv

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.util.DisplayMetrics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.ErrorStatus
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.data.AppSettingsRepository
import com.johnseymour.solarseasons.hasWidgets
import com.johnseymour.solarseasons.models.SunInfo
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.preferredTimeString
import com.johnseymour.solarseasons.showNotificationsRationaleDialogue
import com.johnseymour.solarseasons.ui.theme.LocalSolarColors
import com.johnseymour.solarseasons.ui.theme.SolarColors
import com.johnseymour.solarseasons.ui.theme.SolarSeasonsTheme

@Composable
fun CurrentUvScreen(
    onOpenSettings: () -> Unit,
    showPersistedError: Boolean = false,
    viewModel: CurrentUvViewModel = viewModel(),
)
{
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by AppSettingsRepository.settings.collectAsStateWithLifecycle()
    val useCustomTheme = settings.useCustomTheme

    SolarSeasonsTheme(useCustomTheme = useCustomTheme, uvData = uiState.uvData)
    {
        val colors = LocalSolarColors.current
        val resources = LocalResources.current

        ApplyStatusBarAppearance(useCustomTheme)

        LaunchedEffect(settings.cloudCoverEnabled)
        {
            viewModel.onCloudCoverSettingChanged(context, settings.cloudCoverEnabled)
        }

        DisposableEffect(lifecycleOwner, context)
        {
            val observer = LifecycleEventObserver { _, event ->
                when (event)
                {
                    Lifecycle.Event.ON_START -> viewModel.onStart(context, showPersistedError)
                    Lifecycle.Event.ON_STOP -> viewModel.onStop(context)
                    else -> { }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val permissionsLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == false)
            {
                viewModel.setStatusMessage(resources.getString(R.string.location_permission_generic_rationale))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                permissions[Manifest.permission.POST_NOTIFICATIONS] == false)
            {
                PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_KEY, false)
                    .apply()
            }
        }

        LaunchedEffect(Unit)
        {
            if (viewModel.consumeInitialPermissionRequest())
            {
                requestInitialPermissions(activity, context, resources, permissionsLauncher, viewModel)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.screenBackground)
                .systemBarsPadding()
        )
        {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh(context) },
                modifier = Modifier.fillMaxSize(),
            )
            {
                when
                {
                    uiState.error != null -> ErrorContent(
                        error = uiState.error!!,
                        colors = colors,
                        showAppDetailsButton = uiState.showAppDetailsButton,
                        onOpenAppDetails = { openAppDetails(context) },
                    )

                    uiState.uvData != null -> UvContent(
                        uiState = uiState,
                        colors = colors,
                        forecastCellWidth = cellWidth(context),
                        sunInfoCellWidth = cellWidth(context),
                    )

                    else -> StatusContent(
                        message = uiState.statusMessage ?: stringResource(R.string.activity_status_information_swipe_hint),
                        colors = colors,
                        showAppDetailsButton = uiState.showAppDetailsButton,
                        onOpenAppDetails = { openAppDetails(context) },
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(45.dp)
            )
            {
                Icon(
                    painter = painterResource(R.drawable.settings_icon),
                    contentDescription = stringResource(R.string.activity_settings_button_image_description),
                    tint = colors.settingsIconTint,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun ApplyStatusBarAppearance(useCustomTheme: Boolean)
{
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()

    if (view.isInEditMode) { return }

    DisposableEffect(useCustomTheme, isDarkTheme)
    {
        val window = (view.context as? Activity)?.window
        if (window != null)
        {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !useCustomTheme && !isDarkTheme
        }

        onDispose { }
    }
}

private fun requestInitialPermissions(
    activity: Activity?,
    context: Context,
    resources: android.content.res.Resources,
    launcher: ActivityResultLauncher<Array<String>>,
    viewModel: CurrentUvViewModel,
)
{
    val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val notificationSettingsOn = defaultPreferences.getBoolean(
        Constants.SharedPreferences.UV_PROTECTION_NOTIFICATION_KEY,
        true
    )

    val permissionsToRequest = mutableListOf<String>()

    when
    {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !notificationSettingsOn -> { }

        activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true ->
        {
            context.showNotificationsRationaleDialogue()
        }

        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
        {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    when
    {
        activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) == true ->
        {
            viewModel.setStatusMessage(resources.getString(R.string.location_permission_generic_rationale))
        }

        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ->
        {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ->
        {
            if (context.hasWidgets())
            {
                val backgroundOptionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                {
                    context.packageManager.backgroundPermissionOptionLabel
                }
                else
                {
                    resources.getString(R.string.default_background_permission_option_label)
                }

                viewModel.setStatusMessage(
                    resources.getString(
                        R.string.location_permission_background_rationale,
                        resources.getString(R.string.activity_status_information_swipe_hint),
                        backgroundOptionLabel,
                    )
                )

                viewModel.setAppDetailsButtonVisible(true)
            }
        }
    }

    if (permissionsToRequest.isNotEmpty())
    {
        launcher.launch(permissionsToRequest.toTypedArray())
    }
}

@Composable
private fun ErrorContent(
    error: ErrorStatus,
    colors: SolarColors,
    showAppDetailsButton: Boolean,
    onOpenAppDetails: () -> Unit,
)
{
    val resources = LocalResources.current

    // Scrollable so that a pull-down gesture reaches the parent PullToRefreshBox.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    )
    {
        item {
            Text(
                text = error.statusString(resources),
                color = colors.primaryText,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
        }

        if (showAppDetailsButton)
        {
            item {
                AppDetailsButton(onClick = onOpenAppDetails, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun StatusContent(
    message: String,
    colors: SolarColors,
    showAppDetailsButton: Boolean,
    onOpenAppDetails: () -> Unit,
)
{
    // Scrollable so that a pull-down gesture reaches the parent PullToRefreshBox.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    )
    {
        item {
            Text(
                text = message,
                color = colors.primaryText,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
        }

        if (showAppDetailsButton)
        {
            item {
                AppDetailsButton(onClick = onOpenAppDetails, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun AppDetailsButton(onClick: () -> Unit, modifier: Modifier = Modifier)
{
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.light_window_background),
            contentColor = colorResource(R.color.dark_text),
        ),
    )
    {
        Text(
            text = stringResource(R.string.activity_open_app_settings_text),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun UvContent(
    uiState: CurrentUvUiState,
    colors: SolarColors,
    forecastCellWidth: androidx.compose.ui.unit.Dp,
    sunInfoCellWidth: androidx.compose.ui.unit.Dp,
)
{
    val context = LocalContext.current
    val uvData = uiState.uvData ?: return
    var selectedSunTime by remember { mutableStateOf<SunInfo.SunTimeData?>(null) }

    val forecastListState = rememberLazyListState()
    val sunInfoListState = rememberLazyListState()

    LaunchedEffect(uiState.forecast, uiState.forecastInitialIndex)
    {
        if (uiState.forecast.isNotEmpty())
        {
            forecastListState.scrollToItem(
                uiState.forecastInitialIndex.coerceIn(0, uiState.forecast.lastIndex)
            )
        }
    }

    LaunchedEffect(uiState.sunInfoTimes, uiState.sunInfoInitialIndex)
    {
        if (uiState.sunInfoTimes.isNotEmpty())
        {
            sunInfoListState.scrollToItem(
                uiState.sunInfoInitialIndex.coerceIn(0, uiState.sunInfoTimes.lastIndex)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    )
    {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(R.string.uv_value, uvData.uv),
            color = colors.uvValueColor,
            fontSize = 96.sp,
            lineHeight = 100.sp,
        )

        uvData.cityName?.let()
        {
            Text(
                text = it,
                color = colors.primaryText,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        Text(
            text = stringResource(uvData.uvLevelTextInt),
            color = colors.primaryText,
            fontSize = 18.sp,
        )

        Text(
            text = stringResource(R.string.latest_update, preferredTimeString(context, uvData.uvTime)),
            color = colors.primaryText,
            fontSize = 12.sp,
        )

        uvData.cloudFactoredUV?.let()
        {
            Text(
                text = stringResource(R.string.estimated_uv_value, it),
                color = colors.primaryText,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        uvData.cloudCoverTextInt?.let()
        {
            Text(
                text = stringResource(it),
                color = colors.primaryText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = stringResource(R.string.max_uv, uvData.uvMax),
            color = colors.primaryText,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 12.dp),
        )

        Text(
            text = stringResource(R.string.max_uv_time, preferredTimeString(context, uvData.uvMaxTime)),
            color = colors.primaryText,
            fontSize = 12.sp,
        )

        if (uiState.forecast.isNotEmpty())
        {
            SectionContainer(modifier = Modifier.padding(top = 24.dp))
            {
                SectionLabel(
                    text = stringResource(R.string.uv_forecast_label),
                    modifier = Modifier.padding(top = 8.dp)
                )

                LazyRow(
                    state = forecastListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                )
                {
                    itemsIndexed(uiState.forecast)
                    { index, forecast ->
                        val previousUv = uiState.forecast.getOrNull(index - 1)?.uv ?: 0F
                        val nextUv = uiState.forecast.getOrNull(index + 1)?.uv ?: 0F

                        UvForecastCell(
                            forecast = forecast,
                            previousUv = previousUv,
                            nextUv = nextUv,
                            maxYValue = maxOf(
                                Constants.GENERAL_MAXIMUM_UV,
                                uiState.forecast.maxOfOrNull { it.uv } ?: 0F
                            ),
                            cellWidth = forecastCellWidth,
                            textColor = colors.primaryText,
                        )
                    }
                }
            }
        }

        val safeExposure = uvData.safeExposure
        if (safeExposure != null && safeExposure.isNotEmpty())
        {
            SectionContainer(modifier = Modifier.padding(top = 24.dp))
            {
                SectionLabel(
                    text = stringResource(R.string.skin_exposure_label),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column(modifier = Modifier.fillMaxWidth())
                {
                    safeExposure.entries.toList().chunked(2).forEach()
                    { rowEntries ->
                        Row(modifier = Modifier.fillMaxWidth())
                        {
                            rowEntries.forEach()
                            { entry ->
                                Box(modifier = Modifier.weight(1F))
                                {
                                    SkinExposureCell(
                                        exposureEntry = entry,
                                        titleColor = colors.primaryText,
                                        secondaryColor = if (colors.isCustomTheme) colors.primaryText else colors.secondaryText,
                                    )
                                }
                            }

                            if (rowEntries.size == 1)
                            {
                                Spacer(modifier = Modifier.weight(1F))
                            }
                        }
                    }
                }
            }
        }

        SectionContainer(modifier = Modifier.padding(top = 24.dp))
        {
            SectionLabel(
                text = stringResource(R.string.sun_info_list_title_label),
                modifier = Modifier.padding(top = 8.dp)
            )

            SectionSubLabel(text = stringResource(R.string.sun_info_list_title_sub_label))

            LazyRow(
                state = sunInfoListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
            )
            {
                items(uiState.sunInfoTimes)
                { sunTime ->
                    SunInfoCell(
                        sunTime = sunTime,
                        cellWidth = sunInfoCellWidth,
                        titleColor = colors.primaryText,
                        timeColor = if (colors.isCustomTheme) colors.primaryText else colors.secondaryText,
                        onClick = { selectedSunTime = sunTime },
                    )
                }
            }
        }

        SectionContainer(modifier = Modifier.padding(top = 24.dp, bottom = 24.dp))
        {
            var useShortLabel by remember { mutableStateOf(false) }

            Text(
                text = stringResource(
                    if (useShortLabel) R.string.sun_progress_label_shortened else R.string.sun_progress_label
                ),
                color = colors.primaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
                onTextLayout = { if (it.lineCount > 1) useShortLabel = true },
            )

            LinearProgressIndicator(
                progress = { uvData.sunProgressPercent / 100F },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = colors.progressTint,
                trackColor = colors.progressTint.copy(alpha = 0.2F),
            )
        }
    }

    selectedSunTime?.let()
    { sunTime ->
        AlertDialog(
            onDismissRequest = { selectedSunTime = null },
            icon = {
                Icon(
                    painter = painterResource(sunTime.imageResourceInt),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            },
            title = {
                Text(
                    text = stringResource(sunTime.nameResourceInt),
                    color = colors.primaryText,
                )
            },
            text = {
                Text(
                    text = stringResource(SunInfo.sunTimeDescription(sunTime.nameResourceInt)),
                    color = colors.primaryText,
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedSunTime = null })
                {
                    Text(text = stringResource(R.string.close_window))
                }
            },
        )
    }
}

private fun cellWidth(context: Context): androidx.compose.ui.unit.Dp
{
    val is24HourFormat = DateFormat.is24HourFormat(context)

    return if (context.resources.displayMetrics.densityDpi < DisplayMetrics.DENSITY_XXXHIGH && is24HourFormat)
    {
        60.dp
    }
    else
    {
        70.dp
    }
}

private fun openAppDetails(context: Context)
{
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply()
    {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = Uri.fromParts("package", context.packageName, null)
    }

    context.startActivity(intent)
}
