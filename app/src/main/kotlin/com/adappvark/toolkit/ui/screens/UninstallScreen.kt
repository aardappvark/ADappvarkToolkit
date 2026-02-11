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
import com.adappvark.toolkit.data.UninstalledApp
import com.adappvark.toolkit.data.ProtectedApps
import androidx.compose.ui.text.style.TextDecoration
import com.adappvark.toolkit.service.PackageManagerService
import com.adappvark.toolkit.service.PaymentService
import com.adappvark.toolkit.ui.components.PaymentConfirmationDialog
import com.adappvark.toolkit.ui.components.PaymentErrorDialog
import com.adappvark.toolkit.ui.components.PaymentSuccessDialog
import com.adappvark.toolkit.ui.components.PricingBanner
import com.adappvark.toolkit.service.PaymentMethod
import android.util.Log
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
    var uninstalledApps by remember { mutableStateOf<List<UninstalledApp>>(emptyList()) }
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

    // Scan dApps on first load
    LaunchedEffect(currentFilter) {
        isLoading = true
        dAppList = packageService.scanInstalledApps(filter = currentFilter)
        // Load uninstalled apps (not yet reinstalled) to show as faded items
        val history = uninstallHistory.getHistory()
        val installedPackages = dAppList.map { it.packageName }.toSet()
        uninstalledApps = history
            .filter { !it.reinstalled && it.packageName !in installedPackages }
            .sortedBy { it.appName.lowercase() }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                selected = false,
                onClick = { /* Locked - coming soon */ },
                enabled = false,
                label = { Text("All Apps (Coming Soon)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Coming Soon",
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

                // Recently Uninstalled Section - shown faded as "already removed"
                if (uninstalledApps.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recently Uninstalled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(uninstalledApps, key = { "uninstalled_${it.packageName}" }) { app ->
                        UninstalledDAppItem(app = app)
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
                if (isPaymentRequired) {
                    showPaymentDialog = true
                } else {
                    showConfirmDialog = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDApps.isNotEmpty() && !isUninstalling
        ) {
            if (isPaymentRequired) {
                Icon(Icons.Filled.Payment, contentDescription = null)
            } else {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isPaymentRequired)
                    "Pay & Uninstall ${selectedDApps.size} dApps"
                else
                    "Uninstall ${selectedDApps.size} dApps"
            )
        }
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
                // Proceed with uninstall anyway
                showConfirmDialog = true
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
                                    // Rescan installed apps
                                    dAppList = packageService.scanInstalledApps(filter = currentFilter)
                                    // Refresh uninstalled list
                                    val history = uninstallHistory.getHistory()
                                    val installedPkgs = dAppList.map { it.packageName }.toSet()
                                    uninstalledApps = history
                                        .filter { !it.reinstalled && it.packageName !in installedPkgs }
                                        .sortedBy { it.appName.lowercase() }
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

@Composable
fun UninstalledDAppItem(app: UninstalledApp) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = "v${app.versionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "UNINSTALLED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Perform bulk uninstall using standard Android uninstall intents.
 *
 * Uses adaptive waiting: after firing each ACTION_DELETE intent,
 * polls PackageManager to confirm the package is actually gone
 * before proceeding to the next one. The user confirms each
 * uninstall via the standard system dialog.
 */
suspend fun performBulkUninstall(
    context: android.content.Context,
    packageNames: List<String>,
    onStart: () -> Unit,
    onProgress: suspend (Int, Int, String) -> Unit,
    onComplete: suspend () -> Unit
) {
    onStart()
    val TAG = "BulkUninstall"

    // Pre-operation: hint GC to free memory before bulk operation
    // This helps prevent system popup dialogs caused by memory pressure
    Runtime.getRuntime().gc()
    delay(500)

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

    Log.d(TAG, "Starting bulk uninstall: ${installedPackages.size} packages (filtered from ${packageNames.size})")

    // Track packages that failed to uninstall for retry
    val failedPackages = mutableListOf<String>()

    installedPackages.forEachIndexed { index, packageName ->
        // Double-check still installed right before triggering (in case prior uninstall removed it)
        val stillInstalled = try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }

        if (!stillInstalled) {
            Log.d(TAG, "[${ index + 1}/${installedPackages.size}] $packageName already gone, skipping")
            onProgress(index + 1, installedPackages.size, "$packageName (already removed)")
            return@forEachIndexed
        }

        // Every 5 packages: let the system breathe to prevent memory-pressure popups
        // This is a mitigation for occasional system "not responding" dialogs on
        // devices with limited RAM (like Solana Seeker) during rapid sequential uninstalls
        if (index > 0 && index % 5 == 0) {
            Log.d(TAG, "Breathing pause at package #$index to reduce memory pressure")
            Runtime.getRuntime().gc()
            delay(1500)
        }

        Log.d(TAG, "[${index + 1}/${installedPackages.size}] Uninstalling: $packageName")
        onProgress(index + 1, installedPackages.size, packageName)

        // Dismiss any lingering system dialog from previous uninstall before starting next one
        // This clears stale package installer dialogs that may not have been auto-dismissed
        try {
            val backIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Briefly return to home to clear dialog stack, then proceed
            context.startActivity(backIntent)
            delay(200)
        } catch (e: Exception) {
            Log.w(TAG, "Could not dismiss lingering dialog: ${e.message}")
        }

        // Trigger standard Android uninstall
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        // Adaptive wait: poll until the package is actually gone
        // or we hit the timeout (max 15 seconds per app)
        val maxWaitMs = 15_000L
        val pollIntervalMs = 500L
        var waited = 0L
        var uninstalled = false

        // Initial wait for the dialog to appear and user to confirm
        delay(1000)
        waited += 1000

        while (waited < maxWaitMs) {
            val isGone = try {
                pm.getPackageInfo(packageName, 0)
                false // Still installed
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                true // Successfully uninstalled
            }

            if (isGone) {
                uninstalled = true
                Log.d(TAG, "  ✓ $packageName removed after ${waited}ms")
                break
            }

            delay(pollIntervalMs)
            waited += pollIntervalMs
        }

        if (!uninstalled) {
            Log.w(TAG, "  ✗ $packageName NOT removed after ${waited}ms (timeout)")
            failedPackages.add(packageName)
        }

        // Settle time before next intent - slightly longer to prevent dialog accumulation
        // that can cause system popup windows on memory-constrained devices
        delay(500)
    }

    // Retry phase: attempt failed packages one more time
    if (failedPackages.isNotEmpty()) {
        Log.d(TAG, "Retry phase: ${failedPackages.size} packages failed first attempt")
        delay(2000) // Let system settle before retrying

        val retryFailed = mutableListOf<String>()

        failedPackages.forEachIndexed { index, packageName ->
            // Check if it was actually uninstalled in the meantime
            val stillInstalled = try {
                pm.getPackageInfo(packageName, 0)
                true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                false
            }

            if (!stillInstalled) {
                Log.d(TAG, "  Retry [${index + 1}/${failedPackages.size}] $packageName already gone")
                return@forEachIndexed
            }

            Log.d(TAG, "  Retry [${index + 1}/${failedPackages.size}] $packageName")
            onProgress(
                installedPackages.size - failedPackages.size + index + 1,
                installedPackages.size + failedPackages.size,
                "$packageName (retry)"
            )

            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // Adaptive wait for retry (same logic, slightly longer max)
            val maxRetryWaitMs = 20_000L
            val pollIntervalMs = 500L
            var waited = 0L
            var uninstalled = false

            delay(1500) // Slightly longer initial wait on retry
            waited += 1500

            while (waited < maxRetryWaitMs) {
                val isGone = try {
                    pm.getPackageInfo(packageName, 0)
                    false
                } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                    true
                }

                if (isGone) {
                    uninstalled = true
                    Log.d(TAG, "  ✓ Retry: $packageName removed after ${waited}ms")
                    break
                }

                delay(pollIntervalMs)
                waited += pollIntervalMs
            }

            if (!uninstalled) {
                Log.w(TAG, "  ✗ Retry: $packageName still NOT removed after ${waited}ms")
                retryFailed.add(packageName)
            }

            delay(300)
        }

        if (retryFailed.isNotEmpty()) {
            Log.w(TAG, "Final: ${retryFailed.size} packages could not be uninstalled: ${retryFailed.joinToString()}")
        }
    }

    // Final settle
    delay(1000)
    Log.d(TAG, "Bulk uninstall complete")

    onComplete()
}
