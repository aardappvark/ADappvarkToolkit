package com.adappvark.toolkit.data.model

/**
 * Subscription plans for ADappvark Toolkit
 */
enum class SubscriptionPlan(
    val id: String,
    val displayName: String,
    val priceInLamports: Long,  // SOL is stored in lamports (1 SOL = 1,000,000,000 lamports)
    val priceDisplay: String,
    val features: List<String>
) {
    UNINSTALL_ONLY(
        id = "uninstall_suite",
        displayName = "Uninstall Suite",
        priceInLamports = 5_000_000L,  // 0.005 SOL
        priceDisplay = "0.005 SOL/week",
        features = listOf(
            "Bulk dApp uninstallation",
            "Smart filtering & selection",
            "Storage analysis",
            "One-click cleanup"
        )
    ),
    
    REINSTALL_ONLY(
        id = "reinstall_suite",
        displayName = "Reinstall Suite",
        priceInLamports = 10_000_000L,  // 0.01 SOL
        priceDisplay = "0.01 SOL/week",
        features = listOf(
            "Automated dApp Store navigation",
            "Batch reinstallation",
            "Install history tracking",
            "Optional auto-clicking"
        )
    ),
    
    COMPLETE_BUNDLE(
        id = "complete_toolkit",
        displayName = "Complete Toolkit",
        priceInLamports = 12_000_000L,  // 0.012 SOL (20% savings)
        priceDisplay = "0.012 SOL/week",
        features = listOf(
            "Everything included",
            "Weekly automation schedules",
            "Advanced analytics",
            "Priority support",
            "Save 20%!"
        )
    ),

    AUTO_ACCEPT(
        id = "auto_accept",
        displayName = "Auto-Accept",
        priceInLamports = 10_000_000L,  // 0.01 SOL ≈ 1 SKR
        priceDisplay = "1 SKR / 0.01 SOL",
        features = listOf(
            "Auto-tap OK on uninstall dialogs",
            "Auto-tap Install on reinstall prompts",
            "7-day access"
        )
    );
    
    /**
     * Get price in SOL (human-readable)
     */
    fun getPriceInSol(): Double {
        return priceInLamports / 1_000_000_000.0
    }
    
    /**
     * Check if this plan includes uninstall feature
     */
    fun hasUninstall(): Boolean {
        return this == UNINSTALL_ONLY || this == COMPLETE_BUNDLE
    }
    
    /**
     * Check if this plan includes reinstall feature
     */
    fun hasReinstall(): Boolean {
        return this == REINSTALL_ONLY || this == COMPLETE_BUNDLE
    }

    /**
     * Check if this plan includes auto-accept feature
     */
    fun hasAutoAccept(): Boolean {
        return this == AUTO_ACCEPT || this == COMPLETE_BUNDLE
    }

    /**
     * Get price in SKR equivalent
     */
    fun getPriceInSkr(): Double {
        // Assuming 1 SOL ≈ 100 SKR for now
        return getPriceInSol() * 100.0
    }
}

/**
 * Subscription status
 */
data class SubscriptionStatus(
    val plan: SubscriptionPlan?,
    val isActive: Boolean,
    val expiresAt: Long?,  // Unix timestamp in milliseconds
    val lastPaymentTxId: String?
) {
    /**
     * Check if subscription is currently valid
     */
    fun isValid(): Boolean {
        if (!isActive || expiresAt == null) return false
        return System.currentTimeMillis() < expiresAt
    }
    
    /**
     * Get days until expiration
     */
    fun daysUntilExpiration(): Int {
        if (expiresAt == null) return 0
        val diff = expiresAt - System.currentTimeMillis()
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
    
    /**
     * Check if feature is available
     */
    fun hasFeature(feature: Feature): Boolean {
        if (!isValid() || plan == null) return false
        
        return when (feature) {
            Feature.BULK_UNINSTALL -> plan.hasUninstall()
            Feature.BULK_REINSTALL -> plan.hasReinstall()
            Feature.AUTO_ACCEPT -> plan.hasAutoAccept()
        }
    }
}

/**
 * Features gated by subscription
 */
enum class Feature {
    BULK_UNINSTALL,
    BULK_REINSTALL,
    AUTO_ACCEPT
}

/**
 * Payment transaction record
 */
data class PaymentTransaction(
    val txId: String,
    val plan: SubscriptionPlan,
    val amountLamports: Long,
    val timestamp: Long,
    val status: PaymentStatus
)

enum class PaymentStatus {
    PENDING,
    CONFIRMED,
    FAILED
}
