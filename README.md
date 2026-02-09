# 🐜 ADappvark Toolkit

**First in line. Every dApp, every time.**

Professional dApp lifecycle management for Solana Seeker power users.

---

## 📋 Project Overview

**Package:** `com.adappvark.toolkit`  
**Version:** 1.0.0 (Beta)  
**Target Platform:** Solana dApp Store (Seeker exclusive)  
**Minimum SDK:** Android 8.0 (API 26)  
**Target SDK:** Android 14 (API 34)

### What is ADappvark Toolkit?

ADappvark Toolkit is the first professional-grade app management solution for the Solana Seeker ecosystem. It enables power users to efficiently manage hundreds of dApps with enterprise automation features.

**Core Features:**
- ✅ **Bulk Uninstall** - Silent uninstall via Shizuku (no confirmations)
- 🚧 **Batch Reinstall** - Automated reinstallation (Phase 2)
- 💰 **Weekly Subscriptions** - Tiered pricing (0.005-0.012 SOL/week)
- 🎨 **8-bit Mascot** - Varky the ADappvark (retro charm)
- 🎯 **Material 3 UI** - Modern, polished interface

---

## 🏗️ Architecture

### Technology Stack

**Frontend:**
- Kotlin 1.9.20
- Jetpack Compose (Material 3)
- Navigation Compose
- Coroutines + Flow

**Core Services:**
- **Shizuku API** - Silent package uninstall
- **PackageManager** - App scanning and filtering
- **Solana MWA** - Payment processing (TODO)

**Key Dependencies:**
```gradle
// Shizuku for system access
implementation("dev.rikka.shizuku:api:13.1.5")

// Solana Mobile Wallet Adapter
implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.0")

// Jetpack Compose & Material 3
implementation("androidx.compose.material3:material3:1.2.0")
```

### Project Structure

```
app/src/main/kotlin/com/adappvark/toolkit/
├── MainActivity.kt                    # Entry point
├── data/
│   └── model/
│       ├── DAppInfo.kt               # dApp data model
│       └── Subscription.kt           # Subscription plans & status
├── service/
│   ├── ShizukuManager.kt             # Shizuku integration (CORE)
│   └── PackageManagerService.kt      # Package scanning
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                  # Material 3 + Solana colors
│   │   └── Type.kt                   # Typography
│   ├── navigation/
│   │   └── AppNavigation.kt          # Bottom nav setup
│   └── screens/
│       ├── HomeScreen.kt             # Dashboard & setup
│       ├── UninstallScreen.kt        # Bulk uninstall (WORKING)
│       ├── SubscriptionScreen.kt     # Plan selection
│       └── SettingsScreen.kt         # Settings & Shizuku status
```

---

## 🚀 Current Status (Day 1)

### ✅ **COMPLETED:**

1. **Project Scaffold**
   - Full Gradle build configuration
   - All dependencies integrated
   - Proper package structure

2. **Core Services**
   - ✅ ShizukuManager - Complete with bulk uninstall
   - ✅ PackageManagerService - Scanning & filtering
   - ✅ Data models (DAppInfo, Subscription plans)

3. **UI/UX**
   - ✅ Material 3 theme with Solana branding
   - ✅ Bottom navigation
   - ✅ Home screen with setup checklist
   - ✅ **Uninstall screen (FUNCTIONAL)**
   - ✅ Subscription plan display
   - ✅ Settings screen with Shizuku status

4. **Features Working:**
   - Package scanning (all apps or dApp Store only)
   - Multi-select dApp list
   - Bulk uninstall with progress tracking
   - Shizuku permission management

### 🚧 **TODO (Next Steps):**

#### **Phase 1: Polish & Testing (Days 2-3)**
- [ ] Add Solana MWA payment integration
- [ ] Implement subscription gating logic
- [ ] Create 8-bit mascot sprites (Varky)
- [ ] Add animations and transitions
- [ ] Improve error handling
- [ ] Test on actual Seeker device

#### **Phase 2: Reinstall Feature (Days 4-5)**
- [ ] Deep link automation for dApp Store
- [ ] Accessibility Service integration
- [ ] Manual fallback mode
- [ ] Install history tracking

#### **Phase 3: Launch Prep (Day 6)**
- [ ] Legal documents (Privacy Policy, ToS, EULA)
- [ ] App icon design (512x512)
- [ ] Screenshots for dApp Store
- [ ] Submit to Solana dApp Store

