# Solana dApp Store Submission Guide

## App Listing Information

### **App Name**
AardAppvark Toolkit

### **Subtitle** (50 chars max)
dApp Lifecycle Manager for Solana Seeker

### **Short Description** (80 chars max)
Bulk uninstall & reinstall dApps, auto-accept dialogs, SGT verification. Seeker-only.

### **Full Description** (4000 chars max)

```
First in line. Every dApp, every time.

AardAppvark Toolkit is the only dApp lifecycle management tool built for Solana Seeker. Bulk uninstall, bulk reinstall, and auto-accept system dialogs — all with a beautiful liquid glass UI.

WHY AARDAPPVARK?

Managing hundreds of dApps on Seeker means endless tapping through confirmation dialogs. AardAppvark automates the tedious parts so you can focus on what matters.

CORE FEATURES

BULK UNINSTALL
Select dApps and remove them in one operation. Filter by dApp Store apps only, sort by name, size, or install date. Search to find exactly what you need.

BULK REINSTALL
Restore your dApp collection from the Solana dApp Store. Auto-install triggers dApp Store deep links for each selected app.

AUTO-ACCEPT (7-day pass)
Auto-tap system uninstall and reinstall confirmation dialogs. No more tapping "OK" 200 times. Requires a one-time 1 SKR / 0.01 SOL payment for 7 days of access.

SMART FAVOURITES
Star your favourite dApps so they're never accidentally removed during bulk operations.

SGT VERIFICATION
Seeker Genesis Token holders get the full dApp unlocked. Real on-chain SGT verification via the seeker-verify library.

WALLET LOGIN (SIWS)
Non-custodial Sign In With Solana triggers the Seeker's physical side-button fingerprint confirmation for maximum security.

ANALYTICS DASHBOARD
Track installed dApps, storage usage, uninstall/reinstall stats, and recovered storage.

PAYMENT MODEL

Free: Up to 4 apps per bulk operation
Bulk (5+ apps): 1 SKR / 0.01 SOL flat fee per operation
Auto-Accept: 1 SKR / 0.01 SOL for a 7-day pass
SGT Holders: Full dApp unlocked

All payments are real on-chain SOL transfers via Solana Mobile Wallet Adapter. No subscriptions — one-time pass or per-operation.

LIQUID GLASS UI

Beautiful glassmorphism design with animated gradients, AMOLED-dark theme, breathing glow effects, and Solana purple/green colour palette.

PRIVACY FIRST

Zero analytics or tracking. No cloud database. All data stored locally on your device. Non-custodial wallet integration. GDPR and CCPA compliant. Full data export and deletion available.

PERFECT FOR

Airdrop hunters managing 100+ dApps
Developers testing multiple applications
Storage-conscious power users
Privacy advocates cleaning up old dApps

REQUIREMENTS

Solana Seeker device
Android 8.0+
Solana wallet for payments and SIWS

SUPPORT

Email: aardappvark@proton.me
Discord: https://discord.gg/sGXgKG4U
Website: https://aardappvark.github.io/ADappvarkToolkit

First in line. Every dApp, every time.
```

---

## Category

**Primary:** Tools & Utilities
**Secondary:** Productivity

---

## Keywords (10 max)

1. dApp manager
2. bulk uninstall
3. bulk reinstall
4. Seeker utilities
5. auto-accept
6. SGT verification
7. dApp automation
8. Solana tools
9. SIWS
10. storage cleanup

---

## Age Rating

**Rating:** 3+ (Everyone)
**Reason:** Utility app, no objectionable content

---

## Support URLs

- **Website:** https://aardappvark.github.io/ADappvarkToolkit
- **Privacy Policy:** https://aardappvark.github.io/ADappvarkToolkit/privacy.html
- **Support Email:** aardappvark@proton.me
- **Terms of Service:** https://aardappvark.github.io/ADappvarkToolkit/terms.html
- **Discord:** https://discord.gg/sGXgKG4U

---

