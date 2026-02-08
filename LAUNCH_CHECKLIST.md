# 🚀 LAUNCH CHECKLIST - ADappvark Toolkit

## 📋 **PRE-LAUNCH CHECKLIST**

### **🔴 CRITICAL (Must Complete)**

- [ ] **Set Payment Wallet Address**
  - File: `AppConfig.kt` (centralized - all services now use this!)
  - Line 14: Set `WALLET_ADDRESS` to your Solana wallet
  - Current: `DD4aPDhf396NhNDxa4PBVf1u3uzCUP2QYm2dFMmJWq2Q`
  - Verify address is correct (double-check!)

- [ ] **Implement RPC Client**
  - Add OkHttp dependency
  - Implement `getRecentBlockhash()`
  - Test on devnet
  - Estimated time: 2 hours

- [ ] **Test on Real Seeker Device**
  - Install Shizuku
  - Grant permissions
  - Test bulk uninstall (10+ apps)
  - Test payment on devnet
  - Estimated time: 2-3 hours

- [ ] **Verify Payment Per Operation**
  - Confirm payment is required for EACH bulk operation (6+ apps)
  - Test: Pay for uninstall → complete → select 6+ apps again → should require NEW payment
  - Test: Pay for reinstall → complete → select 6+ apps again → should require NEW payment
  - No "paid" state should persist between operations
  - Each bulk uninstall/reinstall batch must trigger fresh payment flow

- [x] **Remove Debug Code** ✅ COMPLETED
  - Removed all println debug statements from ReinstallScreen.kt
  - Replaced with Log.d() calls (stripped in release builds)
  - Debug functions exist but not exposed in UI
  - Clean: No test data hardcoded

- [ ] **Switch to Mainnet**
  - Change `CLUSTER = "devnet"` → `"mainnet-beta"` in `AppConfig.kt`
  - All services now use centralized AppConfig (PaymentService, PaymentConfig, TermsAndWalletScreen)
  - Only after thorough devnet testing!

---

### **🟡 IMPORTANT (Should Complete)**

- [ ] **Create Release Signing Key**
  ```bash
  keytool -genkey -v -keystore adappvark-release.jks \
      -keyalg RSA -keysize 2048 -validity 10000 \
      -alias adappvark
  ```
  - Store keystore safely (backup!)
  - Remember password

- [ ] **Build Signed Release APK**
  - Configure `keystore.properties`
  - Run: `./gradlew assembleRelease`
  - Test release APK on device
  - Verify: No debug code runs

- [ ] **Create Legal Documents**
  - Privacy Policy (required)
  - Terms of Service (required)
  - EULA (recommended)
  - Host on website or GitHub Pages

- [ ] **App Store Assets**
  - Screenshots (4-6 images at 1920x1080)
  - Feature graphic (1200x600)
  - App icon (all densities)
  - Short description (80 chars)
  - Full description (4000 chars)

---

### **🟢 OPTIONAL (Nice to Have)**

- [ ] **Mascot Animations**
  - Animated state transitions
  - Loading spinner with Varky
  - Success celebration

- [ ] **Polish UI**
  - Smooth transitions
  - Haptic feedback
  - Sound effects (optional)
  - Refined spacing/padding

- [ ] **Analytics**
  - Track uninstall counts
  - Monitor subscription conversions
  - Crash reporting (Firebase?)

---

## 🛠️ **IMPLEMENTATION GUIDE**

### **Step 1: RPC Client (2 hours)**

#### **Add Dependencies**
`app/build.gradle.kts`:
```kotlin
dependencies {
    // Existing dependencies...
    
    // HTTP client for Solana RPC
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}
```

Apply plugin at top:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "1.9.20"
}
```

#### **Create RPC Client**
New file: `app/src/main/kotlin/com/adappvark/toolkit/service/SolanaRpcClient.kt`

```kotlin
package com.adappvark.toolkit.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SolanaRpcClient(private val cluster: String = "devnet") {
    
    private val rpcUrl = when (cluster) {
        "mainnet-beta" -> "https://api.mainnet-beta.solana.com"
        "devnet" -> "https://api.devnet.solana.com"
        "testnet" -> "https://api.testnet.solana.com"
        else -> throw IllegalArgumentException("Unknown cluster: $cluster")
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Get recent blockhash from Solana cluster
     */
    suspend fun getRecentBlockhash(): String = withContext(Dispatchers.IO) {
        val requestBody = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "getLatestBlockhash"
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url(rpcUrl)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("RPC request failed: ${response.code}")
        }
        
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response")
        
        val jsonElement = json.parseToJsonElement(responseBody)
        val blockhash = jsonElement.jsonObject["result"]
            ?.jsonObject?.get("value")
            ?.jsonObject?.get("blockhash")
            ?.jsonPrimitive?.content
            ?: throw Exception("No blockhash in response")
        
        blockhash
    }
    
    /**
     * Get transaction status
     */
    suspend fun getTransactionStatus(signature: String): TransactionStatus = 
        withContext(Dispatchers.IO) {
        val requestBody = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "getSignatureStatuses",
                "params": [["$signature"]]
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url(rpcUrl)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response")
        
        val jsonElement = json.parseToJsonElement(responseBody)
        val status = jsonElement.jsonObject["result"]
            ?.jsonObject?.get("value")
            ?.jsonArray?.get(0)
        
        when {
            status == null || status is JsonNull -> TransactionStatus.NOT_FOUND
            status.jsonObject["confirmationStatus"]?.jsonPrimitive?.content == "finalized" ->
                TransactionStatus.CONFIRMED
            else -> TransactionStatus.PENDING
        }
    }
}

