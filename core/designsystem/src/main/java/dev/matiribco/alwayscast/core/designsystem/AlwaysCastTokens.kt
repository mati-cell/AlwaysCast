@file:Suppress("MagicNumber")

package dev.matiribco.alwayscast.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AlwaysCastTokens {
    object Spacing {
        val xxs: Dp = 2.dp
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
        val xxl: Dp = 32.dp
    }

    object Radius {
        val input: Dp = 8.dp
        val card: Dp = 16.dp
        val modal: Dp = 24.dp
        val full: Dp = 999.dp
    }

    object Palette {
        val BackgroundDark = Color(0xFF0B0E14)
        val SurfaceDark = Color(0xFF131720)
        val SurfaceElevatedDark = Color(0xFF1B212D)
        val SurfaceHighlightDark = Color(0xFF262E3E)
        val BorderHairlineDark = Color(0x26FFFFFF)

        val Accent = Color(0xFF3B82F6)
        val AccentHover = Color(0xFF60A5FA)
        val AccentContainer = Color(0xFF1E3A8A)
        val OnAccent = Color(0xFFFFFFFF)

        val TextPrimaryDark = Color(0xFFF1F5F9)
        val TextSecondaryDark = Color(0xFF94A3B8)
        val TextTertiaryDark = Color(0xFF64748B)

        val SemanticSuccess = Color(0xFF10B981)
        val SemanticError = Color(0xFFEF4444)
        val SemanticLive = Color(0xFFDC2626)
    }

    object Typography {
        val TabularTime =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
                fontFeatureSettings = "tnum",
            )
    }
}
