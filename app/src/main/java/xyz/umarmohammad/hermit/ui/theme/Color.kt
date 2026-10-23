package xyz.umarmohammad.hermit.ui.theme

import androidx.compose.ui.graphics.Color

// Hermit Calm Monastic Design Tokens
val Fondo = Color(0xFF191A18)          // Fondo general
val Superficie = Color(0xFF242521)     // Paneles y tarjetas
val SuperficieSubtle = Color(0xFF2E302B) // Paneles sutiles / divisores
val Texto = Color(0xFFF0EDE5)          // Texto principal
val Secundario = Color(0xFFB9B8AE)     // Texto auxiliar
val Acento = Color(0xFFA5B092)         // Acciones y estado activo
val Aviso = Color(0xFFD58B70)          // Alertas y estados críticos

// Semantic references mapped to the new palette
val PrimaryTeal = Acento
val PrimaryTealLight = Color(0xFFBAC4A9)
val PrimaryTealDark = Color(0xFF838E71)

val AccentAmber = Aviso
val AccentSage = Acento
val AccentBlue = Color(0xFF8E9B84)
val AccentPurple = Color(0xFFB4A89C)

// Surfaces - Dark (Hermit sanctuary mode)
val SurfaceDark = Fondo
val SurfaceDarkCard = Superficie
val SurfaceDarkSubtle = SuperficieSubtle
val TextDarkPrimary = Texto
val TextDarkSecondary = Secundario

// Surfaces - Light (Warm parchment sanctuary)
val SurfaceLight = Fondo
val SurfaceLightCard = Superficie
val SurfaceLightSubtle = SuperficieSubtle
val TextLightPrimary = Texto
val TextLightSecondary = Secundario

val BorderLight = Color(0xFF383A34)
val BorderDark = Color(0xFF383A34)
val DestructiveRed = Aviso
