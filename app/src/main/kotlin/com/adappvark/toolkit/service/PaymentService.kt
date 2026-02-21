package com.adappvark.toolkit.service

import android.content.Context
import android.net.Uri
import android.util.Log
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
 * - SOL (Solana native token) via SystemProgram::Transfer
 * - SKR (Seeker SPL token) via TokenProgram::TransferChecked
 */
class PaymentService(
    private val context: Context,
    private val mobileWalletAdapter: MobileWalletAdapter? = null
) {

    companion object {
        private const val TAG = "PaymentService"

        // Free tier threshold - first 4 apps are free
        const val FREE_TIER_LIMIT = 4

        // Flat fee for 5+ apps (paid per bulk operation, not per app)
        const val FLAT_FEE_SOL = 0.01  // 0.01 SOL per bulk operation

        // SKR equivalent (based on SOL/SKR rate - update as needed)
        // Assuming 1 SOL ≈ 100 SKR for now
        const val FLAT_FEE_SKR = 1.0  // 1 SKR for bulk operations

        // Treasury wallet address - uses centralized AppConfig
        val TREASURY_WALLET = AppConfig.Payment.WALLET_ADDRESS

        // SKR token mint address (official Seeker token)
        const val SKR_TOKEN_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"

        // Lamports per SOL
        private const val LAMPORTS_PER_SOL = 1_000_000_000L

        // Base58 alphabet for decoding
        private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

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

        /**
         * Decode a Base58-encoded string to bytes
         */
        fun decodeBase58(input: String): ByteArray {
            var bi = java.math.BigInteger.ZERO
            for (ch in input) {
                val index = BASE58_ALPHABET.indexOf(ch)
                require(index >= 0) { "Invalid Base58 character: $ch" }
                bi = bi.multiply(java.math.BigInteger.valueOf(58)) + java.math.BigInteger.valueOf(index.toLong())
            }

            // Convert to byte array
            val bytes = bi.toByteArray()

            // Count leading 1s (which represent leading zero bytes in Base58)
            val leadingZeros = input.takeWhile { it == '1' }.length

            // Strip any leading zero byte from BigInteger representation
            val stripped = if (bytes.isNotEmpty() && bytes[0] == 0.toByte()) {
                bytes.copyOfRange(1, bytes.size)
            } else {
                bytes
            }

            // Prepend leading zeros
            return ByteArray(leadingZeros) + stripped
        }

        /**
         * Encode bytes to Base58 string
         */
        fun encodeBase58(input: ByteArray): String {
            if (input.isEmpty()) return ""

            var bi = java.math.BigInteger(1, input)
            val sb = StringBuilder()

            while (bi > java.math.BigInteger.ZERO) {
                val (quotient, remainder) = bi.divideAndRemainder(java.math.BigInteger.valueOf(58))
                sb.append(BASE58_ALPHABET[remainder.toInt()])
                bi = quotient
            }

            // Add leading '1' characters for each leading zero byte
            for (byte in input) {
                if (byte == 0.toByte()) sb.append('1') else break
            }

            return sb.reverse().toString()
        }
    }

    private val rpcClient = SolanaRpcClient()
    private val txBuilder = SolanaTransactionBuilder()

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
     * Builds a real SystemProgram::Transfer transaction, signs via MWA, and submits to network.
     * Performs geo-restriction and wallet sanctions check before processing.
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

        // Pre-transaction compliance check: geo-restriction + wallet screening
        val geoService = GeoRestrictionService(context)
        val geoResult = geoService.checkGeoRestriction()
        if (geoResult is GeoRestrictionService.GeoCheckResult.Blocked) {
            onError("Payment blocked: AardAppvark is not available in ${geoResult.countryName} due to sanctions compliance.")
            return
        }
        if (geoResult is GeoRestrictionService.GeoCheckResult.Error) {
            onError("Payment blocked: Unable to verify your location. Location verification is required for sanctions compliance.")
            return
        }

        val adapter = mobileWalletAdapter ?: run {
            onError("Payment service not properly initialized. Please restart the dApp.")
            return
        }

        // Step 1: Get recent blockhash from Solana RPC
        val blockhashResult = rpcClient.getRecentBlockhash()
        val blockhashData = blockhashResult.getOrElse { e ->
            onError("Failed to get blockhash: ${e.message}")
            return
        }

        Log.d(TAG, "Got blockhash: ${blockhashData.blockhash}")

        // Step 2: Decode treasury wallet address from Base58
        val treasuryPubkey: ByteArray
        try {
            treasuryPubkey = decodeBase58(TREASURY_WALLET)
            require(treasuryPubkey.size == 32) { "Treasury wallet decoded to ${treasuryPubkey.size} bytes, expected 32" }
        } catch (e: Exception) {
            onError("Invalid treasury wallet configuration: ${e.message}")
            return
        }

        // Step 3: Decode blockhash from Base58
        val blockhashBytes: ByteArray
        try {
            blockhashBytes = decodeBase58(blockhashData.blockhash)
            require(blockhashBytes.size == 32) { "Blockhash decoded to ${blockhashBytes.size} bytes, expected 32" }
        } catch (e: Exception) {
            onError("Invalid blockhash: ${e.message}")
            return
        }

        // Step 4: Calculate lamports
        val lamports = (amount * LAMPORTS_PER_SOL).toLong()
        Log.d(TAG, "Transfer amount: $amount SOL = $lamports lamports")

        try {
            // Step 5: Connect to wallet, authorize, build tx, sign & send
            val result = adapter.transact(activityResultSender) {
                // Authorize with the wallet
                val authResult = authorize(
                    identityUri = Uri.parse(AppConfig.Identity.URI),
                    iconUri = Uri.parse(AppConfig.Identity.ICON_URI),
                    identityName = AppConfig.Identity.NAME,
                    chain = if (AppConfig.Payment.CLUSTER == "mainnet-beta") "solana:mainnet" else "solana:devnet"
                )

                // Get user's public key from auth result
                val userPubkey = authResult.accounts.firstOrNull()?.publicKey
                    ?: throw Exception("No account returned from wallet authorization")

                Log.d(TAG, "User pubkey: ${userPubkey.size} bytes")

                // Wallet sanctions check on the user's public key
                val walletAddress = encodeBase58(userPubkey)
                if (geoService.isWalletSanctioned(walletAddress)) {
                    throw Exception("Wallet $walletAddress is on a sanctions list. Transaction blocked.")
                }

                // Build the SOL transfer transaction message
                val transactionMessage = txBuilder.buildTransferTransaction(
                    fromPubkey = userPubkey,
                    toPubkey = treasuryPubkey,
                    lamports = lamports,
                    recentBlockhash = blockhashBytes
                )

                Log.d(TAG, "Built transaction message: ${transactionMessage.size} bytes")

                // Sign and send the transaction via the wallet
                val signResult = signAndSendTransactions(
                    transactions = arrayOf(transactionMessage)
                )

                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    // Extract the transaction signature from the result
                    val signatures = result.payload.signatures
                    val signature = if (signatures.isNotEmpty()) {
                        encodeBase58(signatures[0])
                    } else {
                        "TX_${System.currentTimeMillis()}"
                    }
                    Log.d(TAG, "Payment successful! Signature: $signature")
                    onSuccess(signature)
                }
                is TransactionResult.NoWalletFound -> {
                    onError("No wallet found. Please install a Solana wallet app (Phantom, Solflare, etc.).")
                }
                is TransactionResult.Failure -> {
                    Log.e(TAG, "Payment failed", result.e)
                    onError(result.e.message ?: "Payment failed. Please try again.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Payment error", e)
            onError(e.message ?: "Payment failed unexpectedly.")
        }
    }

    /**
     * Request SKR token payment from user's wallet.
     * Builds a real SPL Token TransferChecked transaction, signs via MWA, and submits to network.
     * Includes CreateAssociatedTokenAccount for the treasury if needed.
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

        // Pre-transaction compliance check: geo-restriction + wallet screening
        val geoService = GeoRestrictionService(context)
        val geoResult = geoService.checkGeoRestriction()
        if (geoResult is GeoRestrictionService.GeoCheckResult.Blocked) {
            onError("Payment blocked: AardAppvark is not available in ${geoResult.countryName} due to sanctions compliance.")
            return
        }
        if (geoResult is GeoRestrictionService.GeoCheckResult.Error) {
            onError("Payment blocked: Unable to verify your location. Location verification is required for sanctions compliance.")
            return
        }

        val adapter = mobileWalletAdapter ?: run {
            onError("Payment service not properly initialized. Please restart the dApp.")
            return
        }

        // Step 1: Get recent blockhash
        val blockhashResult = rpcClient.getRecentBlockhash()
        val blockhashData = blockhashResult.getOrElse { e ->
            onError("Failed to get blockhash: ${e.message}")
            return
        }
        Log.d(TAG, "SKR payment — got blockhash: ${blockhashData.blockhash}")

        // Step 2: Get SKR token decimals from on-chain mint
        val tokenInfoResult = rpcClient.getTokenSupply(SKR_TOKEN_MINT)
        val tokenInfo = tokenInfoResult.getOrElse { e ->
            onError("Failed to get SKR token info: ${e.message}")
            return
        }
        val decimals = tokenInfo.decimals
        Log.d(TAG, "SKR token decimals: $decimals")

        // Step 3: Check if treasury already has an ATA for SKR
        val treasuryAtaResult = rpcClient.getTokenAccountsByOwner(TREASURY_WALLET, SKR_TOKEN_MINT)
        val treasuryHasAta = treasuryAtaResult.getOrNull()?.isNotEmpty() == true
        Log.d(TAG, "Treasury has SKR ATA: $treasuryHasAta")

        // Step 4: Decode addresses
        val treasuryPubkey: ByteArray
        val mintPubkey: ByteArray
        val blockhashBytes: ByteArray
        try {
            treasuryPubkey = decodeBase58(TREASURY_WALLET)
            require(treasuryPubkey.size == 32) { "Treasury wallet decoded to ${treasuryPubkey.size} bytes" }
            mintPubkey = decodeBase58(SKR_TOKEN_MINT)
            require(mintPubkey.size == 32) { "SKR mint decoded to ${mintPubkey.size} bytes" }
            blockhashBytes = decodeBase58(blockhashData.blockhash)
            require(blockhashBytes.size == 32) { "Blockhash decoded to ${blockhashBytes.size} bytes" }
        } catch (e: Exception) {
            onError("Invalid address configuration: ${e.message}")
            return
        }

        // Step 5: Calculate token amount in smallest units
        val tokenAmount = (amount * Math.pow(10.0, decimals.toDouble())).toLong()
        Log.d(TAG, "SKR transfer: $amount SKR = $tokenAmount smallest units (decimals=$decimals)")

        try {
            // Step 6: Connect to wallet, authorize, build SPL tx, sign & send
            val result = adapter.transact(activityResultSender) {
                val authResult = authorize(
                    identityUri = Uri.parse(AppConfig.Identity.URI),
                    iconUri = Uri.parse(AppConfig.Identity.ICON_URI),
                    identityName = AppConfig.Identity.NAME,
                    chain = if (AppConfig.Payment.CLUSTER == "mainnet-beta") "solana:mainnet" else "solana:devnet"
                )

                val userPubkey = authResult.accounts.firstOrNull()?.publicKey
                    ?: throw Exception("No account returned from wallet authorization")

                Log.d(TAG, "SKR payment — user pubkey: ${userPubkey.size} bytes")

                // Wallet sanctions check
                val walletAddress = encodeBase58(userPubkey)
                if (geoService.isWalletSanctioned(walletAddress)) {
                    throw Exception("Wallet $walletAddress is on a sanctions list. Transaction blocked.")
                }

                // Build SPL token TransferChecked transaction
                val transactionMessage = txBuilder.buildSplTransferCheckedTransaction(
                    fromWallet = userPubkey,
                    toWallet = treasuryPubkey,
                    mint = mintPubkey,
                    amount = tokenAmount,
                    decimals = decimals,
                    recentBlockhash = blockhashBytes,
                    createRecipientAta = !treasuryHasAta
                )

                Log.d(TAG, "Built SKR SPL transaction: ${transactionMessage.size} bytes (createAta=${!treasuryHasAta})")

                signAndSendTransactions(
                    transactions = arrayOf(transactionMessage)
                )
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signatures = result.payload.signatures
                    val signature = if (signatures.isNotEmpty()) {
                        encodeBase58(signatures[0])
                    } else {
                        "SKR_TX_${System.currentTimeMillis()}"
                    }
                    Log.d(TAG, "SKR payment successful! Signature: $signature")
                    onSuccess(signature)
                }
                is TransactionResult.NoWalletFound -> {
                    onError("No wallet found. Please install a Solana wallet app.")
                }
                is TransactionResult.Failure -> {
                    Log.e(TAG, "SKR payment failed", result.e)
                    onError(result.e.message ?: "SKR payment failed. Please try again.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SKR payment error", e)
            onError(e.message ?: "SKR payment failed unexpectedly.")
        }
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
