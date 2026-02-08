package com.adappvark.toolkit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.BuildConfig
import com.adappvark.toolkit.service.TermsAcceptanceService
import com.adappvark.toolkit.service.UserPreferencesManager
import com.adappvark.toolkit.util.AccessibilityHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDisconnectWallet: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesManager(context) }
    val termsService = remember { TermsAcceptanceService(context) }

    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showConsentRecordDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDisconnectWalletDialog by remember { mutableStateOf(false) }

    // Check accessibility status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = AccessibilityHelper.isAccessibilityServiceEnabled(context)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Wallet Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Wallet",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (userPrefs.isWalletConnected()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = userPrefs.getWalletName() ?: "Connected Wallet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = userPrefs.getShortWalletAddress() ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Connected",
                            tint = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // T&C acceptance info
                    userPrefs.getAcceptanceDateFormatted()?.let { date ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Terms accepted: $date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Consent record info
                    val consentRecord = termsService.getConsentRecord()
                    if (consentRecord != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showConsentRecordDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Consent Record")
                        }
                    }

                    // Disconnect wallet button
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDisconnectWalletDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.LinkOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disconnect Wallet")
                    }
                } else {
                    Text(
                        text = "No wallet connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GDPR / Privacy Rights Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your Privacy Rights",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "GDPR (EU) and CCPA (California) compliant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Export My Data Button
                OutlinedButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export My Data")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Delete My Data Button
                OutlinedButton(
                    onClick = { showDeleteDataDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete My Data")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility Status Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Accessibility Service",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Uninstall/Reinstall")
                    StatusIndicator(isAccessibilityEnabled)
                }

                if (!isAccessibilityEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            AccessibilityHelper.openAccessibilitySettings(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Accessibility")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legal Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Legal",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                ListItem(
                    headlineContent = { Text("Terms of Service") },
                    supportingContent = { Text("v${TermsAcceptanceService.CURRENT_TOS_VERSION}") },
                    leadingContent = {
                        Icon(Icons.Filled.Description, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTermsDialog = true }
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Privacy Policy") },
                    supportingContent = { Text("v${TermsAcceptanceService.CURRENT_PRIVACY_VERSION}") },
                    leadingContent = {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyDialog = true }
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Open Source Licenses") },
                    supportingContent = { Text("Third-party libraries") },
                    leadingContent = {
                        Icon(Icons.Filled.Code, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLicensesDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "About AardAppvark",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME
                )

                SettingsItem(
                    icon = Icons.Filled.Fingerprint,
                    title = "Build",
                    subtitle = BuildConfig.VERSION_CODE.toString()
                )

                SettingsItem(
                    icon = Icons.Filled.Android,
                    title = "Package",
                    subtitle = BuildConfig.APPLICATION_ID
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Branding
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AardAppvark",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "First in line. Every dApp, every time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Built for Solana Seeker",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Consent Record Dialog
    if (showConsentRecordDialog) {
        val consentRecord = termsService.getConsentRecord()
        AlertDialog(
            onDismissRequest = { showConsentRecordDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Verified,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Consent Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (consentRecord != null) {
                        ConsentInfoRow("Status", if (consentRecord.isSigned()) "Cryptographically Signed" else "Recorded")
                        ConsentInfoRow("Wallet", consentRecord.walletAddress.take(8) + "..." + consentRecord.walletAddress.takeLast(8))
                        ConsentInfoRow("Date", consentRecord.getFormattedTimestamp())
                        ConsentInfoRow("Terms Version", consentRecord.tosVersion)
                        ConsentInfoRow("Privacy Version", consentRecord.privacyVersion)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "This record proves you agreed to our Terms of Service and Privacy Policy. It is stored locally on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showConsentRecordDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Export Data Dialog
    if (showExportDialog) {
        val exportData = termsService.exportConsentRecordAsJson()
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = {
                Icon(Icons.Filled.Download, contentDescription = null)
            },
            title = { Text("Export My Data", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Your data export (GDPR Article 20 - Data Portability):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = exportData,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "This includes all personal data we have stored about you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Copy to clipboard
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("AardAppvark Data Export", exportData)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Data copied to clipboard", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Data Dialog
    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete My Data", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "GDPR Article 17 - Right to Erasure",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This will delete all locally stored data including:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("• Consent records", style = MaterialTheme.typography.bodySmall)
                    Text("• Wallet connection data", style = MaterialTheme.typography.bodySmall)
                    Text("• App preferences", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "IMPORTANT:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "• Blockchain transactions cannot be deleted\n• You will need to re-accept Terms to use the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDataDialog = false
                        showDeleteConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete My Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Final Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Are you absolutely sure?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This action cannot be undone. All your data will be permanently deleted and you will need to re-accept the Terms of Service to continue using AardAppvark.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Delete all data
                        termsService.deleteAllConsentData()
                        userPrefs.resetAll()

                        Toast.makeText(context, "All data deleted. Please restart the app.", Toast.LENGTH_LONG).show()
                        showDeleteConfirmDialog = false

                        // In production, you would restart the app or navigate to onboarding
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getTermsOfServiceSummary(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getPrivacyPolicyText(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getOpenSourceLicensesText(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Disconnect Wallet Dialog
    if (showDisconnectWalletDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectWalletDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Disconnect Wallet?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "This will disconnect your wallet from AardAppvark.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "You will need to reconnect and re-accept the Terms of Service to continue using the app.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userPrefs.disconnectWallet()
                        showDisconnectWalletDialog = false
                        onDisconnectWallet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectWalletDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ConsentInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatusIndicator(isActive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isActive) "Enabled" else "Disabled",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getTermsOfServiceSummary(): String {
    return """
AARDAPPVARK TERMS OF SERVICE
Summary Version (v${TermsAcceptanceService.CURRENT_TOS_VERSION})

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

IMPORTANT: By using AardAppvark, you agree to be bound by our full Terms of Service which you accepted during onboarding.

KEY TERMS:

1. ELIGIBILITY
• Must be 18+ years of age
• Not located in sanctioned jurisdictions (Cuba, Iran, North Korea, Syria, Crimea)
• Not on any OFAC, EU, UK, UN, or Australian sanctions lists

2. PAYMENTS
• Operations with up to 4 apps are FREE
• Bulk operations (5+ apps) require 0.01 SOL (or 1 SKR)
• One-time fee for unlimited apps per operation
• All payments are processed on-chain

3. PROHIBITED USES
• No illegal activities in any jurisdiction
• No money laundering or terrorist financing
• No malware distribution
• No circumventing app store terms
• No VPN use to bypass restrictions
• No account abuse

4. LIABILITY
• App provided "AS IS" without warranties
• We are not liable for data loss, fund loss, or damages
• Maximum liability capped at $100 or 12-month payments

5. INDEMNIFICATION
You agree to defend, indemnify, and hold harmless AardAppvark from any claims arising from your use or misuse of the application.

6. DISPUTE RESOLUTION
• Binding arbitration for disputes
• Class action waiver
• Jury trial waiver

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Your consent was cryptographically recorded with your wallet address during onboarding.
    """.trimIndent()
}

private fun getPrivacyPolicyText(): String {
    return """
AARDAPPVARK PRIVACY POLICY
Version ${TermsAcceptanceService.CURRENT_PRIVACY_VERSION}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. INFORMATION WE COLLECT

• Wallet Address: Your Solana public key
• Consent Record: Proof of Terms acceptance
• Credit Transactions: Purchase and usage history
• Device ID: Hashed for privacy
• Usage Data: App operations performed

We DO NOT collect: names, emails, locations, private keys, or browsing history.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

2. LEGAL BASIS (GDPR)

• Contract: To provide our services
• Consent: Your explicit agreement
• Legitimate Interest: Security and fraud prevention
• Legal Obligation: Regulatory compliance

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

3. DATA SHARING

We DO NOT sell your data.

We may share with:
• Blockchain: Public transaction data
• Law Enforcement: If legally required
• Service Providers: Under strict confidentiality

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

4. YOUR RIGHTS

GDPR (EU): Access, rectification, erasure, portability, objection
CCPA (California): Know, delete, opt-out, non-discrimination

Use "Export My Data" and "Delete My Data" in Settings.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

5. DATA RETENTION

• Consent records: Retained as legal proof
• Credits: Until expiration (12 months)
• Blockchain data: Permanent (cannot be deleted)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

6. BLOCKCHAIN TRANSPARENCY

All Solana transactions are publicly visible and permanent.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Contact: Submit inquiries through the Solana dApp Store.
    """.trimIndent()
}

private fun getOpenSourceLicensesText(): String {
    return """
OPEN SOURCE LICENSES

AardAppvark uses the following open source components:

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ANDROID JETPACK
Copyright Google LLC
Apache License 2.0

• Jetpack Compose UI
• Navigation
• Activity
• Core KTX
• Lifecycle

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

KOTLIN
Copyright JetBrains s.r.o.
Apache License 2.0

• Kotlin Standard Library
• Kotlin Coroutines
• Kotlin Serialization

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

MATERIAL DESIGN 3
Copyright Google LLC
Apache License 2.0

• Material3 Components
• Material Icons

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

SOLANA MOBILE
Copyright Solana Mobile Inc.
Apache License 2.0

• Mobile Wallet Adapter Client

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

APACHE LICENSE 2.0

Licensed under the Apache License, Version 2.0.

http://www.apache.org/licenses/LICENSE-2.0
    """.trimIndent()
}
