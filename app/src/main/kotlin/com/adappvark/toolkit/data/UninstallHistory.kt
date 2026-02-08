package com.adappvark.toolkit.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Represents an uninstalled app that can be reinstalled
 */
@Serializable
data class UninstalledApp(
    val packageName: String,
    val appName: String,
    val uninstalledAt: Long,
    val sizeInBytes: Long,
    val versionName: String,
    val reinstalled: Boolean = false,
    val reinstalledAt: Long? = null,
    val skipReinstall: Boolean = false  // If true, don't include in bulk reinstall
)

/**
 * Manages the history of uninstalled apps for the reinstall feature
 */
class UninstallHistory(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val PREFS_NAME = "uninstall_history"
        private const val KEY_HISTORY = "history_list"
    }

    /**
     * Check if an app is actually installed on the device
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Sync the reinstall status with actual device state
     * - If marked reinstalled but app is NOT installed → mark as NOT reinstalled
     * - If marked NOT reinstalled but app IS installed → mark as reinstalled
     * Returns the updated list
     */
    fun syncWithDeviceState(): List<UninstalledApp> {
        val history = getHistory().toMutableList()
        var changed = false

        history.forEachIndexed { index, app ->
            val isInstalled = isAppInstalled(app.packageName)

            if (app.reinstalled && !isInstalled) {
                // Was marked reinstalled but app is not actually installed
                history[index] = app.copy(reinstalled = false, reinstalledAt = null)
                changed = true
            } else if (!app.reinstalled && isInstalled) {
                // Was not marked reinstalled but app is actually installed
                history[index] = app.copy(reinstalled = true, reinstalledAt = System.currentTimeMillis())
                changed = true
            }
        }

        if (changed) {
            saveHistory(history)
        }

        return history
    }

    /**
     * Add an app to the uninstall history
     */
    fun addToHistory(
        packageName: String,
        appName: String,
        sizeInBytes: Long,
        versionName: String
    ) {
        val history = getHistory().toMutableList()

        // Remove if already exists (update)
        history.removeAll { it.packageName == packageName }

        // Add new entry
        history.add(
            UninstalledApp(
                packageName = packageName,
                appName = appName,
                uninstalledAt = System.currentTimeMillis(),
                sizeInBytes = sizeInBytes,
                versionName = versionName
            )
        )

        // Save
        saveHistory(history)
    }

    /**
     * Remove an app from history (e.g., after successful reinstall)
     */
    fun removeFromHistory(packageName: String) {
        val history = getHistory().toMutableList()
        history.removeAll { it.packageName == packageName }
        saveHistory(history)
    }

    /**
     * Mark an app as reinstalled (keeps it in history but shows as reinstalled)
     */
    fun markAsReinstalled(packageName: String) {
        val history = getHistory().toMutableList()
        val index = history.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            history[index] = history[index].copy(
                reinstalled = true,
                reinstalledAt = System.currentTimeMillis()
            )
            saveHistory(history)
        }
    }

    /**
     * Toggle the skipReinstall flag for an app
     * Apps with skipReinstall=true won't be included in bulk reinstall
     */
    fun toggleSkipReinstall(packageName: String): Boolean {
        val history = getHistory().toMutableList()
        val index = history.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            val newValue = !history[index].skipReinstall
            history[index] = history[index].copy(skipReinstall = newValue)
            saveHistory(history)
            return newValue
        }
        return false
    }

    /**
     * Set the skipReinstall flag for an app
     */
    fun setSkipReinstall(packageName: String, skip: Boolean) {
        val history = getHistory().toMutableList()
        val index = history.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            history[index] = history[index].copy(skipReinstall = skip)
            saveHistory(history)
        }
    }

    /**
     * Get all uninstalled apps
     */
    fun getHistory(): List<UninstalledApp> {
        val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clear all history
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(history: List<UninstalledApp>) {
        val jsonString = json.encodeToString(history)
        prefs.edit().putString(KEY_HISTORY, jsonString).apply()
    }
}
