package com.resurface.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Resurface shapes — soft, water-rounded. Buttons/chips full, cards large (16dp),
 * intervention sheet/dialog extra-large (28dp).
 * See openspec/changes/design-system/specs/design-system/spec.md.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // snackbar
    small = RoundedCornerShape(8.dp),        // text fields, menus
    medium = RoundedCornerShape(12.dp),      // small cards
    large = RoundedCornerShape(16.dp),       // cards, FAB, dashboard tiles
    extraLarge = RoundedCornerShape(28.dp),  // nudge sheet, dialogs, bottom sheet
)

/** Extra corner tokens beyond the 5 M3 slots. */
object ResurfaceShapes {
    val largeIncreased = RoundedCornerShape(20.dp) // featured stat card
    val full = RoundedCornerShape(percent = 50)    // buttons, chips, breathing dot
}
