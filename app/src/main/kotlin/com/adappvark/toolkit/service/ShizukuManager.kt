package com.adappvark.toolkit.service

import android.content.Context
import android.content.pm.PackageManager
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

private const val TAG = "ShizukuManager"

/**
 * Manager for silent uninstall operations.
 *
 * Strategies (tried in order):
 * 1. ADappvark daemon — shell-privileged process started via one-time ADB command.
 *    Runs as UID 2000 (shell), listens on abstract local socket.
 *    Survives USB disconnect. User only needs to set up once per reboot.
 * 2. ADB server via reverse port forward — connects to computer's ADB server
 *    on localhost:5037 (requires `adb reverse tcp:5037 tcp:5037` + USB connected)
 * 3. Shizuku binder — if user has Shizuku installed (not required)
 * 4. Direct pm command — tries Runtime.exec pm uninstall (fails on stock Android)
 *
 * Falls back to ACTION_DELETE intents in the calling code if all fail.
 */
class ShizukuManager(private val context: Context) {

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
        /** Port for ADB server protocol (via reverse port forward from computer) */
        private const val ADB_SERVER_PORT = 5037
        /** Abstract local socket name for our privileged daemon */
        const val DAEMON_SOCKET_NAME = "com.adappvark.toolkit.daemon"

