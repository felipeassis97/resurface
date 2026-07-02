package com.resurface.app.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.resurface.app.data.monitored.MonitoredAppsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Always-armed accessibility service. In F3 it only *scopes* itself to the monitored
 * apps — detection (event handling, `canRetrieveWindowContent`) lands in F4.
 *
 * The DataStore is the source of truth; this service is a projection. On every
 * [onServiceConnected] it reconciles the persisted set into `serviceInfo.packageNames`
 * (the OEM-reliable primary path). It also collects the selection Flow, so live edits
 * apply as best-effort — but correctness never depends on the live update.
 */
@AndroidEntryPoint
class ResurfaceAccessibilityService : AccessibilityService() {

    @Inject lateinit var repo: MonitoredAppsRepository

    // AccessibilityService is not a LifecycleService, so there is no lifecycleScope.
    // Main.immediate: setServiceInfo touches connection state and must run on main.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        // onServiceConnected can fire more than once (unbind → rebind); avoid stacking collectors.
        collectJob?.cancel()
        collectJob = serviceScope.launch {
            repo.selection
                // DataStore emits IOException on corruption; a crashing a11y service
                // silently self-disables, so never let the collector throw.
                .catch { Log.e(TAG, "monitored selection stream failed", it) }
                .collect { pkgs -> applyMonitoredScope(pkgs) }
        }
    }

    /**
     * Reconcile `packageNames` from [pkgs]. Mutates the *existing* serviceInfo so the
     * event types, feedback type, flags, and notification timeout from XML are preserved.
     */
    private fun applyMonitoredScope(pkgs: Set<String>) {
        val names = MonitoredScope.packageNamesOrNull(pkgs) ?: return // never widen to all
        val info = serviceInfo ?: return // null before the service is connected
        info.packageNames = names
        // canRetrieveWindowContent stays false: it comes from XML flags (read-only here),
        // and reusing the existing info preserves it. F4 flips it via config, not F3.
        serviceInfo = info
        logInstalledStatus(pkgs)
    }

    /** Surface silent monitoring loss from a missing/variant package (viability-council §9). */
    private fun logInstalledStatus(pkgs: Set<String>) {
        val installed = pkgs.filter { pkg ->
            try {
                packageManager.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
        Log.i(TAG, "monitored scope applied: ${pkgs.size} apps, installed=$installed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: detection is implemented in F4.
    }

    override fun onInterrupt() {
        // No-op.
    }

    override fun onDestroy() {
        // Cancel in onDestroy, NOT onUnbind — the service may unbind then rebind.
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "Mindless"
    }
}
