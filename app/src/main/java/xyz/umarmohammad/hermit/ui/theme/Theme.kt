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

val HermitDarkColorScheme = darkColorScheme(
    primary = DarkAcento,
    onPrimary = DarkFondo,
    primaryContainer = DarkSuperficieSubtle,
    onPrimaryContainer = DarkTexto,
    secondary = DarkSecundario,
    onSecondary = DarkFondo,
    secondaryContainer = DarkSuperficie,
    onSecondaryContainer = DarkTexto,
    background = DarkFondo,
    onBackground = DarkTexto,
    surface = DarkSuperficie,
    onSurface = DarkTexto,
    surfaceVariant = DarkSuperficieSubtle,
    onSurfaceVariant = DarkSecundario,
    outline = DarkBorder,
    outlineVariant = DarkSuperficieSubtle,
    error = DarkAviso,
    onError = DarkFondo
)

val HermitLightColorScheme = lightColorScheme(
    primary = LightAcento,
    onPrimary = LightFondo,
    primaryContainer = LightSuperficieSubtle,
    onPrimaryContainer = LightTexto,
    secondary = LightSecundario,
    onSecondary = LightFondo,
    secondaryContainer = LightSuperficie,
    onSecondaryContainer = LightTexto,
    background = LightFondo,
    onBackground = LightTexto,
    surface = LightSuperficie,
    onSurface = LightTexto,
    surfaceVariant = LightSuperficieSubtle,
    onSurfaceVariant = LightSecundario,
    outline = LightBorder,
    outlineVariant = LightSuperficieSubtle,
    error = LightAviso,
    onError = LightFondo
)

@Composable
fun HermitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HermitDarkColorScheme else HermitLightColorScheme

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
