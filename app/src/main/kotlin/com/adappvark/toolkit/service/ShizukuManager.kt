package com.adappvark.toolkit.service

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

private const val TAG = "ShizukuManager"

/**
 * Manager for Shizuku integration and silent uninstall operations.
 *
 * Strategies (tried in order):
 * 1. Shizuku binder — uses IPackageManager via Shizuku's transactRemote
 * 2. ADB server via reverse port forward — connects to computer's ADB server
 *    on localhost:5037 (requires `adb reverse tcp:5037 tcp:5037`)
 * 3. Direct pm command — tries Runtime.exec pm uninstall (works on some ROMs)
 *
 * Falls back to ACTION_DELETE intents in the calling code if all fail.
 */
class ShizukuManager(private val context: Context) {

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
        /** Port for ADB server protocol (via reverse port forward from computer) */
        private const val ADB_SERVER_PORT = 5037
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
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
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
     * Check if silent uninstall is available via any method
     */
    suspend fun canSilentUninstall(): Boolean = withContext(Dispatchers.IO) {
        // Method 1: Shizuku
        if (isShizukuAvailable() && hasPermission()) {
            Log.d(TAG, "Silent uninstall available via Shizuku")
            return@withContext true
        }

        // Method 2: ADB server via reverse port forward (port 5037)
        if (isAdbServerAvailable()) {
            Log.d(TAG, "Silent uninstall available via ADB server (reverse port forward)")
            return@withContext true
        }

        Log.d(TAG, "No silent uninstall method available")
        false
    }

