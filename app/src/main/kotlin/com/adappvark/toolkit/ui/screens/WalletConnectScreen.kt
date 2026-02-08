package com.adappvark.toolkit.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.core.content.ContextCompat
import com.adappvark.toolkit.ui.components.AardvarkIcon

/**
 * Data class representing a wallet app
 */
data class WalletApp(
    val name: String,
    val packageName: String,
    val iconColor: Color,
    val isSeeker: Boolean = false,  // Seeker wallet gets priority
    val isInstalled: Boolean = false
)

/**
 * Wallet connection screen with Solana Mobile Wallet Adapter
 */
@Composable
fun WalletConnectScreen(
    onWalletConnected: (publicKey: String, walletName: String) -> Unit,
    onSkip: () -> Unit = {}
) {
    val context = LocalContext.current

    // List of known Solana wallets (Seeker first as priority)
    val knownWallets = remember {
        listOf(
            WalletApp("Seeker Wallet", "com.solanamobile.seedvault", Color(0xFF9945FF), isSeeker = true),
            WalletApp("Phantom", "app.phantom", Color(0xFF9945FF)),
            WalletApp("Solflare", "com.solflare.mobile", Color(0xFFFC8C03)),
            WalletApp("Backpack", "app.backpack", Color(0xFFE33E3F)),
            WalletApp("Glow", "com.luma.wallet.prod", Color(0xFF00D18C)),
            WalletApp("Slope", "com.orca.slope", Color(0xFF6E56CF)),
            WalletApp("Exodus", "exodusmovement.exodus", Color(0xFF1F2128)),
            WalletApp("Trust Wallet", "com.wallet.crypto.trustapp", Color(0xFF3375BB)),
            WalletApp("Brave Wallet", "com.brave.browser", Color(0xFFFF5500)),
            WalletApp("Ultimate", "finance.ultimate.app", Color(0xFF14F195)),
            WalletApp("Jupiter", "exchange.jup.app", Color(0xFF14F195))
        )
    }

    // Check which wallets are installed
    val installedWallets = remember(knownWallets) {
        knownWallets.map { wallet ->
            try {
                context.packageManager.getPackageInfo(wallet.packageName, 0)
                wallet.copy(isInstalled = true)
            } catch (e: PackageManager.NameNotFoundException) {
                wallet.copy(isInstalled = false)
            }
        }.filter { it.isInstalled }
            .sortedByDescending { it.isSeeker } // Seeker first
    }

    var selectedWallet by remember { mutableStateOf<WalletApp?>(null) }
    var connectedPublicKey by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Continue regardless of permission result
        connectedPublicKey?.let { pubKey ->
            selectedWallet?.let { wallet ->
                onWalletConnected(pubKey, wallet.name)
            }
        }
    }

    // MWA activity launcher
    val mwaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* MWA handles result internally */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header
        if (connectedPublicKey == null) {
            // App Logo
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Connect Your Wallet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect with a Solana wallet to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Free credit bonus notice
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Get 1 FREE credit on first connect!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error message
            connectionError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Wallet list
            if (installedWallets.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Wallets Found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please install a Solana wallet app from the dApp Store",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Available Wallets",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedWallets) { wallet ->
                        WalletCard(
                            wallet = wallet,
                            isConnecting = isConnecting && selectedWallet == wallet,
                            onClick = {
                                if (!isConnecting) {
                                    selectedWallet = wallet
                                    isConnecting = true
                                    connectionError = null

                                    // Simulate wallet connection
                                    // In production, use Solana Mobile Wallet Adapter
                                    // TODO: Integrate actual MWA
                                    simulateWalletConnect(
                                        wallet = wallet,
                                        onSuccess = { pubKey ->
                                            connectedPublicKey = pubKey
                                            isConnecting = false
                                        },
                                        onError = { error ->
                                            connectionError = error
                                            isConnecting = false
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Connected state - show public key and login button
            Spacer(modifier = Modifier.height(48.dp))

            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Wallet Connected!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Wallet info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectedWallet?.name ?: "Wallet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Shortened public key
                    val shortKey = connectedPublicKey?.let { key ->
                        if (key.length > 10) "${key.take(5)}...${key.takeLast(4)}"
                        else key
                    } ?: ""

                    Text(
                        text = shortKey,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Public Key",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login button
            Button(
                onClick = {
                    // Check if we need notification permission (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            showNotificationDialog = true
                            return@Button
                        }
                    }

                    // Proceed to app
                    connectedPublicKey?.let { pubKey ->
                        selectedWallet?.let { wallet ->
                            onWalletConnected(pubKey, wallet.name)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Login,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Change wallet button
            TextButton(
                onClick = {
                    connectedPublicKey = null
                    selectedWallet = null
                }
            ) {
                Text("Use different wallet")
            }
        }
    }

    // Notification permission dialog
    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Enable Notifications") },
            text = {
                Text(
                    "Would you like to receive notifications about your dApp operations and credit balance?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationDialog = false
                        // Continue without notifications
                        connectedPublicKey?.let { pubKey ->
                            selectedWallet?.let { wallet ->
                                onWalletConnected(pubKey, wallet.name)
                            }
                        }
                    }
                ) {
                    Text("Not now")
                }
            }
        )
    }
}

@Composable
fun WalletCard(
    wallet: WalletApp,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting) { onClick() },
        border = if (wallet.isSeeker) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        colors = CardDefaults.cardColors(
            containerColor = if (wallet.isSeeker)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wallet icon placeholder (colored circle)
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = wallet.iconColor.copy(alpha = 0.2f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = wallet.iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (wallet.isSeeker) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text("Recommended")
                        }
                    }
                }
                Text(
                    text = "Tap to connect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Simulate wallet connection for demo purposes
 * In production, replace with actual Solana Mobile Wallet Adapter integration
 */
private fun simulateWalletConnect(
    wallet: WalletApp,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    // Simulate async connection
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        // Generate a fake public key for demo
        val fakePublicKey = buildString {
            val chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
            repeat(44) {
                append(chars.random())
            }
        }
        onSuccess(fakePublicKey)
    }, 1500)
}
