# AardAppvark Toolkit

**First in line. Every dApp, every time.**

The essential dApp lifecycle manager for Solana Seeker.

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?logo=discord&logoColor=white)](https://discord.gg/sGXgKG4U)

---

## Features

- **Bulk Uninstall** - Clean up dApps in seconds with silent ADB-powered removal
- **Bulk Reinstall** - Restore your entire dApp collection from the Solana dApp Store with auto-install
- **Auto-Accept** - Payment-gated auto-tap for system uninstall/reinstall dialogs (7-day pass, 1 SKR / 0.01 SOL)
- **Smart Favourites** - Star your favourite dApps so they're never accidentally removed
- **SGT Verification** - Seeker Genesis Token holders get the full dApp unlocked
- **Wallet Login** - Non-custodial Sign In With Solana (SIWS) with side-button fingerprint
- **Liquid Glass UI** - Beautiful glassmorphism design with animated gradients, AMOLED-dark theme, and breathing glow effects
- **Analytics Dashboard** - Track installed dApps, storage usage, uninstall/reinstall stats
- **Privacy First** - Zero analytics, no cloud, all data local, GDPR/CCPA compliant
- **Geo-Restriction** - Sanctions compliance with periodic location checks

---

## Built For Seeker

Designed exclusively for the Solana Seeker device. Uses standard Android intents for uninstall and dApp Store deep links for reinstall. Sign In With Solana (SIWS) triggers the Seeker's physical side-button fingerprint confirmation for maximum security.

---

## Payment Model

| Tier | Price | Details |
|------|-------|---------|
| Free | $0 | Up to 4 apps per bulk operation |
| Bulk (5+ apps) | 1 SKR / 0.01 SOL | Flat fee per operation, real on-chain payment |
| Auto-Accept | 1 SKR / 0.01 SOL | 7-day pass, auto-tap system dialogs |
| SGT Holders | Full unlock | Verified Seeker Genesis Token holders |

All payments are processed on-chain via Solana Mobile Wallet Adapter.

---

## Privacy First

- No analytics or tracking
- No cloud database or user accounts
- All data stored locally on your device
- Non-custodial wallet integration via Mobile Wallet Adapter
- GDPR and CCPA compliant
- Full data export and deletion in Settings
- Geo-restriction for sanctioned jurisdictions
- On-chain wallet sanctions screening

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3) with Liquid Glass theme
- **Wallet:** Solana Mobile Wallet Adapter 2.0 (SIWS)
- **Payments:** On-chain SOL transfers via MWA
- **SGT Verification:** seeker-verify 1.1.0
- **Target:** Android 15 (API 35), Min SDK 26
- **Theme:** AMOLED-dark with Solana purple/green glassmorphism

---

## Installation

### From Solana dApp Store

1. Open the Solana dApp Store on your Seeker
2. Search for "AardAppvark"
3. Install and launch
4. Sign in with your Seeker wallet (SIWS)

### From APK

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

---

## Build From Source

### Prerequisites

- Android Studio (latest stable)
- Java 17+
- Solana Seeker device

### Build

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease
```

The signed APK will be at `app/build/outputs/apk/release/app-release.apk`.

---

## Version History

| Version | Code | Changes |
|---------|------|---------|
| 1.0.5 | 6 | Liquid glass UI, redesigned entry screen, payment-gated auto-accept, animated logo, glass navigation |
| 1.0.4 | 5 | Settings screen, haptic feedback, pull-to-refresh, screen animations, app preferences |
| 1.0.3 | 4 | Search bars, analytics dashboard, empty states, compact list view |
| 1.0.2 | 3 | SGT verification, credit system, geo-restriction |
| 1.0.1 | 2 | Payment system, bulk reinstall |
| 1.0.0 | 1 | Initial release, bulk uninstall |

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
- seeker-verify

---

**Built for the Solana Mobile ecosystem | MONOLITH Hackathon 2026**
