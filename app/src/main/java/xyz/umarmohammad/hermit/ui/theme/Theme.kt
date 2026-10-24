package xyz.umarmohammad.hermit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val ForestDarkColorScheme = darkColorScheme(
    primary = ForestDarkAcento,
    onPrimary = ForestDarkFondo,
    primaryContainer = ForestDarkSuperficieSubtle,
    onPrimaryContainer = ForestDarkTexto,
    secondary = ForestDarkSecundario,
    onSecondary = ForestDarkFondo,
    secondaryContainer = ForestDarkSuperficie,
    onSecondaryContainer = ForestDarkTexto,
    background = ForestDarkFondo,
    onBackground = ForestDarkTexto,
    surface = ForestDarkSuperficie,
    onSurface = ForestDarkTexto,
    surfaceVariant = ForestDarkSuperficieSubtle,
    onSurfaceVariant = ForestDarkSecundario,
    outline = ForestDarkBorder,
    outlineVariant = ForestDarkSuperficieSubtle,
    error = ForestDarkAviso,
    onError = ForestDarkFondo
)

val ForestLightColorScheme = lightColorScheme(
    primary = ForestLightAcento,
    onPrimary = ForestLightFondo,
    primaryContainer = ForestLightSuperficieSubtle,
    onPrimaryContainer = ForestLightTexto,
    secondary = ForestLightSecundario,
    onSecondary = ForestLightFondo,
    secondaryContainer = ForestLightSuperficie,
    onSecondaryContainer = ForestLightTexto,
    background = ForestLightFondo,
    onBackground = ForestLightTexto,
    surface = ForestLightSuperficie,
    onSurface = ForestLightTexto,
    surfaceVariant = ForestLightSuperficieSubtle,
    onSurfaceVariant = ForestLightSecundario,
    outline = ForestLightBorder,
    outlineVariant = ForestLightSuperficieSubtle,
    error = ForestLightAviso,
    onError = ForestLightFondo
)

val OceanDarkColorScheme = darkColorScheme(
    primary = OceanDarkAcento,
    onPrimary = OceanDarkFondo,
    primaryContainer = OceanDarkSuperficieSubtle,
    onPrimaryContainer = OceanDarkTexto,
    secondary = OceanDarkSecundario,
    onSecondary = OceanDarkFondo,
    secondaryContainer = OceanDarkSuperficie,
    onSecondaryContainer = OceanDarkTexto,
    background = OceanDarkFondo,
    onBackground = OceanDarkTexto,
    surface = OceanDarkSuperficie,
    onSurface = OceanDarkTexto,
    surfaceVariant = OceanDarkSuperficieSubtle,
    onSurfaceVariant = OceanDarkSecundario,
    outline = OceanDarkBorder,
    outlineVariant = OceanDarkSuperficieSubtle,
    error = OceanDarkAviso,
    onError = OceanDarkFondo
)

val OceanLightColorScheme = lightColorScheme(
    primary = OceanLightAcento,
    onPrimary = OceanLightFondo,
    primaryContainer = OceanLightSuperficieSubtle,
    onPrimaryContainer = OceanLightTexto,
    secondary = OceanLightSecundario,
    onSecondary = OceanLightFondo,
    secondaryContainer = OceanLightSuperficie,
    onSecondaryContainer = OceanLightTexto,
    background = OceanLightFondo,
    onBackground = OceanLightTexto,
    surface = OceanLightSuperficie,
    onSurface = OceanLightTexto,
    surfaceVariant = OceanLightSuperficieSubtle,
    onSurfaceVariant = OceanLightSecundario,
    outline = OceanLightBorder,
    outlineVariant = OceanLightSuperficieSubtle,
    error = OceanLightAviso,
    onError = OceanLightFondo
)

val DuneDarkColorScheme = darkColorScheme(
    primary = DuneDarkAcento,
    onPrimary = DuneDarkFondo,
    primaryContainer = DuneDarkSuperficieSubtle,
    onPrimaryContainer = DuneDarkTexto,
    secondary = DuneDarkSecundario,
    onSecondary = DuneDarkFondo,
    secondaryContainer = DuneDarkSuperficie,
    onSecondaryContainer = DuneDarkTexto,
    background = DuneDarkFondo,
    onBackground = DuneDarkTexto,
    surface = DuneDarkSuperficie,
    onSurface = DuneDarkTexto,
    surfaceVariant = DuneDarkSuperficieSubtle,
    onSurfaceVariant = DuneDarkSecundario,
    outline = DuneDarkBorder,
    outlineVariant = DuneDarkSuperficieSubtle,
    error = DuneDarkAviso,
    onError = DuneDarkFondo
)

val DuneLightColorScheme = lightColorScheme(
    primary = DuneLightAcento,
    onPrimary = DuneLightFondo,
    primaryContainer = DuneLightSuperficieSubtle,
    onPrimaryContainer = DuneLightTexto,
    secondary = DuneLightSecundario,
    onSecondary = DuneLightFondo,
    secondaryContainer = DuneLightSuperficie,
    onSecondaryContainer = DuneLightTexto,
    background = DuneLightFondo,
    onBackground = DuneLightTexto,
    surface = DuneLightSuperficie,
    onSurface = DuneLightTexto,
    surfaceVariant = DuneLightSuperficieSubtle,
    onSurfaceVariant = DuneLightSecundario,
    outline = DuneLightBorder,
    outlineVariant = DuneLightSuperficieSubtle,
    error = DuneLightAviso,
    onError = DuneLightFondo
)

// Legacy aliases
val HermitDarkColorScheme = ForestDarkColorScheme
val HermitLightColorScheme = ForestLightColorScheme

@Composable
fun HermitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: String = "forest",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (palette.lowercase()) {
        "ocean" -> if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
        "dune" -> if (darkTheme) DuneDarkColorScheme else DuneLightColorScheme
        else -> if (darkTheme) ForestDarkColorScheme else ForestLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
