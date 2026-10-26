package com.paisatrack.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PaisaLightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = Forest,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = SoftTeal,
    onSecondaryContainer = Forest,
    background = AppBackground,
    onBackground = MainText,
    surface = Color.White,
    onSurface = MainText,
    surfaceVariant = SoftTeal,
    onSurfaceVariant = SecondaryText,
    outline = Border,
    error = Coral,
    onError = Color.White,
)

@Composable
fun PaisaTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaisaLightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
