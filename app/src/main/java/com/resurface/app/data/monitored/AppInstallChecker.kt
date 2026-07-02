package com.resurface.app.data.monitored

import android.content.Context
import android.content.pm.PackageManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** Whether a package is installed. Abstracted so the ViewModel stays JVM-unit-testable. */
interface AppInstallChecker {
    fun isInstalled(packageName: String): Boolean
}

@Singleton
class PackageManagerInstallChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppInstallChecker {
    override fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppInstallCheckerModule {
    @Binds
    abstract fun bindAppInstallChecker(impl: PackageManagerInstallChecker): AppInstallChecker
}
