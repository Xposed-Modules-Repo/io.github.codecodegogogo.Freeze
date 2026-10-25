package com.aistra.hail.ui.main

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.aistra.hail.R
import com.aistra.hail.app.DisguiseSession
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.about.AboutScreen
import com.aistra.hail.ui.apps.AppsScreen
import com.aistra.hail.ui.home.HomeScreen
import com.aistra.hail.ui.settings.SettingsScreen
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HUI

enum class HailRoute {
    Home,
    Settings,
    Apps,
    About,
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DisguiseSession.unlocked = true
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppTheme {
                var route by rememberSaveable { mutableStateOf(HailRoute.Home) }

                fun navigate(next: HailRoute) {
                    route = next
                }

                fun closeCurrent() {
                    route = when (route) {
                        HailRoute.Apps, HailRoute.About -> HailRoute.Settings
                        else -> HailRoute.Home
                    }
                }

                BackHandler(enabled = route != HailRoute.Home) {
                    if (route == HailRoute.Settings) route = HailRoute.Home else route = HailRoute.Settings
                }

                when (route) {
                    HailRoute.Home -> HomeScreen(
                        route = route,
                        onNavigate = ::navigate,
                    )

                    HailRoute.Settings -> SettingsScreen(
                        route = route,
                        onNavigate = ::navigate,
                    )

                    HailRoute.Apps -> AppsScreen(
                        route = route,
                        onNavigate = ::navigate,
                        onBack = ::closeCurrent,
                    )

                    HailRoute.About -> AboutScreen(
                        route = route,
                        onNavigate = ::navigate,
                    )
                }
            }
        }

        if (!HailData.biometricLogin ||
            BiometricManager.from(this)
                .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS
        ) return

        window.decorView.alpha = 0f
        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    HUI.showToast(errString)
                    finishAndRemoveTask()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    window.decorView.alpha = 1f
                }
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.action_biometric))
            .setSubtitle(getString(R.string.msg_biometric))
            .setNegativeButtonText(getString(android.R.string.cancel))
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) DisguiseSession.unlocked = false
        super.onDestroy()
    }
}