## Screenshots Required

### **Mandatory Screenshots (1080x1920 or 1080x2400)**

1. **Entry Screen**
   - Show: Liquid glass welcome screen with animated aardvark icon, "Sign In with Seeker" button
   - Caption: "Welcome to AardAppvark — Sign In with Solana"

2. **Home Screen**
   - Show: Wallet info card, SGT verified badge, analytics dashboard, features list
   - Caption: "Your dApp Management Hub"

3. **Uninstall Screen**
   - Show: dApp list with selections, filter chips, search bar
   - Caption: "Select & Uninstall Multiple dApps at Once"

4. **Reinstall Screen**
   - Show: Reinstall list with dApp Store apps, auto-install in progress
   - Caption: "Restore Your dApp Collection from the dApp Store"

5. **Settings Screen**
   - Show: Glass-themed settings with payment-gated auto-accept toggles
   - Caption: "Auto-Accept, Favourites, and Privacy Controls"

6. **Payment Dialog** (optional)
   - Show: "1 SKR / 0.01 SOL" payment prompt for auto-accept
   - Caption: "Real On-Chain Payments via Mobile Wallet Adapter"

---

## Feature Graphic (1200x600)

**Design Elements:**
- AMOLED-dark background with liquid glass effect
- Purple/green gradient (Solana colours)
- Animated aardvark icon (large, center-right)
- App name "AardAppvark Toolkit" (left, bold, gradient text)
- Tagline "First in line. Every dApp, every time."
- Glass card elements showing feature highlights

---

## App Icon (512x512)

**Design:**
- Stylised aardvark silhouette
- Purple/green gradient fill (Solana colours)
- Dark background
- Should work at small sizes (48x48)

**Colour Scheme:**
- Primary: #9945FF (Solana Purple)
- Secondary: #14F195 (Solana Green)
- Background: #0A0A12 (AMOLED Dark)

---

## Release Notes (v1.0.5)

```
v1.0.5 — Liquid Glass UI + Payment-Gated Auto-Accept

NEW
- Liquid glass UI: glassmorphism cards, animated gradients, AMOLED-dark theme
- Redesigned entry screen with "Sign In with Seeker", "Verify SGT", and "Continue without Wallet" options
- Payment-gated auto-accept: 1 SKR / 0.01 SOL for a 7-day pass
- Animated aardvark icon with breathing glow
- Glass-themed navigation bars with gradient borders

IMPROVED
- Home screen: wallet info card, SGT verified badge, analytics dashboard
- Settings screen: glass cards, payment-gated toggles, feature status display
- SGT holders see "dApp 100% unlocked" across the app

PRIVACY
- Zero analytics or tracking
- All data stored locally
- GDPR and CCPA compliant
- Geo-restriction for sanctioned regions

REQUIREMENTS
- Solana Seeker device
- Android 8.0+
- Solana wallet for SIWS and payments

SUPPORT
aardappvark@proton.me
https://discord.gg/sGXgKG4U
```

---

## Test Instructions for Reviewers

