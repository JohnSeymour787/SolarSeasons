package com.johnseymour.solarseasons.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.johnseymour.solarseasons.ui.currentuv.CurrentUvScreen
import com.johnseymour.solarseasons.ui.onboarding.ApiKeyOnboardingScreen
import com.johnseymour.solarseasons.ui.settings.SettingsScreen

object Routes
{
    const val CURRENT_UV = "current_uv"
    const val SETTINGS = "settings"
    const val API_KEY_ONBOARDING = "api_key_onboarding"
}

@Composable
fun AppNavHost(startDestination: String, showPersistedError: Boolean = false)
{
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
    )
    {
        composable(Routes.CURRENT_UV)
        {
            CurrentUvScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                showPersistedError = showPersistedError,
            )
        }

        composable(Routes.SETTINGS)
        {
            SettingsScreen()
        }

        composable(Routes.API_KEY_ONBOARDING)
        {
            ApiKeyOnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.CURRENT_UV)
                    {
                        popUpTo(Routes.API_KEY_ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
    }
}
