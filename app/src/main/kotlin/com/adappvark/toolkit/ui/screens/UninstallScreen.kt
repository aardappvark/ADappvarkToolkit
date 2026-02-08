package com.adappvark.toolkit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.adappvark.toolkit.data.model.DAppFilter
import com.adappvark.toolkit.data.model.DAppInfo
import com.adappvark.toolkit.data.UninstallHistory
import com.adappvark.toolkit.data.ProtectedApps
import com.adappvark.toolkit.service.PackageManagerService
import com.adappvark.toolkit.service.PaymentService
import com.adappvark.toolkit.ui.components.PaymentConfirmationDialog
import com.adappvark.toolkit.ui.components.PaymentErrorDialog
import com.adappvark.toolkit.ui.components.PaymentSuccessDialog
import com.adappvark.toolkit.ui.components.PricingBanner
import com.adappvark.toolkit.service.PaymentMethod
import com.adappvark.toolkit.util.AccessibilityHelper
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UninstallScreen(
    activityResultSender: ActivityResultSender
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val packageService = remember { PackageManagerService(context) }
    // Create MWA adapter at composition time (before Activity STARTED) to avoid crash
    val walletAdapter = remember { PaymentService.createWalletAdapter() }
    val paymentService = remember { PaymentService(context, walletAdapter) }
    val uninstallHistory = remember { UninstallHistory(context) }
    val protectedApps = remember { ProtectedApps(context) }

    var dAppList by remember { mutableStateOf<List<DAppInfo>>(emptyList()) }
    var selectedDApps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var favouriteApps by remember { mutableStateOf(protectedApps.getProtectedPackages()) }
    var isLoading by remember { mutableStateOf(false) }
    var isUninstalling by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showPaymentSuccess by remember { mutableStateOf(false) }
    var showPaymentError by remember { mutableStateOf(false) }
    var paymentErrorMessage by remember { mutableStateOf("") }
    var lastTransactionSignature by remember { mutableStateOf("") }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf(DAppFilter.DAPP_STORE_ONLY) }
    var sortOption by remember { mutableStateOf(SortOption.ALPHABETICAL) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Separate favourites and regular apps, then sort each group
    val (favouritesList, regularList) = remember(dAppList, favouriteApps, sortOption) {
        val favs = dAppList.filter { favouriteApps.contains(it.packageName) }
        val regs = dAppList.filter { !favouriteApps.contains(it.packageName) }

        val sortFn: (DAppInfo) -> Comparable<*> = when (sortOption) {
            SortOption.ALPHABETICAL -> { it -> it.appName.lowercase() }
            SortOption.DATE_DESC -> { it -> -it.installedTime }
            SortOption.DATE_ASC -> { it -> it.installedTime }
        }

        Pair(
            favs.sortedBy { sortFn(it).toString() },
            regs.sortedBy { sortFn(it).toString() }
        )
    }

    // Combined sorted list for display
    val sortedDAppList = remember(dAppList, sortOption) {
        when (sortOption) {
            SortOption.ALPHABETICAL -> dAppList.sortedBy { it.appName.lowercase() }
            SortOption.DATE_DESC -> dAppList.sortedByDescending { it.installedTime }
            SortOption.DATE_ASC -> dAppList.sortedBy { it.installedTime }
        }
    }

    // Payment system: Free for up to 4 apps, 5+ requires payment per operation
    val isPaymentRequired = paymentService.isPaymentRequired(selectedDApps.size)
    val pricingSummary = paymentService.getPricingSummary(selectedDApps.size)
    var uninstallProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var currentUninstallingApp by remember { mutableStateOf<String?>(null) }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }

    // Check accessibility status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = AccessibilityHelper.isAccessibilityServiceEnabled(context)
            delay(1000) // Check every second
        }
    }

    // Scan dApps on first load
    LaunchedEffect(currentFilter) {
        isLoading = true
        dAppList = packageService.scanInstalledApps(filter = currentFilter)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Accessibility Status Banner - Bright yellow for visibility
        if (!isAccessibilityEnabled) {
            val brightYellow = Color(0xFFFFEB3B) // Bright yellow
            val darkText = Color(0xFF1A1A1A) // Dark text for contrast

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAccessibilityDialog = true },
                colors = CardDefaults.cardColors(
                    containerColor = brightYellow
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = darkText
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto-Uninstall Not Enabled",
                            style = MaterialTheme.typography.titleSmall,
                            color = darkText
                        )
                        Text(
                            "Tap to enable one-tap bulk uninstall",
                            style = MaterialTheme.typography.bodySmall,
                            color = darkText
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = darkText
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentFilter == DAppFilter.DAPP_STORE_ONLY,
                onClick = { currentFilter = DAppFilter.DAPP_STORE_ONLY },
                label = { Text("dApp Store Only") }
            )
            FilterChip(
                selected = currentFilter == DAppFilter.ALL,
                onClick = { currentFilter = DAppFilter.ALL },
                label = { Text("All Apps") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = "Premium",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selection Controls
        if (sortedDAppList.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedDApps.size} of ${sortedDAppList.size} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    TextButton(
                        onClick = {
                            // Select All excludes favourites - they are protected
                            selectedDApps = sortedDAppList
                                .filter { !favouriteApps.contains(it.packageName) }
                                .map { it.packageName }
                                .toSet()
                        }
                    ) {
                        Text("Select All")
                    }

                    TextButton(
                        onClick = { selectedDApps = emptySet() }
                    ) {
                        Text("Clear")
                    }

                    // Sort dropdown
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("A-Z") },
                                onClick = {
                                    sortOption = SortOption.ALPHABETICAL
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == SortOption.ALPHABETICAL) {
                                        Icon(Icons.Filled.Check, null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Newest First") },
                                onClick = {
                                    sortOption = SortOption.DATE_DESC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == SortOption.DATE_DESC) {
                                        Icon(Icons.Filled.Check, null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First") },
                                onClick = {
                                    sortOption = SortOption.DATE_ASC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == SortOption.DATE_ASC) {
                                        Icon(Icons.Filled.Check, null)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // dApp List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (sortedDAppList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No dApps found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favourites Section
                if (favouritesList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Favourites",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(favouritesList, key = { it.packageName }) { dApp ->
                        DAppListItem(
                            dApp = dApp,
                            isSelected = selectedDApps.contains(dApp.packageName),
                            isFavourite = true,
                            onToggle = {
                                selectedDApps = if (selectedDApps.contains(dApp.packageName)) {
                                    selectedDApps - dApp.packageName
                                } else {
                                    selectedDApps + dApp.packageName
                                }
                            },
                            onToggleFavourite = {
                                protectedApps.toggleProtection(dApp.packageName)
                                favouriteApps = protectedApps.getProtectedPackages()
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Regular dApps Section
                if (regularList.isNotEmpty()) {
                    item {
                        Text(
                            text = "dApps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(regularList, key = { it.packageName }) { dApp ->
                        DAppListItem(
                            dApp = dApp,
                            isSelected = selectedDApps.contains(dApp.packageName),
                            isFavourite = false,
                            onToggle = {
                                selectedDApps = if (selectedDApps.contains(dApp.packageName)) {
                                    selectedDApps - dApp.packageName
                                } else {
                                    selectedDApps + dApp.packageName
                                }
                            },
                            onToggleFavourite = {
                                protectedApps.toggleProtection(dApp.packageName)
                                favouriteApps = protectedApps.getProtectedPackages()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Uninstall Progress
        if (isUninstalling && uninstallProgress != null) {
            val (current, total) = uninstallProgress!!

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Uninstalling: $current of $total",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { current.toFloat() / total.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (currentUninstallingApp != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentUninstallingApp!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pricing Banner
        if (selectedDApps.isNotEmpty()) {
            PricingBanner(selectedCount = selectedDApps.size)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Uninstall Button
        Button(
            onClick = {
                if (!isAccessibilityEnabled) {
                    showAccessibilityDialog = true
                } else if (isPaymentRequired) {
                    showPaymentDialog = true
                } else {
                    showConfirmDialog = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDApps.isNotEmpty() && !isUninstalling
        ) {
            if (!isAccessibilityEnabled) {
                Icon(Icons.Filled.Settings, contentDescription = null)
            } else if (isPaymentRequired) {
                Icon(Icons.Filled.Payment, contentDescription = null)
            } else {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                when {
                    !isAccessibilityEnabled -> "Enable Auto-Uninstall"
                    isPaymentRequired -> "Pay & Uninstall ${selectedDApps.size} dApps"
                    else -> "Uninstall ${selectedDApps.size} dApps"
                }
            )
        }
    }

    // Accessibility Setup Dialog
    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Accessibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Enable Auto-Uninstall") },
            text = {
                Column {
                    Text("To uninstall multiple dApps automatically, ADappvark needs Accessibility permission.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "This is a one-time setup:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Tap \"Enable\" below")
                    Text("2. Find \"ADappvark Toolkit\" in the list")
                    Text("3. Toggle it ON and tap \"Allow\"")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "After this, you can bulk uninstall with one tap!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showAccessibilityDialog = false
                    AccessibilityHelper.openAccessibilitySettings(context)
                }) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Payment Dialog for 5+ apps
    if (showPaymentDialog) {
        PaymentConfirmationDialog(
            pricingSummary = pricingSummary,
            operationType = "uninstall",
            onPayWithSOL = {
                isProcessingPayment = true
                scope.launch {
                    paymentService.requestSOLPayment(
                        activityResultSender = activityResultSender,
                        appCount = selectedDApps.size,
                        onSuccess = { signature ->
                            lastTransactionSignature = signature
                            isProcessingPayment = false
                            showPaymentDialog = false
                            showPaymentSuccess = true
                        },
                        onError = { error ->
                            paymentErrorMessage = error
                            isProcessingPayment = false
                            showPaymentDialog = false
                            showPaymentError = true
                        }
                    )
                }
            },
            onPayWithSKR = {
                isProcessingPayment = true
                scope.launch {
                    paymentService.requestSKRPayment(
                        activityResultSender = activityResultSender,
                        appCount = selectedDApps.size,
                        onSuccess = { signature ->
                            lastTransactionSignature = signature
                            isProcessingPayment = false
                            showPaymentDialog = false
                            showPaymentSuccess = true
                        },
                        onError = { error ->
                            paymentErrorMessage = error
                            isProcessingPayment = false
                            showPaymentDialog = false
                            showPaymentError = true
                        }
                    )
                }
            },
            onCancel = {
                showPaymentDialog = false
            },
            isProcessing = isProcessingPayment
        )
    }

    // Payment Success Dialog
    if (showPaymentSuccess) {
        PaymentSuccessDialog(
            transactionSignature = lastTransactionSignature,
            paymentMethod = PaymentMethod.SOL,
            onContinue = {
                showPaymentSuccess = false
                // Now proceed with uninstall
                showConfirmDialog = true
            }
        )
    }

    // Payment Error Dialog
    if (showPaymentError) {
        PaymentErrorDialog(
            errorMessage = paymentErrorMessage,
            onRetry = {
                showPaymentError = false
                showPaymentDialog = true
            },
            onCancel = {
                showPaymentError = false
            }
        )
    }

    // Confirmation Dialog (for free operations or after payment)
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Uninstall") },
            text = {
                Column {
                    Text("Uninstall ${selectedDApps.size} dApps? This action cannot be undone.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        scope.launch {
                            // Save apps to history before uninstalling
                            val appsToUninstall = dAppList.filter { it.packageName in selectedDApps }
                            appsToUninstall.forEach { app ->
                                uninstallHistory.addToHistory(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    sizeInBytes = app.sizeInBytes,
                                    versionName = app.versionName
                                )
                            }

                            performBulkUninstall(
                                context = context,
                                packageNames = selectedDApps.toList(),
                                onStart = {
                                    isUninstalling = true
                                    // Enable auto-click in accessibility service
                                    AccessibilityHelper.enableAutoClick()
                                },
                                onProgress = { current, total, packageName ->
                                    uninstallProgress = current to total
                                    currentUninstallingApp = packageName
                                },
                                onComplete = {
                                    isUninstalling = false
                                    uninstallProgress = null
                                    currentUninstallingApp = null
                                    selectedDApps = emptySet()
                                    // Disable auto-click
                                    AccessibilityHelper.disableAutoClick()
                                    // Rescan
                                    dAppList = packageService.scanInstalledApps(filter = currentFilter)
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Uninstall")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DAppListItem(
    dApp: DAppInfo,
    isSelected: Boolean,
    isFavourite: Boolean = false,
    onToggle: () -> Unit,
    onToggleFavourite: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFavourite -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dApp.appName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${dApp.getFormattedSize()} • v${dApp.versionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Favourite star icon
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Star else Icons.Filled.StarOutline,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = if (isFavourite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Perform bulk uninstall using standard Android uninstall intents
 * The accessibility service will auto-click the confirm buttons
 */
suspend fun performBulkUninstall(
    context: android.content.Context,
    packageNames: List<String>,
    onStart: () -> Unit,
    onProgress: suspend (Int, Int, String) -> Unit,
    onComplete: suspend () -> Unit
) {
    onStart()

    // Filter to only packages that are actually still installed
    val pm = context.packageManager
    val installedPackages = packageNames.filter { packageName ->
        try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false // Skip - already uninstalled
        }
    }

    installedPackages.forEachIndexed { index, packageName ->
        // Double-check still installed right before triggering (in case prior uninstall removed it)
        val stillInstalled = try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }

        if (!stillInstalled) {
            onProgress(index + 1, installedPackages.size, "$packageName (skipped)")
            return@forEachIndexed
        }

        onProgress(index + 1, installedPackages.size, packageName)

        // Trigger standard Android uninstall
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        // Wait for the uninstall dialog to appear
        delay(500)

        // Wait for accessibility service to click the button
        // and for the uninstall to complete
        delay(3000)

        // Small delay before next one to let system settle
        delay(500)
    }

    // Wait a bit more for the last one to fully complete
    delay(2000)

    onComplete()
}
