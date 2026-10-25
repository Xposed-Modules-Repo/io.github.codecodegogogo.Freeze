package com.aistra.hail.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

private val HailBlue = Color(0xFF3683FF)

private val HailLightColors = lightColorScheme(
    primary = HailBlue,
    primaryVariant = HailBlue,
    onTertiaryContainer = HailBlue,
    sliderKeyPointForeground = HailBlue,
)

private val HailDarkColors = darkColorScheme(
    primary = HailBlue,
    primaryVariant = HailBlue,
    onTertiaryContainer = HailBlue,
    sliderKeyPointForeground = HailBlue,
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Keep the app brand color stable even when the platform supports Monet colors.
    val mode = if (darkTheme) {
        ColorSchemeMode.Dark
    } else {
        ColorSchemeMode.Light
    }
    val controller = remember(mode) {
        ThemeController(
            colorSchemeMode = mode,
            lightColors = HailLightColors,
            darkColors = HailDarkColors,
            keyColor = HailBlue,
            isDark = darkTheme,
        )
    }
    MiuixTheme(controller = controller, content = content)
}
