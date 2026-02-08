package com.adappvark.toolkit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.data.model.SubscriptionPlan
import com.adappvark.toolkit.data.model.SubscriptionStatus
import com.adappvark.toolkit.service.SolanaPaymentManager
import com.adappvark.toolkit.service.SubscriptionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SubscriptionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val subscriptionManager = remember { SubscriptionManager(context) }
    var subscriptionStatus by remember { mutableStateOf<SubscriptionStatus?>(null) }
    
    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastTransactionSig by remember { mutableStateOf<String?>(null) }
    
    // Activity result launcher for MWA
    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* MWA handles result internally */ }

    val paymentManager = remember {
        SolanaPaymentManager(context, activityLauncher)
    }
    
    // Load subscription status
    LaunchedEffect(Unit) {
        subscriptionManager.checkExpiration()
        subscriptionStatus = subscriptionManager.getSubscriptionStatus()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Current Subscription Status
        subscriptionStatus?.let { status ->
            if (status.isValid()) {
                CurrentSubscriptionCard(status = status)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        Text(
            text = if (subscriptionStatus?.isValid() == true) "Upgrade or Renew" else "Choose Your Plan",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Weekly subscriptions for professional dApp management",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Payment error display
        if (paymentError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Payment Failed",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            paymentError ?: "Unknown error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Subscription Plans
        SubscriptionPlan.values().forEach { plan ->
            SubscriptionPlanCard(
                plan = plan,
                isCurrentPlan = subscriptionStatus?.plan == plan && subscriptionStatus?.isValid() == true,
                isProcessing = isProcessingPayment,
                onSubscribe = {
                    scope.launch {
                        isProcessingPayment = true
                        paymentError = null
                        
                        try {
                            val result = paymentManager.requestSubscriptionPayment(plan)
                            
                            if (result.isSuccess) {
                                val paymentResult = result.getOrNull()!!
                                lastTransactionSig = paymentResult.transactionSignature
                                
                                // Activate subscription
                                subscriptionManager.activateSubscription(
                                    plan = plan,
                                    transactionId = paymentResult.transactionSignature
                                )
                                
                                // Refresh status
                                subscriptionStatus = subscriptionManager.getSubscriptionStatus()
                                showSuccessDialog = true
                            } else {
                                paymentError = result.exceptionOrNull()?.message ?: "Payment failed"
                            }
                        } catch (e: Exception) {
                            paymentError = e.message ?: "Unknown error occurred"
                        } finally {
                            isProcessingPayment = false
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info Card
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
                    text = "• Accepts SOL and SKR (Seeker) tokens\n" +
                           "• Payments via Solana Mobile Wallet Adapter\n" +
                           "• Subscriptions are weekly, manual renewal\n" +
                           "• Cancel anytime, no hidden fees\n" +
                           "• Verify transaction on Solscan after payment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
    
    // Success Dialog
    if (showSuccessDialog && lastTransactionSig != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Subscription Activated!") },
            text = {
                Column {
                    Text("Your subscription is now active for 7 days.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Transaction: ${lastTransactionSig!!.take(8)}...${lastTransactionSig!!.takeLast(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) {
                    Text("Awesome!")
                }
            }
        )
    }
}

@Composable
fun CurrentSubscriptionCard(status: SubscriptionStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Subscription",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = status.plan?.displayName ?: "Unknown",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Expires In",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${status.daysUntilExpiration()} days",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                if (status.expiresAt != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Expires On",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(Date(status.expiresAt)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isCurrentPlan: Boolean = false,
    isProcessing: Boolean = false,
    onSubscribe: () -> Unit
) {
    val isRecommended = plan == SubscriptionPlan.COMPLETE_BUNDLE
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isRecommended) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.displayName,
                    style = MaterialTheme.typography.titleLarge
                )
                
                if (isRecommended) {
                    Badge {
                        Text("Best Value")
                    }
                }
                
                if (isCurrentPlan) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text("Current")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = plan.priceDisplay,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            plan.features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && !isCurrentPlan
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCurrentPlan) "Current Plan" else "Subscribe Now")
                }
            }
        }
    }
}
