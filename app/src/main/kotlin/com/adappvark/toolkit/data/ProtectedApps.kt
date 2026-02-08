package com.adappvark.toolkit.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Manages apps that are protected from bulk uninstall (favorites)
 * These apps will be excluded from "Select All" in uninstall and shown with a star
 */
class ProtectedApps(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val PREFS_NAME = "protected_apps"
        private const val KEY_PROTECTED = "protected_list"
    }

    /**
     * Check if an app is protected (favorite)
     */
    fun isProtected(packageName: String): Boolean {
        return getProtectedPackages().contains(packageName)
    }

    /**
     * Add an app to protected list
     */
    fun protect(packageName: String) {
        val protected = getProtectedPackages().toMutableSet()
        protected.add(packageName)
        saveProtected(protected)
    }

    /**
     * Remove an app from protected list
     */
    fun unprotect(packageName: String) {
        val protected = getProtectedPackages().toMutableSet()
        protected.remove(packageName)
        saveProtected(protected)
    }

    /**
     * Toggle protection status
     * Returns the new protection state
     */
    fun toggleProtection(packageName: String): Boolean {
        val isCurrentlyProtected = isProtected(packageName)
        if (isCurrentlyProtected) {
            unprotect(packageName)
        } else {
            protect(packageName)
        }
        return !isCurrentlyProtected
    }

    /**
     * Get all protected package names
     */
    fun getProtectedPackages(): Set<String> {
        val jsonString = prefs.getString(KEY_PROTECTED, null) ?: return emptySet()
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Clear all protected apps
     */
    fun clearAll() {
        prefs.edit().remove(KEY_PROTECTED).apply()
    }

    private fun saveProtected(packages: Set<String>) {
        val jsonString = json.encodeToString(packages)
        prefs.edit().putString(KEY_PROTECTED, jsonString).apply()
    }
}
