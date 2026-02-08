package com.adappvark.toolkit.installer

import android.content.Context
import android.content.Intent
import android.util.Log
import com.adappvark.toolkit.data.SolanaDAppRegistry
import com.adappvark.toolkit.data.UninstallHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages direct APK installation bypassing the dApp Store UI.
 *
 * This class orchestrates:
 * 1. Looking up app info from Solana blockchain
 * 2. Downloading APK files
 * 3. Installing APKs using PackageInstaller
 *
 * This approach is more reliable than UI automation because it:
 * - Doesn't depend on dApp Store UI elements
 * - Works consistently regardless of page load times
 * - Can retry downloads if they fail
 */
class DirectInstallManager(private val context: Context) {

    companion object {
        private const val TAG = "DirectInstallManager"
    }

    private val registry = SolanaDAppRegistry()
    private val downloader = ApkDownloader(context)
    private val installer = ApkInstaller(context)

    /**
     * Result of an installation attempt
     */
    sealed class InstallAttemptResult {
        object Success : InstallAttemptResult()
        object AlreadyInstalled : InstallAttemptResult()
        data class NeedsPermission(val intent: Intent) : InstallAttemptResult()
        data class NeedsUserAction(val intent: Intent) : InstallAttemptResult()
        data class NotFoundInRegistry(val packageName: String) : InstallAttemptResult()
        data class DownloadFailed(val reason: String) : InstallAttemptResult()
        data class InstallFailed(val reason: String) : InstallAttemptResult()
        data class FallbackToDAppStore(val packageName: String) : InstallAttemptResult()
    }

    /**
     * Progress callback for tracking installation
     */
    interface InstallProgressListener {
        fun onLookingUp(packageName: String)
        fun onDownloading(packageName: String, progress: Int)
        fun onInstalling(packageName: String)
        fun onComplete(packageName: String, success: Boolean)
    }

    /**
     * Check if we have permission to install packages
     */
    fun canInstallPackages(): Boolean = installer.canInstallPackages()

    /**
     * Get intent to request install permission
     */
    fun getInstallPermissionIntent(): Intent = installer.getInstallPermissionIntent()

    /**
     * Check if an app is already installed
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Attempt to install an app directly by downloading its APK.
     *
     * @param packageName The Android package name to install
     * @param progressListener Optional listener for progress updates
     * @return InstallAttemptResult indicating the outcome
     */
    suspend fun installApp(
        packageName: String,
        progressListener: InstallProgressListener? = null
    ): InstallAttemptResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting direct install for: $packageName")

            // Check if already installed
            if (isAppInstalled(packageName)) {
                Log.i(TAG, "App already installed: $packageName")
                return@withContext InstallAttemptResult.AlreadyInstalled
            }

            // Check install permission first
            if (!canInstallPackages()) {
                Log.w(TAG, "Need install permission")
                return@withContext InstallAttemptResult.NeedsPermission(getInstallPermissionIntent())
            }

            // Step 1: Look up app in Solana registry
            progressListener?.onLookingUp(packageName)
            Log.i(TAG, "Looking up app in Solana registry...")

            val appRelease = registry.findAppByPackageName(packageName)

            if (appRelease == null || appRelease.apkUrl == null) {
                Log.w(TAG, "App not found in registry or no APK URL: $packageName")
                // Fall back to dApp Store UI method
                return@withContext InstallAttemptResult.FallbackToDAppStore(packageName)
            }

            Log.i(TAG, "Found app: ${appRelease.appName} v${appRelease.version}")
            Log.i(TAG, "APK URL: ${appRelease.apkUrl}")

            // Step 2: Download APK
            progressListener?.onDownloading(packageName, 0)
            Log.i(TAG, "Downloading APK...")

            val downloadResult = downloader.downloadApk(
                url = appRelease.apkUrl,
                packageName = packageName,
                expectedSha256 = appRelease.sha256,
                progressListener = object : ApkDownloader.DownloadProgressListener {
                    override fun onProgress(bytesDownloaded: Long, totalBytes: Long, percentComplete: Int) {
                        progressListener?.onDownloading(packageName, percentComplete)
                    }

                    override fun onStatusUpdate(status: String) {
                        Log.d(TAG, "Download status: $status")
                    }
                }
            )

            when (downloadResult) {
                is ApkDownloader.DownloadResult.Error -> {
                    Log.e(TAG, "Download failed: ${downloadResult.message}")
                    return@withContext InstallAttemptResult.DownloadFailed(downloadResult.message)
                }
                is ApkDownloader.DownloadResult.Success -> {
                    Log.i(TAG, "Download complete: ${downloadResult.apkFile.absolutePath}")
                }
            }

            val apkFile = (downloadResult as ApkDownloader.DownloadResult.Success).apkFile

            // Step 3: Install APK
            progressListener?.onInstalling(packageName)
            Log.i(TAG, "Installing APK...")

            val installResult = installer.installApk(apkFile, packageName)

            when (installResult) {
                is ApkInstaller.InstallResult.Success -> {
                    Log.i(TAG, "Installation successful!")
                    progressListener?.onComplete(packageName, true)

                    // Clean up downloaded APK
                    apkFile.delete()

                    InstallAttemptResult.Success
                }

                is ApkInstaller.InstallResult.NeedsPermission -> {
                    Log.w(TAG, "Need install permission")
                    InstallAttemptResult.NeedsPermission(installResult.intent)
                }

                is ApkInstaller.InstallResult.NeedsUserConfirmation -> {
                    Log.i(TAG, "User confirmation required")
                    InstallAttemptResult.NeedsUserAction(installResult.intent)
                }

                is ApkInstaller.InstallResult.Error -> {
                    Log.e(TAG, "Installation failed: ${installResult.message}")
                    progressListener?.onComplete(packageName, false)
                    InstallAttemptResult.InstallFailed(installResult.message)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Direct install failed", e)
            InstallAttemptResult.InstallFailed("Error: ${e.message}")
        }
    }

    /**
     * Install multiple apps with progress tracking.
     *
     * @param packageNames List of package names to install
     * @param onProgress Callback for each app's progress
     * @param onAppComplete Callback when each app finishes
     * @return Map of package name to result
     */
    suspend fun installMultipleApps(
        packageNames: List<String>,
        onProgress: suspend (current: Int, total: Int, packageName: String, status: String) -> Unit,
        onAppComplete: suspend (packageName: String, result: InstallAttemptResult) -> Unit
    ): Map<String, InstallAttemptResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, InstallAttemptResult>()

        packageNames.forEachIndexed { index, packageName ->
            onProgress(index + 1, packageNames.size, packageName, "Looking up...")

            val result = installApp(
                packageName = packageName,
                progressListener = object : InstallProgressListener {
                    override fun onLookingUp(pkg: String) {
                        // Already handled above
                    }

                    override fun onDownloading(pkg: String, progress: Int) {
                        kotlinx.coroutines.runBlocking {
                            onProgress(index + 1, packageNames.size, pkg, "Downloading: $progress%")
                        }
                    }

                    override fun onInstalling(pkg: String) {
                        kotlinx.coroutines.runBlocking {
                            onProgress(index + 1, packageNames.size, pkg, "Installing...")
                        }
                    }

                    override fun onComplete(pkg: String, success: Boolean) {
                        // Handled in result processing
                    }
                }
            )

            results[packageName] = result
            onAppComplete(packageName, result)
        }

        results
    }

    /**
     * Clean up any downloaded APKs
     */
    fun cleanup() {
        downloader.cleanupDownloads()
    }
}
