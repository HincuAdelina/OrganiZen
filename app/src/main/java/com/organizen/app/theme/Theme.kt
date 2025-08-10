package com.organizen.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    background = BgLight,
    onBackground = OnBgLight,
    surface = BgLight,
    onSurface = OnBgLight
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color.Black,
    background = BgDark,
    onBackground = OnBgDark,
    surface = BgDark,
    onSurface = OnBgDark
)

@Composable
fun OrganiZenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
