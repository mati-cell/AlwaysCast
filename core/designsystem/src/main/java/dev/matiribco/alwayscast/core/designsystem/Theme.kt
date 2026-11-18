@file:Suppress("ktlint:standard:function-naming", "FunctionNaming", "MagicNumber")

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

private val DarkColorScheme =
    darkColorScheme(
        primary = AlwaysCastTokens.Palette.Accent,
        onPrimary = AlwaysCastTokens.Palette.OnAccent,
        primaryContainer = AlwaysCastTokens.Palette.AccentContainer,
        onPrimaryContainer = AlwaysCastTokens.Palette.TextPrimaryDark,
        background = AlwaysCastTokens.Palette.BackgroundDark,
        onBackground = AlwaysCastTokens.Palette.TextPrimaryDark,
        surface = AlwaysCastTokens.Palette.SurfaceDark,
        onSurface = AlwaysCastTokens.Palette.TextPrimaryDark,
        surfaceVariant = AlwaysCastTokens.Palette.SurfaceElevatedDark,
        onSurfaceVariant = AlwaysCastTokens.Palette.TextSecondaryDark,
        outline = AlwaysCastTokens.Palette.TextTertiaryDark,
        outlineVariant = AlwaysCastTokens.Palette.BorderHairlineDark,
        error = AlwaysCastTokens.Palette.SemanticError,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E3A8A),
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF475569),
        outline = Color(0xFF94A3B8),
        outlineVariant = Color(0xFFE2E8F0),
        error = AlwaysCastTokens.Palette.SemanticError,
    )

@Composable
fun AlwaysCastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
