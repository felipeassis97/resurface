package com.resurface.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.resurface.app.R

/**
 * Resurface typography — Fraunces (Display/Headline) + Plus Jakarta Sans (Title/Body/Label).
 *
 * Fraunces ships Light 300 / Regular 400 / SemiBold 600 / Italic only — NO Medium 500.
 * Display uses Light (airy = breath); Headline uses Regular; SemiBold is the emphasized cap.
 * Fraunces is never used below 24sp (Headline S floor); all Title/Body/Label use Plus Jakarta.
 * See openspec/changes/design-system/specs/design-system/spec.md.
 */

val Fraunces = FontFamily(
    Font(R.font.fraunces_light, FontWeight.Light),
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

val PlusJakarta = FontFamily(
    Font(R.font.plus_jakarta_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_bold, FontWeight.Bold),
)

val Typography = Typography(
    // Display — Fraunces Light 300 (airy). Floor for Light is 36sp.
    displayLarge = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Light,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Light,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Light,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp,
    ),
    // Headline — Fraunces Regular 400. 24sp is the Fraunces floor.
    headlineLarge = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Normal,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp,
    ),
    // Title — Plus Jakarta SemiBold 600.
    titleLarge = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    // Body — Plus Jakarta Regular 400.
    bodyLarge = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    // Label — Plus Jakarta SemiBold 600 (L) / Medium 500 (M, S).
    labelLarge = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)

/**
 * Stat / numeric styles with tabular figures ("tnum") so digit width does not
 * jitter as counters tick (e.g. minutes scrolled). Use these instead of the
 * proportional Display/Body styles for live numbers.
 */
object ResurfaceTextStyles {
    /** Big "rising tide" number — Fraunces Light, tabular. */
    val statDisplay = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Light,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    )

    /** Inline stat number in dashboards — Plus Jakarta, tabular. */
    val statBody = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )
}
