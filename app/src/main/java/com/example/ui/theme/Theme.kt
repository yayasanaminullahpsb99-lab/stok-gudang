package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val IndustrialDarkColorScheme = darkColorScheme(
    primary = LaserAmber500,
    onPrimary = Slate950,
    primaryContainer = LaserAmber900,
    onPrimaryContainer = LaserAmber400,
    secondary = CyberTeal400,
    onSecondary = Slate950,
    secondaryContainer = CyberTeal900,
    onSecondaryContainer = CyberTeal400,
    tertiary = Emerald400,
    onTertiary = Slate950,
    tertiaryContainer = Emerald950,
    onTertiaryContainer = Emerald400,
    error = Rose400,
    onError = Slate950,
    errorContainer = Rose950,
    onErrorContainer = Rose400,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800
)

private val IndustrialLightColorScheme = lightColorScheme(
    primary = LaserAmber600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = LaserAmber900,
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = CyberTeal900,
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate200,
    onSurfaceVariant = Slate700,
    outline = Slate400
)

val WarehouseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun StokGudangProTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IndustrialDarkColorScheme else IndustrialLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = WarehouseShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    StokGudangProTheme(darkTheme = darkTheme, content = content)
}
