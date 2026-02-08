package com.adappvark.toolkit.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

/**
 * Installs APK files using Android's PackageInstaller API.
 *
 * This provides programmatic app installation without requiring
 * UI interaction (after initial permission grant).
 */
class ApkInstaller(private val context: Context) {

    companion object {
        private const val TAG = "ApkInstaller"
        private const val INSTALL_ACTION = "com.adappvark.toolkit.INSTALL_COMPLETE"
        private const val INSTALL_TIMEOUT_MS = 120000L // 2 minutes
    }

    /**
     * Result of an installation operation
     */
    sealed class InstallResult {
        object Success : InstallResult()
        data class NeedsPermission(val intent: Intent) : InstallResult()
        data class NeedsUserConfirmation(val intent: Intent) : InstallResult()
        data class Error(val message: String, val statusCode: Int? = null) : InstallResult()
    }

    /**
     * Check if the app has permission to install packages
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true // Pre-Oreo doesn't need this permission
        }
    }

    /**
     * Get intent to request install packages permission
     */
    fun getInstallPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    /**
     * Install an APK file using PackageInstaller.
     *
     * @param apkFile The APK file to install
     * @param packageName The expected package name
     * @return InstallResult indicating success, need for permission, or error
     */
    suspend fun installApk(apkFile: File, packageName: String): InstallResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Installing APK: ${apkFile.absolutePath}")

            if (!apkFile.exists()) {
                return@withContext InstallResult.Error("APK file not found: ${apkFile.absolutePath}")
            }

            // Check permission
            if (!canInstallPackages()) {
                Log.w(TAG, "Need permission to install packages")
                return@withContext InstallResult.NeedsPermission(getInstallPermissionIntent())
            }

            // Use PackageInstaller API
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setInstallReason(PackageManager.INSTALL_REASON_USER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            // Write APK to session
            try {
                session.openWrite("package", 0, apkFile.length()).use { outputStream ->
                    apkFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    session.fsync(outputStream)
                }
            } catch (e: Exception) {
                session.abandon()
                throw e
            }

            // Create pending intent for result
            val intent = Intent(INSTALL_ACTION).apply {
                setPackage(context.packageName)
                putExtra("package_name", packageName)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            // Wait for installation result
            val result = withTimeoutOrNull(INSTALL_TIMEOUT_MS) {
                suspendCancellableCoroutine<InstallResult> { continuation ->
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context?, resultIntent: Intent?) {
                            try {
                                context.unregisterReceiver(this)
                            } catch (e: Exception) {
                                // Already unregistered
                            }

                            if (resultIntent == null) {
                                continuation.resume(InstallResult.Error("No result received"))
                                return
                            }

                            val status = resultIntent.getIntExtra(
                                PackageInstaller.EXTRA_STATUS,
                                PackageInstaller.STATUS_FAILURE
                            )

                            when (status) {
                                PackageInstaller.STATUS_SUCCESS -> {
                                    Log.i(TAG, "Installation successful: $packageName")
                                    continuation.resume(InstallResult.Success)
                                }

                                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                                    val confirmIntent = resultIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                                    if (confirmIntent != null) {
                                        Log.i(TAG, "User confirmation required")
                                        continuation.resume(InstallResult.NeedsUserConfirmation(confirmIntent))
                                    } else {
                                        continuation.resume(InstallResult.Error("User action required but no intent"))
                                    }
                                }

                                else -> {
                                    val message = resultIntent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                                        ?: getStatusMessage(status)
                                    Log.e(TAG, "Installation failed: $status - $message")
                                    continuation.resume(InstallResult.Error(message, status))
                                }
                            }
                        }
                    }

                    // Register receiver
                    val filter = IntentFilter(INSTALL_ACTION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        context.registerReceiver(receiver, filter)
                    }

                    continuation.invokeOnCancellation {
                        try {
                            context.unregisterReceiver(receiver)
                            session.abandon()
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }

                    // Commit the session
                    session.commit(pendingIntent.intentSender)
                }
            }

            result ?: InstallResult.Error("Installation timed out")

        } catch (e: Exception) {
            Log.e(TAG, "Installation error", e)
            InstallResult.Error("Installation failed: ${e.message}")
        }
    }

    /**
     * Install using the traditional ACTION_VIEW intent (fallback method).
     * This always requires user interaction but is more reliable.
     */
    fun installApkWithIntent(apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    /**
     * Get human-readable message for PackageInstaller status codes
     */
    private fun getStatusMessage(status: Int): String {
        return when (status) {
            PackageInstaller.STATUS_FAILURE -> "Installation failed"
            PackageInstaller.STATUS_FAILURE_ABORTED -> "Installation aborted"
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "Installation blocked"
            PackageInstaller.STATUS_FAILURE_CONFLICT -> "Conflicting package already installed"
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "Package incompatible with device"
            PackageInstaller.STATUS_FAILURE_INVALID -> "Invalid package"
            PackageInstaller.STATUS_FAILURE_STORAGE -> "Insufficient storage"
            else -> "Unknown error: $status"
        }
    }
}
