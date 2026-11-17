@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private const val COLOR_PRIMARY_DARK = 0xFF90CAF9
private const val COLOR_SECONDARY_DARK = 0xFF80CBC4
private const val COLOR_TERTIARY_DARK = 0xFFFFCC80
private const val COLOR_BACKGROUND_DARK = 0xFF121212
private const val COLOR_SURFACE_DARK = 0xFF1E1E1E
private const val COLOR_ERROR_DARK = 0xFFEF5350

private const val COLOR_PRIMARY_LIGHT = 0xFF1976D2
private const val COLOR_SECONDARY_LIGHT = 0xFF00897B
private const val COLOR_TERTIARY_LIGHT = 0xFFFB8C00
private const val COLOR_BACKGROUND_LIGHT = 0xFFF5F5F5
private const val COLOR_SURFACE_LIGHT = 0xFFFFFFFF
private const val COLOR_ERROR_LIGHT = 0xFFD32F2F

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(COLOR_PRIMARY_DARK),
        secondary = Color(COLOR_SECONDARY_DARK),
        tertiary = Color(COLOR_TERTIARY_DARK),
        background = Color(COLOR_BACKGROUND_DARK),
        surface = Color(COLOR_SURFACE_DARK),
        error = Color(COLOR_ERROR_DARK),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(COLOR_PRIMARY_LIGHT),
        secondary = Color(COLOR_SECONDARY_LIGHT),
        tertiary = Color(COLOR_TERTIARY_LIGHT),
        background = Color(COLOR_BACKGROUND_LIGHT),
        surface = Color(COLOR_SURFACE_LIGHT),
        error = Color(COLOR_ERROR_LIGHT),
    )

@Composable
fun AlwaysCastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
