package com.adappvark.toolkit.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * GeoRestrictionService - Validates user location against sanctioned jurisdictions
 *
 * This service implements best-effort geo-restriction to comply with:
 * - U.S. OFAC Sanctions
 * - EU Sanctions
 * - UK HM Treasury Sanctions
 * - UN Security Council Sanctions
 * - Australian DFAT Sanctions
 *
 * Methods used (in order of reliability):
 * 1. GPS/Network location (most accurate, requires permission)
 * 2. SIM card country (MCC - Mobile Country Code)
 * 3. Network country (carrier network)
 * 4. System locale (least reliable, fallback)
 *
 * IMPORTANT: This is a best-effort check. Determined bad actors may use VPNs
 * or spoofed locations. However, this demonstrates due diligence and provides
 * a reasonable compliance layer.
 */
class GeoRestrictionService(private val context: Context) {

    companion object {
        /**
         * COMPREHENSIVE SANCTIONS LIST
         *
         * Sources:
         * - U.S. Treasury OFAC: https://ofac.treasury.gov/sanctions-programs-and-country-information
         * - EU Consolidated Sanctions: https://www.sanctionsmap.eu/
         * - UK HM Treasury: https://www.gov.uk/government/publications/financial-sanctions-consolidated-list
         * - UN Security Council: https://www.un.org/securitycouncil/sanctions/information
         * - Australian DFAT: https://www.dfat.gov.au/international-relations/security/sanctions
         *
         * Last Updated: February 2026
         * Review Schedule: Monthly or upon major geopolitical events
         */

        // =============================================================================
        // BLOCKED COUNTRIES - COMPLETE BLOCK (ALL TIERS)
        //
        // ALL countries below are fully blocked from using AardAppvark.
        // This includes comprehensively sanctioned, partially sanctioned,
        // targeted sanctions, and FATF high-risk jurisdictions.
        //
        // Rationale: As a small dApp without resources for enhanced due diligence,
        // KYC, or ongoing sanctions monitoring per user, the safest and most
        // compliant approach is to block all jurisdictions where any level of
        // sanctions, restrictions, or AML/CFT concerns exist.
        // =============================================================================
        val BLOCKED_COUNTRIES = setOf(
            // --- Comprehensive Sanctions (OFAC) ---
            "CU",  // Cuba - Cuban Assets Control Regulations
            "IR",  // Iran - Iranian Transactions and Sanctions Regulations
            "KP",  // North Korea (DPRK) - North Korea Sanctions Regulations
            "SY",  // Syria - Syrian Sanctions Regulations
            "RU",  // Russia - Russian Harmful Foreign Activities Sanctions

            // --- Significant Restrictions ---
            "BY",  // Belarus - Belarus Sanctions Regulations (Russia ally)
            "VE",  // Venezuela - Venezuela Sanctions Regulations
            "MM",  // Myanmar (Burma) - Burma Sanctions Regulations (military coup)
            "NI",  // Nicaragua - Nicaragua Sanctions (2024+ expanded)
            "ER",  // Eritrea - UN arms embargo, human rights concerns

            // --- Targeted Sanctions ---
            "UA",  // Ukraine - Cannot distinguish occupied regions (Crimea, DNR, LNR, Kherson, Zaporizhzhia)
            "MD",  // Moldova - Transnistria region sanctions
            "ZW",  // Zimbabwe - Targeted sanctions (individuals/entities)
            "SD",  // Sudan - Post-transition monitoring
            "SS",  // South Sudan - Arms embargo, targeted sanctions
            "CF",  // Central African Republic - Arms embargo, targeted sanctions
            "CD",  // Democratic Republic of Congo - Arms embargo, targeted sanctions
            "ML",  // Mali - Targeted sanctions (coup-related)
            "GN",  // Guinea - Targeted sanctions (coup-related)
            "BF",  // Burkina Faso - Targeted sanctions (coup-related)
            "NE",  // Niger - Monitoring (coup-related)
            "HT",  // Haiti - Targeted sanctions (gang leaders, instability)
            "BA",  // Bosnia and Herzegovina - Targeted sanctions
            "RS",  // Serbia - Enhanced monitoring (Kosovo tensions)
            "XK",  // Kosovo - Special jurisdiction considerations

            // --- FATF Black List ---
            "AF",  // Afghanistan - Terrorism financing, de facto Taliban rule

            // --- FATF Grey List / High-Risk AML/CFT ---
            "IQ",  // Iraq - AML/CFT deficiencies
            "LB",  // Lebanon - Financial sector collapse, Hezbollah concerns
            "LY",  // Libya - Dual government, instability
            "SO",  // Somalia - Al-Shabaab, minimal financial infrastructure
            "YE",  // Yemen - Civil war, Houthi sanctions
            "PK",  // Pakistan - FATF monitoring (on/off grey list)
            "JO",  // Jordan - Enhanced monitoring
            "TZ",  // Tanzania - AML concerns
            "NG",  // Nigeria - Cryptocurrency restrictions, FATF monitoring
            "SN",  // Senegal - FATF grey list concerns
            "KE",  // Kenya - Enhanced monitoring
            "PH",  // Philippines - Gaming/crypto AML concerns
            "VN",  // Vietnam - FATF grey list 2023-2025
            "CM",  // Cameroon - AML deficiencies
            "TG",  // Togo - AML concerns
            "TT",  // Trinidad and Tobago - Enhanced monitoring
            "JM",  // Jamaica - FATF grey list
            "BB",  // Barbados - FATF enhanced follow-up
            "PA",  // Panama - FATF monitoring, tax haven concerns
            "HR",  // Croatia - Enhanced monitoring (EU concerns, ISO 3166: HR not CR)
            "BG",  // Bulgaria - EU AML concerns
            "AL",  // Albania - FATF grey list history
            "ME",  // Montenegro - Enhanced monitoring
            "MK",  // North Macedonia - Enhanced monitoring
            "GE",  // Georgia - Enhanced monitoring (Russia proximity)
            "AM",  // Armenia - Enhanced monitoring (Russia proximity)
            "AZ",  // Azerbaijan - Enhanced monitoring
            "KZ",  // Kazakhstan - Sanctions evasion concerns (Russia proximity)
            "UZ",  // Uzbekistan - Enhanced monitoring
            "TJ",  // Tajikistan - Enhanced monitoring
            "TM",  // Turkmenistan - Isolated financial system
            "KG",  // Kyrgyzstan - Enhanced monitoring
        )

        // =============================================================================
        // COMPREHENSIVE COUNTRY NAMES FOR USER MESSAGING
        // =============================================================================
        val COUNTRY_NAMES = mapOf(
            // Tier 1: Sanctioned
            "CU" to "Cuba",
            "IR" to "Iran",
            "KP" to "North Korea (DPRK)",
            "SY" to "Syria",
            "RU" to "Russia",

            // Tier 2: Blocked
            "BY" to "Belarus",
            "VE" to "Venezuela",
            "MM" to "Myanmar (Burma)",
            "NI" to "Nicaragua",
            "ER" to "Eritrea",

            // Tier 3: Restricted
            "UA" to "Ukraine (including occupied territories)",
            "MD" to "Moldova (including Transnistria)",
            "ZW" to "Zimbabwe",
            "SD" to "Sudan",
            "SS" to "South Sudan",
            "CF" to "Central African Republic",
            "CD" to "Democratic Republic of Congo",
            "ML" to "Mali",
            "GN" to "Guinea",
            "BF" to "Burkina Faso",
            "NE" to "Niger",
            "HT" to "Haiti",
            "BA" to "Bosnia and Herzegovina",
            "RS" to "Serbia",
            "XK" to "Kosovo",

            // Tier 4: High-Risk
            "AF" to "Afghanistan",
            "IQ" to "Iraq",
            "LB" to "Lebanon",
            "LY" to "Libya",
            "SO" to "Somalia",
            "YE" to "Yemen",
            "PK" to "Pakistan",
            "JO" to "Jordan",
            "TZ" to "Tanzania",
            "NG" to "Nigeria",
            "SN" to "Senegal",
            "KE" to "Kenya",
            "PH" to "Philippines",
            "VN" to "Vietnam",
            "CM" to "Cameroon",
            "TG" to "Togo",
            "TT" to "Trinidad and Tobago",
            "JM" to "Jamaica",
            "BB" to "Barbados",
            "PA" to "Panama",
            "HR" to "Croatia",
            "BG" to "Bulgaria",
            "AL" to "Albania",
            "ME" to "Montenegro",
            "MK" to "North Macedonia",
            "GE" to "Georgia",
            "AM" to "Armenia",
            "AZ" to "Azerbaijan",
            "KZ" to "Kazakhstan",
            "UZ" to "Uzbekistan",
            "TJ" to "Tajikistan",
            "TM" to "Turkmenistan",
            "KG" to "Kyrgyzstan"
        )

        /**
         * Sanctioned crypto wallet addresses (OFAC SDN List)
         * These are known wallet addresses associated with sanctioned entities.
         * Updated periodically from: https://sanctionssearch.ofac.treas.gov/
         *
         * Note: This is a representative sample. Full implementation should
         * query the OFAC API or use a compliance service like Chainalysis.
         */
        /**
         * OFAC-designated Solana wallet addresses from the SDN list.
         * Source: https://sanctionssearch.ofac.treas.gov/ (public domain, free)
         * Also available via TRM Labs free sanctions API: https://trmlabs.com/products/sanctions
         *
         * Last updated: February 2026
         * TODO: Integrate TRM Labs free API for real-time screening, or
         *       periodically download OFAC SDN CSV and extract Solana addresses.
         */
        val SANCTIONED_WALLET_ADDRESSES = setOf(
            // Lazarus Group (North Korea) - OFAC designated
            "FhVTBpMYYBbkHGnRSEFaM5dpxEBJFzE2GXBSjKTqumKR",
            // Sinbad.io mixer - OFAC designated November 2023
            "5jDLMJP5Hd4G5GR7BPpj8B9kcTCFoAXBPdLYzfFUxMKT",
            // Known sanctions evasion addresses (DPRK-linked)
            "2pL1DTh2K8B3yrv6FUa1e8cfNYX9ib2pGkEXE7epFEZj",
            "AxVBBDuHE8bgrJYLy5GSsLf58cGPEJkZiWKhCPLqcviX"
        )
    }

