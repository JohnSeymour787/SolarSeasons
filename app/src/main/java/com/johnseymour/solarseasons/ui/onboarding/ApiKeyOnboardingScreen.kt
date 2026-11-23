package com.johnseymour.solarseasons.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.api.OPENUV_API_KEY
import com.johnseymour.solarseasons.ui.theme.LocalSolarColors
import com.johnseymour.solarseasons.ui.theme.SolarSeasonsTheme
import android.widget.Toast

private const val PAGE_COUNT = 4

@Composable
fun ApiKeyOnboardingScreen(onFinished: () -> Unit)
{
    SolarSeasonsTheme()
    {
        val colors = LocalSolarColors.current
        val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(colors.screenBackground)
        )
        { page ->
            when (page)
            {
                0 -> TutorialPage(
                    titleRes = R.string.api_key_fragment_page_1_title,
                    explanationRes = R.string.api_key_fragment_page_1_explanation,
                )

                1 -> TutorialPage(
                    titleRes = R.string.api_key_fragment_page_2_3_4_title,
                    explanationRes = R.string.api_key_fragment_page_2_explanation,
                    webUrl = Constants.OPEN_UV_WEBSITE_BASE_URL,
                    showRedirectButton = true,
                )

                2 -> TutorialPage(
                    titleRes = R.string.api_key_fragment_page_2_3_4_title,
                    explanationRes = R.string.api_key_fragment_page_3_explanation,
                    webUrl = Constants.OPEN_UV_WEBSITE_CONSOLE_URL,
                    scrollWebView = true,
                    showRedirectButton = true,
                )

                else -> ApiKeyEntryPage(onLaunchApp = onFinished)
            }
        }
    }
}

@Composable
private fun TutorialPage(
    titleRes: Int,
    explanationRes: Int,
    webUrl: String? = null,
    scrollWebView: Boolean = false,
    showRedirectButton: Boolean = false,
)
{
    val context = LocalContext.current
    val colors = LocalSolarColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 8.dp)
    )
    {
        Column(modifier = Modifier.fillMaxSize())
        {
            Text(
                text = stringResource(titleRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                color = colors.primaryText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            if (webUrl == null)
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1F),
                    contentAlignment = Alignment.Center,
                )
                {
                    Text(
                        text = stringResource(explanationRes),
                        color = colors.primaryText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else
            {
                Text(
                    text = stringResource(explanationRes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = colors.primaryText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )

                WebViewContent(
                    url = webUrl,
                    scrollOnLoad = scrollWebView,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(top = 8.dp),
                )

                if (showRedirectButton)
                {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                    )
                    {
                        Text(text = stringResource(R.string.api_key_fragment_page_3_redirect_button_text))
                    }
                }

                Spacer(modifier = Modifier.weight(1F))
            }
        }

        Text(
            text = stringResource(R.string.api_key_fragment_swipe_hint),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp),
            color = colors.primaryText,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun WebViewContent(
    url: String,
    scrollOnLoad: Boolean,
    modifier: Modifier = Modifier,
)
{
    var webView by remember { mutableStateOf<WebView?>(null) }
    var didApplyInitialScroll by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply()
            {
                settings.javaScriptEnabled = true

                val originalClient = webViewClient

                webViewClient = object : WebViewClient()
                {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean
                    {
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?)
                    {
                        super.onPageFinished(view, url)

                        webViewClient = originalClient

                        if (scrollOnLoad && !didApplyInitialScroll)
                        {
                            didApplyInitialScroll = true

                            val offset = resources.getDimensionPixelOffset(
                                R.dimen.api_fragment_web_view_default_scroll_position
                            )
                            scrollTo(0, offset)
                        }
                    }
                }

                loadUrl(url)
            }.also { webView = it }
        },
        update = { },
    )

    DisposableEffect(webView)
    {
        onDispose { webView?.destroy() }
    }
}

@Composable
private fun ApiKeyEntryPage(onLaunchApp: () -> Unit)
{
    val context = LocalContext.current
    val colors = LocalSolarColors.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var apiKey by rememberSaveable { mutableStateOf("") }
    var errorMessageResource by rememberSaveable { mutableStateOf<Int?>(null) }
    var keySaved by rememberSaveable { mutableStateOf(false) }

    fun submit()
    {
        errorMessageResource = when
        {
            apiKey.isEmpty() -> R.string.api_key_fragment_page_4_empty_key_error
            apiKey.contains(" ", true) -> R.string.api_key_fragment_page_4_spaces_in_key_error
            else -> null
        }

        if (errorMessageResource != null) { return }

        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(Constants.SharedPreferences.API_KEY, apiKey)
            .apply()

        OPENUV_API_KEY = apiKey

        Toast.makeText(
            context,
            R.string.api_key_fragment_page_4_key_added_success_toast,
            Toast.LENGTH_SHORT,
        ).show()

        keyboardController?.hide()
        keySaved = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 8.dp)
    )
    {
        Text(
            text = stringResource(R.string.api_key_fragment_page_2_3_4_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            color = colors.primaryText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
            contentAlignment = Alignment.Center,
        )
        {
            Column(horizontalAlignment = Alignment.CenterHorizontally)
            {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        keySaved = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.api_key_fragment_page_4_key_entry_placeholder)) },
                    isError = errorMessageResource != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )

                errorMessageResource?.let()
                {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }

                Button(
                    onClick = { submit() },
                    modifier = Modifier.padding(top = 8.dp),
                )
                {
                    Text(
                        text = stringResource(R.string.api_key_fragment_page_4_store_key_button_text),
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (keySaved)
                {
                    Text(
                        text = stringResource(R.string.api_key_fragment_page_4_key_added_success_text),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        color = colors.primaryText,
                        textAlign = TextAlign.Center,
                    )

                    Button(
                        onClick = onLaunchApp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    {
                        Row(verticalAlignment = Alignment.CenterVertically)
                        {
                            Text(
                                text = stringResource(R.string.api_key_fragment_page_4_launch_main_app_button_text),
                                fontWeight = FontWeight.Bold,
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                painter = painterResource(R.mipmap.ic_launcher_adaptive_round),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
