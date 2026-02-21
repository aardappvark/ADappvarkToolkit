package com.adappvark.toolkit.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.adappvark.toolkit.AppConfig
import com.adappvark.toolkit.data.model.SubscriptionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager for Solana payments via Mobile Wallet Adapter.
 *
 * Current state: All bulk operations are temporarily free during the hackathon/launch period.
 * When the free period ends, this manager will handle real SPL token transfers (SKR)
 * via MWA for non-SGT users.
 *
 * SGT (Seeker Genesis Token) holders always get free access — verified via seeker-verify.
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
     * Request payment for a subscription plan.
     *
     * Currently returns a free-tier grant since all operations are temporarily free.
     * When real payments are enabled, this will build and send an SPL token transfer
     * via MWA to the payment wallet.
     *
     * @return Result with payment result on success
     */
    suspend fun requestSubscriptionPayment(
        plan: SubscriptionPlan
    ): Result<PaymentResult> = withContext(Dispatchers.IO) {
        try {
            // Temporarily free — no real payment required during launch period.
            // This path will be replaced with real MWA SPL token transfer when
            // the free period ends and SKR payments are enabled.
            val grantId = "free_grant_${System.currentTimeMillis()}"

            Result.success(
                PaymentResult(
                    transactionSignature = grantId,
                    plan = plan,
                    amount = 0.0,  // Free during launch
                    timestamp = System.currentTimeMillis(),
                    isFreeGrant = true
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connect to Solana wallet.
     * Opens the wallet app for authorization.
     */
    fun connectWallet(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("solana-wallet://")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

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
     * Verify transaction on-chain.
     * @param signature Transaction signature to verify
     * @return true if transaction is confirmed
     */
    suspend fun verifyTransaction(signature: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Free grants are always valid — no on-chain verification needed
            if (signature.startsWith("free_grant_")) {
                return@withContext Result.success(true)
            }

            // Real on-chain verification for actual SPL token transfers
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
    val timestamp: Long,
    val isFreeGrant: Boolean = false
) {
    /**
     * Get short signature for display (first 8 + last 8 chars)
     */
    fun getShortSignature(): String {
        if (isFreeGrant) return "Free Grant"
        return if (transactionSignature.length >= 16) {
            "${transactionSignature.take(8)}...${transactionSignature.takeLast(8)}"
        } else {
            transactionSignature
        }
    }

    /**
     * Get Solscan URL for this transaction (only for real on-chain transactions)
     */
    fun getSolscanUrl(): String? {
        if (isFreeGrant) return null
        return "https://solscan.io/tx/$transactionSignature"
    }
}