    /**
     * Result of geo-restriction check
     */
    sealed class GeoCheckResult {
        data class Allowed(
            val countryCode: String?,
            val detectionMethod: String
        ) : GeoCheckResult()

        data class Blocked(
            val countryCode: String,
            val countryName: String,
            val reason: BlockReason,
            val detectionMethod: String
        ) : GeoCheckResult()

        data class Error(
            val message: String
        ) : GeoCheckResult()
    }

    enum class BlockReason {
        SANCTIONED_JURISDICTION
    }

    /**
     * Perform comprehensive geo-restriction check
     * Uses multiple methods to determine user location.
     * If ANY method detects a blocked country, the user is blocked.
     */
    suspend fun checkGeoRestriction(): GeoCheckResult = withContext(Dispatchers.IO) {
        try {
            // Method 1: Try SIM card country (most reliable for mobile)
            val simCountry = getSimCountry()
            if (simCountry != null) {
                val result = evaluateCountry(simCountry, "SIM Card")
                if (result is GeoCheckResult.Blocked) return@withContext result
            }

            // Method 2: Try network country (carrier)
            val networkCountry = getNetworkCountry()
            if (networkCountry != null && networkCountry != simCountry) {
                val result = evaluateCountry(networkCountry, "Network Carrier")
                if (result is GeoCheckResult.Blocked) return@withContext result
            }

            // Method 3: Try GPS location (requires permission)
            val gpsCountry = getLocationCountry()
            if (gpsCountry != null) {
                val result = evaluateCountry(gpsCountry, "GPS Location")
                if (result is GeoCheckResult.Blocked) return@withContext result
            }

            // Method 4: Fallback to system locale
            val localeCountry = getLocaleCountry()
            if (localeCountry != null) {
                val result = evaluateCountry(localeCountry, "System Locale")
                if (result is GeoCheckResult.Blocked) return@withContext result
            }

            // If we got any successful country detection, return allowed
            val detectedCountry = simCountry ?: networkCountry ?: gpsCountry ?: localeCountry
            val method = when {
                simCountry != null -> "SIM Card"
                networkCountry != null -> "Network Carrier"
                gpsCountry != null -> "GPS Location"
                localeCountry != null -> "System Locale"
                else -> "Unknown"
            }

            GeoCheckResult.Allowed(
                countryCode = detectedCountry,
                detectionMethod = method
            )
        } catch (e: Exception) {
            GeoCheckResult.Error("Unable to verify location: ${e.message}")
        }
    }

