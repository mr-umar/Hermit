package xyz.umarmohammad.hermit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Static palette tokens - Dark Sanctuary
val DarkFondo = Color(0xFF191A18)          // Fondo general OLED/dark
val DarkSuperficie = Color(0xFF242521)     // Paneles y tarjetas oscuras
val DarkSuperficieSubtle = Color(0xFF2E302B) // Paneles sutiles / divisores
val DarkTexto = Color(0xFFF0EDE5)          // Texto principal claro
val DarkSecundario = Color(0xFFB9B8AE)     // Texto auxiliar gris cálido
val DarkAcento = Color(0xFFA5B092)         // Salvia acento
val DarkAviso = Color(0xFFD58B70)          // Terracota alerta
val DarkBorder = Color(0xFF383A34)         // Borde oscuro sutil

// Static palette tokens - Warm Monastic Light
val LightFondo = Color(0xFFF6F3EC)         // Fondo pergamino cálido
val LightSuperficie = Color(0xFFFFFFFF)    // Tarjetas blanco puro
val LightSuperficieSubtle = Color(0xFFEBE8E0) // Paneles secundarios
val LightTexto = Color(0xFF191A18)         // Tinta oscura legible
val LightSecundario = Color(0xFF63655E)    // Texto auxiliar piedra
val LightAcento = Color(0xFF4A5C43)        // Salvia bosque profundo
val LightAviso = Color(0xFFB54C34)         // Terracota alerta
val LightBorder = Color(0xFFD3D0C7)        // Borde claro sutil

// Context-sensitive dynamic tokens reading directly from active MaterialTheme
val Fondo: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val Superficie: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val SuperficieSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val Texto: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val Secundario: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val Acento: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val Aviso: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.error

val BorderDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

// Semantic references mapped dynamically
val PrimaryTeal: Color
    @Composable
    @ReadOnlyComposable
    get() = Acento

val PrimaryTealLight = Color(0xFFBAC4A9)
val PrimaryTealDark = Color(0xFF838E71)

val AccentAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = Aviso

val AccentSage: Color
    @Composable
    @ReadOnlyComposable
    get() = Acento

val AccentBlue = Color(0xFF8E9B84)
val AccentPurple = Color(0xFFB4A89C)

// Surfaces - Dark (Hermit sanctuary mode)
val SurfaceDark = DarkFondo
val SurfaceDarkCard = DarkSuperficie
val SurfaceDarkSubtle = DarkSuperficieSubtle
val TextDarkPrimary = DarkTexto
val TextDarkSecondary = DarkSecundario

// Surfaces - Light (Warm parchment sanctuary)
val SurfaceLight = LightFondo
val SurfaceLightCard = LightSuperficie
val SurfaceLightSubtle = LightSuperficieSubtle
val TextLightPrimary = LightTexto
val TextLightSecondary = LightSecundario

val BorderLight = LightBorder
val DestructiveRed: Color
    @Composable
    @ReadOnlyComposable
    get() = Aviso