    /**
     * Silently uninstall a package
     * Tries Shizuku first, then ADB-over-localhost, then direct pm command
     */
    suspend fun uninstallPackage(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        // Validate package name to prevent shell injection
        if (!packageName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_.]*$"))) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid package name: $packageName")
            )
        }

        // Method 1: Shizuku binder
        if (isShizukuAvailable() && hasPermission()) {
            val result = uninstallViaShizuku(packageName)
            if (result.isSuccess) return@withContext result
            Log.w(TAG, "Shizuku uninstall failed for $packageName, trying ADB TCP")
        }

        // Method 2: ADB server via reverse port forward
        val adbResult = uninstallViaAdbServer(packageName)
        if (adbResult.isSuccess) return@withContext adbResult

        // Method 3: Direct pm command (might work on some ROMs)
        val directResult = uninstallViaDirect(packageName)
        if (directResult.isSuccess) return@withContext directResult

        Result.failure(Exception("All silent uninstall methods failed for $packageName"))
    }

    /**
     * Bulk uninstall multiple packages silently
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

            // Small delay between uninstalls
            kotlinx.coroutines.delay(200)
        }

        results
    }

    // ---- Private implementation methods ----

    /**
     * Uninstall via Shizuku's IPackageManager binder
     */
    private suspend fun uninstallViaShizuku(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Use ShizukuBinderWrapper to get IPackageManager with shell privileges
            val binder = SystemServiceHelper.getSystemService("package")
            if (binder == null) {
                return@withContext Result.failure(Exception("Cannot get package service binder"))
            }
            val wrappedBinder = ShizukuBinderWrapper(binder)

            // Use the wrapped binder to call deletePackage via reflection
            val ipmClass = Class.forName("android.content.pm.IPackageManager\$Stub")
            val asInterface = ipmClass.getMethod("asInterface", android.os.IBinder::class.java)
            val ipm = asInterface.invoke(null, wrappedBinder)

            // Try deletePackage (API varies by Android version)
            try {
                // Android 14+: deletePackage(String packageName, int versionCode, IPackageDeleteObserver2 observer, int userId, String callerPackageName)
                val deleteMethod = ipm.javaClass.getMethod(
                    "deletePackage",
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    Class.forName("android.content.pm.IPackageDeleteObserver2"),
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                deleteMethod.invoke(ipm, packageName, -1, null, 0, context.packageName)
                Log.d(TAG, "Shizuku binder deletePackage called for $packageName")

                // Wait briefly and verify
                kotlinx.coroutines.delay(500)
                val stillInstalled = isPackageInstalled(packageName)
                if (!stillInstalled) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Package still installed after deletePackage"))
                }
            } catch (e: NoSuchMethodException) {
                Log.w(TAG, "deletePackage method not found, trying alternative signatures")
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku binder uninstall failed", e)
            Result.failure(e)
        }
    }

    /**
     * Uninstall via ADB server protocol over reverse port forward.
     *
     * Requires `adb reverse tcp:5037 tcp:5037` to have been run beforehand.
     * This forwards the device's localhost:5037 to the computer's ADB server.
     * The ADB server speaks a text-based protocol: hex-length-prefixed messages,
     * with OKAY/FAIL responses.
     */
    private suspend fun uninstallViaAdbServer(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val socket = java.net.Socket()
            socket.soTimeout = 10_000 // 10s read timeout
            try {
                socket.connect(java.net.InetSocketAddress("127.0.0.1", ADB_SERVER_PORT), 3000)
            } catch (e: Exception) {
                Log.w(TAG, "ADB server not reachable on port $ADB_SERVER_PORT: ${e.message}")
                return@withContext Result.failure(Exception("ADB server not available on port $ADB_SERVER_PORT"))
            }

            val output = socket.use { sock ->
                val os = sock.getOutputStream()
                val inputStream = sock.getInputStream()

                // ADB server protocol: 4-char hex length prefix + message body
                fun sendAdbMessage(msg: String) {
                    val hexLen = String.format("%04x", msg.length)
                    os.write(hexLen.toByteArray(Charsets.US_ASCII))
                    os.write(msg.toByteArray(Charsets.US_ASCII))
                    os.flush()
                }

                fun readStatus(): String {
                    val buf = ByteArray(4)
                    var offset = 0
                    while (offset < 4) {
                        val n = inputStream.read(buf, offset, 4 - offset)
                        if (n < 0) return "EOF"
                        offset += n
                    }
                    return String(buf, Charsets.US_ASCII)
                }

                fun readAll(): String {
                    val sb = StringBuilder()
                    val buf = ByteArray(4096)
                    while (true) {
                        val n = try { inputStream.read(buf) } catch (_: Exception) { -1 }
                        if (n <= 0) break
                        sb.append(String(buf, 0, n, Charsets.US_ASCII))
                    }
                    return sb.toString()
                }

                // Step 1: Select transport (connects to any attached device)
                sendAdbMessage("host:transport-any")
                val transportStatus = readStatus()
                if (transportStatus != "OKAY") {
                    Log.w(TAG, "ADB transport-any failed: $transportStatus")
                    return@withContext Result.failure(Exception("ADB transport failed: $transportStatus"))
                }
                Log.d(TAG, "ADB transport-any: OKAY")

                // Step 2: Send shell command to pm uninstall
                val shellCmd = "shell:pm uninstall --user 0 $packageName"
                sendAdbMessage(shellCmd)
                val shellStatus = readStatus()
                if (shellStatus != "OKAY") {
                    Log.w(TAG, "ADB shell command failed: $shellStatus")
                    return@withContext Result.failure(Exception("ADB shell failed: $shellStatus"))
                }
                Log.d(TAG, "ADB shell command accepted: OKAY")

                // Step 3: Read shell output (pm uninstall result)
                readAll()
            }

            val trimmedOutput = output.trim()
            Log.d(TAG, "ADB server uninstall $packageName result: '$trimmedOutput'")

            if (trimmedOutput.contains("Success", ignoreCase = true)) {
                Result.success(true)
            } else {
                Result.failure(Exception("ADB server uninstall failed: $trimmedOutput"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "ADB server uninstall failed for $packageName", e)
            Result.failure(e)
        }
    }

    /**
     * Check if ADB server is reachable via reverse port forward on localhost:5037.
     * Sends a simple "host:version" query to verify it's really the ADB server.
     */
    private fun isAdbServerAvailable(): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.soTimeout = 3000
            socket.connect(java.net.InetSocketAddress("127.0.0.1", ADB_SERVER_PORT), 2000)

            val os = socket.getOutputStream()
            val inputStream = socket.getInputStream()

            // Send "host:version" — a lightweight query to verify ADB server
            val msg = "host:version"
            val hexLen = String.format("%04x", msg.length)
            os.write(hexLen.toByteArray(Charsets.US_ASCII))
            os.write(msg.toByteArray(Charsets.US_ASCII))
            os.flush()

            // Read 4-byte status
            val statusBuf = ByteArray(4)
            var offset = 0
            while (offset < 4) {
                val n = inputStream.read(statusBuf, offset, 4 - offset)
                if (n < 0) break
                offset += n
            }
            val status = String(statusBuf, Charsets.US_ASCII)
            socket.close()

            val available = status == "OKAY"
            Log.d(TAG, "ADB server check on port $ADB_SERVER_PORT: status=$status available=$available")
            available
        } catch (e: Exception) {
            Log.d(TAG, "ADB server not available on port $ADB_SERVER_PORT: ${e.message}")
            false
        }
    }

    /**
     * Try direct pm uninstall via Runtime.exec (works on some ROMs or with elevated permissions)
     */
    private suspend fun uninstallViaDirect(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("pm", "uninstall", "--user", "0", packageName))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            process.waitFor()

            Log.d(TAG, "Direct pm uninstall $packageName: output='$output' error='$error'")

            if (output.contains("Success", ignoreCase = true)) {
                Result.success(true)
            } else {
                Result.failure(Exception("Direct pm uninstall failed: ${error.ifEmpty { output }}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