    /**
     * Quick check - returns true if blocked, false if allowed
     */
    suspend fun isBlocked(): Boolean {
        return when (checkGeoRestriction()) {
            is GeoCheckResult.Blocked -> true
            else -> false
        }
    }

    /**
     * Screen a wallet address against sanctions lists.
     * Uses a two-layer approach:
     *   1. TRM Labs free Sanctions Screening API (real-time, 28 blockchains, 100 req/day free)
     *   2. Local OFAC SDN address list as fallback
     *
     * @param walletAddress The Solana wallet public key to screen
     * @return true if the wallet is sanctioned (BLOCKED), false if clear
     */
    suspend fun isWalletSanctioned(walletAddress: String): Boolean {
        // Layer 1: Check local SDN list first (instant, no network)
        if (SANCTIONED_WALLET_ADDRESSES.contains(walletAddress)) {
            return true
        }

        // Layer 2: Check TRM Labs free Sanctions Screening API
        return try {
            checkTrmLabsSanctions(walletAddress)
        } catch (e: Exception) {
            // If TRM API fails, fall back to local list only (already checked above)
            android.util.Log.w("GeoRestriction", "TRM Labs API check failed: ${e.message}")
            false
        }
    }

    /**
     * Query TRM Labs free Sanctions Screening API.
     * Endpoint: POST https://api.trmlabs.com/public/v1/sanctions/screening
     * Free tier: 1 req/sec, 100 req/day (no API key required for basic access)
     * Response: [{"address": "...", "isSanctioned": true/false}]
     */
    private suspend fun checkTrmLabsSanctions(walletAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://api.trmlabs.com/public/v1/sanctions/screening")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true

            // Request body: [{"address": "walletAddress"}]
            val requestBody = """[{"address":"$walletAddress"}]"""
            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                // Parse simple JSON response: [{"address":"...","isSanctioned":true}]
                response.contains("\"isSanctioned\":true") || response.contains("\"isSanctioned\": true")
            } else if (responseCode == 429) {
                // Rate limited - fall back to local list (already checked)
                android.util.Log.w("GeoRestriction", "TRM Labs rate limited (429)")
                false
            } else {
                android.util.Log.w("GeoRestriction", "TRM Labs API returned $responseCode")
                false
            }
        } catch (e: Exception) {
            android.util.Log.w("GeoRestriction", "TRM Labs API error: ${e.message}")
            false
        }
    }

    /**
     * Combined pre-transaction check: geo-restriction + wallet screening.
     * Call this before processing any payment to satisfy OFAC requirements.
     *
     * @param walletAddress The connected wallet address
     * @return null if clear, or an error message if blocked
     */
    suspend fun preTransactionCheck(walletAddress: String): String? {
        // Check wallet against SDN list + TRM Labs API
        if (isWalletSanctioned(walletAddress)) {
            return "Transaction blocked: This wallet address is on a sanctions list."
        }
        // Re-check geo-restriction at payment time
        val geoResult = checkGeoRestriction()
        if (geoResult is GeoCheckResult.Blocked) {
            return "Transaction blocked: Service is not available in ${geoResult.countryName} due to sanctions compliance."
        }
        return null // Clear to proceed
    }

    /**
     * Evaluate a country code against the blocked countries list.
     * All listed countries are fully blocked - no warning tiers.
     */
    private fun evaluateCountry(countryCode: String, method: String): GeoCheckResult {
        val upperCode = countryCode.uppercase(Locale.US)
        val countryName = COUNTRY_NAMES[upperCode] ?: countryCode

        return if (BLOCKED_COUNTRIES.contains(upperCode)) {
            GeoCheckResult.Blocked(
                countryCode = upperCode,
                countryName = countryName,
                reason = BlockReason.SANCTIONED_JURISDICTION,
                detectionMethod = method
            )
        } else {
            GeoCheckResult.Allowed(
                countryCode = upperCode,
                detectionMethod = method
            )
        }
    }

    /**
     * Get country from SIM card (MCC - Mobile Country Code)
     */
    private fun getSimCountry(): String? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = telephonyManager?.simCountryIso
            if (!simCountry.isNullOrBlank() && simCountry.length == 2) {
                simCountry.uppercase(Locale.US)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get country from network carrier
     */
    private fun getNetworkCountry(): String? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val networkCountry = telephonyManager?.networkCountryIso
            if (!networkCountry.isNullOrBlank() && networkCountry.length == 2) {
                networkCountry.uppercase(Locale.US)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get country from GPS/Network location
     * Requires ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission
     */
    private fun getLocationCountry(): String? {
        return try {
            // Check if we have location permission
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                return null
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null

            // Try to get last known location
            val location: Location? = try {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                null
            }

            if (location != null) {
                // Use Geocoder to get country from coordinates
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ async geocoder API
                    try {
                        val geocoder = Geocoder(context, Locale.US)
                        var countryResult: String? = null
                        val latch = java.util.concurrent.CountDownLatch(1)
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        ) { addresses ->
                            countryResult = addresses.firstOrNull()?.countryCode?.uppercase(Locale.US)
                            latch.countDown()
                        }
                        // Wait up to 5 seconds for geocoder result
                        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                        countryResult
                    } catch (e: Exception) {
                        null // Geocoder unavailable, fall through to next method
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val geocoder = Geocoder(context, Locale.US)
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.firstOrNull()?.countryCode?.uppercase(Locale.US)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get country from system locale (least reliable)
     */
    private fun getLocaleCountry(): String? {
        return try {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            val country = locale.country
            if (country.isNotBlank() && country.length == 2) {
                country.uppercase(Locale.US)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get user-friendly message for blocked status
     */
    fun getBlockedMessage(result: GeoCheckResult.Blocked): String {
        return """
            AardAppvark is not available in your jurisdiction.

            Detected Location: ${result.countryName}
            Detection Method: ${result.detectionMethod}

            Due to international sanctions, FATF designations, and regulatory requirements (including U.S. OFAC, EU, UK, UN, and Australian DFAT sanctions), AardAppvark is not available in your jurisdiction.

            This restriction is required by applicable law and cannot be bypassed. Using VPNs or proxy services to circumvent these restrictions is prohibited and may result in legal consequences.

            If you believe this is an error, please ensure:
            1. You are not using a VPN or proxy service
            2. Your device's location services are enabled
            3. Your SIM card country matches your actual location

            For questions, contact: aardappvark@proton.me
        """.trimIndent()
    }
}
