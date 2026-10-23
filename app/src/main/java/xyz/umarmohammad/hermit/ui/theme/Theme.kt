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

private val HermitColorScheme = darkColorScheme(
    primary = Acento,
    onPrimary = Fondo,
    primaryContainer = SuperficieSubtle,
    onPrimaryContainer = Texto,
    secondary = Secundario,
    onSecondary = Fondo,
    secondaryContainer = Superficie,
    onSecondaryContainer = Texto,
    background = Fondo,
    onBackground = Texto,
    surface = Superficie,
    onSurface = Texto,
    surfaceVariant = SuperficieSubtle,
    onSurfaceVariant = Secundario,
    outline = BorderDark,
    error = Aviso,
    onError = Fondo
)

@Composable
fun HermitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Hermit enforces its signature monastic calm palette across all themes for focus purity
    val colorScheme = HermitColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
