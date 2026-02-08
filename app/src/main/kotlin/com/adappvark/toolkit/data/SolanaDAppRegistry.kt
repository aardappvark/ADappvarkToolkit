package com.adappvark.toolkit.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Queries the Solana blockchain to find dApp metadata and APK download URLs.
 *
 * The Solana dApp Store uses Metaplex NFTs as a registry:
 * - Publisher NFTs (collections) contain App NFTs
 * - App NFTs contain Release NFTs
 * - Release NFTs contain the actual APK download URL in their metadata
 *
 * We use the Helius DAS API (Digital Asset Standard) to query this data.
 */
class SolanaDAppRegistry {

    companion object {
        private const val TAG = "SolanaDAppRegistry"

        // Helius provides free DAS API access (rate limited)
        // Users can also use their own RPC endpoint
        private const val HELIUS_RPC = "https://mainnet.helius-rpc.com/?api-key=15319bf4-5b40-4958-ac8d-6313aa55eb92"

        // Known Solana dApp Store publisher collection address
        // This is the root collection that contains all published dApps
        // TODO: Verify this is the correct mainnet address
        private const val DAPP_STORE_COLLECTION = "DAPs7HxJJC3aLHBfwSPKkCyhzCK8c2m4SqTxEQSgLqUp"
    }

    /**
     * Result of looking up an app in the dApp Store registry
     */
    data class AppRelease(
        val packageName: String,
        val appName: String,
        val version: String,
        val apkUrl: String?,
        val iconUrl: String?,
        val sha256: String?,
        val minSdkVersion: Int?,
        val releaseNftAddress: String
    )

    /**
     * Search for an app by its Android package name and get the latest release info.
     *
     * @param packageName The Android package name (e.g., "com.example.app")
     * @return AppRelease with APK download URL, or null if not found
     */
    suspend fun findAppByPackageName(packageName: String): AppRelease? = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Searching for app: $packageName")

            // Strategy 1: Search all assets for matching android_package
            val release = searchByPackageName(packageName)
            if (release != null) {
                Log.i(TAG, "Found app via search: ${release.appName} - APK: ${release.apkUrl}")
                return@withContext release
            }

            // Strategy 2: Try known naming conventions
            // Some dApps use predictable NFT names
            val releaseByName = searchByAppName(packageName)
            if (releaseByName != null) {
                Log.i(TAG, "Found app via name search: ${releaseByName.appName}")
                return@withContext releaseByName
            }

