package com.appinspector.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryEmerald,
    secondary = SecondaryTeal,
    tertiary = AccentYellow,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = SurfaceDark,
    onSecondary = SurfaceDark,
    onTertiary = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun AppInspectorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
