package com.adappvark.toolkit.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.adappvark.toolkit.data.model.DAppFilter
import com.adappvark.toolkit.data.model.DAppInfo
import com.adappvark.toolkit.data.model.DAppSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for managing package scanning and filtering
 */
class PackageManagerService(private val context: Context) {
    
    private val packageManager = context.packageManager
    
    /**
     * Scan all installed packages and return as DAppInfo list
     */
    suspend fun scanInstalledApps(
        filter: DAppFilter = DAppFilter.ALL,
        sort: DAppSort = DAppSort.NAME_ASC
    ): List<DAppInfo> = withContext(Dispatchers.IO) {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        
        val dAppList = packages
            .filter { shouldIncludePackage(it, filter) }
            .mapNotNull { packageInfo ->
                try {
                    createDAppInfo(packageInfo)
                } catch (e: Exception) {
                    null  // Skip packages that fail to load
                }
            }
        
        sortDApps(dAppList, sort)
    }
    
    /**
     * Scan only dApp Store installed apps
     */
    suspend fun scanDAppStoreApps(): List<DAppInfo> = withContext(Dispatchers.IO) {
        scanInstalledApps(filter = DAppFilter.DAPP_STORE_ONLY)
    }
    
    /**
     * Get specific package info
     */
    suspend fun getPackageInfo(packageName: String): DAppInfo? = withContext(Dispatchers.IO) {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            createDAppInfo(packageInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    
    /**
     * Calculate total storage used by selected packages
     */
    suspend fun calculateTotalStorage(packageNames: List<String>): Long = withContext(Dispatchers.IO) {
        var total = 0L
        packageNames.forEach { packageName ->
            total += getPackageStorageSize(packageName)
        }
        total
    }
    
    /**
     * Get storage size for a package
     */
    private fun getPackageStorageSize(packageName: String): Long {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val appInfo = packageInfo.applicationInfo
            
            // Basic size estimation (code + data dirs)
            // Note: This is approximate. Full size calculation requires StorageStatsManager
            // which needs additional permissions
            java.io.File(appInfo.sourceDir).length()
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Check if package should be included based on filter
     */
    private fun shouldIncludePackage(packageInfo: PackageInfo, filter: DAppFilter): Boolean {
        val appInfo = packageInfo.applicationInfo

        // Never include our own app or critical dependencies in the list
        val excludedPackages = setOf(
            "com.adappvark.toolkit",        // Our own app
            "moe.shizuku.privileged.api",   // Shizuku (required for silent uninstall)
            "com.solanamobile.dappstore",   // Solana dApp Store
            "com.solanamobile.apps"         // Solana dApp Store (alt package)
        )
        if (packageInfo.packageName in excludedPackages) {
            return false
        }

        // Never include system apps unless specifically requested
        if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
            filter != DAppFilter.ALL) {
            return false
        }
        
        return when (filter) {
            DAppFilter.ALL -> true
            
            DAppFilter.DAPP_STORE_ONLY -> {
                val installSource = getInstallSource(packageInfo.packageName)
                // Match apps explicitly installed by the dApp Store
                installSource == "com.solanamobile.apps" ||
                installSource == "com.solanamobile.dappstore" ||
                // Also match non-system, non-Play Store apps (installer=null)
                // These are typically dApp Store or ADB-installed Solana ecosystem apps
                // The dApp Store doesn't always set itself as the installer source
                (installSource == null && appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                    && appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0)
            }
            
            DAppFilter.LARGE_SIZE -> {
                val size = getPackageStorageSize(packageInfo.packageName)
                size > 100 * 1024 * 1024  // > 100MB
            }
            
            DAppFilter.OLD_INSTALLS -> {
                val installTime = packageInfo.firstInstallTime
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
                installTime < thirtyDaysAgo
            }
        }
    }
    
    /**
     * Create DAppInfo from PackageInfo
     */
    private fun createDAppInfo(packageInfo: PackageInfo): DAppInfo {
        val appInfo = packageInfo.applicationInfo
        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val icon = packageManager.getApplicationIcon(appInfo)
        val size = getPackageStorageSize(packageInfo.packageName)
        val installSource = getInstallSource(packageInfo.packageName)
        
        return DAppInfo(
            packageName = packageInfo.packageName,
            appName = appName,
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = packageInfo.longVersionCode,
            icon = icon,
            installedTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            sizeInBytes = size,
            isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            installSource = installSource
        )
    }
    
    /**
     * Get the install source for a package
     */
    private fun getInstallSource(packageName: String): String? {
        return try {
            packageManager.getInstallerPackageName(packageName)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Sort dApps list
     */
    private fun sortDApps(dApps: List<DAppInfo>, sort: DAppSort): List<DAppInfo> {
        return when (sort) {
            DAppSort.NAME_ASC -> dApps.sortedBy { it.appName.lowercase() }
            DAppSort.NAME_DESC -> dApps.sortedByDescending { it.appName.lowercase() }
            DAppSort.SIZE_ASC -> dApps.sortedBy { it.sizeInBytes }
            DAppSort.SIZE_DESC -> dApps.sortedByDescending { it.sizeInBytes }
            DAppSort.INSTALL_DATE_ASC -> dApps.sortedBy { it.installedTime }
            DAppSort.INSTALL_DATE_DESC -> dApps.sortedByDescending { it.installedTime }
        }
    }
    
    /**
     * Get statistics for installed dApps
     */
    suspend fun getDAppStatistics(): DAppStatistics = withContext(Dispatchers.IO) {
        val allApps = scanInstalledApps(DAppFilter.ALL)
        val dAppStoreApps = allApps.filter { it.isDAppStoreApp() }
        val totalSize = allApps.sumOf { it.sizeInBytes }
        val dAppStoreSize = dAppStoreApps.sumOf { it.sizeInBytes }
        
        DAppStatistics(
            totalApps = allApps.size,
            dAppStoreApps = dAppStoreApps.size,
            totalSizeBytes = totalSize,
            dAppStoreSizeBytes = dAppStoreSize,
            avgSizeBytes = if (dAppStoreApps.isNotEmpty()) {
                dAppStoreSize / dAppStoreApps.size
            } else 0
        )
    }
}

/**
 * Statistics about installed dApps
 */
data class DAppStatistics(
    val totalApps: Int,
    val dAppStoreApps: Int,
    val totalSizeBytes: Long,
    val dAppStoreSizeBytes: Long,
    val avgSizeBytes: Long
) {
    fun getFormattedTotalSize(): String {
        return formatBytes(totalSizeBytes)
    }
    
    fun getFormattedDAppStoreSize(): String {
        return formatBytes(dAppStoreSizeBytes)
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
