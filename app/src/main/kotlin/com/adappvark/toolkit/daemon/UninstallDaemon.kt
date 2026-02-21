package com.adappvark.toolkit.daemon

import android.net.LocalServerSocket
import android.net.LocalSocket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

/**
 * Privileged daemon that runs as shell UID (2000) via app_process.
 *
 * Started by the user running a one-time ADB command:
 *   adb shell sh /sdcard/Android/data/com.adappvark.toolkit/files/start_daemon.sh
 *
 * The daemon listens on an abstract local socket for uninstall commands
 * from the ADappvark app. It executes pm uninstall with shell privileges,
 * bypassing the per-app confirmation dialog.
 *
 * Security: Only accepts commands from our app's UID (checked via socket peer credentials).
 *
 * Survives USB disconnect but NOT device reboot.
 */
object UninstallDaemon {

    const val SOCKET_NAME = "com.adappvark.toolkit.daemon"
    private const val TAG = "ADappvarkDaemon"

    /** Expected app UID — set at startup by querying pm */
    private var allowedUid = -1

    @JvmStatic
    fun main(args: Array<String>) {
        println("$TAG: starting...")

        // Resolve our app's UID so we only accept commands from it
        allowedUid = resolveAppUid("com.adappvark.toolkit")
        if (allowedUid < 0) {
            System.err.println("$TAG: FATAL - could not resolve app UID, exiting")
            System.exit(1)
        }
        println("$TAG: will accept commands from UID $allowedUid")

        var server: LocalServerSocket? = null
        try {
            server = LocalServerSocket(SOCKET_NAME)
            println("$TAG: listening on abstract socket: $SOCKET_NAME")

            while (true) {
                var client: LocalSocket? = null
                try {
                    client = server.accept()

                    // Security check: verify caller is our app
                    val creds = client.peerCredentials
                    if (creds.uid != allowedUid) {
                        System.err.println("$TAG: rejected connection from UID ${creds.uid}")
                        client.close()
                        continue
                    }

                    val reader = BufferedReader(InputStreamReader(client.inputStream))
                    val writer = PrintWriter(client.outputStream, true)

                    val line = reader.readLine()
                    if (line == null) {
                        writer.println("ERROR: empty command")
                        client.close()
                        continue
                    }

                    val response = handleCommand(line.trim())
                    writer.println(response)
                    client.close()

                } catch (e: Exception) {
                    System.err.println("$TAG: client error: ${e.message}")
                    try { client?.close() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            System.err.println("$TAG: FATAL server error: ${e.message}")
            e.printStackTrace()
            System.exit(1)
        }
    }

    private fun handleCommand(command: String): String {
        if (command == "ping") {
            return "pong"
        }

        if (command.startsWith("uninstall ")) {
            val pkg = command.substring("uninstall ".length).trim()

            // Validate package name to prevent shell injection
            if (!pkg.matches(Regex("^[a-zA-Z][a-zA-Z0-9_.]*$"))) {
                return "ERROR: invalid package name"
            }

            return executeUninstall(pkg)
        }

        return "ERROR: unknown command"
    }

    private fun executeUninstall(packageName: String): String {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("pm", "uninstall", "--user", "0", packageName)
            )

            val stdout = BufferedReader(InputStreamReader(process.inputStream))
            val stderr = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?
            while (stdout.readLine().also { line = it } != null) {
                output.append(line)
            }
            while (stderr.readLine().also { line = it } != null) {
                output.append(line)
            }

            process.waitFor()
            val result = output.toString().trim()
            println("$TAG: uninstall $packageName -> $result")
            if (result.isEmpty()) "Success" else result

        } catch (e: Exception) {
            System.err.println("$TAG: uninstall failed: ${e.message}")
            "ERROR: ${e.message}"
        }
    }

    /**
     * Resolve the UID for a package name using pm dump.
     * We're running as shell so we can query the package manager directly.
     */
    private fun resolveAppUid(packageName: String): Int {
        try {
            val process = Runtime.getRuntime().exec(
                arrayOf("pm", "dump", packageName)
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                // Look for "userId=10123" in the dump output
                if (trimmed.startsWith("userId=")) {
                    val uidStr = trimmed.substring("userId=".length).trim()
                    process.destroy()
                    return uidStr.toInt()
                }
            }
            process.destroy()

            // Fallback: try dumpsys approach
            return resolveAppUidFallback(packageName)

        } catch (e: Exception) {
            System.err.println("$TAG: resolveAppUid failed: ${e.message}")
            return -1
        }
    }

    private fun resolveAppUidFallback(packageName: String): Int {
        try {
            val process = Runtime.getRuntime().exec(
                arrayOf("dumpsys", "package", packageName)
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.startsWith("userId=")) {
                    val uidStr = trimmed.substring("userId=".length).trim()
                    process.destroy()
                    return uidStr.toInt()
                }
            }
            process.destroy()
        } catch (e: Exception) {
            System.err.println("$TAG: resolveAppUidFallback failed: ${e.message}")
        }
        return -1
    }
}
