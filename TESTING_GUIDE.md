# 🧪 DAY 3 - Testing Guide

## ✅ **Testing Checklist**

### **CRITICAL: Set Your Payment Wallet**

**Before any testing, update this file:**
`app/src/main/kotlin/com/adappvark/toolkit/service/SolanaPaymentManager.kt`

Line ~18:
```kotlin
const val PAYMENT_WALLET_ADDRESS = "YOUR_ACTUAL_SOLANA_WALLET_HERE"
```

**Steps:**
1. Create/use a Solana wallet (Phantom, Solflare, etc.)
2. Copy your public key
3. Replace the placeholder
4. **IMPORTANT:** Start with devnet testing!

---

## 🔧 **Build & Install**

### **1. Build Debug APK**
```bash
cd ADappvarkToolkit

# Sync Gradle dependencies
./gradlew clean build

# Build debug APK
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

### **2. Install on Seeker Device**
```bash
# Connect via USB or wireless debugging
adb devices

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or from Android Studio: Run → Run 'app'
```

---

## 🧪 **Test Plan**

### **Phase 1: Setup & Permissions (30 min)**

#### **Test 1.1: Shizuku Installation**
- [ ] Install Shizuku on Seeker
- [ ] Enable Developer Options
- [ ] Enable Wireless Debugging  
- [ ] Start Shizuku and pair
- [ ] Verify Shizuku is running (green icon)

#### **Test 1.2: App Launch**
- [ ] Launch ADappvark Toolkit
- [ ] HomeScreen loads without crashes
- [ ] Bottom navigation works
- [ ] All 4 screens accessible

#### **Test 1.3: Shizuku Permission**
- [ ] Go to Settings screen
- [ ] Shizuku status shows "Not Running" or "Permission Required"
- [ ] Click "Grant Permission"
- [ ] Permission dialog appears
- [ ] Grant permission
- [ ] Status updates to "Active"

**Expected Issues:**
- Permission might require app restart
- Sometimes needs 2-3 attempts

---

### **Phase 2: Package Scanning (15 min)**

#### **Test 2.1: Scan All Apps**
- [ ] Go to Uninstall tab
- [ ] Select "All Apps" filter
- [ ] Wait for scan to complete
- [ ] Verify app list displays
- [ ] Check app names, icons, sizes

#### **Test 2.2: Scan dApp Store Only**
- [ ] Select "dApp Store Only" filter
- [ ] Wait for scan
- [ ] Verify only dApp Store apps show
- [ ] Count: should match installed dApps

#### **Test 2.3: Selection**
- [ ] Click individual checkboxes
- [ ] Verify selection count updates
- [ ] Click "Select All"
- [ ] Verify all checked
- [ ] Click "Clear"
- [ ] Verify all unchecked

**Expected Behavior:**
- Scan should complete in <5 seconds for 100 apps
- Icons should display correctly
- Sizes should show (MB)

---

### **Phase 3: Subscription (WITHOUT Payment) (20 min)**

#### **Test 3.1: Free Mode (No Subscription)**
- [ ] Go to Uninstall tab
- [ ] Select 5 test apps
- [ ] Click "Uninstall X dApps"
- [ ] Should show "Subscription Required" dialog
- [ ] Button shows lock icon
- [ ] Click "View Plans" (TODO: should navigate)

#### **Test 3.2: Subscription Screen**
- [ ] Go to Subscription tab
- [ ] Verify 3 plans display
- [ ] Check prices: 0.005, 0.01, 0.012 SOL
- [ ] Verify "Best Value" badge on Complete
- [ ] All features listed correctly

#### **Test 3.3: Mock Subscription Activation**

**Temporarily bypass payment for testing:**

Edit `SubscriptionManager.kt`, add this function:
```kotlin
// DEBUG ONLY - Remove before production
suspend fun activateDebugSubscription() {
    activateSubscription(
        plan = SubscriptionPlan.COMPLETE_BUNDLE,
        transactionId = "DEBUG_TX_${System.currentTimeMillis()}"
    )
}
```

Then in Subscription screen, add debug button:
```kotlin
// DEBUG Button
Button(onClick = {
    scope.launch {
        subscriptionManager.activateDebugSubscription()
        subscriptionStatus = subscriptionManager.getSubscriptionStatus()
    }
}) {
    Text("DEBUG: Activate")
}
```

- [ ] Click DEBUG button
- [ ] Subscription activates
- [ ] Green "Active" card appears
- [ ] Shows "7 days" remaining
- [ ] Shows expiration date

---

### **Phase 4: Bulk Uninstall (CRITICAL) (30 min)**

**Prerequisites:**
- ✅ Shizuku running & permission granted
- ✅ Debug subscription activated
- ✅ Test apps installed (install 5-10 small test apps from dApp Store)

#### **Test 4.1: Single Uninstall**
- [ ] Select 1 test app
- [ ] Click "Uninstall 1 dApps"
- [ ] Confirm dialog appears
- [ ] Click "Uninstall"
- [ ] Progress bar shows
- [ ] App uninstalls silently (no system dialog)
- [ ] Success!

#### **Test 4.2: Bulk Uninstall (5 apps)**
- [ ] Select 5 test apps
- [ ] Click "Uninstall 5 dApps"
- [ ] Confirm
- [ ] Watch progress: 1/5, 2/5, etc.
- [ ] Current app name shows
- [ ] All 5 uninstall successfully
- [ ] List refreshes automatically

#### **Test 4.3: Large Batch (50+ apps)**
**WARNING:** This permanently deletes apps!
- [ ] Install 50 test apps first
- [ ] Select all
- [ ] Confirm uninstall
- [ ] Progress tracking works
- [ ] No crashes or hangs
- [ ] All complete successfully

**Expected Performance:**
- ~100ms per app
- 50 apps = ~5 seconds
- 400 apps = ~40 seconds

**Common Issues:**
- If fails: Check Shizuku logs
- If permission denied: Re-grant in Shizuku app
- If too slow: Device might be throttling

---

### **Phase 5: Payment Flow (DEVNET ONLY) (45 min)**

**⚠️ CRITICAL: Use DEVNET for testing!**

#### **Setup Devnet Testing**

1. **Update SolanaPaymentManager.kt:**
```kotlin
// Line ~50, in authorize() call:
cluster = "devnet"  // Change from "mainnet-beta"
```

2. **Get Devnet SOL:**
- Create devnet wallet in Phantom/Solflare
- Visit https://solfaucet.com/
- Request devnet SOL (free)
- Confirm you have 1+ SOL on devnet

3. **Update payment address:**
```kotlin
const val PAYMENT_WALLET_ADDRESS = "YOUR_DEVNET_WALLET_ADDRESS"
```

#### **Test 5.1: Payment Dialog**
- [ ] Remove debug subscription
- [ ] Go to Subscription tab
- [ ] Select a plan
- [ ] Click "Subscribe Now"
- [ ] MWA launches wallet selector
- [ ] Select your test wallet

**If MWA doesn't launch:**
- Check wallet is installed
- Try restarting app
- Check Android logs: `adb logcat | grep MWA`

#### **Test 5.2: Transaction Approval**
- [ ] Wallet shows transaction details
- [ ] Correct amount (0.005/0.01/0.012 SOL)
- [ ] Correct recipient address
- [ ] Click "Approve"
- [ ] Transaction submits

**Expected Flow:**
1. Loading spinner shows
2. Wallet app opens
3. User approves
4. Returns to ADappvark
5. Success dialog shows
6. Subscription activates

#### **Test 5.3: On-Chain Verification**
- [ ] Copy transaction signature
- [ ] Visit https://explorer.solana.com/?cluster=devnet
- [ ] Paste signature
- [ ] Verify:
   - [ ] Status: Confirmed
   - [ ] Amount: Correct SOL
   - [ ] Recipient: Your wallet
   - [ ] Fee: ~0.000005 SOL

#### **Test 5.4: Subscription Persistence**
- [ ] Kill app completely
- [ ] Relaunch
- [ ] Subscription still active
- [ ] Days remaining correct
- [ ] Transaction ID preserved

---

## 🐛 **Known Issues & Solutions**

### **Issue 1: "Recent blockhash not implemented"**
**Symptom:** Payment fails with blockhash error

**Solution:**
The `getRecentBlockhash()` function needs RPC implementation.

**Quick Fix (for testing):**
```kotlin
private suspend fun getRecentBlockhash(): ByteArray {
    // Hardcoded recent blockhash for testing
    // In production, fetch from RPC
    return Base58.decode("11111111111111111111111111111111")
}
```

**Proper Fix:**
Implement RPC call to Solana cluster:
```kotlin
val rpcUrl = "https://api.devnet.solana.com"
val response = httpClient.post(rpcUrl) {
    setBody("""{"jsonrpc":"2.0","id":1,"method":"getLatestBlockhash"}""")
}
```

### **Issue 2: "Transaction construction failed"**
**Symptom:** Payment crashes on transaction building

**Debug:**
```bash
adb logcat | grep SolanaTransaction
```

**Common Causes:**
- Base58 encoding issue
- Wrong pubkey size
- Invalid instruction data

### **Issue 3: "Shizuku permission denied"**
**Symptom:** Uninstall fails even with permission

**Solutions:**
1. Re-grant permission in Shizuku app
2. Restart Shizuku service
3. Restart ADappvark app
4. Check: `adb shell pm list packages | grep shizuku`

### **Issue 4: "Subscription doesn't persist"**
**Symptom:** Subscription resets after app restart

**Check:**
```bash
adb shell run-as com.adappvark.toolkit ls -la files/datastore/
```

Should see `subscription_prefs.preferences_pb`

**Fix:** Ensure DataStore is properly initialized

---

## 📊 **Test Results Template**

### **Device Info**
- Device: Solana Seeker (Saga)
- Android: 14
- Build: [Your build number]

### **Test Results**

| Test | Status | Notes |
|------|--------|-------|
| Shizuku Setup | ✅ / ❌ |  |
| Package Scanning | ✅ / ❌ |  |
| Subscription Gating | ✅ / ❌ |  |
| Bulk Uninstall (5 apps) | ✅ / ❌ |  |
| Bulk Uninstall (50 apps) | ✅ / ❌ |  |
| Payment Flow (devnet) | ✅ / ❌ |  |
| Transaction Verification | ✅ / ❌ |  |
| Subscription Persistence | ✅ / ❌ |  |

### **Performance Metrics**
- App size: _____ MB
- Scan time (100 apps): _____ seconds
- Uninstall time (50 apps): _____ seconds
- Memory usage: _____ MB

### **Critical Bugs Found**
1. 
2. 
3. 

---

## ✅ **Production Readiness Checklist**

Before launching on mainnet:

- [ ] All tests pass on devnet
- [ ] No critical bugs
- [ ] Payment flow tested 10+ times
- [ ] Bulk uninstall tested with 100+ apps
- [ ] App doesn't crash
- [ ] Proper error messages
- [ ] Debug code removed
- [ ] Payment wallet address set (mainnet)
- [ ] Cluster changed to "mainnet-beta"
- [ ] ProGuard rules tested (release build)

---

## 🚀 **Next Steps After Testing**

### **If All Tests Pass:**
1. Create signed release APK
2. Prepare dApp Store assets
3. Submit for review

### **If Issues Found:**
1. Document all bugs
2. Prioritize by severity
3. Fix critical issues
4. Re-test
5. Repeat until stable

---

## 📞 **Debug Commands**

```bash
# View app logs
adb logcat | grep ADappvark

# View Shizuku logs
adb logcat | grep Shizuku

# View MWA logs
adb logcat | grep MobileWallet

# Check app storage
adb shell run-as com.adappvark.toolkit ls -R

# Clear app data (reset)
adb shell pm clear com.adappvark.toolkit

# Uninstall completely
adb uninstall com.adappvark.toolkit
```

---

**Time Estimate:** 2-3 hours for complete testing
**Critical Path:** Shizuku → Subscription → Uninstall → Payment
**Success Criteria:** All core features work without crashes
