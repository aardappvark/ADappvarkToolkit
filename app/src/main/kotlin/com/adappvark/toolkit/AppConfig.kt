package com.adappvark.toolkit

/**
 * Application-wide configuration constants
 */
object AppConfig {
    
    /**
     * Payment Configuration
     */
    object Payment {
        const val WALLET_ADDRESS = "DD4aPDhf396NhNDxa4PBVf1u3uzCUP2QYm2dFMmJWq2Q"

        // Production: mainnet-beta via Helius
        const val CLUSTER = "mainnet-beta"
        const val RPC_ENDPOINT = "https://mainnet.helius-rpc.com/?api-key=15319bf4-5b40-4958-ac8d-6313aa55eb92"

        // For testing, use devnet:
        // const val CLUSTER = "devnet"
        // const val RPC_ENDPOINT = "https://api.devnet.solana.com"
    }
    
    /**
     * App Identity for MWA
     */
    object Identity {
        const val URI = "https://aardappvark.github.io/ADappvarkToolkit"
        const val ICON_URI = "favicon.png"
        const val NAME = "AardAppvark Toolkit"
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

        const val TESTING_BYPASS_PAYMENT = false  // Production: payments enabled
    }
    
    /**
     * URLs
     */
    object Urls {
        const val WEBSITE = "https://aardappvark.github.io/ADappvarkToolkit"
        const val PRIVACY_POLICY = "https://aardappvark.github.io/ADappvarkToolkit/privacy.html"
        const val TERMS_OF_SERVICE = "https://aardappvark.github.io/ADappvarkToolkit/terms.html"
        const val SUPPORT_EMAIL = "aardappvark@proton.me"
        
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
