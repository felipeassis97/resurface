package com.resurface.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Resurface color tokens — fixed Material 3 schemes built from four key colors
 * (teal / slate-teal / dawn-amber / cool-neutral), "surfacing for air".
 *
 * Values are normative; see openspec/changes/design-system/specs/design-system/spec.md.
 * Do NOT pair colors outside their intended on-/container pairs.
 */

// ---- Light scheme ----
val LightPrimary = Color(0xFF006A65)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFA4F2EA)
val LightOnPrimaryContainer = Color(0xFF00201F)
val LightSecondary = Color(0xFF4A6360)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFCCE8E3)
val LightOnSecondaryContainer = Color(0xFF051F1D)
val LightTertiary = Color(0xFF7C5800)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDEA1)
val LightOnTertiaryContainer = Color(0xFF271900)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)
val LightSurface = Color(0xFFF5FAF7)
val LightOnSurface = Color(0xFF161D1C)
val LightOnSurfaceVariant = Color(0xFF3F4946)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFEFF5F2)
val LightSurfaceContainer = Color(0xFFEAEFEC)
val LightSurfaceContainerHigh = Color(0xFFE4EAE7)
val LightSurfaceContainerHighest = Color(0xFFDEE4E1)
val LightSurfaceDim = Color(0xFFD5DBD8)
val LightSurfaceBright = Color(0xFFF5FAF7)
val LightOutline = Color(0xFF6F7A77)
val LightOutlineVariant = Color(0xFFBFC9C5)
val LightInverseSurface = Color(0xFF2B3231)
val LightInverseOnSurface = Color(0xFFECF2EF)
val LightInversePrimary = Color(0xFF5DDBD1)

// ---- Dark scheme ----
val DarkPrimary = Color(0xFF5DDBD1)
val DarkOnPrimary = Color(0xFF003735)
val DarkPrimaryContainer = Color(0xFF00504C)
val DarkOnPrimaryContainer = Color(0xFFA4F2EA)
val DarkSecondary = Color(0xFFB1CCC7)
val DarkOnSecondary = Color(0xFF1C3531)
val DarkSecondaryContainer = Color(0xFF334B48)
val DarkOnSecondaryContainer = Color(0xFFCCE8E3)
val DarkTertiary = Color(0xFFF2BE48)
val DarkOnTertiary = Color(0xFF412D00)
val DarkTertiaryContainer = Color(0xFF5E4200)
val DarkOnTertiaryContainer = Color(0xFFFFDEA1)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
val DarkSurface = Color(0xFF0E1413)
val DarkOnSurface = Color(0xFFDEE4E1)
val DarkOnSurfaceVariant = Color(0xFFBFC9C5)
val DarkSurfaceContainerLowest = Color(0xFF090F0E)
val DarkSurfaceContainerLow = Color(0xFF161D1C)
val DarkSurfaceContainer = Color(0xFF1A2120)
val DarkSurfaceContainerHigh = Color(0xFF242B2A)
val DarkSurfaceContainerHighest = Color(0xFF2F3635)
val DarkSurfaceDim = Color(0xFF0E1413)
val DarkSurfaceBright = Color(0xFF343B39)
val DarkOutline = Color(0xFF899491)
val DarkOutlineVariant = Color(0xFF3F4946)
val DarkInverseSurface = Color(0xFFDEE4E1)
val DarkInverseOnSurface = Color(0xFF2B3231)
val DarkInversePrimary = Color(0xFF006A65)

// ---- Custom: success ("focus kept" / positive reinforcement) ----
// Harmonized toward primary teal. Lives outside M3 ColorScheme; surfaced via
// ResurfaceColors extension (see Theme.kt). Used for the "you chose to stop" moment.
val LightSuccess = Color(0xFF3B6939)
val LightOnSuccess = Color(0xFFFFFFFF)
val LightSuccessContainer = Color(0xFFBCF0B4)
val LightOnSuccessContainer = Color(0xFF00210A)
val DarkSuccess = Color(0xFFA1D399)
val DarkOnSuccess = Color(0xFF0A3910)
val DarkSuccessContainer = Color(0xFF235021)
val DarkOnSuccessContainer = Color(0xFFBCF0B4)
