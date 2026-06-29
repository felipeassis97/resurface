package com.resurface.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * No-op stub so the user can enable the accessibility service during onboarding.
 *
 * Detection logic (scroll/window-state handling, armed/active lifecycle) lands in F4;
 * this stub intentionally performs no detection or intervention.
 */
class ResurfaceAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: detection is implemented in F4.
    }

    override fun onInterrupt() {
        // No-op.
    }
}
