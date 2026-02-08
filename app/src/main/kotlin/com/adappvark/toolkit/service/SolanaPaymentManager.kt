package com.adappvark.toolkit.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.adappvark.toolkit.AppConfig
import com.adappvark.toolkit.data.model.SubscriptionPlan
import com.adappvark.toolkit.util.Base58
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager for Solana Mobile Wallet Adapter payments
 * Handles subscription payments via MWA
 *
 * Note: This is a simplified implementation for initial build.
 * Full MWA integration will be added when testing on device.
 */
class SolanaPaymentManager(
    private val context: Context,
    private val activityLauncher: ActivityResultLauncher<Intent>?
) {

    companion object {
        // Payment wallet address from AppConfig
        private val PAYMENT_WALLET_ADDRESS = AppConfig.Payment.WALLET_ADDRESS

        // App identity for MWA
        private val APP_IDENTITY_URI = AppConfig.Identity.URI
        private val APP_IDENTITY_ICON_URI = AppConfig.Identity.ICON_URI
        private val APP_IDENTITY_NAME = AppConfig.Identity.NAME
    }

    // Store wallet connection state
    private var connectedPublicKey: String? = null
    private var authToken: String? = null

    /**
     * Request payment for a subscription plan
     * @return Result with transaction signature on success
     */
    suspend fun requestSubscriptionPayment(
        plan: SubscriptionPlan
    ): Result<PaymentResult> = withContext(Dispatchers.IO) {
        try {
            // For testing/demo, simulate a successful payment
            // In production with real MWA, this would:
            // 1. Connect to wallet
            // 2. Build transaction
            // 3. Sign and send via MWA

            // Generate a mock transaction signature for testing
            val mockSignature = "test_" + System.currentTimeMillis().toString(16) + "_" +
                plan.name.lowercase().take(8)

            Result.success(
                PaymentResult(
                    transactionSignature = mockSignature,
                    plan = plan,
                    amount = plan.getPriceInSol(),
                    timestamp = System.currentTimeMillis()
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connect to Solana wallet
     * Opens the wallet app for authorization
     */
    fun connectWallet(): Boolean {
        return try {
            // Create MWA intent to connect to wallet
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("solana-wallet://")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if any wallet app can handle this
            if (intent.resolveActivity(context.packageManager) != null) {
                activityLauncher?.launch(intent) ?: context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if wallet is connected
     */
    fun isConnected(): Boolean {
        return connectedPublicKey != null
    }

    /**
     * Get connected wallet address
     */
    fun getConnectedAddress(): String? {
        return connectedPublicKey
    }

    /**
     * Verify transaction on-chain
     * @param signature Transaction signature to verify
     * @return true if transaction is confirmed
     */
    suspend fun verifyTransaction(signature: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // For mock transactions (starting with "test_"), always return success
            if (signature.startsWith("test_")) {
                return@withContext Result.success(true)
            }

            val rpcClient = SolanaRpcClient()
            val result = rpcClient.confirmTransaction(signature)
            if (result.isSuccess) {
                val status = result.getOrNull()
                Result.success(status == TransactionStatus.Finalized || status == TransactionStatus.Confirmed)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnect wallet
     */
    fun disconnect() {
        connectedPublicKey = null
        authToken = null
    }
}

/**
 * Result of wallet authorization
 */
data class AuthorizationResult(
    val publicKey: String,
    val token: String,
    val walletUriBase: String?
)

/**
 * Result of successful payment
 */
data class PaymentResult(
    val transactionSignature: String,
    val plan: SubscriptionPlan,
    val amount: Double,
    val timestamp: Long
) {
    /**
     * Get short signature for display (first 8 + last 8 chars)
     */
    fun getShortSignature(): String {
        return if (transactionSignature.length >= 16) {
            "${transactionSignature.take(8)}...${transactionSignature.takeLast(8)}"
        } else {
            transactionSignature
        }
    }

    /**
     * Get Solscan URL for this transaction
     */
    fun getSolscanUrl(): String {
        return "https://solscan.io/tx/$transactionSignature"
    }
}
