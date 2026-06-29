package com.resurface.app.permission

/**
 * The permissions Resurface requests during onboarding (the core detection trio).
 *
 * [Type.SPECIAL] permissions cannot use the runtime dialog — they are granted by
 * sending the user to a system Settings screen and re-checking status on resume.
 * [Type.RUNTIME] permissions use the Activity Result permission dialog.
 *
 * Overlay (SYSTEM_ALERT_WINDOW) and Bluetooth are intentionally absent — they are
 * requested just-in-time by their own features (F5/F6).
 */
enum class AppPermission(val type: Type) {
    USAGE_ACCESS(Type.SPECIAL),
    ACCESSIBILITY(Type.SPECIAL),
    NOTIFICATIONS(Type.RUNTIME);

    enum class Type { SPECIAL, RUNTIME }

    companion object {
        /** All permissions required to leave onboarding. */
        val required: List<AppPermission> = entries.toList()
    }
}
