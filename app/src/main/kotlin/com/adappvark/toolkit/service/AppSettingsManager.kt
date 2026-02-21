package com.adappvark.toolkit.service

import android.content.Context
import android.content.SharedPreferences

/**
 * AppSettingsManager — Persists user-facing UI/behaviour preferences.
 *
 * All toggles default to sensible values and are read synchronously
 * via SharedPreferences (no suspend needed).
 */
class AppSettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "aardappvark_app_settings"

        // Appearance
        private const val KEY_DARK_MODE = "dark_mode"               // "system", "dark", "light"
        private const val KEY_COMPACT_LIST = "compact_list"         // boolean

        // Haptics
        private const val KEY_HAPTICS_SELECTION = "haptics_selection"       // tap/toggle haptics
        private const val KEY_HAPTICS_UNINSTALL = "haptics_uninstall"       // uninstall button
        private const val KEY_HAPTICS_REINSTALL = "haptics_reinstall"       // reinstall button

        // Uninstall behaviour
        private const val KEY_AUTO_ACCEPT_DELETE = "auto_accept_delete"     // auto-tap OK on delete dialog
        private const val KEY_CONFIRM_BEFORE_UNINSTALL = "confirm_before_uninstall" // show confirm dialog

        // Reinstall behaviour
        private const val KEY_AUTO_ACCEPT_INSTALL = "auto_accept_install"   // auto-tap Install button
        private const val KEY_PULL_TO_REFRESH = "pull_to_refresh"           // enable pull-to-refresh

        // List display
        private const val KEY_SHOW_PACKAGE_NAMES = "show_package_names"     // show package names in lists
        private const val KEY_SHOW_UNINSTALLED_SECTION = "show_uninstalled_section"   // show faded uninstalled
        private const val KEY_SHOW_NOT_INSTALLED_SECTION = "show_not_installed_section" // show registry section

        // Animations
        private const val KEY_SCREEN_ANIMATIONS = "screen_animations"       // transitions between tabs

        // Default filter
        private const val KEY_DEFAULT_FILTER = "default_filter"             // "dapp_store", "all", "large", "old"

        // Payment gating for auto-accept
        private const val KEY_AUTO_ACCEPT_PAID_UNTIL = "auto_accept_paid_until" // expiry timestamp
    }

    // ==================== Appearance ====================

    fun getDarkMode(): String = prefs.getString(KEY_DARK_MODE, "system") ?: "system"
    fun setDarkMode(mode: String) = prefs.edit().putString(KEY_DARK_MODE, mode).apply()

    fun isCompactList(): Boolean = prefs.getBoolean(KEY_COMPACT_LIST, false)
    fun setCompactList(enabled: Boolean) = prefs.edit().putBoolean(KEY_COMPACT_LIST, enabled).apply()

    // ==================== Haptics ====================

    fun isSelectionHapticsEnabled(): Boolean = prefs.getBoolean(KEY_HAPTICS_SELECTION, true)
    fun setSelectionHaptics(enabled: Boolean) = prefs.edit().putBoolean(KEY_HAPTICS_SELECTION, enabled).apply()

    fun isUninstallHapticsEnabled(): Boolean = prefs.getBoolean(KEY_HAPTICS_UNINSTALL, true)
    fun setUninstallHaptics(enabled: Boolean) = prefs.edit().putBoolean(KEY_HAPTICS_UNINSTALL, enabled).apply()

    fun isReinstallHapticsEnabled(): Boolean = prefs.getBoolean(KEY_HAPTICS_REINSTALL, true)
    fun setReinstallHaptics(enabled: Boolean) = prefs.edit().putBoolean(KEY_HAPTICS_REINSTALL, enabled).apply()

    // ==================== Auto-Accept Payment Gating ====================

    /**
     * Check if auto-accept features are paid for.
     * Returns true if the current time is before the paid-until expiry.
     */
    fun isAutoAcceptPaidFor(): Boolean {
        val paidUntil = prefs.getLong(KEY_AUTO_ACCEPT_PAID_UNTIL, 0L)
        return System.currentTimeMillis() < paidUntil
    }

    /**
     * Set the auto-accept payment expiry timestamp.
     * Called after a successful payment transaction.
     */
    fun setAutoAcceptPaidUntil(expiresAt: Long) {
        prefs.edit().putLong(KEY_AUTO_ACCEPT_PAID_UNTIL, expiresAt).apply()
    }

    /**
     * Get remaining days on auto-accept pass.
     */
    fun getAutoAcceptDaysRemaining(): Int {
        val paidUntil = prefs.getLong(KEY_AUTO_ACCEPT_PAID_UNTIL, 0L)
        val remaining = paidUntil - System.currentTimeMillis()
        return if (remaining > 0) (remaining / (1000 * 60 * 60 * 24)).toInt() else 0
    }

    // ==================== Uninstall Behaviour ====================

    /** Raw setting value (user preference, not gated) */
    fun isAutoAcceptDeleteSettingOn(): Boolean = prefs.getBoolean(KEY_AUTO_ACCEPT_DELETE, true)

    /** Effective value: setting AND (payment OR SGT verified) required */
    fun isAutoAcceptDeleteEnabled(isSgtVerified: Boolean = false): Boolean =
        isAutoAcceptDeleteSettingOn() && (isAutoAcceptPaidFor() || isSgtVerified)
    fun setAutoAcceptDelete(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_ACCEPT_DELETE, enabled).apply()

    fun isConfirmBeforeUninstallEnabled(): Boolean = prefs.getBoolean(KEY_CONFIRM_BEFORE_UNINSTALL, true)
    fun setConfirmBeforeUninstall(enabled: Boolean) = prefs.edit().putBoolean(KEY_CONFIRM_BEFORE_UNINSTALL, enabled).apply()

    // ==================== Reinstall Behaviour ====================

    /** Raw setting value (user preference, not gated) */
    fun isAutoAcceptInstallSettingOn(): Boolean = prefs.getBoolean(KEY_AUTO_ACCEPT_INSTALL, true)

    /** Effective value: setting AND (payment OR SGT verified) required */
    fun isAutoAcceptInstallEnabled(isSgtVerified: Boolean = false): Boolean =
        isAutoAcceptInstallSettingOn() && (isAutoAcceptPaidFor() || isSgtVerified)
    fun setAutoAcceptInstall(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_ACCEPT_INSTALL, enabled).apply()

    fun isPullToRefreshEnabled(): Boolean = prefs.getBoolean(KEY_PULL_TO_REFRESH, true)
    fun setPullToRefresh(enabled: Boolean) = prefs.edit().putBoolean(KEY_PULL_TO_REFRESH, enabled).apply()

    // ==================== List Display ====================

    fun isShowPackageNamesEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_PACKAGE_NAMES, false)
    fun setShowPackageNames(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_PACKAGE_NAMES, enabled).apply()

    fun isShowUninstalledSectionEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_UNINSTALLED_SECTION, true)
    fun setShowUninstalledSection(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_UNINSTALLED_SECTION, enabled).apply()

    fun isShowNotInstalledSectionEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_NOT_INSTALLED_SECTION, true)
    fun setShowNotInstalledSection(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_NOT_INSTALLED_SECTION, enabled).apply()

    // ==================== Animations ====================

    fun isScreenAnimationsEnabled(): Boolean = prefs.getBoolean(KEY_SCREEN_ANIMATIONS, true)
    fun setScreenAnimations(enabled: Boolean) = prefs.edit().putBoolean(KEY_SCREEN_ANIMATIONS, enabled).apply()

    // ==================== Default Filter ====================

    fun getDefaultFilter(): String = prefs.getString(KEY_DEFAULT_FILTER, "dapp_store") ?: "dapp_store"
    fun setDefaultFilter(filter: String) = prefs.edit().putString(KEY_DEFAULT_FILTER, filter).apply()

    // ==================== Reset ====================

    fun resetAll() {
        prefs.edit().clear().apply()
    }
}
