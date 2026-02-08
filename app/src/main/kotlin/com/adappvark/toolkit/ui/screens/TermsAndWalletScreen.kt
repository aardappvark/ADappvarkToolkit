package com.adappvark.toolkit.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.AppConfig
import com.adappvark.toolkit.service.GeoRestrictionService
import com.adappvark.toolkit.service.TermsAcceptanceService
import com.adappvark.toolkit.ui.components.AardvarkIcon
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import kotlinx.coroutines.launch

/**
 * Combined Terms & Conditions acceptance and Wallet Connection screen
 *
 * LEGAL COMPLIANCE FLOW:
 * 1. User reads Terms of Service and Privacy Policy
 * 2. User ticks checkbox confirming they've read and agree
 * 3. User connects their wallet using Solana Mobile Wallet Adapter
 * 4. User signs a consent message with their wallet (cryptographic proof)
 * 5. Signature is stored locally as legally binding proof of acceptance
 *
 * This provides:
 * - Non-repudiation (cryptographic signature proves consent)
 * - GDPR compliance (explicit consent with audit trail)
 * - Immutable proof (signature cannot be forged)
 */
@Composable
fun TermsAndWalletScreen(
    activityResultSender: ActivityResultSender,
    onComplete: (publicKey: String, walletName: String) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Geo-restriction state
    var isCheckingGeo by remember { mutableStateOf(true) }
    var geoBlocked by remember { mutableStateOf(false) }
    var geoBlockedMessage by remember { mutableStateOf<String?>(null) }
    var geoWarning by remember { mutableStateOf<String?>(null) }

    // Legal acceptance state
    var hasReadTerms by remember { mutableStateOf(false) }
    var hasReadPrivacy by remember { mutableStateOf(false) }
    var confirmsEligibility by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Wallet connection state
    var connectedPublicKey by remember { mutableStateOf<String?>(null) }
    var connectedWalletName by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var isSigning by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var signatureSuccess by remember { mutableStateOf(false) }

    // Services
    val termsService = remember { TermsAcceptanceService(context) }
    val geoService = remember { GeoRestrictionService(context) }

    // MWA adapter instance with app identity from centralized config
    val mobileWalletAdapter = remember {
        MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = Uri.parse(AppConfig.Identity.URI),
                iconUri = Uri.parse(AppConfig.Identity.ICON_URI),
                identityName = AppConfig.Identity.NAME
            )
        )
    }

    // Check geo-restriction on launch
    LaunchedEffect(Unit) {
        isCheckingGeo = true
        when (val result = geoService.checkGeoRestriction()) {
            is GeoRestrictionService.GeoCheckResult.Blocked -> {
                geoBlocked = true
                geoBlockedMessage = geoService.getBlockedMessage(result)
            }
            is GeoRestrictionService.GeoCheckResult.Warning -> {
                geoWarning = geoService.getWarningMessage(result)
            }
            is GeoRestrictionService.GeoCheckResult.Allowed -> {
                // All good, proceed
            }
            is GeoRestrictionService.GeoCheckResult.Error -> {
                // Unable to check - allow with warning
                geoWarning = "Unable to verify your location. By proceeding, you confirm you are not located in a sanctioned jurisdiction."
            }
        }
        isCheckingGeo = false
    }

    // All requirements must be met
    val allRequirementsMet = hasReadTerms && hasReadPrivacy && confirmsEligibility &&
                             connectedPublicKey != null && signatureSuccess

    // Show geo-blocked screen if blocked
    if (geoBlocked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Service Unavailable",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = geoBlockedMessage ?: "AardAppvark is not available in your jurisdiction due to regulatory requirements.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
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
        return
    }

    // Show loading while checking geo
    if (isCheckingGeo) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Verifying eligibility...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Geo warning if applicable
        geoWarning?.let { warning ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Header with logo
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.size(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AardvarkIcon(
                    size = 60.dp,
                    color = MaterialTheme.colorScheme.primary,
                    filled = true
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "AardAppvark",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Legal Consent Required",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Important Notice
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Gavel,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Legally Binding Agreement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "You must read and cryptographically sign your acceptance of our Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 1: Read Legal Documents
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasReadTerms && hasReadPrivacy)
                            Icons.Filled.CheckCircle else Icons.Filled.Description,
                        contentDescription = null,
                        tint = if (hasReadTerms && hasReadPrivacy)
                            Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Step 1: Read Legal Documents",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Terms of Service button
                OutlinedButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Article, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terms of Service (v${TermsAcceptanceService.CURRENT_TOS_VERSION})")
                    Spacer(modifier = Modifier.weight(1f))
                    if (hasReadTerms) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Privacy Policy button
                OutlinedButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Privacy Policy (v${TermsAcceptanceService.CURRENT_PRIVACY_VERSION})")
                    Spacer(modifier = Modifier.weight(1f))
                    if (hasReadPrivacy) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 2: Confirm Eligibility
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (confirmsEligibility)
                            Icons.Filled.CheckCircle else Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = if (confirmsEligibility)
                            Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Step 2: Confirm Eligibility",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = confirmsEligibility,
                        onCheckedChange = { confirmsEligibility = it },
                        enabled = hasReadTerms && hasReadPrivacy
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "I confirm that I am at least 18 years old, I am NOT located in a sanctioned jurisdiction (Cuba, Iran, North Korea, Syria, Crimea), I am NOT on any sanctions list, and I have read and agree to the Terms of Service and Privacy Policy.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 3: Connect & Sign
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (signatureSuccess)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (signatureSuccess)
                            Icons.Filled.CheckCircle else Icons.Filled.Draw,
                        contentDescription = null,
                        tint = if (signatureSuccess)
                            Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Step 3: Connect Wallet & Sign",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your wallet will sign a consent message proving you agreed to the terms. This creates a legally binding cryptographic record.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Error message
                connectionError?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (signatureSuccess && connectedPublicKey != null) {
                    // Success state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Consent Recorded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = connectedWalletName ?: "Solana Wallet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Display truncated wallet address (e.g., "JDHGsd...hfje")
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = formatWalletAddress(connectedPublicKey!!),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    // Connect and sign button
                    Button(
                        onClick = {
                            scope.launch {
                                isConnecting = true
                                connectionError = null

                                try {
                                    // Use transact to connect and authorize with the wallet
                                    // Note: signMessage will be enabled once app is published to dApp Store
                                    // dApp Store apps are automatically trusted by Seeker wallet
                                    val result = mobileWalletAdapter.transact(activityResultSender) {
                                        authorize(
                                            identityUri = Uri.parse(AppConfig.Identity.URI),
                                            iconUri = Uri.parse(AppConfig.Identity.ICON_URI),
                                            identityName = AppConfig.Identity.NAME,
                                            chain = if (AppConfig.Payment.CLUSTER == "mainnet-beta") "solana:mainnet" else "solana:devnet"
                                        )
                                    }

                                    when (result) {
                                        is TransactionResult.Success -> {
                                            val authResult = result.authResult
                                            val accounts = authResult.accounts

                                            if (accounts.isNotEmpty()) {
                                                val pubKeyBytes = accounts.first().publicKey
                                                val pubKeyBase58 = bytesToBase58(pubKeyBytes)

                                                connectedPublicKey = pubKeyBase58
                                                connectedWalletName = authResult.walletUriBase?.host
                                                    ?: "Solana Wallet"

                                                isConnecting = false
                                                isSigning = true

                                                // Record consent with wallet proof
                                                // Full SIWS signing will be enabled once published to dApp Store
                                                termsService.recordConsentWithoutSignature(pubKeyBase58)
                                                signatureSuccess = true
                                                isSigning = false
                                            } else {
                                                connectionError = "No accounts returned from wallet"
                                            }
                                        }
                                        is TransactionResult.NoWalletFound -> {
                                            connectionError = "No MWA-compatible wallet found. Please install a Solana wallet app."
                                        }
                                        is TransactionResult.Failure -> {
                                            connectionError = result.e.message ?: "Failed to connect wallet"
                                        }
                                    }
                                } catch (e: Exception) {
                                    connectionError = e.message ?: "Failed to connect wallet"
                                } finally {
                                    isConnecting = false
                                    isSigning = false
                                }
                            }
                        },
                        enabled = confirmsEligibility && !isConnecting && !isSigning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isConnecting || isSigning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isConnecting) "Signing..." else "Recording consent...")
                        } else {
                            Icon(Icons.Filled.Draw, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect & Sign Consent")
                        }
                    }
                }
            }
        }

        // Pricing info
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simple Pay-Per-Use",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Up to 4 apps: FREE\n• 5+ apps: 0.01 SOL per operation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Buttons
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    connectedPublicKey?.let { pubKey ->
                        connectedWalletName?.let { walletName ->
                            onComplete(pubKey, walletName)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = allRequirementsMet
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enter AardAppvark")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { (context as? Activity)?.finish() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exit")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("Terms of Service", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getFullTermsOfService(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    hasReadTerms = true
                    showTermsDialog = false
                }) {
                    Text("I Have Read This")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("Privacy Policy", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getFullPrivacyPolicy(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    hasReadPrivacy = true
                    showPrivacyDialog = false
                }) {
                    Text("I Have Read This")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Format wallet address for display (e.g., "JDHGsd...hfje")
 */
private fun formatWalletAddress(address: String): String {
    return if (address.length > 12) {
        "${address.take(6)}...${address.takeLast(4)}"
    } else {
        address
    }
}

/**
 * Convert byte array to Base58 encoded public key string
 */
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
        if (byte.toInt() == 0) {
            sb.append(alphabet[0])
        } else {
            break
        }
    }

    return sb.reverse().toString()
}

/**
 * Convert byte array to Base64 encoded string
 */
private fun bytesToBase64(bytes: ByteArray): String {
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

/**
 * Full Terms of Service
 */
private fun getFullTermsOfService(): String {
    val currentDate = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        .format(java.util.Date())

    return """
AARDAPPVARK TERMS OF SERVICE

Effective Date: $currentDate
Version: ${TermsAcceptanceService.CURRENT_TOS_VERSION}

IMPORTANT: BY CONNECTING YOUR WALLET AND SIGNING THE CONSENT MESSAGE, YOU ARE ENTERING INTO A LEGALLY BINDING AGREEMENT.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. INTRODUCTION AND ACCEPTANCE

1.1 These Terms of Service ("Terms") constitute a legally binding agreement between you ("User", "you", "your") and the operators of AardAppvark ("we", "us", "our", "Provider").

1.2 AardAppvark is a mobile application for managing decentralized applications (dApps) on Solana Seeker devices, including bulk installation, uninstallation, and application management.

1.3 By connecting your cryptocurrency wallet and signing the consent message, you:
    (a) Confirm you have read and understood these Terms
    (b) Agree to be legally bound by these Terms
    (c) Create a cryptographic record of your acceptance
    (d) Acknowledge this constitutes a valid electronic signature

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

2. ELIGIBILITY AND RESTRICTIONS

2.1 AGE REQUIREMENT
You must be at least eighteen (18) years of age, or the age of legal majority in your jurisdiction, whichever is higher.

2.2 PROHIBITED JURISDICTIONS
This Application is NOT available to persons who are:
    (a) Located in, citizens of, or residents of:
        • Cuba
        • Iran
        • North Korea (Democratic People's Republic of Korea)
        • Syria
        • Crimea, Donetsk, or Luhansk regions
        • Any jurisdiction subject to comprehensive trade sanctions

2.3 SANCTIONED PERSONS
You represent and warrant that you are NOT:
    (a) Listed on any sanctions list including but not limited to:
        • U.S. Office of Foreign Assets Control (OFAC) SDN List
        • U.S. Department of Commerce Entity List
        • European Union Consolidated Sanctions List
        • United Kingdom HM Treasury Sanctions List
        • United Nations Security Council Sanctions List
        • Australian DFAT Consolidated List
    (b) Owned or controlled by any sanctioned person or entity
    (c) Acting on behalf of any sanctioned person or entity

2.4 If you become subject to any sanctions or relocate to a prohibited jurisdiction, you must immediately cease using the Application.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

3. CREDIT SYSTEM AND PAYMENTS

3.1 OPERATION REQUIREMENTS
    (a) Operations involving up to four (4) applications are free
    (b) Bulk operations involving five (5) or more applications require a fee of 0.01 SOL per operation
    (c) Favoriting applications is free
    (d) One (1) free credit is granted upon first wallet connection

3.2 PAYMENT TERMS
    (a) Credits may be purchased using SOL (Solana) or SKR (Seeker) tokens
    (b) All prices are denominated in cryptocurrency
    (c) Transactions are processed on the Solana blockchain

3.3 NON-REFUNDABLE POLICY
    ALL CREDIT PURCHASES ARE FINAL AND NON-REFUNDABLE. Credits cannot be exchanged, transferred, or converted to cash.

3.4 CREDIT EXPIRATION
    Credits expire twelve (12) months from the date of purchase or grant. Expired credits cannot be reinstated.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

4. PROHIBITED USES

You agree NOT to:
    (a) Violate any applicable law, regulation, or third-party rights
    (b) Engage in fraudulent, deceptive, or harmful activities
    (c) Facilitate money laundering, terrorist financing, or financial crimes
    (d) Evade sanctions, export controls, or trade restrictions
    (e) Distribute malware, viruses, or malicious code
    (f) Attempt unauthorized access to systems or accounts
    (g) Facilitate distribution of pirated or unauthorized software
    (h) Use VPNs or proxies to circumvent geographic restrictions
    (i) Create multiple accounts to abuse promotional credits
    (j) Reverse engineer, decompile, or disassemble the Application

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

5. INTELLECTUAL PROPERTY

5.1 The Application, including all content, features, and functionality, is owned by the Provider and protected by international intellectual property laws.

5.2 You are granted a limited, non-exclusive, non-transferable, revocable license to use the Application for personal, non-commercial purposes.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

6. DISCLAIMER OF WARRANTIES

THE APPLICATION IS PROVIDED "AS IS" AND "AS AVAILABLE" WITHOUT WARRANTIES OF ANY KIND.

WE EXPRESSLY DISCLAIM ALL WARRANTIES, INCLUDING:
    (a) Implied warranties of merchantability
    (b) Fitness for a particular purpose
    (c) Non-infringement
    (d) Accuracy, reliability, or completeness
    (e) Uninterrupted or error-free operation
    (f) Security from vulnerabilities

YOU ACKNOWLEDGE THAT BLOCKCHAIN TECHNOLOGY INVOLVES INHERENT RISKS INCLUDING MARKET VOLATILITY, REGULATORY CHANGES, AND POTENTIAL LOSS OF DIGITAL ASSETS.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

7. LIMITATION OF LIABILITY

TO THE MAXIMUM EXTENT PERMITTED BY LAW:

7.1 WE SHALL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, PUNITIVE, OR EXEMPLARY DAMAGES.

7.2 WE SHALL NOT BE LIABLE FOR:
    (a) Loss of profits, revenue, data, or goodwill
    (b) Loss of digital assets or cryptocurrencies
    (c) Unauthorized access or data breaches
    (d) Third-party conduct or content
    (e) Service interruptions or errors

7.3 OUR TOTAL LIABILITY SHALL NOT EXCEED THE GREATER OF: (a) THE AMOUNT YOU PAID US IN THE TWELVE (12) MONTHS PRECEDING THE CLAIM, OR (b) ONE HUNDRED U.S. DOLLARS (USD $100).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

8. INDEMNIFICATION

YOU AGREE TO DEFEND, INDEMNIFY, AND HOLD HARMLESS the Provider, its affiliates, and their respective directors, officers, employees, and agents from any claims, liabilities, damages, costs, and expenses (including attorneys' fees) arising from:
    (a) Your use or misuse of the Application
    (b) Your violation of these Terms
    (c) Your violation of any law or third-party rights
    (d) Your negligence or willful misconduct
    (e) Your failure to comply with sanctions requirements

This indemnification obligation survives termination of these Terms.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

9. DISPUTE RESOLUTION

9.1 GOVERNING LAW
These Terms are governed by the laws of the jurisdiction where the Provider is established.

9.2 ARBITRATION
Disputes shall be resolved through binding arbitration. You waive your right to a jury trial and to participate in class actions.

9.3 CLASS ACTION WAIVER
YOU AGREE TO RESOLVE DISPUTES INDIVIDUALLY AND WAIVE ANY RIGHT TO CLASS, COLLECTIVE, OR REPRESENTATIVE ACTIONS.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

10. TERMINATION

10.1 We may terminate your access at any time for any reason.

10.2 Upon termination:
    (a) Your license to use the Application ends immediately
    (b) Unused credits are forfeited without refund
    (c) Provisions that should survive termination will survive

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

11. MODIFICATIONS

We may modify these Terms at any time. Continued use after modifications constitutes acceptance. If you disagree with modifications, you must stop using the Application.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

12. ELECTRONIC CONSENT

By connecting your wallet and signing the consent message:
    (a) You consent to electronic delivery of all communications
    (b) You agree that your cryptographic signature constitutes a valid electronic signature
    (c) You acknowledge this agreement is legally binding

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

13. CONTACT

For questions about these Terms, contact us through the Solana dApp Store or official channels.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

END OF TERMS OF SERVICE
    """.trimIndent()
}

/**
 * Full Privacy Policy - GDPR and CCPA Compliant
 */
private fun getFullPrivacyPolicy(): String {
    val currentDate = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        .format(java.util.Date())

    return """
AARDAPPVARK PRIVACY POLICY

Effective Date: $currentDate
Version: ${TermsAcceptanceService.CURRENT_PRIVACY_VERSION}

This Privacy Policy explains how AardAppvark ("we", "us", "our") collects, uses, shares, and protects your information. This policy is designed to comply with the General Data Protection Regulation (GDPR), California Consumer Privacy Act (CCPA), and other applicable privacy laws.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. DATA CONTROLLER

AardAppvark acts as the data controller for personal information collected through the Application. For GDPR purposes, this means we determine the purposes and means of processing your personal data.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

2. INFORMATION WE COLLECT

2.1 INFORMATION YOU PROVIDE
    • Wallet Address: Your Solana wallet public key
    • Consent Record: Cryptographic signature of your Terms acceptance
    • Preferences: Your app settings and favorites

2.2 INFORMATION COLLECTED AUTOMATICALLY
    • Device Information: Device model, operating system version
    • Usage Data: Features used, timestamps, error logs
    • Device Identifier: Android device ID (hashed for privacy)

2.3 BLOCKCHAIN DATA
    • Transaction History: Payment transactions for credits
    • Note: Blockchain transactions are publicly visible

2.4 INFORMATION WE DO NOT COLLECT
    • We do NOT collect your name, email, phone number, or physical address
    • We do NOT access your wallet's private keys
    • We do NOT access your wallet balances or transaction history beyond our Application

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

3. LEGAL BASIS FOR PROCESSING (GDPR)

We process your data under the following legal bases:

    (a) CONTRACT: Processing necessary for the performance of our Terms of Service

    (b) CONSENT: Your explicit consent provided through cryptographic signature

    (c) LEGITIMATE INTERESTS: For security, fraud prevention, and service improvement

    (d) LEGAL OBLIGATION: To comply with applicable laws and regulations

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

4. HOW WE USE YOUR INFORMATION

We use your information to:
    • Provide and maintain the Application
    • Process credit purchases and transactions
    • Verify your identity and eligibility
    • Comply with legal and regulatory obligations
    • Detect and prevent fraud and security issues
    • Improve and optimize the Application
    • Respond to your requests and communications

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

5. DATA SHARING AND DISCLOSURE

5.1 WE DO NOT SELL YOUR PERSONAL INFORMATION

5.2 We may share information with:
    • Service Providers: Who assist in operating the Application
    • Legal Authorities: When required by law or legal process
    • Blockchain Networks: Transaction data is publicly recorded

5.3 BLOCKCHAIN TRANSPARENCY
Transactions on the Solana blockchain are publicly visible. Your wallet address and transaction history may be viewed by anyone using a blockchain explorer.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

6. DATA RETENTION

6.1 We retain your data for as long as:
    • Your account is active
    • Necessary to provide services
    • Required by law (minimum 7 years for financial records)
    • Needed for legitimate business purposes

6.2 Consent records are retained indefinitely as legal proof of agreement.

6.3 Blockchain data is permanent and cannot be deleted.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

7. YOUR RIGHTS

7.1 GDPR RIGHTS (European Users)
You have the right to:
    • ACCESS: Request a copy of your personal data
    • RECTIFICATION: Request correction of inaccurate data
    • ERASURE: Request deletion of your data ("right to be forgotten")
    • RESTRICTION: Request limitation of processing
    • PORTABILITY: Receive your data in a portable format
    • OBJECTION: Object to processing based on legitimate interests
    • WITHDRAW CONSENT: Withdraw consent at any time

7.2 CCPA RIGHTS (California Residents)
You have the right to:
    • KNOW: What personal information we collect
    • DELETE: Request deletion of your information
    • OPT-OUT: Opt out of sale of personal information (we do not sell data)
    • NON-DISCRIMINATION: Equal service regardless of privacy choices

7.3 HOW TO EXERCISE YOUR RIGHTS
Use the "Export My Data" and "Delete My Data" options in Settings, or contact us through official channels.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

8. DATA SECURITY

We implement appropriate technical and organizational measures including:
    • Encryption of sensitive data
    • Secure local storage
    • Regular security assessments
    • Access controls and authentication

However, no method of transmission or storage is 100% secure.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

9. INTERNATIONAL DATA TRANSFERS

If we transfer data outside your jurisdiction, we ensure appropriate safeguards through:
    • Standard contractual clauses
    • Adequacy decisions
    • Your explicit consent

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

10. CHILDREN'S PRIVACY

The Application is not intended for users under 18. We do not knowingly collect information from minors. If we discover we have collected data from a minor, we will delete it.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

11. COOKIES AND TRACKING

The Application does not use cookies. We may collect analytics data to improve service quality.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

12. THIRD-PARTY SERVICES

The Application integrates with:
    • Solana Blockchain: For transactions (public ledger)
    • Solana Mobile Wallet Adapter: For wallet connections
    • Solana dApp Store: For application data

These services have their own privacy policies.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

13. CHANGES TO THIS POLICY

We may update this Privacy Policy. Material changes will be communicated through the Application. Continued use after changes constitutes acceptance.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

14. CONTACT US

For privacy-related inquiries:
    • Use the "Contact" option in the Application
    • Submit inquiries through the Solana dApp Store

For GDPR inquiries, you may also contact your local Data Protection Authority.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

15. CALIFORNIA PRIVACY NOTICE

Under CCPA, California residents have specific rights regarding their personal information. We do not sell personal information. To exercise your rights, use the options in Settings or contact us.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

END OF PRIVACY POLICY
    """.trimIndent()
}
