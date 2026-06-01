package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val ForestGreenPrimary = Color(0xFF15803D) // Elegant rich forest green for high-prestige light theme accent

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryRoyalGreen,
    onPrimary = Color(0xFF020906), // Rich dark forest color for high contrast
    secondary = SandalwoodGold, // Beautiful gold sandalwood secondary
    onSecondary = SpaceDarkBackground,
    tertiary = RoyalGold, // Royal bright gold for highlights
    onTertiary = SpaceDarkBackground,
    background = SpaceDarkBackground,
    onBackground = Color.White,
    surface = CorporateSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF0C2B1D),
    onSurfaceVariant = Color(0xFFDCFCE7),
    error = OverdueRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = Color.White,
    secondary = SandalwoodGoldDark,
    onSecondary = Color.White,
    tertiary = RoyalGold,
    background = PureWhiteBackground,
    surface = CorporateSurfaceLight,
    onBackground = DeepSlateText,
    onSurface = DeepSlateText,
    error = OverdueRed,
    onError = Color.White,
    surfaceVariant = SoftSteelBorders,
    onSurfaceVariant = SandalwoodGoldDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Reusable Frosted Glass modifier in Android for Jetpack Compose.
 */
fun Modifier.glassCardAdaptive(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    isDarkMode: Boolean = true
): Modifier = this.then(
    Modifier
        .clip(shape)
        .background(
            if (isDarkMode) Color(0xFF0C2B1D).copy(alpha = 0.55f) // Rich dark jade/emerald matte surface glass
            else Color(0xFFFFFFFF).copy(alpha = 0.96f) // Higher opacity so white blocks pop cleanly off the warm sand background
        )
        .border(
            width = borderWidth,
            brush = Brush.linearGradient(
                colors = if (isDarkMode) {
                    listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.12f),
                        Color(0xFFFFFFFF).copy(alpha = 0.02f)
                    )
                } else {
                    listOf(
                        Color(0xFFB58E49).copy(alpha = 0.22f), // Sandalwood gold border tint
                        Color(0xFFB58E49).copy(alpha = 0.05f)
                    )
                }
            ),
            shape = shape
        )
)

@Composable
fun MeshGradientBackground(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkMode) SpaceDarkBackground else PureWhiteBackground)
            .drawBehind {
                val w = size.width
                val h = size.height
                if (isDarkMode) {
                    // Royal Green glowing blob top-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PrimaryRoyalGreen.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.15f, h * 0.15f),
                            radius = w * 0.9f
                        )
                    )
                    // Sandalwood Gold glowing blob middle-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SandalwoodGold.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.85f, h * 0.45f),
                            radius = w * 0.7f
                        )
                    )
                    // Subtle dark secondary black glow bottom-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.85f),
                            radius = w * 0.8f
                        )
                    )
                } else {
                    // Rich Forest Green subtle glow top-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ForestGreenPrimary.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.15f, h * 0.20f),
                            radius = w * 0.7f
                        )
                    )
                    // Soft Sandalwood Gold warmth bottom-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SandalwoodGold.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.85f, h * 0.80f),
                            radius = w * 0.7f
                        )
                    )
                }
            }
    ) {
        content()
    }
}