```
AardAppvark Toolkit — Reviewer Test Guide (v1.0.5)

This app does NOT require Shizuku or any special setup.
It uses standard Android intents for uninstall and dApp Store deep links for reinstall.

QUICK START:
1. Install the APK on a Solana Seeker device
2. Launch the app
3. You will see the entry screen with three options:
   a) "Sign In with Seeker" — connects wallet via SIWS (side-button fingerprint)
   b) "Verify SGT" — same SIWS flow, emphasises SGT verification
   c) "Continue without Wallet" — skip wallet, accept T&C to enter app

TEST SCENARIOS:

1. ENTRY SCREEN (no wallet needed)
   - Tap "Continue without Wallet"
   - Accept Terms & Conditions checkbox
   - Tap "Enter App"
   - Verify: enters main app without wallet

2. WALLET CONNECTION (requires wallet app)
   - Tap "Sign In with Seeker"
   - Approve SIWS in wallet (side-button fingerprint)
   - Verify: home screen shows wallet address

3. HOME SCREEN
   - Verify: wallet info card, analytics dashboard, features list
   - If SGT holder: green "dApp 100% unlocked" badge appears

4. UNINSTALL TAB
   - Navigate to Uninstall tab
   - Verify: dApp list loads with installed apps
   - Select 2-3 apps, tap "Uninstall Selected"
   - Standard Android uninstall dialogs appear for each app
   - If auto-accept is active (paid), dialogs auto-tap

5. REINSTALL TAB
   - Navigate to Reinstall tab
   - Verify: previously uninstalled dApps shown
   - Tap a dApp to reinstall — opens dApp Store listing

6. SETTINGS
   - Navigate to Settings tab
   - Auto-accept toggles show lock icon (payment required)
   - Without payment: tapping toggle shows payment dialog
   - Payment dialog shows "1 SKR / 0.01 SOL" pricing
   - Other settings: favourites, haptic feedback, compact view

PAYMENT TESTING:
- The app uses mainnet SOL for payments (0.01 SOL per operation)
- Payment is only required for 5+ apps or auto-accept
- Under 4 apps is free
- SGT holders get full access for free

NO SPECIAL PERMISSIONS REQUIRED:
- No Shizuku
- No ADB grants
- No root access
- Standard Android permissions only

CONTACT FOR REVIEW SUPPORT:
aardappvark@proton.me
```

---

## Privacy & Legal Compliance

### **Privacy Policy URL**
https://aardappvark.github.io/ADappvarkToolkit/privacy.html

### **Terms of Service URL**
https://aardappvark.github.io/ADappvarkToolkit/terms.html

### **Data Collection Statement**

```
AardAppvark Toolkit collects ZERO personal data.

What we access:
- Installed app list (local only, never transmitted)
- Wallet address (for payment and SGT verification only)
- Approximate location (geo-restriction compliance only, never stored)

What we DON'T collect:
- Name, email, or contact info
- Device identifiers
- Usage analytics
- Crash reports
- Browsing data

All app data is stored locally on your device using Android SharedPreferences.
We do not operate any servers or databases.
Blockchain payments are public by design (inherent to Solana).

See full privacy policy: https://aardappvark.github.io/ADappvarkToolkit/privacy.html
```

---

## Build Information

### **Version Information**
- Version Name: 1.0.5
- Version Code: 6
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 35 (Android 15)
- Compile SDK: 35

### **Package Name**
com.adappvark.toolkit

### **Signing**
- Signed with Roaring Trades release keystore
- SHA-256: ac693213aec9befa200be59881b9decd37646b1761591edc9bc6c144192d100b
- R8 minification enabled
- ProGuard rules configured

### **Build Command**
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease
```

### **Output APK**
`app/build/outputs/apk/release/app-release.apk`

---

## Submission Checklist

**Before Submitting:**

- [ ] All screenshots taken (1080x2400)
- [ ] Feature graphic created (1200x600)
- [ ] App icon finalized (512x512)
- [ ] Privacy Policy live at URL
- [ ] Terms of Service live at URL
- [ ] Support email active
- [ ] Website deployed
- [ ] Release APK signed with Roaring Trades keystore
- [ ] APK signature verified (SHA-256 matches)
- [ ] No test activities in release manifest
- [ ] Tested on real Seeker device
- [ ] Fresh install + launch — no crashes
- [ ] SIWS wallet connection tested
- [ ] Payment flow tested (mainnet)
- [ ] All text proofread
- [ ] versionCode higher than any previously submitted

**Post-Submission:**

- [ ] Monitor review status
- [ ] Respond to reviewer questions promptly
- [ ] Prepare MONOLITH hackathon submission

---

## Contact

**Developer Contact:**
- Email: aardappvark@proton.me
- Discord: https://discord.gg/sGXgKG4U

---

**Built for the Solana Mobile ecosystem | MONOLITH Hackathon 2026**
