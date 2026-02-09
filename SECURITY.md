# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in AardAppvark, please report it responsibly.

**Email:** aardappvark@proton.me

**Subject line:** `[SECURITY] Brief description of vulnerability`

### What to include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

### Response timeline:
- **Acknowledgement:** Within 48 hours
- **Initial assessment:** Within 7 days
- **Fix timeline:** Depends on severity (critical: ASAP, high: 14 days, medium: 30 days)

### Scope

The following are in scope:
- AardAppvark Android application code
- Privacy and data handling issues
- Authentication/authorization bypasses
- Geo-restriction bypass vulnerabilities
- Payment processing vulnerabilities
- Accessibility service misuse vectors

The following are out of scope:
- Solana blockchain vulnerabilities (report to Solana Foundation)
- Solana Mobile Wallet Adapter vulnerabilities (report to Solana Mobile)
- Shizuku vulnerabilities (report to Shizuku maintainers)
- Android OS vulnerabilities (report to Google)
- Social engineering attacks
- Denial of service attacks

### Safe harbour

We will not take legal action against researchers who:
- Make a good faith effort to avoid privacy violations and data destruction
- Report vulnerabilities promptly
- Do not exploit vulnerabilities beyond what is necessary to demonstrate them
- Do not publicly disclose before a fix is available

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest  | Yes       |

## Security Architecture

AardAppvark is a local-first, decentralised application:
- **No backend servers** - all data stored locally on device
- **No cloud databases** - no Firebase, Supabase, or API endpoints
- **No user accounts** - wallet address is the only identifier
- **No data transmission** - no analytics, crash reporting, or telemetry
- **Non-custodial** - app never accesses wallet private keys
- **Geo-restricted** - 54 countries blocked via multi-method location verification
- **Sanctions screening** - OFAC SDN list + TRM Labs API wallet screening
