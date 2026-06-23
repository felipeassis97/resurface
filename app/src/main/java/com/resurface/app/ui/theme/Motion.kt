package com.resurface.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Resurface motion — calm "tidal" language.
 *
 * - Armed indicator: breathing loop (~4s inhale / 6s exhale).
 * - Intervention surface: rises from bottom (emphasized-decelerate, ~400ms),
 *   exits with emphasized-accelerate (~200ms), like breaking the water surface.
 * - Usage counter: rises via low-stiffness spring (rising tide).
 *
 * Honor reduce-motion: breathing -> static glow; intervention -> cross-fade.
 * See openspec/changes/design-system/specs/design-system/spec.md.
 */
object ResurfaceMotion {
    // M3 emphasized easing set.
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // Durations (ms).
    const val NudgeEnterMillis = 400
    const val NudgeExitMillis = 200

    // Breathing loop (ms) — slow, calm, asymmetric inhale/exhale.
    const val BreatheInhaleMillis = 4000
    const val BreatheExhaleMillis = 6000

    /** Low-stiffness spring for the rising-tide counter and gentle component motion. */
    fun <T> tidalSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow,
    )
}
