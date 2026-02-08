package com.adappvark.toolkit

/**
 * Application-wide configuration constants
 */
object AppConfig {
    
    /**
     * Payment Configuration
     * TODO: Replace with your actual Solana wallet address before launch
     */
    object Payment {
        // CRITICAL: Replace this with your actual Solana wallet address
        const val WALLET_ADDRESS = "DD4aPDhf396NhNDxa4PBVf1u3uzCUP2QYm2dFMmJWq2Q"  // Placeholder - REPLACE!
        
        // Cluster configuration
        const val CLUSTER = "devnet"  // Use "devnet" for testing
        const val RPC_ENDPOINT = "https://api.devnet.solana.com"
        
        // For testing, use devnet:
        // const val CLUSTER = "devnet"
        // const val RPC_ENDPOINT = "https://api.devnet.solana.com"
    }
    
    /**
     * App Identity for MWA
     */
    object Identity {
        const val URI = "https://adappvark.xyz"
        const val ICON_URI = "https://adappvark.xyz/favicon.ico"
        const val NAME = "ADappvark Toolkit"
    }
    
    /**
     * Subscription Configuration
     */
    object Subscription {
        const val DURATION_DAYS = 7
        const val DURATION_MS = DURATION_DAYS * 24 * 60 * 60 * 1000L
    }
    
    /**
     * Feature Flags
     */
    object Features {
        const val ENABLE_ANALYTICS = false  // Privacy-first: no analytics
        const val ENABLE_CRASH_REPORTING = false  // No crash reporting
        const val ENABLE_DEBUG_LOGGING = BuildConfig.DEBUG

        // TODO: Set to false before dApp Store submission
        const val TESTING_BYPASS_PAYMENT = true  // Bypass payment for testing
    }
    
    /**
     * URLs
     */
    object Urls {
        const val WEBSITE = "https://adappvark.xyz"
        const val PRIVACY_POLICY = "https://adappvark.xyz/privacy"
        const val TERMS_OF_SERVICE = "https://adappvark.xyz/terms"
        const val SUPPORT_EMAIL = "support@adappvark.xyz"
        
        // Solana Explorer (cluster-aware)
        fun getSolscanUrl(signature: String): String {
            val clusterParam = if (Payment.CLUSTER == "mainnet-beta") "" else "?cluster=${Payment.CLUSTER}"
            return "https://solscan.io/tx/$signature$clusterParam"
        }

        fun getExplorerUrl(signature: String): String {
            val clusterParam = if (Payment.CLUSTER == "mainnet-beta") "" else "?cluster=${Payment.CLUSTER}"
            return "https://explorer.solana.com/tx/$signature$clusterParam"
        }
    }
    
    /**
     * Shizuku Configuration
     */
    object Shizuku {
        const val PERMISSION_REQUEST_CODE = 1001
        const val PACKAGE_NAME = "moe.shizuku.privileged.api"
    }
}
