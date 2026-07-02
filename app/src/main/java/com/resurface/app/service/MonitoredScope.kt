package com.resurface.app.service

/**
 * Pure scope decision, extracted so it is JVM-unit-testable (the service itself is
 * framework-bound). Maps the monitored set to the `packageNames` array for
 * `setServiceInfo`, or `null` when empty.
 *
 * Returning `null` for an empty set is deliberate: an empty array has ambiguous
 * semantics and has historically been treated as "listen to ALL packages", which
 * would silently widen monitoring and corrupt a frozen-set study. The caller must
 * early-return on `null` rather than pass it through.
 */
object MonitoredScope {
    fun packageNamesOrNull(pkgs: Set<String>): Array<String>? =
        if (pkgs.isEmpty()) null else pkgs.toTypedArray()
}
