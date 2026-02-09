package com.adappvark.toolkit.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.data.UninstallHistory
import com.adappvark.toolkit.data.model.DAppFilter
import com.adappvark.toolkit.service.PackageManagerService
import com.adappvark.toolkit.service.UserPreferencesManager
import com.adappvark.toolkit.ui.components.AardvarkIcon
@Composable
fun HomeScreen(
    onDisconnectWallet: () -> Unit = {}
) {
    val context = LocalContext.current
    val packageService = remember { PackageManagerService(context) }
    val uninstallHistory = remember { UninstallHistory(context) }
    val userPrefs = remember { UserPreferencesManager(context) }

    var isWalletConnected by remember { mutableStateOf(userPrefs.isWalletConnected()) }
    var walletAddress by remember { mutableStateOf(userPrefs.getShortWalletAddress()) }
    var fullWalletAddress by remember { mutableStateOf(userPrefs.getWalletPublicKey()) }
    var walletName by remember { mutableStateOf(userPrefs.getWalletName()) }
    var dAppCount by remember { mutableStateOf(0) }
    var totalStorageMB by remember { mutableStateOf(0L) }
    var uninstallCount by remember { mutableStateOf(0) }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    // Load stats
    LaunchedEffect(Unit) {
        val dApps = packageService.scanInstalledApps(filter = DAppFilter.DAPP_STORE_ONLY)
        dAppCount = dApps.size
        totalStorageMB = dApps.sumOf { it.sizeInBytes } / (1024 * 1024)
        uninstallCount = uninstallHistory.getHistory().size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome Section
        Spacer(modifier = Modifier.height(24.dp))

        // App Icon - Custom Aardvark
        Card(
            modifier = Modifier.size(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AardvarkIcon(
                    size = 90.dp,
                    color = MaterialTheme.colorScheme.primary,
                    filled = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to AardAppvark!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "First in line. Every dApp, every time.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Connected Wallet Card
        if (isWalletConnected && walletAddress != null) {
            WalletInfoCard(
                walletAddress = walletAddress!!,
                walletName = walletName ?: "Solana Wallet",
                onDisconnect = { showDisconnectDialog = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Setup Checklist
        SetupChecklistCard(
            title = "Setup Checklist",
            items = listOf(
                SetupItem(
                    "Wallet Connected",
                    isWalletConnected,
                    if (isWalletConnected && walletAddress != null) walletAddress!! else "Connect Solana wallet"
                )
            ),
            onConnectWallet = {
                // Wallet is already connected during onboarding
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats Card
        QuickStatsCard(
            dAppCount = dAppCount,
            storageMB = totalStorageMB,
            uninstallCount = uninstallCount
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Features Overview
        FeaturesCard()
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
                    "This will disconnect your wallet from AardAppvark. You will need to reconnect and re-accept the Terms of Service to continue using the app."
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
fun SetupChecklistCard(
    title: String,
    items: List<SetupItem>,
    onConnectWallet: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { item ->
                SetupItemRow(
                    item = item,
                    onClick = {
                        when (item.title) {
                            "Connect Wallet" -> onConnectWallet()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SetupItemRow(item: SetupItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isComplete) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (item.isComplete)
                Color(0xFF4CAF50) // Green checkmark
            else
                MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!item.isComplete) {
            TextButton(onClick = onClick) {
                Text("Setup")
            }
        }
    }
}

@Composable
fun QuickStatsCard(
    dAppCount: Int = 0,
    storageMB: Long = 0,
    uninstallCount: Int = 0
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                    label = "Uninstalled",
                    value = uninstallCount.toString(),
                    sublabel = "total"
                )
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
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        if (sublabel != null) {
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun FeaturesCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureRow(Icons.Filled.Delete, "Bulk Uninstall", "Clean up dApps in seconds")
            FeatureRow(Icons.Filled.Download, "Batch Reinstall", "Restore your dApp collection")
            FeatureRow(Icons.Filled.AccountBalanceWallet, "Wallet Login", "Connect with Solana wallet")
        }
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
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
fun WalletInfoCard(
    walletAddress: String,
    walletName: String,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wallet icon
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Wallet info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = walletName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = walletAddress,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Disconnect button
            IconButton(
                onClick = onDisconnect
            ) {
                Icon(
                    imageVector = Icons.Filled.LinkOff,
                    contentDescription = "Disconnect Wallet",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
