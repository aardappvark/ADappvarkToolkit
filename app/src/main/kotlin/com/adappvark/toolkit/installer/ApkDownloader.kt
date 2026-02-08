package com.adappvark.toolkit.installer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads APK files from URLs with progress tracking and integrity verification.
 */
class ApkDownloader(private val context: Context) {

    companion object {
        private const val TAG = "ApkDownloader"
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT = 30000
        private const val READ_TIMEOUT = 60000
    }

    /**
     * Result of a download operation
     */
    sealed class DownloadResult {
        data class Success(val apkFile: File, val sha256: String) : DownloadResult()
        data class Error(val message: String, val exception: Exception? = null) : DownloadResult()
    }

    /**
     * Progress callback for download tracking
     */
    interface DownloadProgressListener {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long, percentComplete: Int)
        fun onStatusUpdate(status: String)
    }

    /**
     * Download an APK from a URL.
     *
     * @param url The URL to download from
     * @param packageName The package name (used for filename)
     * @param expectedSha256 Optional SHA256 hash to verify integrity
     * @param progressListener Optional listener for progress updates
     * @return DownloadResult indicating success or failure
     */
    suspend fun downloadApk(
        url: String,
        packageName: String,
        expectedSha256: String? = null,
        progressListener: DownloadProgressListener? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting download: $url")
            progressListener?.onStatusUpdate("Connecting...")

            // Create downloads directory in app's private storage
            val downloadsDir = File(context.cacheDir, "apk_downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Clean filename
            val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val apkFile = File(downloadsDir, "${safePackageName}.apk")

            // Delete old file if exists
            if (apkFile.exists()) {
                apkFile.delete()
            }

            // Open connection
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("User-Agent", "ADappvark/1.0")

            // Handle redirects
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext DownloadResult.Error("HTTP error: $responseCode")
            }

            val totalBytes = connection.contentLengthLong
            Log.i(TAG, "Download size: $totalBytes bytes")
            progressListener?.onStatusUpdate("Downloading...")

            // Download with progress tracking
            val digest = MessageDigest.getInstance("SHA-256")
            var bytesDownloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        // Report progress
                        val percent = if (totalBytes > 0) {
                            ((bytesDownloaded * 100) / totalBytes).toInt()
                        } else {
                            -1 // Unknown size
                        }
                        progressListener?.onProgress(bytesDownloaded, totalBytes, percent)
                    }
                }
            }

            Log.i(TAG, "Download complete: ${apkFile.absolutePath}")

            // Calculate SHA256
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            Log.i(TAG, "SHA256: $sha256")

            // Verify integrity if expected hash provided
            if (expectedSha256 != null && !sha256.equals(expectedSha256, ignoreCase = true)) {
                apkFile.delete()
                return@withContext DownloadResult.Error(
                    "Integrity check failed. Expected: $expectedSha256, Got: $sha256"
                )
            }

            progressListener?.onStatusUpdate("Download complete")
            DownloadResult.Success(apkFile, sha256)

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            DownloadResult.Error("Download failed: ${e.message}", e)
        }
    }

    /**
     * Clean up downloaded APK files
     */
    fun cleanupDownloads() {
        try {
            val downloadsDir = File(context.cacheDir, "apk_downloads")
            if (downloadsDir.exists()) {
                downloadsDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up downloads", e)
        }
    }

    /**
     * Get the path to a previously downloaded APK
     */
    fun getDownloadedApk(packageName: String): File? {
        val safePackageName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val apkFile = File(context.cacheDir, "apk_downloads/${safePackageName}.apk")
        return if (apkFile.exists()) apkFile else null
    }
}
