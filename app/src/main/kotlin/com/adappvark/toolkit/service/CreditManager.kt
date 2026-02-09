package com.adappvark.toolkit.service

import android.content.Context
import android.content.SharedPreferences
import com.adappvark.toolkit.BuildConfig
import com.adappvark.toolkit.config.CreditState
import com.adappvark.toolkit.config.PaymentConfig

/**
 * CreditManager - Manages credit balance for bulk operations
 *
 * Credit System Rules:
 * - Credits are non-refundable
 * - Credits expire 12 months after purchase
 * - 1 credit = bulk operation on 5+ apps (uninstall or reinstall)
 * - Up to 4 apps per operation are FREE
 * - Adding/removing favourites is FREE (no credit cost)
 * - 1 FREE credit granted on first wallet connect to Seeker device
 */
class CreditManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "aardappvark_credits"
        private const val KEY_BALANCE = "credit_balance"
        private const val KEY_EXPIRES_AT = "credit_expires_at"
        private const val KEY_LAST_TRANSACTION = "last_transaction_id"
        private const val KEY_TOTAL_PURCHASED = "total_credits_purchased"
        private const val KEY_TOTAL_USED = "total_credits_used"
        private const val KEY_FREE_CREDIT_GRANTED = "free_credit_granted"
        private const val KEY_CONNECTED_WALLET = "connected_wallet_address"
        private const val KEY_WALLET_CONNECT_TIME = "wallet_connect_timestamp"
    }

    /**
     * Get current credit state
     */
    fun getCreditState(): CreditState {
        val balance = prefs.getInt(KEY_BALANCE, 0)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L).takeIf { it > 0 }
        val lastTransaction = prefs.getString(KEY_LAST_TRANSACTION, null)

        // Check if credits have expired
        if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
            // Credits expired - reset balance
            clearExpiredCredits()
            return CreditState(balance = 0, expiresAt = null, lastPurchaseTransactionId = null)
        }

        return CreditState(
            balance = balance,
            expiresAt = expiresAt,
            lastPurchaseTransactionId = lastTransaction
        )
    }

    /**
     * Get current credit balance (after checking expiration)
     */
    fun getBalance(): Int {
        return getCreditState().balance
    }

    /**
     * Check if user has enough credits for operation
     * @param appCount Total number of apps in the bulk operation (INCLUDING favourites)
     * @return true if user has credits OR if operation is free (up to 4 apps)
     */
    fun hasCreditsFor(appCount: Int): Boolean {
        // Up to 4 apps are free
        if (appCount <= PaymentConfig.FREE_TIER_LIMIT) {
            return true
        }
        // 5+ apps require 1 credit
        return getBalance() >= 1
    }

    /**
     * Calculate credits required for operation
     * @param appCount Total number of apps (INCLUDING favourites - prevents gaming)
     * @return Credits required (0 for up to 4 apps, 1 for 5+ apps)
     */
    fun creditsRequired(appCount: Int): Int {
        return if (appCount > PaymentConfig.FREE_TIER_LIMIT) 1 else 0
    }

    /**
     * Consume credit for bulk operation
     * @param appCount Total number of apps operated on (INCLUDING favourites)
     * @return true if credit was consumed, false if no credit was needed or available
     */
    fun consumeCredit(appCount: Int): Boolean {
        val required = creditsRequired(appCount)
        if (required == 0) {
            // No credit needed for single app
            return true
        }

        val currentBalance = getBalance()
        if (currentBalance < required) {
            return false
        }

        // Deduct credit
        val newBalance = currentBalance - required
        prefs.edit()
            .putInt(KEY_BALANCE, newBalance)
            .putInt(KEY_TOTAL_USED, getTotalUsed() + required)
            .apply()

        return true
    }

    /**
     * Add credits after successful purchase
     * @param creditCount Number of credits to add
     * @param transactionId Solana transaction signature
     */
    fun addCredits(creditCount: Int, transactionId: String) {
        val currentBalance = getBalance()
        val currentExpiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        // Calculate new expiration (12 months from now)
        val newExpiresAt = System.currentTimeMillis() +
            (PaymentConfig.CREDIT_EXPIRATION_DAYS.toLong() * 24 * 60 * 60 * 1000)

        // If credits already exist, extend expiration to the later date
        val finalExpiresAt = if (currentExpiresAt > 0 && currentBalance > 0) {
            maxOf(currentExpiresAt, newExpiresAt)
        } else {
            newExpiresAt
        }

        prefs.edit()
            .putInt(KEY_BALANCE, currentBalance + creditCount)
            .putLong(KEY_EXPIRES_AT, finalExpiresAt)
            .putString(KEY_LAST_TRANSACTION, transactionId)
            .putInt(KEY_TOTAL_PURCHASED, getTotalPurchased() + creditCount)
            .apply()
    }

    /**
     * Get total credits ever purchased
     */
    fun getTotalPurchased(): Int {
        return prefs.getInt(KEY_TOTAL_PURCHASED, 0)
    }

    /**
     * Get total credits ever used
     */
    fun getTotalUsed(): Int {
        return prefs.getInt(KEY_TOTAL_USED, 0)
    }

    /**
     * Clear expired credits
     */
    private fun clearExpiredCredits() {
        prefs.edit()
            .putInt(KEY_BALANCE, 0)
            .putLong(KEY_EXPIRES_AT, 0L)
            .apply()
    }

    /**
     * Check if credits will expire soon (within 30 days)
     */
    fun isExpiringSoon(): Boolean {
        val state = getCreditState()
        if (state.balance == 0 || state.expiresAt == null) return false
        return state.daysUntilExpiration() <= 30
    }

    /**
     * Get formatted expiration message
     */
    fun getExpirationMessage(): String? {
        val state = getCreditState()
        if (state.balance == 0 || state.expiresAt == null) return null

        val days = state.daysUntilExpiration()
        return when {
            days <= 0 -> "Credits expired"
            days == 1 -> "Credits expire tomorrow"
            days <= 7 -> "Credits expire in $days days"
            days <= 30 -> "Credits expire in $days days"
            else -> null
        }
    }

    /**
     * Check if free credit has already been granted for wallet connect
     */
    fun hasReceivedFreeCredit(): Boolean {
        return prefs.getBoolean(KEY_FREE_CREDIT_GRANTED, false)
    }

    /**
     * Grant 1 free credit on first wallet connect to Seeker device
     * @param walletAddress The connected wallet public key
     * @return true if credit was granted, false if already received
     */
    fun grantWalletConnectCredit(walletAddress: String): Boolean {
        if (hasReceivedFreeCredit()) {
            return false
        }

        // Add 1 free credit
        addCredits(1, "seeker_wallet_connect_bonus")

        // Mark as granted and store wallet info
        prefs.edit()
            .putBoolean(KEY_FREE_CREDIT_GRANTED, true)
            .putString(KEY_CONNECTED_WALLET, walletAddress)
            .putLong(KEY_WALLET_CONNECT_TIME, System.currentTimeMillis())
            .apply()

        return true
    }

    /**
     * Get connected wallet address (if any)
     */
    fun getConnectedWallet(): String? {
        return prefs.getString(KEY_CONNECTED_WALLET, null)
    }

    /**
     * Get wallet connect timestamp
     */
    fun getWalletConnectTime(): Long {
        return prefs.getLong(KEY_WALLET_CONNECT_TIME, 0L)
    }

    /**
     * FOR TESTING ONLY: Set credit balance directly
     * Gated behind BuildConfig.DEBUG to prevent abuse in production
     */
    fun debugSetCredits(balance: Int, expiresInDays: Int = 365) {
        if (!BuildConfig.DEBUG) return  // No-op in production
        val expiresAt = System.currentTimeMillis() +
            (expiresInDays.toLong() * 24 * 60 * 60 * 1000)
        prefs.edit()
            .putInt(KEY_BALANCE, balance)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    /**
     * FOR TESTING ONLY: Reset free credit flag
     * Gated behind BuildConfig.DEBUG to prevent abuse in production
     */
    fun debugResetFreeCredit() {
        if (!BuildConfig.DEBUG) return  // No-op in production
        prefs.edit()
            .putBoolean(KEY_FREE_CREDIT_GRANTED, false)
            .apply()
    }

    /**
     * Reset all credit data (GDPR Right to Erasure support)
     */
    fun resetAll() {
        prefs.edit().clear().apply()
    }
}
