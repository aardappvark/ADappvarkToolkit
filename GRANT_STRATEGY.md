# ADappvark Grant Strategy

## Priority Order

### 1. MONOLITH Hackathon (CURRENT — Deadline March 9, 2026)
- $125K+ in prizes (10x $10K winners + 5x $5K honorable mentions)
- **$10K bonus** for best SKR integration
- Submit via align.nexus / solanamobile.com/hackathon
- Judging: Mobile-first UX, Solana Mobile Stack integration, creative Solana usage, vision & clarity

### 2. Solana dApp Store Submission
Get the dApp live on the Seeker dApp Store.
This is a prerequisite for the Solana Mobile builder grant.

### 3. Solana Mobile Builder Grants (AFTER dApp Store launch)
- URL: https://airtable.com/appw7jfRXG6Joia2b/pagGNMPX6qleBYHNp/form
- Up to $10K per team
- Requires: mobile-first UX, SMS integration, detailed budget, public good commitment
- Grants are awarded AFTER dApp is submitted to the store
- Apply after the dApp is live and has initial usage metrics
- Highlight: Only dApp lifecycle management tool for Seeker

### 4. Superteam Grants (FUTURE)
- URL: https://superteam.fun/earn/grants
- Equity-free grants for Solana ecosystem projects
- STRATEGY: Fork the codebase and create an open-source version:
  - Remove payment gateways (SOL/SKR fees)
  - Make all features free
  - Open source under MIT or Apache 2.0 license
  - Position as a public good for the Solana ecosystem
- Keep the paid version on dApp Store as the commercial product

## Payment Model (v1.0.5 — Feb 2026)

| Tier | Price | Details |
|------|-------|---------|
| Free | $0 | Up to 4 apps per bulk operation |
| Bulk (5+ apps) | 1 SKR / 0.01 SOL | Flat fee per operation |
| Auto-Accept | 1 SKR / 0.01 SOL | 7-day pass, auto-tap system dialogs |
| SGT Holders | Full unlock | Verified Seeker Genesis Token holders |

- All payments are real on-chain SOL transfers via MWA
- SKR pricing displayed in UI, processed as SOL equivalent
- No subscription model — one-time pass or per-operation
- Credits expire 12 months after purchase

## Solana Mobile Stack Integration

| Component | Status | Details |
|-----------|--------|---------|
| Mobile Wallet Adapter 2.0 | ✅ | SIWS with side-button fingerprint |
| Sign In With Solana (SIWS) | ✅ | Full authentication flow |
| On-chain payments | ✅ | Real SOL transfers to treasury |
| SGT verification | ✅ | seeker-verify 1.1.0 library |
| SKR token | ⚠️ Partial | Pricing displayed, processes as SOL |
| Seed Vault | ✅ Indirect | SIWS triggers Seeker side-button |

## Hackathon Strengths

- **Unique product** — Only dApp lifecycle management tool for Seeker
- **Real on-chain payments** — Not mocked, actual SOL transfers
- **SGT verification** — Real on-chain SGT checking via seeker-verify
- **SIWS** — Side-button fingerprint, not basic authorize()
- **Liquid glass UI** — Eye-catching glassmorphism design
- **Privacy-first** — Zero analytics, GDPR/CCPA, geo-blocking
- **Production-ready** — Release APK signed, deployed to Seeker

## Hackathon Gaps (Priority Actions)

1. **Real SKR payment** — Implement actual SPL token transfer for $10K SKR bonus
2. **Demo video** — Record for "Vision & Clarity" judging criteria
3. **Submit to dApp Store** — Looks great on hackathon submission

## Notes
- v1.0.5 introduced: Liquid glass UI, redesigned entry screen, payment-gated auto-accept, animated logo
- Free tier is 4 apps (consistent across all UI and legal text)
- Auto-accept is a 7-day pass, not a subscription
