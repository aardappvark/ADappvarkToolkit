package com.adappvark.toolkit.service

import android.content.Context
import android.net.Uri
import com.adappvark.toolkit.AppConfig
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult

/**
 * PaymentService - Handles SOL and SKR payments for bulk operations
 *
 * Pricing Model:
 * - Up to 4 apps: FREE
 * - 5+ apps: 0.01 SOL (or 1 SKR) per bulk operation
 *
 * Payment Options:
 * - SOL (Solana native token)
 * - SKR (Seeker token - future implementation)
 */
class PaymentService(
    private val context: Context,
    private val mobileWalletAdapter: MobileWalletAdapter? = null
) {

    companion object {
        // Free tier threshold - first 4 apps are free
        const val FREE_TIER_LIMIT = 4

        // Flat fee for 5+ apps (paid per bulk operation, not per app)
        const val FLAT_FEE_SOL = 0.01  // 0.01 SOL per bulk operation

        // SKR equivalent (based on SOL/SKR rate - update as needed)
        // Assuming 1 SOL ≈ 100 SKR for now
        const val FLAT_FEE_SKR = 1.0  // 1 SKR for bulk operations

        // Treasury wallet address - uses centralized AppConfig
        val TREASURY_WALLET = AppConfig.Payment.WALLET_ADDRESS

        // SKR token mint address (replace with actual when available)
        const val SKR_TOKEN_MINT = "SKRTokenMint11111111111111111111111111111"

        /**
         * Create a shared MobileWalletAdapter instance.
         * Must be called before Activity is STARTED (e.g., in remember{} at composition time).
         */
        fun createWalletAdapter(): MobileWalletAdapter {
            return MobileWalletAdapter(
                connectionIdentity = ConnectionIdentity(
                    identityUri = Uri.parse(AppConfig.Identity.URI),
                    iconUri = Uri.parse(AppConfig.Identity.ICON_URI),
                    identityName = AppConfig.Identity.NAME
                )
            )
        }
    }

    /**
     * Check if payment is required for the given app count
     */
    fun isPaymentRequired(appCount: Int): Boolean {
        if (AppConfig.Features.TESTING_BYPASS_PAYMENT) return false
        return appCount > FREE_TIER_LIMIT
    }

    /**
     * Calculate the fee in SOL for a given number of apps
     * First 4 apps are free, 5+ apps is a flat fee per operation
     */
    fun calculateFeeSOL(appCount: Int): Double {
        return if (appCount <= FREE_TIER_LIMIT) 0.0 else FLAT_FEE_SOL
    }

    /**
     * Calculate the fee in SKR for a given number of apps
     * First 4 apps are free, 5+ apps is a flat fee per operation
     */
    fun calculateFeeSKR(appCount: Int): Double {
        return if (appCount <= FREE_TIER_LIMIT) 0.0 else FLAT_FEE_SKR
    }

    /**
     * Format SOL amount for display
     */
    fun formatSOL(amount: Double): String {
        return if (amount == 0.0) {
            "FREE"
        } else {
            String.format("%.4f SOL", amount)
        }
    }

    /**
     * Format SKR amount for display
     */
    fun formatSKR(amount: Double): String {
        return if (amount == 0.0) {
            "FREE"
        } else {
            String.format("%.1f SKR", amount)
        }
    }

    /**
     * Get pricing summary for display
     */
    fun getPricingSummary(appCount: Int): PricingSummary {
        val solFee = calculateFeeSOL(appCount)
        val skrFee = calculateFeeSKR(appCount)

        return PricingSummary(
            appCount = appCount,
            isFree = !isPaymentRequired(appCount),
            solAmount = solFee,
            skrAmount = skrFee,
            solFormatted = formatSOL(solFee),
            skrFormatted = formatSKR(skrFee)
        )
    }

    /**
     * Request SOL payment from user's wallet
     * Note: Full implementation requires building and signing a SOL transfer transaction
     */
    suspend fun requestSOLPayment(
        activityResultSender: ActivityResultSender,
        appCount: Int,
        onSuccess: (transactionSignature: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val amount = calculateFeeSOL(appCount)
        if (amount == 0.0) {
            onSuccess("FREE_TRANSACTION")
            return
        }

        val adapter = mobileWalletAdapter ?: run {
            onError("Payment service not properly initialized. Please restart the app.")
            return
        }

        try {
            // Connect to wallet and prepare transaction
            val result = adapter.transact(activityResultSender) {
                // First authorize using centralized AppConfig
                val authResult = authorize(
                    identityUri = Uri.parse(AppConfig.Identity.URI),
                    iconUri = Uri.parse(AppConfig.Identity.ICON_URI),
                    identityName = AppConfig.Identity.NAME,
                    chain = if (AppConfig.Payment.CLUSTER == "mainnet-beta") "solana:mainnet" else "solana:devnet"
                )

                // In a full implementation, we would:
                // 1. Build a SOL transfer transaction to TREASURY_WALLET
                // 2. Request signAndSendTransactions()
                // 3. Return the transaction signature

                // For now, return auth result - actual payment needs RPC integration
                authResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    // In production, this would be the actual transaction signature
                    // For now, we generate a placeholder
                    val mockSignature = "PAYMENT_${System.currentTimeMillis()}_${appCount}apps_${amount}SOL"
                    onSuccess(mockSignature)
                }
                is TransactionResult.NoWalletFound -> {
                    onError("No wallet found. Please install a Solana wallet app.")
                }
                is TransactionResult.Failure -> {
                    onError(result.e.message ?: "Payment failed")
                }
            }
        } catch (e: Exception) {
            onError(e.message ?: "Payment failed")
        }
    }

    /**
     * Request SKR token payment from user's wallet
     * Note: SKR token payment requires SPL token transfer implementation
     */
    suspend fun requestSKRPayment(
        activityResultSender: ActivityResultSender,
        appCount: Int,
        onSuccess: (transactionSignature: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val amount = calculateFeeSKR(appCount)
        if (amount == 0.0) {
            onSuccess("FREE_TRANSACTION")
            return
        }

        // SKR payment implementation would be similar to SOL but using SPL token transfer
        // For now, show that SKR is coming soon
        onError("SKR payments coming soon! Please use SOL for now.")
    }
}

/**
 * Pricing summary for display
 */
data class PricingSummary(
    val appCount: Int,
    val isFree: Boolean,
    val solAmount: Double,
    val skrAmount: Double,
    val solFormatted: String,
    val skrFormatted: String
) {
    fun getDescription(): String {
        return if (isFree) {
            "Free for $appCount apps"
        } else {
            "$appCount apps • $solFormatted or $skrFormatted"
        }
    }
}

/**
 * Payment transaction result
 */
sealed class PaymentTransactionResult {
    data class Success(
        val transactionSignature: String,
        val paymentMethod: PaymentMethod,
        val amount: Double
    ) : PaymentTransactionResult()

    data class Error(val message: String) : PaymentTransactionResult()

    object Cancelled : PaymentTransactionResult()

    object Free : PaymentTransactionResult()
}

enum class PaymentMethod {
    SOL,
    SKR,
    FREE
}
