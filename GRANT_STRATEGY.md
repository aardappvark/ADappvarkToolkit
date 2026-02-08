# ADappvark Grant Strategy

## Priority Order

### 1. Solana dApp Store Submission (FIRST)
Get the dApp live on the Seeker dApp Store before applying for any grants.
This is a prerequisite for the Solana Mobile grant.

### 2. Solana Mobile Grants (AFTER dApp Store launch)
- URL: https://airtable.com/appw7jfRXG6Joia2b/pagGNMPX6qleBYHNp/form
- Apply after the dApp is live and has initial usage metrics
- Demonstrate value to the Seeker ecosystem
- Highlight: Only dApp lifecycle management tool for Seeker

### 3. Superteam Grants (FUTURE)
- URL: https://superteam.fun/earn/grants
- Equity-free grants for Solana ecosystem projects
- STRATEGY: Fork the codebase and create an open-source version:
  - Remove payment gateways (SOL/SKR fees)
  - Remove credit system
  - Make all features free
  - Open source under MIT or Apache 2.0 license
  - Position as a public good for the Solana ecosystem
- This increases chances of approval significantly
- Keep the paid version on dApp Store as the commercial product
- The open-source fork would be a separate project/repo

## dApp Store Submission Checklist

### Critical (Must Complete Before Submission)
- [ ] Set production wallet address in AppConfig.kt (replace placeholder)
- [ ] Switch from devnet to mainnet-beta
- [ ] Test on real Seeker device with Shizuku
- [ ] Create release signing keystore
- [ ] Build signed release APK
- [ ] Deploy website at https://adappvark.xyz
- [ ] Privacy Policy live at https://adappvark.xyz/privacy
- [ ] Terms of Service live at https://adappvark.xyz/terms
- [ ] Set up support email (support@adappvark.xyz)
- [ ] GitHub repo for licensing/legal URLs

### Assets Needed
- [ ] App icon (512x512)
- [ ] Feature graphic (1200x600)
- [ ] Screenshots (1080x1920 or 1080x2400, minimum 4)
- [ ] Promo video (optional, 30-60 seconds)

### GitHub Setup Required
- [ ] Create GitHub repository (public or private)
- [ ] Host legal documents (Privacy Policy, Terms, Licenses)
- [ ] Set up GitHub Pages or link to adappvark.xyz
- [ ] APK can be hosted in GitHub Releases as alternative distribution

## Payment Model (Consolidated - Feb 9, 2026)
- Up to 4 apps per operation: FREE
- 5+ apps per operation: 0.01 SOL flat fee
- 1 free credit on first wallet connect
- Credits expire 12 months after purchase
- Accepts SOL and SKR (Seeker) tokens
- No subscription model (removed)
- No credit bundles (removed)

## Notes
- The credit bundle pricing model was removed in favour of simple per-operation flat fee
- The subscription model (weekly plans) was removed - dead code remains but is not wired into navigation
- Free tier changed from 5 to 4 apps for consistency across all UI and legal text