        /**
         * Write the daemon startup script to app's external files dir.
         * User runs: adb shell sh /sdcard/Android/data/com.adappvark.toolkit/files/start_daemon.sh
         */
        fun writeDaemonScript(context: Context): File? {
            return try {
                val dir = context.getExternalFilesDir(null) ?: return null
                val script = File(dir, "start_daemon.sh")
                script.writeText("""
#!/system/bin/sh
# ADappvark Toolkit — Privileged Daemon Starter
# Run this ONCE after each reboot for silent uninstall without USB:
#   adb shell sh ${script.absolutePath}
#
# After running, you can disconnect USB. The daemon stays running
# until the device reboots.

PKG="com.adappvark.toolkit"
DAEMON_CLASS="com.adappvark.toolkit.daemon.UninstallDaemon"

# Find APK path
APK_PATH=${'$'}(pm path ${'$'}PKG 2>/dev/null | head -1 | sed 's/^package://')
if [ -z "${'$'}APK_PATH" ]; then
    echo "ERROR: ${'$'}PKG not installed"
    exit 1
fi

# Kill any existing daemon
pkill -f "nice-name=adappvark_daemon" 2>/dev/null
sleep 1

# Start daemon via app_process (runs as shell UID 2000)
echo "Starting ADappvark daemon..."
(CLASSPATH="${'$'}APK_PATH" /system/bin/app_process \
    /system/bin \
    --nice-name=adappvark_daemon \
    ${'$'}DAEMON_CLASS) &

DAEMON_PID=${'$'}!
sleep 2

# Verify it started
if kill -0 ${'$'}DAEMON_PID 2>/dev/null; then
    echo "ADappvark daemon running (PID ${'$'}DAEMON_PID)"
    echo "You can now disconnect USB. Silent uninstall is active."
    exit 0
else
    echo "ERROR: daemon failed to start"
    exit 1
fi
""".trimIndent() + "\n")
                script.setReadable(true, false)
                Log.d(TAG, "Daemon script written to: ${script.absolutePath}")
                script
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write daemon script", e)
                null
            }
        }
    }

    // ---- Daemon socket methods (Method 1 — preferred) ----

    /**
     * Check if our privileged daemon is running by sending a ping.
     */
    fun isDaemonRunning(): Boolean {
        return try {
            val socket = LocalSocket()
            socket.connect(LocalSocketAddress(
                DAEMON_SOCKET_NAME,
                LocalSocketAddress.Namespace.ABSTRACT
            ))
            val writer = PrintWriter(socket.outputStream, true)
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            writer.println("ping")
            val response = reader.readLine()
            socket.close()
            val running = response == "pong"
            Log.d(TAG, "Daemon ping: $response (running=$running)")
            running
        } catch (e: Exception) {
            Log.d(TAG, "Daemon not reachable: ${e.message}")
            false
        }
    }

    /**
     * Uninstall via our privileged daemon's local socket.
     */
    private suspend fun uninstallViaDaemon(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val socket = LocalSocket()
            socket.connect(LocalSocketAddress(
                DAEMON_SOCKET_NAME,
                LocalSocketAddress.Namespace.ABSTRACT
            ))
            val writer = PrintWriter(socket.outputStream, true)
            val reader = BufferedReader(InputStreamReader(socket.inputStream))

            writer.println("uninstall $packageName")
            val response = reader.readLine() ?: "no response"
            socket.close()

            Log.d(TAG, "Daemon uninstall $packageName: $response")

            if (response.contains("Success", ignoreCase = true)) {
                Result.success(true)
            } else {
                Result.failure(Exception("Daemon uninstall failed: $response"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Daemon uninstall failed for $packageName: ${e.message}")
            Result.failure(e)
        }
    }

    // ---- Shizuku methods (Method 3 — optional companion app) ----

    fun isShizukuAvailable(): Boolean {
        return try {
            rikka.shizuku.Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return if (isShizukuAvailable()) {
            try {
                rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    // ---- Public API ----

    /**
     * Check if silent uninstall is available via any method
     */
    suspend fun canSilentUninstall(): Boolean = withContext(Dispatchers.IO) {
        // Method 1: Our privileged daemon (best — works without USB)
        if (isDaemonRunning()) {
            Log.d(TAG, "Silent uninstall available via ADappvark daemon")
            return@withContext true
        }

        // Method 2: ADB server via reverse port forward (requires USB)
        if (isAdbServerAvailable()) {
            Log.d(TAG, "Silent uninstall available via ADB server (reverse port forward)")
            return@withContext true
        }

        // Method 3: Shizuku (optional companion app)
        if (isShizukuAvailable() && hasShizukuPermission()) {
            Log.d(TAG, "Silent uninstall available via Shizuku")
            return@withContext true
        }

        Log.d(TAG, "No silent uninstall method available")
        false
    }

    /**
     * Silently uninstall a package.
     * Tries daemon first, then ADB server, then Shizuku, then direct pm.
     */
    suspend fun uninstallPackage(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        // Validate package name to prevent injection
        if (!packageName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_.]*$"))) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid package name: $packageName")
            )
        }

        // Method 1: ADappvark daemon (preferred — no USB needed)
        if (isDaemonRunning()) {
            val result = uninstallViaDaemon(packageName)
            if (result.isSuccess) return@withContext result
            Log.w(TAG, "Daemon uninstall failed for $packageName, trying ADB server")
        }

        // Method 2: ADB server via reverse port forward
        val adbResult = uninstallViaAdbServer(packageName)
        if (adbResult.isSuccess) return@withContext adbResult

        // Method 3: Shizuku binder
        if (isShizukuAvailable() && hasShizukuPermission()) {
            val result = uninstallViaShizuku(packageName)
            if (result.isSuccess) return@withContext result
            Log.w(TAG, "Shizuku uninstall failed for $packageName")
        }

        // Method 4: Direct pm command (unlikely to work on stock Android)
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
            val binder = rikka.shizuku.SystemServiceHelper.getSystemService("package")
                ?: return@withContext Result.failure(Exception("Cannot get package service binder"))
            val wrappedBinder = rikka.shizuku.ShizukuBinderWrapper(binder)

            val ipmClass = Class.forName("android.content.pm.IPackageManager\$Stub")
            val asInterface = ipmClass.getMethod("asInterface", android.os.IBinder::class.java)
            val ipm = asInterface.invoke(null, wrappedBinder)

            try {
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

                kotlinx.coroutines.delay(500)
                val stillInstalled = isPackageInstalled(packageName)
                if (!stillInstalled) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Package still installed after deletePackage"))
                }
            } catch (e: NoSuchMethodException) {
                Log.w(TAG, "deletePackage method not found")
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku binder uninstall failed", e)
            Result.failure(e)
        }
    }

    /**
     * Uninstall via ADB server protocol over reverse port forward.
     * Requires USB + `adb reverse tcp:5037 tcp:5037`.
     */
    private suspend fun uninstallViaAdbServer(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val socket = java.net.Socket()
            socket.soTimeout = 10_000
            try {
                socket.connect(java.net.InetSocketAddress("127.0.0.1", ADB_SERVER_PORT), 3000)
            } catch (e: Exception) {
                Log.w(TAG, "ADB server not reachable on port $ADB_SERVER_PORT: ${e.message}")
                return@withContext Result.failure(Exception("ADB server not available"))
            }

            val output = socket.use { sock ->
                val os = sock.getOutputStream()
                val inputStream = sock.getInputStream()

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

                sendAdbMessage("host:transport-any")
                val transportStatus = readStatus()
                if (transportStatus != "OKAY") {
                    return@withContext Result.failure(Exception("ADB transport failed: $transportStatus"))
                }

                val shellCmd = "shell:pm uninstall --user 0 $packageName"
                sendAdbMessage(shellCmd)
                val shellStatus = readStatus()
                if (shellStatus != "OKAY") {
                    return@withContext Result.failure(Exception("ADB shell failed: $shellStatus"))
                }

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
     * Check if ADB server is reachable via reverse port forward.
     */
    private fun isAdbServerAvailable(): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.soTimeout = 3000
            socket.connect(java.net.InetSocketAddress("127.0.0.1", ADB_SERVER_PORT), 2000)

            val os = socket.getOutputStream()
            val inputStream = socket.getInputStream()

            val msg = "host:version"
            val hexLen = String.format("%04x", msg.length)
            os.write(hexLen.toByteArray(Charsets.US_ASCII))
            os.write(msg.toByteArray(Charsets.US_ASCII))
            os.flush()

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
            Log.d(TAG, "ADB server check: status=$status available=$available")
            available
        } catch (e: Exception) {
            Log.d(TAG, "ADB server not available: ${e.message}")
            false
        }
    }

    /**
     * Try direct pm uninstall via Runtime.exec (works on some ROMs)
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
