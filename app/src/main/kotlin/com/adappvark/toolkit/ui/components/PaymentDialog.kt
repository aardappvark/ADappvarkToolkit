package com.adappvark.toolkit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.service.PaymentMethod
import com.adappvark.toolkit.service.PricingSummary

/**
 * Payment confirmation dialog for bulk operations (5+ apps)
 */
@Composable
fun PaymentConfirmationDialog(
    pricingSummary: PricingSummary,
    operationType: String, // "uninstall" or "reinstall"
    // Payment required for 5+ apps (first 4 are free)
    onPayWithSOL: () -> Unit,
    onPayWithSKR: () -> Unit,
    onCancel: () -> Unit,
    isProcessing: Boolean = false,
    processingMessage: String = "Processing payment..."
) {
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onCancel() },
        icon = {
            Icon(
                imageVector = Icons.Filled.Payment,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Bulk Operation Payment",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Operation summary
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${pricingSummary.appCount} apps",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "to $operationType",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pricing explanation
                Text(
                    text = "Bulk operations (5+ apps) require a fee per use",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isProcessing) {
                    // Processing state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = processingMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    // Payment options
                    Text(
                        text = "Choose payment method:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // SOL Payment Button
                    PaymentOptionButton(
                        label = "Pay with SOL",
                        amount = pricingSummary.solFormatted,
                        icon = Icons.Filled.CurrencyBitcoin,
                        onClick = onPayWithSOL,
                        isPrimary = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // SKR Payment Button
                    PaymentOptionButton(
                        label = "Pay with SKR",
                        amount = pricingSummary.skrFormatted,
                        icon = Icons.Filled.Token,
                        onClick = onPayWithSKR,
                        isPrimary = false,
                        subtitle = "Seeker Token"
                    )
                }
            }
        },
        confirmButton = {
            // Empty - payment buttons are in the content
        },
        dismissButton = {
            if (!isProcessing) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun PaymentOptionButton(
    label: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean,
    subtitle: String? = null
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = amount,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = amount,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Simple info banner to show pricing on selection screens
 * First 4 apps are free, 5+ apps require 0.01 SOL per bulk operation
 *
 * NOTE: Payment is temporarily disabled — kept for future re-enablement
 */
@Composable
fun PricingBanner(
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    TemporarilyFreeBanner(selectedCount = selectedCount, modifier = modifier)
}

/**
 * Banner that shows all operations are temporarily free.
 * Replaces PricingBanner while payment is disabled.
 */
@Composable
fun TemporarilyFreeBanner(
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Temporarily Free",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "$selectedCount apps selected — all operations are free during early access",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Payment success dialog
 */
@Composable
fun PaymentSuccessDialog(
    transactionSignature: String,
    paymentMethod: PaymentMethod,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinue,
        icon = {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF4CAF50)
            )
        },
        title = {
            Text(
                text = "Payment Successful!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your payment has been processed.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Transaction ID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (transactionSignature.length > 20) {
                                "${transactionSignature.take(10)}...${transactionSignature.takeLast(8)}"
                            } else {
                                transactionSignature
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("Continue")
            }
        }
    )
}

/**
 * Payment error dialog
 */
@Composable
fun PaymentErrorDialog(
    errorMessage: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Payment Failed",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
