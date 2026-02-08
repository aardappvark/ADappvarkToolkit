package com.adappvark.toolkit.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.adappvark.toolkit.service.AutoUninstallService

/**
 * Helper utility for managing Accessibility Service
 */
object AccessibilityHelper {

    /**
     * Check if our accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, AutoUninstallService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }

        return false
    }

    /**
     * Open accessibility settings - goes directly to our service's page if possible
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Check if the service is actually running
     */
    fun isServiceRunning(): Boolean {
        return AutoUninstallService.isServiceRunning
    }

    /**
     * Enable auto-click mode for UNINSTALL (call before starting uninstall sequence)
     */
    fun enableAutoClick() {
        AutoUninstallService.currentMode = AutoUninstallService.Companion.Mode.UNINSTALL
        AutoUninstallService.autoClickEnabled = true
    }

    /**
     * Enable auto-click mode for INSTALL (call before starting reinstall sequence)
     */
    fun enableAutoClickForInstall() {
        AutoUninstallService.currentMode = AutoUninstallService.Companion.Mode.INSTALL
        AutoUninstallService.autoClickEnabled = true
    }

    /**
     * Enable auto-click mode for CATEGORY_INSTALL
     * This mode scans category list views and clicks Install for matching apps
     */
    fun enableCategoryInstallMode(appNames: Set<String>) {
        AutoUninstallService.setTargetApps(appNames)
        AutoUninstallService.currentMode = AutoUninstallService.Companion.Mode.CATEGORY_INSTALL
        AutoUninstallService.autoClickEnabled = true
    }

    /**
     * Get the count of how many Install buttons have been clicked
     */
    fun getInstallClickCount(): Int {
        return AutoUninstallService.installClickCount
    }

    /**
     * Get the set of app names that have been successfully installed (clicked)
     */
    fun getInstalledAppNames(): Set<String> {
        return AutoUninstallService.installedAppNames.toSet()
    }

    /**
     * Get the set of app names that were found already installed (Open button)
     */
    fun getAlreadyInstalledAppNames(): Set<String> {
        return AutoUninstallService.alreadyInstalledAppNames.toSet()
    }

    /**
     * Clear the target apps list
     */
    fun clearTargetApps() {
        AutoUninstallService.clearTargetApps()
    }

    /**
     * Disable auto-click mode (call after uninstall/install sequence completes)
     */
    fun disableAutoClick() {
        AutoUninstallService.autoClickEnabled = false
    }

    /**
     * Check if the accessibility service found an "Open" button
     * This indicates the app is already installed
     */
    fun wasOpenButtonFound(): Boolean {
        return AutoUninstallService.foundOpenButton
    }

    /**
     * Check if installation is currently in progress (Cancel button visible)
     */
    fun isInstallationInProgress(): Boolean {
        return AutoUninstallService.installationInProgress
    }

    /**
     * Check if the Install button was successfully clicked
     * This is set immediately when we click Install, before Cancel button appears
     * Critical for large apps where Cancel button may take time to appear
     */
    fun wasInstallButtonClicked(): Boolean {
        return AutoUninstallService.installButtonClicked
    }

    /**
     * Reset the "Open" button flag before checking each app
     */
    fun resetOpenButtonFlag() {
        AutoUninstallService.resetOpenButtonFlag()
    }
}