            Log.w(TAG, "App not found in dApp Store registry: $packageName")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for app: $packageName", e)
            null
        }
    }

    /**
     * Search for assets that have matching android_package in their metadata
     */
    private suspend fun searchByPackageName(packageName: String): AppRelease? = withContext(Dispatchers.IO) {
        try {
            // Use DAS API searchAssets method
            val requestBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", "adappvark-search")
                put("method", "searchAssets")
                put("params", JSONObject().apply {
                    put("page", 1)
                    put("limit", 100)
                    // Search in the dApp Store context
                    put("grouping", JSONArray().apply {
                        put(JSONArray().apply {
                            put("collection")
                            put(DAPP_STORE_COLLECTION)
                        })
                    })
                })
            }

            val response = makeRpcRequest(requestBody.toString())
            if (response != null) {
                val result = response.optJSONObject("result")
                val items = result?.optJSONArray("items")

                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val release = parseReleaseFromAsset(item, packageName)
                        if (release != null) {
                            return@withContext release
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchByPackageName", e)
            null
        }
    }

    /**
     * Search for assets by app name (fallback method)
     */
    private suspend fun searchByAppName(packageName: String): AppRelease? = withContext(Dispatchers.IO) {
        try {
            // Extract likely app name from package (e.g., "com.jito.app" -> "jito")
            val nameParts = packageName.split(".")
            val likelyName = nameParts.getOrNull(1) ?: nameParts.lastOrNull() ?: return@withContext null

            val requestBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", "adappvark-name-search")
                put("method", "searchAssets")
                put("params", JSONObject().apply {
                    put("page", 1)
                    put("limit", 50)
                    put("burnt", false)
                })
            }

            val response = makeRpcRequest(requestBody.toString())
            if (response != null) {
                val result = response.optJSONObject("result")
                val items = result?.optJSONArray("items")

                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val content = item.optJSONObject("content")
                        val metadata = content?.optJSONObject("metadata")
                        val name = metadata?.optString("name", "")?.lowercase() ?: ""

                        if (name.contains(likelyName.lowercase())) {
                            val release = parseReleaseFromAsset(item, packageName)
                            if (release != null) {
                                return@withContext release
                            }
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchByAppName", e)
            null
        }
    }

    /**
     * Parse a DAS API asset response into an AppRelease
     */
    private fun parseReleaseFromAsset(asset: JSONObject, targetPackageName: String): AppRelease? {
        return try {
            val id = asset.optString("id", "")
            val content = asset.optJSONObject("content") ?: return null
            val metadata = content.optJSONObject("metadata") ?: return null
            val jsonUri = content.optString("json_uri", "")

            val name = metadata.optString("name", "Unknown")

            // Check for Solana dApp Store extensions
            val extensions = asset.optJSONObject("content")
                ?.optJSONObject("metadata")
                ?.optJSONObject("extensions")
                ?.optJSONObject("solana_dapp_store")

            // Look for android_package in various places
            var androidPackage = extensions?.optString("android_package", "")
            if (androidPackage.isNullOrEmpty()) {
                // Try attributes
                val attributes = metadata.optJSONArray("attributes")
                if (attributes != null) {
                    for (i in 0 until attributes.length()) {
                        val attr = attributes.getJSONObject(i)
                        if (attr.optString("trait_type") == "android_package") {
                            androidPackage = attr.optString("value", "")
                            break
                        }
                    }
                }
            }

            // If we found a matching package, extract APK URL
            if (androidPackage == targetPackageName || androidPackage.isNullOrEmpty()) {
                // Get APK URL from files array or extensions
                var apkUrl: String? = null
                var iconUrl: String? = null
                var sha256: String? = null
                var version = "1.0.0"

                // Check files array
                val files = content.optJSONArray("files")
                if (files != null) {
                    for (i in 0 until files.length()) {
                        val file = files.getJSONObject(i)
                        val uri = file.optString("uri", "")
                        val mime = file.optString("mime", "")

                        if (mime == "application/vnd.android.package-archive" || uri.endsWith(".apk")) {
                            apkUrl = uri
                            sha256 = file.optString("sha256", null)
                        } else if (mime.startsWith("image/")) {
                            iconUrl = uri
                        }
                    }
                }

                // Check extensions for APK info
                if (apkUrl == null && extensions != null) {
                    apkUrl = extensions.optString("apk_url", null)
                        ?: extensions.optString("install_url", null)
                    version = extensions.optString("version", version)
                    sha256 = extensions.optString("sha256", sha256)
                }

                // If we still don't have APK URL, try fetching the full metadata JSON
                if (apkUrl == null && jsonUri.isNotEmpty()) {
                    val fullMetadata = fetchJsonMetadata(jsonUri)
                    if (fullMetadata != null) {
                        apkUrl = extractApkUrlFromMetadata(fullMetadata)
                        if (apkUrl != null) {
                            sha256 = fullMetadata.optString("sha256", null)
                        }
                    }
                }

                if (apkUrl != null) {
                    return AppRelease(
                        packageName = androidPackage ?: targetPackageName,
                        appName = name,
                        version = version,
                        apkUrl = apkUrl,
                        iconUrl = iconUrl,
                        sha256 = sha256,
                        minSdkVersion = extensions?.optInt("min_sdk_version"),
                        releaseNftAddress = id
                    )
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing asset", e)
            null
        }
    }

    /**
     * Fetch the full JSON metadata from a URI (Arweave, IPFS, etc.)
     */
    private fun fetchJsonMetadata(uri: String): JSONObject? {
        return try {
            val url = normalizeUri(uri)
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                JSONObject(response)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching metadata from $uri", e)
            null
        }
    }

    /**
     * Extract APK URL from full metadata JSON
     */
    private fun extractApkUrlFromMetadata(metadata: JSONObject): String? {
        // Try various known paths
        val paths = listOf(
            listOf("properties", "files"),
            listOf("files"),
            listOf("extensions", "solana_dapp_store", "files"),
            listOf("android", "apk")
        )

        for (path in paths) {
            var current: Any? = metadata
            for (key in path) {
                current = when (current) {
                    is JSONObject -> current.opt(key)
                    else -> null
                }
                if (current == null) break
            }

            when (current) {
                is JSONArray -> {
                    for (i in 0 until current.length()) {
                        val item = current.opt(i)
                        val uri = when (item) {
                            is JSONObject -> {
                                val uri = item.optString("uri", "")
                                val type = item.optString("type", "") + item.optString("mime", "")
                                if (uri.endsWith(".apk") || type.contains("android")) uri else null
                            }
                            is String -> if (item.endsWith(".apk")) item else null
                            else -> null
                        }
                        if (!uri.isNullOrEmpty()) return normalizeUri(uri)
                    }
                }
                is String -> if (current.endsWith(".apk")) return normalizeUri(current)
            }
        }

        // Direct fields
        return metadata.optString("apk_url", null)
            ?: metadata.optString("download_url", null)
            ?: metadata.optString("install_url", null)
    }

    /**
     * Normalize URIs (convert IPFS/Arweave to HTTP gateway URLs)
     */
    private fun normalizeUri(uri: String): String {
        return when {
            uri.startsWith("ipfs://") -> "https://ipfs.io/ipfs/${uri.removePrefix("ipfs://")}"
            uri.startsWith("ar://") -> "https://arweave.net/${uri.removePrefix("ar://")}"
            uri.startsWith("arweave://") -> "https://arweave.net/${uri.removePrefix("arweave://")}"
            else -> uri
        }
    }

    /**
     * Make an RPC request to the Solana DAS API
     */
    private fun makeRpcRequest(body: String): JSONObject? {
        return try {
            val connection = URL(HELIUS_RPC).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            connection.outputStream.write(body.toByteArray())

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                JSONObject(response)
            } else {
                Log.e(TAG, "RPC request failed: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "RPC request error", e)
            null
        }
    }
}
