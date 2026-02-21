# Launch Checklist — AardAppvark Toolkit v1.0.5

## Pre-Launch (Critical)

- [x] **Wallet address configured** — `AppConfig.kt` → `Payment.WALLET_ADDRESS`
- [x] **Mainnet configured** — `AppConfig.kt` → `CLUSTER = "mainnet-beta"`
- [x] **SIWS wallet connection** — `mobileWalletAdapter.signIn()` with side-button fingerprint
- [x] **SGT verification** — seeker-verify 1.1.0 via JitPack
- [x] **Payment system** — Real on-chain SOL transfers via MWA
- [x] **Payment-gated auto-accept** — 1 SKR / 0.01 SOL for 7-day pass
- [x] **Liquid glass UI** — Glassmorphism cards, animated gradients, AMOLED-dark
- [x] **Entry screen redesign** — Sign In with Seeker / Verify SGT / Continue without Wallet
- [x] **Animated aardvark icon** — Breathing glow, gradient fill
- [x] **Glass navigation** — Transparent top/bottom bars with gradient borders
- [x] **Analytics dashboard** — Installed, storage, favourites, uninstalled, reinstalled, recovered
- [x] **Search bars** — Uninstall and reinstall screens
- [x] **Favourites system** — Star dApps to protect from bulk operations
- [x] **Haptic feedback** — Settings toggle
- [x] **Compact list view** — Settings toggle
- [x] **Geo-restriction** — Sanctioned region blocking
- [x] **Privacy compliance** — GDPR/CCPA, zero analytics, local data only
- [x] **Legal docs hosted** — Privacy Policy + Terms of Service on GitHub Pages
- [x] **Debug code removed** — No println, no test data
- [x] **ProGuard/R8 rules** — MWA, seeker-verify, Compose, crypto, coroutines
- [x] **Test activities removed** — `tools:node="remove"` for InstrumentationActivityInvoker
- [x] **EncryptedSharedPreferences fallback** — Falls back to standard SharedPreferences if Tink unavailable
- [x] **Startup crash protection** — All onCreate logic wrapped in try/catch

## Build & Deploy

- [x] **Release APK signed** — Roaring Trades keystore (SHA-256: `ac693213...`)
- [x] **Tested on Seeker** — Fresh install, no crashes, logcat clean
- [ ] **Screenshots captured** — Entry, Home, Uninstall, Reinstall, Settings (1080x2400)
- [ ] **Feature graphic** — 1200x600, liquid glass design
- [ ] **App icon finalised** — 512x512

## dApp Store Submission

- [ ] **Submit APK** — v1.0.5, versionCode 6
- [ ] **App listing** — Name, description, screenshots, graphic (see `submission/DAPP_STORE_GUIDE.md`)
- [ ] **Reviewer test guide** — Included in submission guide
- [ ] **Monitor review** — Respond to questions at aardappvark@proton.me

## MONOLITH Hackathon (Deadline: March 9, 2026)

- [ ] **Demo video** — Record on Seeker showing full flow
- [ ] **Submit to hackathon** — align.nexus / solanamobile.com/hackathon
- [ ] **Real SKR payment** — Implement actual SPL token transfer ($10K SKR bonus)

## Post-Launch

- [ ] **Announce on Discord** — https://discord.gg/sGXgKG4U
- [ ] **Apply for Solana Mobile Builder Grant** — After dApp Store approval
- [ ] **Monitor user feedback** — Email + Discord

## Configuration Reference

All critical config is centralised in `AppConfig.kt`:

| Setting | Value | Location |
|---------|-------|----------|
| Treasury wallet | `DD4aPDhf396N...` | `AppConfig.Payment.WALLET_ADDRESS` |
| Cluster | `mainnet-beta` | `AppConfig.Payment.CLUSTER` |
| RPC endpoint | `api.mainnet-beta.solana.com` | `AppConfig.Payment.RPC_ENDPOINT` |
| App identity URI | `aardappvark.github.io/ADappvarkToolkit` | `AppConfig.Identity.URI` |
| Auto-accept duration | 7 days | `AppConfig.Subscription.DURATION_DAYS` |
| Payment bypass | `false` | `AppConfig.Features.TESTING_BYPASS_PAYMENT` |

## Version History

| Version | Code | Status |
|---------|------|--------|
| 1.0.5 | 6 | Current — Liquid glass UI, payment-gated auto-accept |
| 1.0.4 | 5 | Settings, haptic feedback, pull-to-refresh |
| 1.0.3 | 4 | Search bars, analytics dashboard |
| 1.0.2 | 3 | SGT verification, credit system |
| 1.0.1 | 2 | Payment system, bulk reinstall |
| 1.0.0 | 1 | Initial release, bulk uninstall |

---

**Built for the Solana Mobile ecosystem | MONOLITH Hackathon 2026**
