package com.adappvark.toolkit.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.adappvark.toolkit.data.UninstallHistory
import com.adappvark.toolkit.data.ProtectedApps
import com.adappvark.toolkit.data.model.DAppFilter
import com.adappvark.toolkit.service.AppSettingsManager
import com.adappvark.toolkit.service.CreditManager
import com.adappvark.toolkit.service.PackageManagerService
import com.adappvark.toolkit.service.SeekerVerificationService
import com.adappvark.toolkit.service.UserPreferencesManager
import com.adappvark.toolkit.ui.components.AnimatedAardvarkIcon
import com.adappvark.toolkit.ui.theme.*

@Composable
fun HomeScreen(
    onDisconnectWallet: () -> Unit = {}
) {
    val context = LocalContext.current
    val packageService = remember { PackageManagerService(context) }
    val uninstallHistory = remember { UninstallHistory(context) }
    val userPrefs = remember { UserPreferencesManager(context) }
    val seekerVerifier = remember { SeekerVerificationService(context) }
    val creditManager = remember { CreditManager(context) }
    val appSettings = remember { AppSettingsManager(context) }

    var isWalletConnected by remember { mutableStateOf(userPrefs.isWalletConnected()) }
    var walletAddress by remember { mutableStateOf(userPrefs.getShortWalletAddress()) }
    var fullWalletAddress by remember { mutableStateOf(userPrefs.getWalletPublicKey()) }
    var walletName by remember { mutableStateOf(userPrefs.getWalletName()) }
    val protectedApps = remember { ProtectedApps(context) }

    var dAppCount by remember { mutableStateOf(0) }
    var totalStorageMB by remember { mutableStateOf(0L) }
    var uninstallCount by remember { mutableStateOf(0) }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    // Analytics state
    var reinstalledCount by remember { mutableStateOf(0) }
    var pendingReinstallCount by remember { mutableStateOf(0) }
    var storageRecoveredMB by remember { mutableStateOf(0L) }
    var favouriteCount by remember { mutableStateOf(0) }
    var reinstallRate by remember { mutableStateOf(0f) }

    // Seeker verification state
    val isVerifiedSeeker = remember { seekerVerifier.isVerifiedSeeker() }
    val sgtMemberNumber = remember { seekerVerifier.getSgtMemberNumber() }

    // Load stats
    LaunchedEffect(Unit) {
        val dApps = packageService.scanInstalledApps(filter = DAppFilter.DAPP_STORE_ONLY)
        dAppCount = dApps.size
        totalStorageMB = dApps.sumOf { it.sizeInBytes } / (1024 * 1024)

        val history = uninstallHistory.getHistory()
        uninstallCount = history.size
        reinstalledCount = history.count { it.reinstalled }
        pendingReinstallCount = history.count { !it.reinstalled && !it.skipReinstall }
        storageRecoveredMB = history.filter { !it.reinstalled }.sumOf { it.sizeInBytes } / (1024 * 1024)
        favouriteCount = protectedApps.getProtectedPackages().size
        reinstallRate = if (history.isNotEmpty()) {
            (reinstalledCount.toFloat() / history.size) * 100f
        } else 0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero: Animated Aardvark icon with glow
        AnimatedAardvarkIcon(
            size = 100.dp,
            glowColor = SolanaPurple
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "AardAppvark",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SolanaPurple,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "First in line. Every dApp, every time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Editor's Choice / Seeker Toolkit badge
        GlassCard(
            glowColor = SolanaGreenGlow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = SolanaGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Seeker Toolkit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SolanaGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Built for Solana Seeker",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connected Wallet Card
        if (isWalletConnected && walletAddress != null) {
            WalletInfoCard(
                walletAddress = walletAddress!!,
                walletName = walletName ?: "Solana Wallet",
                onDisconnect = { showDisconnectDialog = true }
            )

            // Seeker Verification Badge
            if (isVerifiedSeeker) {
                Spacer(modifier = Modifier.height(8.dp))
                SeekerVerifiedCard(sgtMemberNumber = sgtMemberNumber)
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else if (userPrefs.hasSkippedWallet()) {
            // No wallet — show connect CTA
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No Wallet Connected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Connect Seeker Genesis Token for free full functionality",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onDisconnectWallet() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolanaPurple
                    )
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Wallet")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Analytics Dashboard
        AnalyticsDashboardCard(
            dAppCount = dAppCount,
            storageMB = totalStorageMB,
            uninstallCount = uninstallCount,
            reinstalledCount = reinstalledCount,
            pendingReinstallCount = pendingReinstallCount,
            storageRecoveredMB = storageRecoveredMB,
            favouriteCount = favouriteCount,
            reinstallRate = reinstallRate
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Features Overview
        FeaturesCard()

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Disconnect Wallet Confirmation Dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Disconnect Wallet?") },
            text = {
                Text(
                    "This will disconnect your wallet from AardAppvark. You will need to reconnect and re-accept the Terms of Service to continue using the dApp."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        userPrefs.disconnectWallet()
                        showDisconnectDialog = false
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
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AnalyticsDashboardCard(
    dAppCount: Int = 0,
    storageMB: Long = 0,
    uninstallCount: Int = 0,
    reinstalledCount: Int = 0,
    pendingReinstallCount: Int = 0,
    storageRecoveredMB: Long = 0,
    favouriteCount: Int = 0,
    reinstallRate: Float = 0f
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = SolanaPurpleGlow
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Analytics,
                contentDescription = null,
                tint = SolanaPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Analytics Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top row: Installed / Storage Used / Favourites
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Installed",
                value = dAppCount.toString(),
                sublabel = "dApps"
            )
            StatItem(
                label = "Storage",
                value = formatStorage(storageMB),
                sublabel = "used"
            )
            StatItem(
                label = "Favourites",
                value = favouriteCount.toString(),
                sublabel = "protected"
            )
        }

        if (uninstallCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))

            GlassDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: Uninstalled / Reinstalled / Recovered
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Uninstalled",
                    value = uninstallCount.toString(),
                    sublabel = "total"
                )
                StatItem(
                    label = "Reinstalled",
                    value = reinstalledCount.toString(),
                    sublabel = "${reinstallRate.toInt()}% rate"
                )
                StatItem(
                    label = "Recovered",
                    value = formatStorage(storageRecoveredMB),
                    sublabel = "freed"
                )
            }

            // Pending reinstall indicator
            if (pendingReinstallCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SolanaPurple.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pending,
                            contentDescription = null,
                            tint = SolanaPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$pendingReinstallCount dApp${if (pendingReinstallCount != 1) "s" else ""} available for reinstall",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SolanaPurple
                        )
                    }
                }
            }
        }
    }
}

