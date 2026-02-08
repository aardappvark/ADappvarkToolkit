package com.adappvark.toolkit.data.model

import android.graphics.drawable.Drawable

/**
 * Represents a dApp installed on the device
 */
data class DAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val installedTime: Long,
    val lastUpdateTime: Long,
    val sizeInBytes: Long,
    val isSystemApp: Boolean,
    val installSource: String?
) {
    /**
     * Check if this is a dApp from Solana dApp Store
     */
    fun isDAppStoreApp(): Boolean {
        return installSource == "com.solanamobile.apps" || 
               installSource == "com.solanamobile.dappstore"
    }
    
    /**
     * Format size for display
     */
    fun getFormattedSize(): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            sizeInBytes < 1024 * 1024 * 1024 -> "${sizeInBytes / (1024 * 1024)} MB"
            else -> "${sizeInBytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Get days since install
     */
    fun getDaysSinceInstall(): Int {
        val now = System.currentTimeMillis()
        val diff = now - installedTime
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}

/**
 * Filter criteria for dApp list
 */
enum class DAppFilter {
    ALL,
    DAPP_STORE_ONLY,
    LARGE_SIZE,  // > 100MB
    OLD_INSTALLS  // > 30 days
}

/**
 * Sort criteria for dApp list
 */
enum class DAppSort {
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
    INSTALL_DATE_ASC,
    INSTALL_DATE_DESC
}
