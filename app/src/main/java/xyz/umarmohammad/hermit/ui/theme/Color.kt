package xyz.umarmohammad.hermit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Static palette tokens - Forest (Default - Salvia / Bosque)
val ForestDarkFondo = Color(0xFF191A18)          // Fondo general OLED/dark
val ForestDarkSuperficie = Color(0xFF242521)     // Paneles y tarjetas oscuras
val ForestDarkSuperficieSubtle = Color(0xFF2E302B) // Paneles sutiles / divisores
val ForestDarkTexto = Color(0xFFF0EDE5)          // Texto principal claro
val ForestDarkSecundario = Color(0xFFB9B8AE)     // Texto auxiliar gris cálido
val ForestDarkAcento = Color(0xFFA5B092)         // Salvia acento
val ForestDarkAviso = Color(0xFFD58B70)          // Terracota alerta
val ForestDarkBorder = Color(0xFF383A34)         // Borde oscuro sutil

val ForestLightFondo = Color(0xFFF6F3EC)         // Fondo pergamino cálido
val ForestLightSuperficie = Color(0xFFFFFFFF)    // Tarjetas blanco puro
val ForestLightSuperficieSubtle = Color(0xFFEBE8E0) // Paneles secundarios
val ForestLightTexto = Color(0xFF191A18)         // Tinta oscura legible
val ForestLightSecundario = Color(0xFF63655E)    // Texto auxiliar piedra
val ForestLightAcento = Color(0xFF4A5C43)        // Salvia bosque profundo
val ForestLightAviso = Color(0xFFB54C34)         // Terracota alerta
val ForestLightBorder = Color(0xFFD3D0C7)        // Borde claro sutil

// Backwards-compatible aliases for Forest
val DarkFondo = ForestDarkFondo
val DarkSuperficie = ForestDarkSuperficie
val DarkSuperficieSubtle = ForestDarkSuperficieSubtle
val DarkTexto = ForestDarkTexto
val DarkSecundario = ForestDarkSecundario
val DarkAcento = ForestDarkAcento
val DarkAviso = ForestDarkAviso
val DarkBorder = ForestDarkBorder

val LightFondo = ForestLightFondo
val LightSuperficie = ForestLightSuperficie
val LightSuperficieSubtle = ForestLightSuperficieSubtle
val LightTexto = ForestLightTexto
val LightSecundario = ForestLightSecundario
val LightAcento = ForestLightAcento
val LightAviso = ForestLightAviso
val LightBorder = ForestLightBorder

// Static palette tokens - Ocean (Océano / Ártico)
val OceanDarkFondo = Color(0xFF0F151B)
val OceanDarkSuperficie = Color(0xFF182028)
val OceanDarkSuperficieSubtle = Color(0xFF222C36)
val OceanDarkTexto = Color(0xFFEDF3F7)
val OceanDarkSecundario = Color(0xFFA8B7C2)
val OceanDarkAcento = Color(0xFF7FAEC2)          // Glacial nordic blue
val OceanDarkAviso = Color(0xFFD58B70)
val OceanDarkBorder = Color(0xFF2C3946)

val OceanLightFondo = Color(0xFFF2F6F9)
val OceanLightSuperficie = Color(0xFFFFFFFF)
val OceanLightSuperficieSubtle = Color(0xFFE3ECF2)
val OceanLightTexto = Color(0xFF141E26)
val OceanLightSecundario = Color(0xFF5A6C78)
val OceanLightAcento = Color(0xFF2D5568)         // Deep marine blue
val OceanLightAviso = Color(0xFFB54C34)
val OceanLightBorder = Color(0xFFCBD6DE)

// Static palette tokens - Dune (Duna / Terracota)
val DuneDarkFondo = Color(0xFF181513)
val DuneDarkSuperficie = Color(0xFF231E1B)
val DuneDarkSuperficieSubtle = Color(0xFF2E2723)
val DuneDarkTexto = Color(0xFFF5EFEB)
val DuneDarkSecundario = Color(0xFFBFB2A8)
val DuneDarkAcento = Color(0xFFDCA172)           // Warm terracotta / sand
val DuneDarkAviso = Color(0xFFD58B70)
val DuneDarkBorder = Color(0xFF3D342E)

val DuneLightFondo = Color(0xFFF8F5EE)
val DuneLightSuperficie = Color(0xFFFFFFFF)
val DuneLightSuperficieSubtle = Color(0xFFEFE8DD)
val DuneLightTexto = Color(0xFF211712)
val DuneLightSecundario = Color(0xFF70645B)
val DuneLightAcento = Color(0xFF8B4722)          // Grounded terracotta umber
val DuneLightAviso = Color(0xFFB54C34)
val DuneLightBorder = Color(0xFFD8D0C3)

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
