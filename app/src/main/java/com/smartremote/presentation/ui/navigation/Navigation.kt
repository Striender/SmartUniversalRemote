package com.smartremote.presentation.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smartremote.presentation.ui.auth.AuthScreen
import com.smartremote.presentation.ui.devices.AddDeviceScreen
import com.smartremote.presentation.ui.devices.DeviceDetailScreen
import com.smartremote.presentation.ui.home.HomeScreen
import com.smartremote.presentation.ui.remote.RemoteControlScreen
import com.smartremote.presentation.ui.automation.AutomationScreen
import com.smartremote.presentation.ui.energy.EnergyScreen
import com.smartremote.presentation.ui.ai.AiAssistantScreen
import com.smartremote.presentation.ui.settings.SettingsScreen
import com.smartremote.presentation.ui.voice.VoiceCommandScreen

// ─── Route Constants ──────────────────────────────────────────────────────────

object Routes {
    const val AUTH           = "auth"
    const val HOME           = "home"
    const val ADD_DEVICE     = "add_device"
    const val DEVICE_DETAIL  = "device/{deviceId}"
    const val REMOTE_CONTROL = "remote/{deviceId}"
    const val AUTOMATION     = "automation"
    const val ENERGY         = "energy"
    const val AI_ASSISTANT   = "ai_assistant"
    const val VOICE_COMMAND  = "voice_command"
    const val SETTINGS       = "settings"

    fun deviceDetail(deviceId: String) = "device/$deviceId"
    fun remoteControl(deviceId: String) = "remote/$deviceId"
}

// ─── Navigation Host ──────────────────────────────────────────────────────────

@Composable
fun SmartRemoteNavHost(
    navController: NavHostController,
    startDestination: String = Routes.AUTH
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition  = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) },
        popExitTransition  = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
    ) {
        composable(Routes.AUTH) {
            AuthScreen(onAuthenticated = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.AUTH) { inclusive = true }
                }
            })
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToDevice     = { navController.navigate(Routes.deviceDetail(it)) },
                onNavigateToRemote     = { navController.navigate(Routes.remoteControl(it)) },
                onNavigateToAddDevice  = { navController.navigate(Routes.ADD_DEVICE) },
                onNavigateToAutomation = { navController.navigate(Routes.AUTOMATION) },
                onNavigateToEnergy     = { navController.navigate(Routes.ENERGY) },
                onNavigateToAi         = { navController.navigate(Routes.AI_ASSISTANT) },
                onNavigateToVoice      = { navController.navigate(Routes.VOICE_COMMAND) },
                onNavigateToSettings   = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ADD_DEVICE) {
            AddDeviceScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.DEVICE_DETAIL,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            DeviceDetailScreen(
                deviceId = deviceId,
                onBack   = { navController.popBackStack() },
                onOpenRemote = { navController.navigate(Routes.remoteControl(it)) }
            )
        }

        composable(
            Routes.REMOTE_CONTROL,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            RemoteControlScreen(
                deviceId = deviceId,
                onBack   = { navController.popBackStack() }
            )
        }

        composable(Routes.AUTOMATION) {
            AutomationScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ENERGY) {
            EnergyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.AI_ASSISTANT) {
            AiAssistantScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.VOICE_COMMAND) {
            VoiceCommandScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

// ─── Bottom Nav Items ─────────────────────────────────────────────────────────

enum class BottomNavItem(val route: String, val icon: String, val label: String) {
    HOME("home_tab", "home", "Home"),
    DEVICES("devices_tab", "devices", "Devices"),
    AUTOMATION("automation", "auto_awesome", "Scenes"),
    ENERGY("energy", "bolt", "Energy"),
    AI("ai_assistant", "smart_toy", "AI")
}
