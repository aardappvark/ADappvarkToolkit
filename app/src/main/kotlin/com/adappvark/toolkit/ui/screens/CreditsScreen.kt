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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.config.CreditState
import com.adappvark.toolkit.service.CreditManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreditsScreen() {
    val context = LocalContext.current

    val creditManager = remember { CreditManager(context) }
    var creditState by remember { mutableStateOf(creditManager.getCreditState()) }

    // Refresh credit state
    LaunchedEffect(Unit) {
        creditState = creditManager.getCreditState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Current Credit Balance Card
        CreditBalanceCard(
            creditState = creditState,
            expirationWarning = creditManager.getExpirationMessage()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // How It Works Card
        HowCreditsWorkCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Info Card
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
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Payment Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Up to 4 apps per operation: FREE\n" +
                           "• 5+ apps: 0.01 SOL per operation\n" +
                           "• Accepts SOL and SKR (Seeker) tokens\n" +
                           "• Payments via Solana Mobile Wallet\n" +
                           "• All payments are non-refundable\n" +
                           "• Verify transactions on Solscan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun CreditBalanceCard(
    creditState: CreditState,
    expirationWarning: String? = null
) {
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
                text = "Your Credits",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = creditState.balance.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (creditState.balance > 0 && creditState.expiresAt != null) {
                Spacer(modifier = Modifier.height(8.dp))

                val expiryDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(creditState.expiresAt))

                Text(
                    text = "Valid until $expiryDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            if (expirationWarning != null) {
                Spacer(modifier = Modifier.height(8.dp))
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
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = expirationWarning,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HowCreditsWorkCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How It Works",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CreditInfoRow(
                icon = Icons.Filled.CheckCircle,
                text = "Up to 4 apps per operation: FREE"
            )
            CreditInfoRow(
                icon = Icons.Filled.Payment,
                text = "5+ apps: 0.01 SOL per bulk operation"
            )
            CreditInfoRow(
                icon = Icons.Filled.Star,
                text = "Favouriting apps is free"
            )
            CreditInfoRow(
                icon = Icons.Filled.CardGiftcard,
                text = "1 FREE credit on first wallet connect"
            )
            CreditInfoRow(
                icon = Icons.Filled.Schedule,
                text = "Credits expire 12 months after purchase"
            )
        }
    }
}

@Composable
fun CreditInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
