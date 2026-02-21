package com.adappvark.toolkit.service

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * UserPreferencesManager - Manages user preferences, T&C acceptance, and onboarding state
 *
 * Tracks:
 * - Terms and Conditions acceptance with wallet address and timestamp
 * - Wallet connection state
 * - Notification permission status
 * - Onboarding completion
 */
class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "aardappvark_user_prefs"

        // T&C acceptance
        private const val KEY_TNC_ACCEPTED = "tnc_accepted"
        private const val KEY_TNC_ACCEPTED_TIMESTAMP = "tnc_accepted_timestamp"
        private const val KEY_TNC_ACCEPTED_WALLET = "tnc_accepted_wallet"
        private const val KEY_TNC_VERSION = "tnc_version"

        // Wallet connection
        private const val KEY_WALLET_CONNECTED = "wallet_connected"
        private const val KEY_WALLET_PUBLIC_KEY = "wallet_public_key"
        private const val KEY_WALLET_NAME = "wallet_name"
        private const val KEY_WALLET_CONNECT_TIMESTAMP = "wallet_connect_timestamp"

        // Onboarding
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_SKIPPED_WALLET = "skipped_wallet"
        private const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"

        // Current T&C version - increment when T&C changes
        const val CURRENT_TNC_VERSION = 1
    }

    // ==================== Terms & Conditions ====================

    /**
     * Check if user has accepted current T&C version
     */
    fun hasAcceptedTerms(): Boolean {
        val accepted = prefs.getBoolean(KEY_TNC_ACCEPTED, false)
        val acceptedVersion = prefs.getInt(KEY_TNC_VERSION, 0)
        return accepted && acceptedVersion >= CURRENT_TNC_VERSION
    }

    /**
     * Record T&C acceptance with wallet address and timestamp
     * @param walletAddress The wallet public key used to accept
     */
    fun acceptTerms(walletAddress: String) {
        val timestamp = System.currentTimeMillis()
        prefs.edit()
            .putBoolean(KEY_TNC_ACCEPTED, true)
            .putLong(KEY_TNC_ACCEPTED_TIMESTAMP, timestamp)
            .putString(KEY_TNC_ACCEPTED_WALLET, walletAddress)
            .putInt(KEY_TNC_VERSION, CURRENT_TNC_VERSION)
            .apply()
    }

    /**
     * Get T&C acceptance details
     */
    fun getTermsAcceptanceDetails(): TermsAcceptance? {
        if (!hasAcceptedTerms()) return null

        return TermsAcceptance(
            walletAddress = prefs.getString(KEY_TNC_ACCEPTED_WALLET, null) ?: "",
            timestamp = prefs.getLong(KEY_TNC_ACCEPTED_TIMESTAMP, 0L),
            version = prefs.getInt(KEY_TNC_VERSION, 0)
        )
    }

    /**
     * Get formatted acceptance date
     */
    fun getAcceptanceDateFormatted(): String? {
        val timestamp = prefs.getLong(KEY_TNC_ACCEPTED_TIMESTAMP, 0L)
        if (timestamp == 0L) return null

        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    // ==================== Wallet Connection ====================

    /**
     * Check if wallet is connected
     */
    fun isWalletConnected(): Boolean {
        return prefs.getBoolean(KEY_WALLET_CONNECTED, false)
    }

    /**
     * Save wallet connection
     * @param publicKey The wallet public key
     * @param walletName The wallet app name (e.g., "Solflare", "Phantom")
     */
    fun saveWalletConnection(publicKey: String, walletName: String) {
        prefs.edit()
            .putBoolean(KEY_WALLET_CONNECTED, true)
            .putString(KEY_WALLET_PUBLIC_KEY, publicKey)
            .putString(KEY_WALLET_NAME, walletName)
            .putLong(KEY_WALLET_CONNECT_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    /**
     * Get connected wallet public key
     */
    fun getWalletPublicKey(): String? {
        return prefs.getString(KEY_WALLET_PUBLIC_KEY, null)
    }

    /**
     * Get connected wallet name
     */
    fun getWalletName(): String? {
        return prefs.getString(KEY_WALLET_NAME, null)
    }

    /**
     * Get shortened wallet address for display (e.g., "FFWGS...1jfg")
     */
    fun getShortWalletAddress(): String? {
        val publicKey = getWalletPublicKey() ?: return null
        if (publicKey.length < 8) return publicKey
        return "${publicKey.take(5)}...${publicKey.takeLast(4)}"
    }

    /**
     * Disconnect wallet
     */
    fun disconnectWallet() {
        prefs.edit()
            .putBoolean(KEY_WALLET_CONNECTED, false)
            .remove(KEY_WALLET_PUBLIC_KEY)
            .remove(KEY_WALLET_NAME)
            .apply()
    }

    // ==================== Onboarding ====================

    /**
     * Check if onboarding is complete (T&C accepted + wallet connected OR skipped)
     */
    fun isOnboardingComplete(): Boolean {
        return hasAcceptedTerms() && (isWalletConnected() || hasSkippedWallet())
    }

    /**
     * Mark that the user skipped wallet connection
     */
    fun setSkippedWallet() {
        prefs.edit().putBoolean(KEY_SKIPPED_WALLET, true).apply()
    }

    /**
     * Check if user skipped wallet connection
     */
    fun hasSkippedWallet(): Boolean {
        return prefs.getBoolean(KEY_SKIPPED_WALLET, false)
    }

    /**
     * Clear the skipped-wallet flag (when user later connects a wallet)
     */
    fun clearSkippedWallet() {
        prefs.edit().putBoolean(KEY_SKIPPED_WALLET, false).apply()
    }

    /**
     * Mark onboarding as complete
     */
    fun setOnboardingComplete() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .apply()
    }

    // ==================== Notifications ====================

    /**
     * Check if notification permission has been asked
     */
    fun hasAskedNotificationPermission(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false)
    }

    /**
     * Mark notification permission as asked
     */
    fun setNotificationPermissionAsked(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true)
            .putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
            .apply()
    }

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, false)
    }

    // ==================== Reset (for testing) ====================

    /**
     * Reset all user preferences (for testing)
     */
    fun resetAll() {
        prefs.edit().clear().apply()
    }
}

/**
 * Data class for T&C acceptance details
 */
data class TermsAcceptance(
    val walletAddress: String,
    val timestamp: Long,
    val version: Int
) {
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}
