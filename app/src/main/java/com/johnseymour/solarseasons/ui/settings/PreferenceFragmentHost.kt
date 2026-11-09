package com.johnseymour.solarseasons.ui.settings

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment

/**
 * Hosts the existing PreferenceFragmentCompat inside a Compose screen.
 *
 * The fragment is added to the activity's FragmentManager using a stable container id, and removed
 * again when the composable leaves composition.
 */
@Composable
fun PreferenceFragmentHost(modifier: Modifier = Modifier)
{
    val context = LocalContext.current
    val activity = context.findFragmentActivity() ?: return
    val fragmentManager = activity.supportFragmentManager
    val containerId = rememberSaveable { View.generateViewId() }
    var containerView by remember { mutableStateOf<FragmentContainerView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FragmentContainerView(ctx).apply { id = containerId }.also { containerView = it }
        },
    )

    LaunchedEffect(containerView, containerId)
    {
        if (containerView != null && fragmentManager.findFragmentById(containerId) == null)
        {
            fragmentManager.beginTransaction()
                .replace(containerId, PreferenceScreenFragment())
                .commitNowAllowingStateLoss()
        }
    }

    DisposableEffect(containerId)
    {
        onDispose()
        {
            fragmentManager.findFragmentById(containerId)?.let()
            {
                fragmentManager.beginTransaction()
                    .remove(it)
                    .commitAllowingStateLoss()
            }
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity?
{
    var currentContext: Context? = this

    while (currentContext is ContextWrapper)
    {
        if (currentContext is FragmentActivity) { return currentContext }
        currentContext = currentContext.baseContext
    }

    return currentContext as? FragmentActivity
}
