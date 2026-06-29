package com.resurface.app.permission

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.resurface.app.service.ResurfaceAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for permission grant status. Status is always read live
 * from the OS — never from a persisted flag — because the user can revoke any
 * permission in system settings outside the app.
 */
@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun isGranted(permission: AppPermission): Boolean = when (permission) {
        AppPermission.USAGE_ACCESS -> isUsageAccessGranted()
        AppPermission.ACCESSIBILITY -> isAccessibilityEnabled()
        AppPermission.NOTIFICATIONS -> isNotificationsGranted()
    }

    /** Live status for every required permission. */
    fun statuses(): Map<AppPermission, Boolean> =
        AppPermission.required.associateWith { isGranted(it) }

    fun allRequiredGranted(): Boolean = AppPermission.required.all { isGranted(it) }

    private fun isUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(context, ResurfaceAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        for (component in splitter) {
            if (component.equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    private fun isNotificationsGranted(): Boolean {
        // POST_NOTIFICATIONS only exists on API 33+. Below that it is not applicable.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Settings intent to grant a special permission. Returns null for runtime
     * permissions, which are requested through the Activity Result dialog instead.
     */
    fun settingsIntent(permission: AppPermission): Intent? = when (permission) {
        AppPermission.USAGE_ACCESS ->
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        AppPermission.ACCESSIBILITY ->
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        AppPermission.NOTIFICATIONS -> null
    }
}
