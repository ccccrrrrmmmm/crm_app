package ru.greenland.crm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = ContainerLight,
    onPrimaryContainer = OnContainerLight,
    inversePrimary = White,
    secondary = Black,
    onSecondary = White,
    secondaryContainer = ContainerLight,
    onSecondaryContainer = OnContainerLight,
    tertiary = Black,
    onTertiary = White,
    tertiaryContainer = ContainerLight,
    onTertiaryContainer = OnContainerLight,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = GraySurfaceLight,
    onSurfaceVariant = GrayTextLight,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    inverseSurface = Black,
    inverseOnSurface = White,
    outline = GrayOutlineLight,
    outlineVariant = GrayOutlineLight,
    scrim = Black,
    error = Black,
    onError = White,
    errorContainer = ContainerLight,
    onErrorContainer = OnContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = ContainerDark,
    onPrimaryContainer = OnContainerDark,
    inversePrimary = Black,
    secondary = White,
    onSecondary = Black,
    secondaryContainer = ContainerDark,
    onSecondaryContainer = OnContainerDark,
    tertiary = White,
    onTertiary = Black,
    tertiaryContainer = ContainerDark,
    onTertiaryContainer = OnContainerDark,
    background = Black,
    onBackground = White,
    surface = Ink90,
    onSurface = White,
    surfaceVariant = GraySurfaceDark,
    onSurfaceVariant = GrayTextDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = White,
    inverseOnSurface = Black,
    outline = GrayOutlineDark,
    outlineVariant = GrayOutlineDark,
    scrim = Black,
    error = White,
    onError = Black,
    errorContainer = ContainerDark,
    onErrorContainer = OnContainerDark,
)

@Composable
fun GreenlandCrmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GreenlandTypography,
        content = content,
    )
}