private fun formatStorage(mb: Long): String {
    return when {
        mb >= 1024 -> "${mb / 1024} GB"
        else -> "$mb MB"
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    sublabel: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SolanaGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (sublabel != null) {
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun FeaturesCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Stars,
                contentDescription = null,
                tint = SolanaPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        HomeFeatureRow(Icons.Filled.Delete, "Bulk Uninstall", "Clean up dApps in seconds", SolanaPurple)
        HomeFeatureRow(Icons.Filled.Download, "Batch Reinstall", "Restore your dApp collection", SolanaGreen)
        HomeFeatureRow(Icons.Filled.AutoMode, "Auto-Accept", "Skip system dialogs automatically", AardvarkTan)
        HomeFeatureRow(Icons.Filled.AccountBalanceWallet, "Wallet Login", "Sign in with Solana (SIWS)", SolanaPurple)
    }
}

@Composable
fun HomeFeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, tint: Color = SolanaPurple) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class SetupItem(
    val title: String,
    val isComplete: Boolean,
    val description: String
)

@Composable
fun SeekerVerifiedCard(sgtMemberNumber: Long?) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = SolanaGreenGlow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Verified Seeker",
                tint = SolanaPurple,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Verified Seeker",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SolanaPurple
                    )
                    sgtMemberNumber?.let { num ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "#$num",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SolanaGreen
                        )
                    }
                }
                Text(
                    text = "SGT holder — dApp 100% unlocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Green check badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SolanaGreen.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = SolanaGreen,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
fun WalletInfoCard(
    walletAddress: String,
    walletName: String,
    onDisconnect: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = SolanaPurpleGlow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wallet icon with glow ring
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = SolanaPurple.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = SolanaPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Wallet info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = walletName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = walletAddress,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SolanaPurple
                )
            }

            // Connected indicator
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Connected",
                tint = SolanaGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
