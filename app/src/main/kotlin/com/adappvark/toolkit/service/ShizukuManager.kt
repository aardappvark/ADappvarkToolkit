package com.adappvark.toolkit.service

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Manager for Shizuku integration
 * Handles permission requests and silent uninstall operations
 */
class ShizukuManager(private val context: Context) {
    
    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
    
    /**
     * Check if Shizuku is installed and running
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if we have Shizuku permission
     */
    fun hasPermission(): Boolean {
        return if (isShizukuAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }
    
    /**
     * Request Shizuku permission
     */
    fun requestPermission(onResult: (Boolean) -> Unit) {
        if (hasPermission()) {
            onResult(true)
            return
        }
        
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    onResult(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
        
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
    }
    
    /**
     * Silently uninstall a package using Shizuku
     * @param packageName Package to uninstall
     * @return true if uninstall initiated successfully
     */
    suspend fun uninstallPackage(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission()) {
                return@withContext Result.failure(
                    SecurityException("Shizuku permission not granted")
                )
            }
            
            // Execute uninstall command via Shizuku
            val result = executeShellCommand("pm uninstall --user 0 $packageName")
            
            if (result.contains("Success", ignoreCase = true)) {
                Result.success(true)
            } else {
                Result.failure(Exception("Uninstall failed: $result"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bulk uninstall multiple packages
     * @param packageNames List of packages to uninstall
     * @param onProgress Callback for progress updates (current, total, packageName)
     * @return Results for each package
     */
    suspend fun bulkUninstall(
        packageNames: List<String>,
        onProgress: suspend (Int, Int, String) -> Unit
    ): List<Pair<String, Result<Boolean>>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, Result<Boolean>>>()
        
        packageNames.forEachIndexed { index, packageName ->
            onProgress(index + 1, packageNames.size, packageName)
            
            val result = uninstallPackage(packageName)
            results.add(packageName to result)
            
            // Small delay to avoid overwhelming the system
            kotlinx.coroutines.delay(100)
        }
        
        results
    }
    
    /**
     * Execute a shell command via Shizuku using IPackageManager binder
     */
    private suspend fun executeShellCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            // Use Runtime.exec with Shizuku's elevated permissions
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            process.waitFor()

            if (error.isNotEmpty() && !output.contains("Success", ignoreCase = true)) {
                error
            } else {
                output
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    
    /**
     * Get detailed package info via Shizuku
     */
    suspend fun getPackageSize(packageName: String): Long = withContext(Dispatchers.IO) {
        try {
            val result = executeShellCommand("du -sb /data/app/$packageName-* 2>/dev/null | awk '{print \$1}'")
            result.trim().toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Check if a package is currently installed
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
