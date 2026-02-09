# AardAppvark Incident Response Plan

**Version:** 1.0.0
**Last Updated:** February 9, 2026
**Contact:** aardappvark@proton.me

---

## 1. Scope

This plan covers security incidents affecting AardAppvark Toolkit, including:
- Vulnerability discoveries in application code
- Compromise of signing keys or distribution channel
- OFAC/sanctions list updates requiring immediate action
- Data handling incidents (despite local-only architecture)
- Supply chain compromises (dependency vulnerabilities)
- Abuse of accessibility service

---

## 2. Incident Classification

| Severity | Definition | Response Time | Examples |
|----------|-----------|---------------|----------|
| **CRITICAL** | Active exploitation, user funds at risk | Immediate (< 4 hours) | Signing key compromise, payment bypass, wallet drain vector |
| **HIGH** | Exploitable vulnerability, no active exploitation | < 24 hours | Geo-restriction bypass, shell injection, sanctions evasion |
| **MEDIUM** | Vulnerability requiring user interaction | < 7 days | Data leak via backup, permission escalation |
| **LOW** | Minor issue, minimal impact | < 30 days | UI information disclosure, non-sensitive log exposure |

---

## 3. Response Phases

### Phase 1: DETECT
- Monitor SECURITY.md email (aardappvark@proton.me) for vulnerability reports
- Monitor GitHub repository for security-related issues
- Review Solana dApp Store feedback
- Monitor OFAC SDN list updates (monthly at minimum)
- Monitor FATF grey/black list changes (quarterly)
- Monitor dependency vulnerabilities via GitHub Dependabot

### Phase 2: ASSESS
- Confirm the incident is genuine (not false positive)
- Classify severity using table above
- Determine affected versions and user impact
- Document findings

### Phase 3: CONTAIN
- **For code vulnerabilities:**
  - Develop and test fix locally
  - If CRITICAL: push emergency update to dApp Store immediately
  - If actively exploited: consider pulling current version from dApp Store
- **For sanctions list updates:**
  - Add new addresses to SANCTIONED_WALLET_ADDRESSES
  - Add new countries to BLOCKED_COUNTRIES if needed
  - Deploy update
- **For signing key compromise:**
  - Generate new signing keystore immediately
  - Revoke old key
  - Notify dApp Store of key rotation
  - Issue signed advisory

### Phase 4: NOTIFY
- **Users:** In-app notification on next launch (for HIGH/CRITICAL)
- **dApp Store:** Contact Solana dApp Store review team
- **Authorities:** If legally required (e.g., notifiable data breach under Australian Privacy Act — unlikely given local-only architecture)
- **Public:** GitHub Security Advisory for open-source component issues

### Phase 5: REMEDIATE
- Deploy fix to dApp Store
- Update SECURITY.md with disclosure details (after fix is available)
- Update sanctions lists if applicable
- Document lessons learned

### Phase 6: REVIEW
- Conduct post-incident review within 7 days
- Update this plan if gaps identified
- Update code/architecture if systemic issue found

---

## 4. Contact Information

| Role | Contact |
|------|---------|
| Primary Contact | aardappvark@proton.me |
| GitHub Repository | https://github.com/aardappvark/ADappvarkToolkit |
| dApp Store | Solana dApp Store Discord |

---

## 5. Regulatory Notification Requirements

### Australia (Privacy Act 1988 — Notifiable Data Breaches)
- **Threshold:** Eligible data breach likely to result in serious harm
- **Applicability:** LOW — AardAppvark stores no personal data on servers. Local-only data is controlled by the user.
- **If triggered:** Notify OAIC within 30 days and affected individuals as soon as practicable

### EU (GDPR — Article 33/34)
- **Threshold:** Personal data breach likely to result in risk to rights/freedoms
- **Applicability:** LOW — No server-side EU data processing
- **If triggered:** Notify supervisory authority within 72 hours

### USA (State breach notification laws)
- **Threshold:** Varies by state; generally covers PII (name + SSN, financial account, etc.)
- **Applicability:** VERY LOW — AardAppvark does not collect PII
- **If triggered:** Notify affected individuals per state requirements

---

## 6. OFAC/Sanctions Update Schedule

| Task | Frequency | Source |
|------|-----------|--------|
| Review OFAC SDN list | Monthly | https://sanctionslist.ofac.treas.gov/ |
| Review FATF grey/black lists | Quarterly | https://www.fatf-gafi.org/ |
| Review EU consolidated list | Monthly | https://data.europa.eu/data/datasets/consolidated-list-of-persons-groups-and-entities-subject-to-eu-financial-sanctions |
| Review UK sanctions | Monthly | https://www.gov.uk/government/publications/the-uk-sanctions-list |
| Review AU DFAT list | Monthly | https://www.dfat.gov.au/international-relations/security/sanctions/consolidated-list |
| TRM Labs API status | Ongoing | Automated via API integration |

---

## 7. Document Storage

This incident response plan is stored:
- **Primary:** `/legal/INCIDENT_RESPONSE_PLAN.md` in the project repository
- **Backup:** GitHub repository (pushed with code)

Review and update this plan at least annually or after any incident.
