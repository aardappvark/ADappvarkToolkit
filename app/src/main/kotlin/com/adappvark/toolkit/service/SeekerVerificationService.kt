package com.adappvark.toolkit.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.adappvark.toolkit.AppConfig
import com.midmightbit.sgt.SgtChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SeekerVerificationService - Verifies Seeker Genesis Token (SGT) on-chain
 *
 * Uses the seeker-verify library to check if a connected wallet holds
 * a valid SGT — a soulbound Token-2022 NFT that proves ownership of
 * a physical Solana Seeker device.
 *
 * VERIFICATION FLOW:
 * 1. User connects wallet via MWA (triggers side-button confirmation on Seeker)
 * 2. After wallet connect, we check on-chain for SGT in that wallet
 * 3. If SGT found → verified Seeker device → grant 2 free credits
 * 4. Results are cached for 24 hours to avoid excessive RPC calls
 *
 * BONUS FOR VERIFIED SEEKERS:
 * - 2 free credits (1 bulk uninstall + 1 bulk reinstall)
 * - Instead of the generic 1 credit for any wallet connect
 */
class SeekerVerificationService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "SeekerVerify"
        private const val PREFS_NAME = "aardappvark_seeker_verify"

        // SGT verification state
        private const val KEY_HAS_SGT = "has_sgt"
        private const val KEY_SGT_MEMBER_NUMBER = "sgt_member_number"
        private const val KEY_SGT_MINT_ADDRESS = "sgt_mint_address"
        private const val KEY_SGT_CHECKED_AT = "sgt_checked_at"
        private const val KEY_SGT_WALLET_ADDRESS = "sgt_wallet_address"
        private const val KEY_SEEKER_BONUS_GRANTED = "seeker_bonus_granted"

        // Cache duration: 24 hours
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L

        // Bonus credits for verified Seeker owners
        const val SEEKER_BONUS_CREDITS = 2  // 1 bulk uninstall + 1 bulk reinstall
    }

    /**
     * Check if wallet holds a Seeker Genesis Token (SGT)
     *
     * Uses 24h cache to avoid excessive RPC calls.
     * On Seeker devices, the MWA sign-in triggers the physical
     * side-button confirmation before this check runs.
     *
     * @param walletAddress The connected wallet public key (Base58)
     * @return SgtVerificationResult with verification details
     */
    suspend fun verifySeekerDevice(walletAddress: String): SgtVerificationResult {
        // Check if we have a cached result for this wallet within 24h
        if (!shouldRecheck(walletAddress)) {
            val cached = getCachedResult()
            Log.d(TAG, "SGT check skipped (cached within 24h) — hasSgt=${cached.hasSgt}")
            return cached
        }

        Log.d(TAG, "SGT check starting for wallet: ${walletAddress.take(8)}...")

        return withContext(Dispatchers.IO) {
            try {
                val rpcUrl = AppConfig.Payment.RPC_ENDPOINT
                Log.d(TAG, "SGT check calling RPC: ${rpcUrl.take(50)}...")

                val result = SgtChecker.getWalletSgtInfo(walletAddress, rpcUrl)

                result.fold(
                    onSuccess = { sgtInfo ->
                        // Must use local variable for smart cast (cross-module Long? bug)
                        val memberNum = sgtInfo.memberNumber
                        val mintAddr = sgtInfo.sgtMintAddress

                        Log.d(TAG, "SGT check SUCCESS — hasSgt=${sgtInfo.hasSgt}, member=#$memberNum")

                        // Cache the result
                        saveSgtResult(
                            walletAddress = walletAddress,
                            hasSgt = sgtInfo.hasSgt,
                            memberNumber = memberNum,
                            mintAddress = mintAddr
                        )

                        SgtVerificationResult(
                            hasSgt = sgtInfo.hasSgt,
                            memberNumber = memberNum,
                            sgtMintAddress = mintAddr,
                            isFromCache = false
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "SGT check FAILED — ${error.message}", error)

                        // On error, return cached value (if any)
                        getCachedResult().copy(isFromCache = true, error = error.message)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "SGT verification error: ${e.message}", e)
                getCachedResult().copy(isFromCache = true, error = e.message)
            }
        }
    }

    /**
     * Check if we should re-verify SGT status
     * Re-checks every 24 hours, or if wallet changed, or if never checked.
     */
    private fun shouldRecheck(walletAddress: String): Boolean {
        val checkedAt = prefs.getLong(KEY_SGT_CHECKED_AT, 0L)
        val cachedWallet = prefs.getString(KEY_SGT_WALLET_ADDRESS, null)

        // Different wallet → must recheck
        if (cachedWallet != walletAddress) return true

        // Never checked → must check
        if (checkedAt == 0L) return true

        // Cached within 24h → skip
        return System.currentTimeMillis() - checkedAt > CACHE_DURATION_MS
    }

    /**
     * Save SGT verification result to cache
     */
    private fun saveSgtResult(
        walletAddress: String,
        hasSgt: Boolean,
        memberNumber: Long?,
        mintAddress: String?
    ) {
        prefs.edit().apply {
            putBoolean(KEY_HAS_SGT, hasSgt)
            putString(KEY_SGT_WALLET_ADDRESS, walletAddress)
            putLong(KEY_SGT_CHECKED_AT, System.currentTimeMillis())
            if (memberNumber != null) {
                putLong(KEY_SGT_MEMBER_NUMBER, memberNumber)
            } else {
                remove(KEY_SGT_MEMBER_NUMBER)
            }
            if (mintAddress != null) {
                putString(KEY_SGT_MINT_ADDRESS, mintAddress)
            } else {
                remove(KEY_SGT_MINT_ADDRESS)
            }
            apply()
        }
    }

    /**
     * Get cached SGT verification result
     */
    private fun getCachedResult(): SgtVerificationResult {
        val hasSgt = prefs.getBoolean(KEY_HAS_SGT, false)
        val memberNumber = prefs.getLong(KEY_SGT_MEMBER_NUMBER, -1L).takeIf { it >= 0 }
        val mintAddress = prefs.getString(KEY_SGT_MINT_ADDRESS, null)

        return SgtVerificationResult(
            hasSgt = hasSgt,
            memberNumber = memberNumber,
            sgtMintAddress = mintAddress,
            isFromCache = true
        )
    }

    // ==================== Public State Accessors ====================

    /**
     * Check if current wallet is a verified Seeker device (from cache)
     */
    fun isVerifiedSeeker(): Boolean {
        return prefs.getBoolean(KEY_HAS_SGT, false)
    }

    /**
     * Get SGT member number (from cache)
     */
    fun getSgtMemberNumber(): Long? {
        val num = prefs.getLong(KEY_SGT_MEMBER_NUMBER, -1L)
        return if (num >= 0) num else null
    }

    /**
     * Check if Seeker bonus credits have already been granted
     */
    fun hasSeekerBonusBeenGranted(): Boolean {
        return prefs.getBoolean(KEY_SEEKER_BONUS_GRANTED, false)
    }

    /**
     * Mark Seeker bonus credits as granted
     */
    fun markSeekerBonusGranted() {
        prefs.edit()
            .putBoolean(KEY_SEEKER_BONUS_GRANTED, true)
            .apply()
    }

    /**
     * Reset all verification data (GDPR Right to Erasure)
     */
    fun resetAll() {
        prefs.edit().clear().apply()
    }
}

/**
 * Result of SGT verification check
 */
data class SgtVerificationResult(
    val hasSgt: Boolean,
    val memberNumber: Long? = null,
    val sgtMintAddress: String? = null,
    val isFromCache: Boolean = false,
    val error: String? = null
) {
    /**
     * Get formatted display string for SGT status
     */
    fun getDisplayString(): String {
        if (!hasSgt) return "Not verified"
        return memberNumber?.let { "Seeker #$it" } ?: "Verified Seeker"
    }
}
