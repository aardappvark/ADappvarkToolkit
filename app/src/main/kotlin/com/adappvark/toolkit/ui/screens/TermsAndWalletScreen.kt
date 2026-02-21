package com.adappvark.toolkit.ui.screens

import android.Manifest
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.AppConfig
import com.adappvark.toolkit.service.GeoRestrictionService
import com.adappvark.toolkit.service.TermsAcceptanceService
import com.adappvark.toolkit.ui.components.AnimatedAardvarkIcon
import com.adappvark.toolkit.ui.theme.*
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.common.signin.SignInWithSolana
import kotlinx.coroutines.launch

/**
 * Redesigned Welcome & Sign-In Screen with liquid glass UI
 *
 * Three entry paths:
 * 1. Sign In with Seeker (SIWS) — full access
 * 2. SGT verification card — same SIWS but emphasizes SGT badge
 * 3. Skip (Continue without Wallet) — checkbox T&C, no payment access
 */
@Composable
fun TermsAndWalletScreen(
    activityResultSender: ActivityResultSender,
    onComplete: (publicKey: String?, walletName: String?) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Geo-restriction state
    var isCheckingGeo by remember { mutableStateOf(true) }
    var geoBlocked by remember { mutableStateOf(false) }
    var geoBlockedMessage by remember { mutableStateOf<String?>(null) }

    // Sign-in state
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // Skip sign-in state
    var showSkipPanel by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }

    // Legal dialog state
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Services
    val termsService = remember { TermsAcceptanceService(context) }
    val geoService = remember { GeoRestrictionService(context) }

    // MWA adapter
    val mobileWalletAdapter = remember {
        MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = Uri.parse(AppConfig.Identity.URI),
                iconUri = Uri.parse(AppConfig.Identity.ICON_URI),
                identityName = AppConfig.Identity.NAME
            )
        )
    }

    // Geo check function
    fun performGeoCheck() {
        scope.launch {
            isCheckingGeo = true
            when (val result = geoService.checkGeoRestriction()) {
                is GeoRestrictionService.GeoCheckResult.Blocked -> {
                    geoBlocked = true
                    geoBlockedMessage = geoService.getBlockedMessage(result)
                }
                is GeoRestrictionService.GeoCheckResult.Allowed -> { /* proceed */ }
                is GeoRestrictionService.GeoCheckResult.Error -> {
                    geoBlocked = true
                    geoBlockedMessage = "Unable to verify your location. Location verification is required for sanctions compliance."
                }
            }
            isCheckingGeo = false
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> performGeoCheck() }

    // On launch: request location permission for geo check
    LaunchedEffect(Unit) {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            performGeoCheck()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // SIWS sign-in function
    fun performSignIn() {
        scope.launch {
            isConnecting = true
            connectionError = null
            try {
                val signInPayload = SignInWithSolana.Payload(
                    Uri.parse(AppConfig.Identity.URI).host,
                    "Sign in to AardAppvark Toolkit with your Seeker wallet"
                )
                val result = mobileWalletAdapter.signIn(activityResultSender, signInPayload)

                when (result) {
                    is TransactionResult.Success -> {
                        val signInResult = result.payload
                        val pubKeyBytes = signInResult.publicKey
                        val pubKeyBase58 = bytesToBase58(pubKeyBytes)
                        val walletName = result.authResult.walletUriBase?.host ?: "Seeker"

                        termsService.recordConsentWithoutSignature(pubKeyBase58)
                        onComplete(pubKeyBase58, walletName)
                    }
                    is TransactionResult.NoWalletFound -> {
                        connectionError = "No MWA-compatible wallet found. Install a Solana wallet app."
                    }
                    is TransactionResult.Failure -> {
                        connectionError = result.e.message ?: "Failed to connect wallet"
                    }
                }
            } catch (e: Exception) {
                connectionError = e.message ?: "Failed to connect wallet"
            } finally {
                isConnecting = false
            }
        }
    }

    // ==================== GEO BLOCKED SCREEN ====================
    if (geoBlocked) {
        AnimatedGradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GlassCard(glowColor = Color(0xFFFF4444).copy(alpha = 0.15f)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Service Unavailable",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = geoBlockedMessage ?: "AardAppvark is not available in your jurisdiction.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { (context as? Activity)?.finish() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exit")
                }
            }
        }
        return
    }

    // ==================== LOADING SCREEN ====================
    if (isCheckingGeo) {
        AnimatedGradientBackground {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = SolanaPurple)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verifying eligibility...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // ==================== WELCOME SCREEN ====================
    AnimatedGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Hero: Animated Aardvark icon with glow
            AnimatedAardvarkIcon(
                size = 140.dp,
                glowColor = SolanaPurple
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App name
            Text(
                text = "AardAppvark",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = SolanaPurple
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "First in line. Every dApp, every time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ==================== SIGN IN OPTIONS ====================

            // Primary CTA: Sign In with Seeker
            GlassCard(
                elevated = true,
                glowColor = SolanaPurpleGlow
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = SolanaPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelMedium,
                            color = SolanaPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { performSignIn() },
                        enabled = !isConnecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolanaPurple,
                            contentColor = Color.White
                        )
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Signing in...")
                        } else {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Seeker", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Uses Sign In With Solana (SIWS) + side-button confirmation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary: SGT Verification card
            GlassCard(glowColor = SolanaGreenGlow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isConnecting) { performSignIn() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = SolanaGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Seeker Genesis Token",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SolanaGreen
                        )
                        Text(
                            text = "Verify your SGT to unlock the full dApp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = SolanaGreen.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Error message
            connectionError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                GlassCard(glowColor = Color(0xFFFF4444).copy(alpha = 0.1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider with "or"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassDivider(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "or",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                GlassDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skip: Continue without Wallet
            if (!showSkipPanel) {
                TextButton(onClick = { showSkipPanel = true }) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Continue without Wallet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                // Inline T&C acceptance panel
                GlassCard {
                    Column {
                        Text(
                            text = "Accept Terms to Continue",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Without a wallet, auto-accept features and paid operations will be unavailable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            Checkbox(
                                checked = acceptedTerms,
                                onCheckedChange = { acceptedTerms = it },
                                colors = CheckboxDefaults.colors(checkedColor = SolanaPurple)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "I am 18+, not in a sanctioned jurisdiction, and I agree to the Terms of Service and Privacy Policy.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onComplete(null, null) },
                            enabled = acceptedTerms,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Enter AardAppvark")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Legal footer with clickable links
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolanaPurple.copy(alpha = 0.8f),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { showTermsDialog = true }
                )
                Text(
                    text = "  |  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolanaPurple.copy(alpha = 0.8f),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { showPrivacyDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exit button
            TextButton(onClick = { (context as? Activity)?.finish() }) {
                Text(
                    text = "Exit",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ==================== LEGAL DIALOGS ====================

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = getFullTermsOfService(), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) { Text("Close") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = getFullPrivacyPolicy(), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }
}

// ==================== Helper Functions ====================

private fun formatWalletAddress(address: String): String {
    return if (address.length > 12) "${address.take(6)}...${address.takeLast(4)}" else address
}

private fun bytesToBase58(bytes: ByteArray): String {
    val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    var num = java.math.BigInteger(1, bytes)
    val sb = StringBuilder()
    val base = java.math.BigInteger.valueOf(58)
    while (num > java.math.BigInteger.ZERO) {
        val divRem = num.divideAndRemainder(base)
        sb.append(alphabet[divRem[1].toInt()])
        num = divRem[0]
    }
    for (byte in bytes) {
        if (byte.toInt() == 0) sb.append(alphabet[0]) else break
    }
    return sb.reverse().toString()
}

private fun bytesToBase64(bytes: ByteArray): String {
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

// ==================== Legal Documents ====================

private fun getFullTermsOfService(): String {
    val currentDate = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        .format(java.util.Date())

    return """
AARDAPPVARK TERMS OF SERVICE

Effective Date: $currentDate
Version: ${TermsAcceptanceService.CURRENT_TOS_VERSION}

IMPORTANT: BY CONNECTING YOUR WALLET AND SIGNING THE CONSENT MESSAGE, YOU ARE ENTERING INTO A LEGALLY BINDING AGREEMENT.

1. INTRODUCTION AND ACCEPTANCE
These Terms of Service constitute a legally binding agreement between you and the operators of AardAppvark. AardAppvark is a mobile application for managing decentralized applications (dApps) on Solana Seeker devices.

2. ELIGIBILITY AND RESTRICTIONS
You must be at least 18 years old. This Application is NOT available to persons located in sanctioned jurisdictions including Cuba, Iran, North Korea, Syria, Russia, Belarus, Venezuela, Myanmar, Nicaragua, Eritrea, and other FATF-listed countries.

3. CREDIT SYSTEM AND PAYMENTS
Operations involving up to 4 applications are free. Bulk operations involving 5+ applications require a fee of 1 SKR (approx 0.01 SOL) per operation. Verified Seeker device owners (SGT holders) receive 2 free credits.

4. PROHIBITED USES
You agree NOT to violate any applicable law, engage in fraudulent activities, facilitate money laundering, evade sanctions, or use VPNs to circumvent geographic restrictions.

5. DISCLAIMER OF WARRANTIES
THE APPLICATION IS PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND.

6. LIMITATION OF LIABILITY
OUR TOTAL LIABILITY SHALL NOT EXCEED THE GREATER OF: (a) THE AMOUNT YOU PAID US IN THE TWELVE MONTHS PRECEDING THE CLAIM, OR (b) ONE HUNDRED U.S. DOLLARS.

7. GOVERNING LAW
These Terms are governed by the laws of Australia.

8. CONTACT
For questions, contact us through the Solana dApp Store or at ${AppConfig.Urls.SUPPORT_EMAIL}.

END OF TERMS OF SERVICE
    """.trimIndent()
}

private fun getFullPrivacyPolicy(): String {
    val currentDate = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        .format(java.util.Date())

    return """
AARDAPPVARK PRIVACY POLICY

Effective Date: $currentDate
Version: ${TermsAcceptanceService.CURRENT_PRIVACY_VERSION}

This Privacy Policy explains how AardAppvark collects, uses, shares, and protects your information. Designed to comply with GDPR, CCPA, and other applicable privacy laws.

1. INFORMATION WE COLLECT
Wallet Address (public key), Consent Record (cryptographic signature), Preferences, approximate location (for sanctions compliance only, not stored).

2. INFORMATION WE DO NOT COLLECT
We do NOT collect your name, email, phone number, device identifiers, wallet balances, or use analytics/advertising trackers.

3. LEGAL BASIS FOR PROCESSING (GDPR)
Contract performance, explicit consent via cryptographic signature, legitimate interests for security, legal obligation for compliance.

4. YOUR RIGHTS
GDPR: Access, Rectification, Erasure, Restriction, Portability, Objection, Withdraw Consent.
CCPA: Know, Delete, Opt-out, Non-discrimination.
Use "Export My Data" and "Delete My Data" in Settings.

5. DATA SECURITY
We implement encryption, secure local storage, and regular security assessments.

6. CONTACT
For privacy inquiries, use Settings or contact ${AppConfig.Urls.SUPPORT_EMAIL}.

END OF PRIVACY POLICY
    """.trimIndent()
}
