package com.adappvark.toolkit.config

import com.adappvark.toolkit.AppConfig

/**
 * Payment Configuration for AardAppvark
 *
 * SUPPORTED TOKENS:
 * - SOL (Solana native token)
 * - SKR (Seeker token) - https://solscan.io/token/SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3
 *
 * WALLET ADDRESS:
 * - Configured in AppConfig.Payment.WALLET_ADDRESS
 *
 * PRICING MODEL:
 * - Up to 4 apps per operation: FREE
 * - 5+ apps per operation: 0.01 SOL flat fee
 * - 1 free credit granted on first wallet connect
 * - Credits are non-refundable and expire 12 months after purchase
 */
object PaymentConfig {

    // Wallet address for receiving payments - uses centralized AppConfig
    val RECEIVING_WALLET_ADDRESS: String get() = AppConfig.Payment.WALLET_ADDRESS

    // Token mint addresses
    const val SOL_MINT = "So11111111111111111111111111111111111111112" // Native SOL (wrapped)
    const val SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3" // Seeker token

    // Credit pricing in SOL (flat fee per bulk operation)
    const val OPERATION_FEE_SOL = 0.01        // 0.01 SOL per bulk operation (5+ apps)

    // Credit expiration
    const val CREDIT_EXPIRATION_DAYS = 365    // 12 months

    // Free tier threshold
    const val FREE_TIER_LIMIT = 4             // Up to 4 apps are free

    /**
     * Check if SKR token is configured
     */
    fun isSkrConfigured(): Boolean = SKR_MINT.isNotBlank()

    /**
     * Check if wallet is configured
     */
    fun isWalletConfigured(): Boolean = RECEIVING_WALLET_ADDRESS.isNotBlank()

    /**
     * Get Solscan URL for transaction verification (cluster-aware)
     */
    fun getSolscanUrl(transactionSignature: String): String {
        val clusterParam = if (AppConfig.Payment.CLUSTER == "mainnet-beta") "" else "?cluster=${AppConfig.Payment.CLUSTER}"
        return "https://solscan.io/tx/$transactionSignature$clusterParam"
    }

    /**
     * Get Solana Explorer URL for transaction verification (cluster-aware)
     */
    fun getExplorerUrl(transactionSignature: String): String {
        val clusterParam = if (AppConfig.Payment.CLUSTER == "mainnet-beta") "" else "?cluster=${AppConfig.Payment.CLUSTER}"
        return "https://explorer.solana.com/tx/$transactionSignature$clusterParam"
    }

    /**
     * Get Solscan URL for SKR token
     */
    fun getSkrTokenUrl(): String {
        val clusterParam = if (AppConfig.Payment.CLUSTER == "mainnet-beta") "" else "?cluster=${AppConfig.Payment.CLUSTER}"
        return "https://solscan.io/token/$SKR_MINT$clusterParam"
    }
}

/**
 * Wallet Connection State
 */
data class WalletState(
    val isConnected: Boolean = false,
    val publicKey: String? = null,
    val walletName: String? = null,
    val solBalance: Double? = null,
    val skrBalance: Double? = null
)

/**
 * Credit Balance State
 */
data class CreditState(
    val balance: Int = 0,
    val expiresAt: Long? = null,
    val lastPurchaseTransactionId: String? = null
) {
    fun isExpired(): Boolean {
        return expiresAt != null && System.currentTimeMillis() > expiresAt
    }

    fun daysUntilExpiration(): Int {
        if (expiresAt == null) return 0
        val diff = expiresAt - System.currentTimeMillis()
        return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }
}
