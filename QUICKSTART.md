# 🚀 QUICKSTART - ADappvark Toolkit

## ⚡ Get Coding in 5 Minutes

### Step 1: Open in Android Studio
```bash
cd ADappvarkToolkit
# Open this folder in Android Studio
```

### Step 2: Sync & Build
1. Wait for Gradle sync (auto-starts)
2. Click "Build → Make Project" (or Ctrl+F9)
3. Should build successfully!

### Step 3: Run on Device/Emulator
```bash
# Connect Seeker device via USB or wireless debugging
adb devices

# Run from Android Studio: Shift+F10
# OR build manually:
./gradlew assembleDebug
```

---

## 🎯 What Works RIGHT NOW

### ✅ Core Functionality
- **Package scanning** - Lists all installed apps
- **dApp filtering** - Shows only dApp Store apps
- **Multi-select UI** - Check boxes to select dApps
- **Shizuku integration** - Requests permission correctly
- **Bulk uninstall** - Actually works! (if Shizuku running)

### 🎨 UI Screens
- Home (with setup checklist)
- Uninstall (fully functional)
- Subscription (plan display only)
- Settings (Shizuku status)

---

## 🔧 Next Priorities (In Order)

### 1. **Test on Real Device** (30 min)
```bash
# Make sure Shizuku is installed & running on Seeker
# Grant ADappvark permission in Shizuku
# Try uninstalling a test app
```

### 2. **Add MWA Payment** (2-3 hours)
Create `SolanaPaymentManager.kt`:
```kotlin
class SolanaPaymentManager(context: Context) {
    suspend fun requestPayment(
        amountLamports: Long,
        recipientAddress: String
    ): Result<String>  // Returns transaction ID
}
```

Integrate in `SubscriptionScreen.kt`

### 3. **Create 8-Bit Mascot** (1-2 hours)
Need PNG sprites (64x64 each):
- `varky_idle.png`
- `varky_working_frame1.png`
- `varky_working_frame2.png`
- `varky_working_frame3.png`
- `varky_success.png`
- `varky_error.png`

Place in: `app/src/main/res/drawable/`

### 4. **Subscription Gating** (1 hour)
Create `SubscriptionManager.kt`:
```kotlin
class SubscriptionManager(context: Context) {
    suspend fun hasActiveSubscription(): Boolean
    suspend fun getSubscriptionStatus(): SubscriptionStatus
    fun hasFeature(feature: Feature): Boolean
}
```

Guard uninstall feature behind subscription check.

### 5. **App Icon** (1 hour)
Design 512x512 icon with:
- Purple/green gradient background
- 8-bit aardvark
- "dApp" badge
- Wrench icon

Export to `mipmap-xxxhdpi/ic_launcher.png` (and all densities)

---

## 📝 Code Hotspots to Edit

### Adding Features
**File:** `UninstallScreen.kt` (line ~140)
- Add filters, sorting, search

### Payment Integration
**File:** `SubscriptionScreen.kt` (line ~85)
```kotlin
// Replace TODO with:
scope.launch {
    val result = paymentManager.requestPayment(
        plan.priceInLamports,
        PAYMENT_WALLET_ADDRESS
    )
    // Handle result
}
```

### Mascot Animation
**File:** `HomeScreen.kt` (line ~35)
```kotlin
// Replace placeholder Icon with:
AnimatedMascot(state = MascotState.IDLE)
```

---

## 🐛 Common Issues & Fixes

### "Shizuku permission denied"
**Fix:** Open Shizuku app → Grant permission to ADappvark manually

### "Package not found"
**Fix:** Ensure you're scanning with correct filter (dApp Store vs All)

### "Build failed: Cannot resolve symbol"
**Fix:** File → Invalidate Caches → Restart

### "MWA not found"
**Fix:** MWA only works on actual Seeker device, not emulator

---

## 🎨 Branding Checklist

Before dApp Store submission:

- [ ] App icon (all densities)
- [ ] 8-bit mascot sprites
- [ ] Feature graphic (1200x600)
- [ ] Screenshots (4-6 images at 1920x1080)
- [ ] Privacy Policy URL
- [ ] Terms of Service URL
- [ ] EULA

---

## 🚢 Shipping Checklist

### Beta Release
- [ ] Test uninstall on 10+ dApps
- [ ] Test payment flow with test SOL
- [ ] Fix any crashes/bugs
- [ ] Add error messages
- [ ] Polish animations

### dApp Store Submission
- [ ] Generate signed APK
- [ ] Fill config.yaml
- [ ] Run dApp-store CLI
- [ ] Submit for review (2-3 days)

---

## 💡 Pro Tips

1. **Use Logcat** - Essential for debugging Shizuku operations
   ```bash
   adb logcat | grep ADappvark
   ```

2. **Test with Real dApps** - Install 5-10 dApp Store apps first

3. **Mock Subscription** - Add debug mode for testing without payment

4. **Shizuku Logs** - Check Shizuku app logs if uninstall fails

5. **Material Theme** - Use Material Theme Builder for color tweaks
   https://material-foundation.github.io/material-theme-builder/

---

## 📚 Useful Resources

- **Shizuku Docs:** https://github.com/RikkaApps/Shizuku
- **Solana MWA Docs:** https://docs.solanamobile.com/android-native/overview
- **Material 3 Guidelines:** https://m3.material.io/
- **Compose Samples:** https://github.com/android/compose-samples

---

## 🎯 Success Metrics

### Beta Goals
- Successfully uninstall 100+ dApps in one session
- Zero crashes during operation
- <5 second scan time for 400 dApps
- Shizuku permission granted in 1 attempt

### Launch Goals
- 100 active users in first week
- 50% conversion to paid subscription
- 4+ star rating in dApp Store
- Featured in "New & Notable"

---

**Ready to build? Let's fucking go! 🚀**

*Questions? Check README.md for full documentation*