---

## 💻 Development Setup

### Prerequisites

1. **Android Studio** (Latest stable)
2. **Java 17**
3. **Solana Seeker device** (or emulator)
4. **Shizuku installed** on test device

### Build Instructions

1. **Clone/Import Project:**
   ```bash
   # Open the ADappvarkToolkit folder in Android Studio
   ```

2. **Sync Gradle:**
   ```
   File → Sync Project with Gradle Files
   ```

3. **Build APK:**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **Install on Device:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Testing Requirements

**Critical:** You MUST have Shizuku running on the test device:

1. Enable Developer Options (tap Build Number 7x)
2. Enable Wireless Debugging
3. Install Shizuku from GitHub: https://github.com/RikkaApps/Shizuku
4. Start Shizuku and pair via Wireless Debugging
5. Grant ADappvark permission in Shizuku

---

## 🎨 Branding Guidelines

### Colors

```kotlin
// Primary
SolanaPurple = #9945FF
SolanaGreen = #14F195

// Secondary
AardvarkBrown = #8B6F47
AardvarkTan = #D4A574
```

### Typography

- **Headings:** Bold, high contrast
- **Body:** Standard weight, readable
- **Accents:** Medium weight for buttons/labels

### Mascot (Varky)

**States to implement:**
- Idle: Standing with toolbox
- Working: Digging animation (3 frames)
- Success: Celebration with trophy
- Error: Confused head-scratch

**Style:** 8-bit pixel art, 64x64 base resolution

---

## 💰 Monetization

### Subscription Plans

| Plan | Price/Week | Features |
|------|-----------|----------|
| **Uninstall Suite** | 0.005 SOL | Bulk uninstall, storage analysis |
| **Reinstall Suite** | 0.01 SOL | Automated reinstall, install tracking |
| **Complete Toolkit** | 0.012 SOL | Everything + weekly automation (Save 20%) |

### Payment Integration (TODO)

Using Solana Mobile Wallet Adapter:
1. User selects plan
2. Connect wallet via MWA
3. Request transaction (0.01 SOL to payment address)
4. Verify confirmation on-chain
5. Activate subscription for 7 days

---

## 🔒 Security & Privacy

### Permissions Required

- `QUERY_ALL_PACKAGES` - Scan installed apps
- `REQUEST_INSTALL_PACKAGES` - For reinstall feature
- `INTERNET` - MWA communication
- Shizuku permission - Silent uninstall

### Data Collection

**We collect ZERO personal data:**
- No analytics
- No tracking
- No user accounts
- Wallet address only for payment verification
- All data stored locally on device

### Legal Compliance

- ✅ GDPR compliant (minimal data)
- ✅ CCPA compliant (no data sale)
- ✅ Transparent permissions
- ✅ Open about Shizuku requirements

---

## 📱 Installation (For Users)

### From Solana dApp Store (COMING SOON)

1. Open Solana dApp Store on Seeker
2. Search "ADappvark"
3. Install & launch
4. Follow setup wizard

### Setup Steps

1. **Install Shizuku** (one-time)
2. **Grant permission** to ADappvark
3. **Choose subscription plan**
4. **Start managing dApps!**

---

## 🐛 Known Issues

- [ ] Shizuku permission dialog sometimes requires app restart
- [ ] Large dApp lists (500+) can be slow to scan
- [ ] No storage size calculation yet (PackageManager limitation)
- [ ] MWA payment not implemented

---

## 🤝 Contributing

This is a commercial product, but we welcome:
- Bug reports
- Feature suggestions
- UI/UX feedback
- Beta testing volunteers

Contact: [Add contact info]

---

## 📄 License

**Proprietary Software**

Copyright © 2026 ADappvark Toolkit. All rights reserved.

Uses open-source components:
- Shizuku (Apache 2.0)
- Jetpack Compose (Apache 2.0)
- Material Design (Apache 2.0)

---

## 🙏 Acknowledgments

- **RikkaApps** for Shizuku
- **Solana Mobile** for Seeker & dApp Store
- **Android community** for tools & libraries

---

## 📞 Support

- Discord: [Add link]
- Twitter: @ADappvark
- Email: aardappvark@proton.me

---

**Built with ❤️ for the Solana Mobile revolution**

*ADappvark Toolkit - First in line, every time* 🐜