enum class TransactionStatus {
    NOT_FOUND,
    PENDING,
    CONFIRMED
}
```

#### **Update SolanaPaymentManager**

Replace `getRecentBlockhash()`:
```kotlin
private val rpcClient = SolanaRpcClient(cluster = "devnet")  // or "mainnet-beta"

private suspend fun getRecentBlockhash(): ByteArray = withContext(Dispatchers.IO) {
    val blockhash = rpcClient.getRecentBlockhash()
    Base58.decode(blockhash)
}
```

Replace `verifyTransaction()`:
```kotlin
suspend fun verifyTransaction(signature: String): Result<Boolean> = 
    withContext(Dispatchers.IO) {
    try {
        val status = rpcClient.getTransactionStatus(signature)
        Result.success(status == TransactionStatus.CONFIRMED)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

### **Step 2: Testing on Devnet (2-3 hours)**

#### **Setup**
1. Change to devnet:
   ```kotlin
   // SolanaPaymentManager.kt
   private val rpcClient = SolanaRpcClient(cluster = "devnet")
   ```

2. Get devnet SOL:
   - Visit https://solfaucet.com/
   - Request 1-2 SOL (free)
   - Confirm in wallet

3. Update payment address to your devnet wallet

#### **Test Plan**
1. Install app on Seeker
2. Complete Shizuku setup
3. Activate debug subscription OR test payment:
   - Select plan
   - Click Subscribe
   - Approve in wallet
   - Verify transaction on Solscan (devnet)
4. Test bulk uninstall with subscription active
5. Kill and restart app - verify subscription persists

#### **Verification**
- Transaction appears on https://explorer.solana.com/?cluster=devnet
- Subscription activates correctly
- Bulk uninstall works
- No crashes

---

### **Step 3: Release Build (1 hour)**

#### **Create Keystore**
```bash
keytool -genkey -v -keystore ~/adappvark-release.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias adappvark

# Answer prompts, set password
```

#### **Configure Signing**
Create `keystore.properties` in project root:
```properties
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=adappvark
storeFile=../adappvark-release.jks
```

Add to `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] ?: "")
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(...)
        }
    }
}
```

#### **Build**
```bash
./gradlew assembleRelease

# Output:
# app/build/outputs/apk/release/app-release.apk
```

---

### **Step 4: dApp Store Submission (2-3 hours)**

#### **Required Assets**

**1. Screenshots** (4-6 images)
- Home screen with Varky
- Uninstall screen with dApps selected
- Subscription plans
- Success/completion state
- Size: 1920x1080 or device resolution
- Format: PNG or JPG

**2. Feature Graphic**
- Size: 1200x600
- Includes: App name, tagline, Varky mascot
- High quality, eye-catching

**3. App Icon**
- Already created: `ic_launcher.xml`
- Export PNG versions if needed

**4. Descriptions**

**Short (80 chars):**
```
Professional dApp management for Solana Seeker power users
```

**Full (4000 chars):**
```
ADappvark Toolkit - First in line. Every dApp, every time.

The first professional-grade dApp lifecycle manager built specifically for the Solana Seeker ecosystem. ADappvark Toolkit enables power users to efficiently manage hundreds of dApps with enterprise-grade automation.

🐜 FEATURES:

✅ BULK UNINSTALL
Silent uninstall via Shizuku integration - no confirmation dialogs, no clicking through 400 prompts. Select your dApps and clean up storage in seconds.

📦 SMART FILTERING
- dApp Store only filter
- Sort by size, install date, or name
- Multi-select with one tap

💰 FLEXIBLE SUBSCRIPTIONS
Weekly subscriptions starting at just 0.005 SOL:
- Uninstall Suite: Storage management
- Reinstall Suite: Batch automation (coming soon)
- Complete Toolkit: Save 20%!

🎨 8-BIT CHARM
Meet Varky, your friendly aardvark assistant! Our unique 8-bit mascot brings personality to productivity.

🔒 PRIVACY FIRST
- Zero analytics
- No tracking
- Local data only
- Transparent permissions

⚡ POWERED BY:
- Shizuku for system-level access
- Solana Mobile Wallet Adapter for payments
- Material 3 design

Perfect for:
- Airdrop farmers managing 100+ dApps
- Developers testing multiple apps
- Storage optimization
- Privacy-focused cleanup

Note: Requires Shizuku for bulk uninstall feature.
```

**5. Category**
- Tools / Productivity

**6. Legal**
- Privacy Policy URL
- Terms of Service URL
- Support email

#### **config.yaml** (if using CLI)
```yaml
name: ADappvark Toolkit
package: com.adappvark.toolkit
version: 1.0.0
versionCode: 1
category: tools
description: Professional dApp lifecycle management for Seeker
website: https://adappvark.xyz
support: support@adappvark.xyz
privacyPolicy: https://adappvark.xyz/privacy
termsOfService: https://adappvark.xyz/terms
```

---

---

## 📝 **DEVELOPMENT NOTES**

### **Wallet Connection & Signing (Updated Feb 2026)**

**Current Status:** Authorize-only (no message signing)

**Why:** During testing, `signMessages()` triggers a wallet warning:
> "We couldn't verify this transaction due to a server error. Only sign transactions on sites you trust."

This happens because the app identity (`https://aardappvark.app`) isn't verified by Seeker wallet.

**Solution:** Once published to the **Solana dApp Store**, apps are automatically trusted by Seeker. After publication:

1. Re-enable `signMessages()` in `TermsAndWalletScreen.kt`:
   - Change `recordConsentWithoutSignature()` to `recordConsentWithSignature()`
   - Add back the `signMessages()` call after `authorize()`
   - This will trigger the double-tap confirmation on Seeker

2. The code for full SIWS is already in `TermsAcceptanceService.kt`:
   - `recordConsentWithSignature()` - stores actual cryptographic signature
   - `generateConsentMessage()` - creates the legal consent message

**Files to modify post-dApp Store publishing:**
- `TermsAndWalletScreen.kt` - uncomment signMessages flow
- See the commented code around line 525

### **Payment Model (Updated Feb 2026)**

**Current Pricing:**
- First **5 apps**: FREE
- **6+ apps**: Flat fee of **0.01 SOL** (or **1 SKR**) per bulk operation
- Each uninstall or reinstall batch (6+ apps) requires a new payment

**Treasury Wallet:** Now centralized in `AppConfig.kt` → `Payment.WALLET_ADDRESS`
- All services (PaymentService, PaymentConfig, SolanaPaymentManager) use AppConfig
- Only need to update ONE file before launch!

**SKR Token:** Update `SKR_TOKEN_MINT` in `PaymentConfig.kt` when SKR is available

### **Configuration Consolidation (Feb 2026)**

All critical configuration is now centralized in `AppConfig.kt`:
- `Payment.WALLET_ADDRESS` - Treasury wallet (used by PaymentService, PaymentConfig)
- `Payment.CLUSTER` - "devnet" or "mainnet-beta" (affects all RPC and chain connections)
- `Payment.RPC_ENDPOINT` - RPC URL
- `Identity.URI/ICON_URI/NAME` - MWA app identity (used by all wallet adapter instances)
- `Urls.PRIVACY_POLICY/TERMS_OF_SERVICE` - Legal document URLs

**To switch to mainnet:**
1. Open `AppConfig.kt`
2. Change `CLUSTER = "devnet"` to `CLUSTER = "mainnet-beta"`
3. Change `RPC_ENDPOINT` to mainnet RPC
4. All services automatically use the new configuration!

### **Open Source Fork for Grants (Future)**

**Purpose:** Create a separate open-source version to qualify for Superteam regional grants

**Plan:**
- Fork the dApp to a new repository
- Remove all payment walls (make fully free)
- Open source under Apache 2.0 or MIT license
- Apply for Superteam Earn grants ($200-$10,000)
- Separate project from commercial version

**Why:** Superteam grants prioritize open-source public goods. The commercial version stays separate.

**Timeline:** After dApp Store launch; separate project

---

## ✅ **FINAL CHECKS**

### **Before Submitting**

- [ ] All debug code removed
- [ ] Payment wallet address set (mainnet)
- [ ] RPC cluster = "mainnet-beta"
- [ ] Release APK signed and tested
- [ ] Legal docs hosted and accessible
- [ ] Support email setup and monitored
- [ ] Screenshots polished
- [ ] Descriptions proofread

### **After Submission**

- [ ] Monitor review status
- [ ] Prepare for user questions
- [ ] Set up crash reporting
- [ ] Plan first update

---

## 🎯 **SUCCESS METRICS**

### **Week 1**
- 100 downloads
- 50 active users
- 20 paying subscribers
- 4+ star rating

### **Month 1**
- 1,000 downloads
- 500 active users
- 100 paying subscribers
- Featured in dApp Store

### **Quarter 1**
- 5,000 downloads
- 2,000 active users
- 500 paying subscribers
- $6K+ MRR

---

## 🚀 **LAUNCH!**

When ready:
1. Double-check all items above
2. Build final release APK
3. Submit to Solana dApp Store
4. Share on Twitter/Discord
5. Monitor for feedback
6. Iterate and improve!

**Expected Review Time:** 2-5 days

**LET'S SHIP IT! 🔥**
