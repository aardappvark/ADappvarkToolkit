# AardAppvark Toolkit

**First in line. Every dApp, every time.**

The essential dApp lifecycle manager for Solana Seeker.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?logo=discord&logoColor=white)](https://discord.gg/sGXgKG4U)

---

## Features

- **Bulk Uninstall** - Clean up dApps in seconds with silent ADB-powered removal
- **Bulk Reinstall** - Restore your entire dApp collection from the Solana dApp Store with auto-install
- **Smart Favourites** - Star your favourite dApps for quick access
- **SGT Verification** - Seeker Genesis Token holders get 2 free credits
- **Wallet Login** - Non-custodial authentication via Solana Mobile Wallet Adapter (SIWS)
- **Temporarily Free** - All features free during early access

---

## Built For Seeker

Designed specifically for the Solana Seeker device. Uses standard Android intents for uninstall and dApp Store deep links for reinstall. No root access required.

---

## Privacy First

- No analytics or tracking
- No cloud database or user accounts
- All data stored locally on your device
- Non-custodial wallet integration via Mobile Wallet Adapter
- GDPR and CCPA compliant
- Full data export and deletion
- Geo-restriction for sanctioned jurisdictions
- On-chain wallet sanctions screening

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Wallet:** Solana Mobile Wallet Adapter 2.0
- **Payments:** On-chain SOL/SKR transfers
- **SGT Verification:** seeker-verify 1.1.0
- **Target:** Android 14+ (API 34), Min SDK 26

---

## Installation

### From Solana dApp Store

1. Open the Solana dApp Store on your Seeker
2. Search for "AardAppvark"
3. Install and launch
4. Accept Terms of Service
5. Connect your Solana wallet (Phantom or Solflare)

### From APK

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

---

## Build From Source

### Prerequisites

- Android Studio (latest stable)
- Java 17+
- Solana Seeker device (or Android emulator)

### Build

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease
```

The signed APK will be at `app/build/outputs/apk/release/app-release.apk`.

---

## Legal

- [Terms of Service](https://aardappvark.github.io/ADappvarkToolkit/terms.html)
- [Privacy Policy](https://aardappvark.github.io/ADappvarkToolkit/privacy.html)
- [Website](https://aardappvark.github.io/ADappvarkToolkit)

---

## Support

- **Discord:** [Join Server](https://discord.gg/sGXgKG4U)
- **Email:** aardappvark@proton.me

---

## License

Proprietary Software. Copyright 2026 AardAppvark. All rights reserved.

Uses open-source components under Apache 2.0:
- Jetpack Compose
- Material Design 3
- Solana Mobile Wallet Adapter

---

**Built for the Solana Mobile ecosystem**
