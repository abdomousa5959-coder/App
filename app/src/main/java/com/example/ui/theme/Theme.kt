package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CyberObsidianScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = ObsidianDeep,
    primaryContainer = CyanDim,
    onPrimaryContainer = CyanElectric,
    secondary = VioletPulse,
    onSecondary = TextPrimary,
    secondaryContainer = VioletDim,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldActive,
    onTertiary = ObsidianDeep,
    background = ObsidianDeep,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianGlass,
    error = CrimsonAlert,
    onError = TextPrimary
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberObsidianScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
