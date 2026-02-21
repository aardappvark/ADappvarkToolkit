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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.data.model.DAppFilter
import com.adappvark.toolkit.data.model.DAppInfo
import com.adappvark.toolkit.data.UninstallHistory
import com.adappvark.toolkit.data.UninstalledApp
import com.adappvark.toolkit.data.ProtectedApps
import com.adappvark.toolkit.data.DAppStoreRegistry
import androidx.compose.ui.text.style.TextDecoration
import com.adappvark.toolkit.service.AppSettingsManager
import com.adappvark.toolkit.service.PackageManagerService
import com.adappvark.toolkit.service.PaymentService
import com.adappvark.toolkit.service.SeekerVerificationService
import com.adappvark.toolkit.service.ShizukuManager
import com.adappvark.toolkit.ui.components.BulkOperationBanner
import com.adappvark.toolkit.ui.components.BulkPaymentChoiceDialog
import com.adappvark.toolkit.ui.components.PaymentSuccessDialog
import com.adappvark.toolkit.ui.components.PaymentErrorDialog
import com.adappvark.toolkit.service.PaymentMethod
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UninstallScreen(
    activityResultSender: ActivityResultSender? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val packageService = remember { PackageManagerService(context) }
    val uninstallHistory = remember { UninstallHistory(context) }
    val protectedApps = remember { ProtectedApps(context) }
    val appSettings = remember { AppSettingsManager(context) }
    val seekerVerifier = remember { SeekerVerificationService(context) }
    val userPrefs = remember { com.adappvark.toolkit.service.UserPreferencesManager(context) }
    // MobileWalletAdapter must be created at composition time (before Activity STARTED)
    val walletAdapter = remember { PaymentService.createWalletAdapter() }
    val paymentService = remember { PaymentService(context, walletAdapter) }

    // Reactive wallet/SGT state
    val isWalletConnected = userPrefs.isWalletConnected()
    val isSgtVerified = if (isWalletConnected) seekerVerifier.isVerifiedSeeker() else false

    var dAppList by remember { mutableStateOf<List<DAppInfo>>(emptyList()) }
    var uninstalledApps by remember { mutableStateOf<List<UninstalledApp>>(emptyList()) }
    // dApp Store apps that are NOT currently installed on the device
    var notInstalledDApps by remember { mutableStateOf<List<DAppStoreRegistry.DAppEntry>>(emptyList()) }
    var selectedDApps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var favouriteApps by remember { mutableStateOf(protectedApps.getProtectedPackages()) }
    var isLoading by remember { mutableStateOf(false) }
    var isUninstalling by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf(DAppFilter.DAPP_STORE_ONLY) }
    var sortOption by remember { mutableStateOf(SortOption.ALPHABETICAL) }
    var showSortMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Payment dialog state (non-SGT bulk operations)
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentInProgress by remember { mutableStateOf(false) }
    var paymentMessage by remember { mutableStateOf("Processing payment...") }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var paymentSuccess by remember { mutableStateOf<String?>(null) }
    // Tracks whether payment was completed for current operation (resets on new selection)
    var paymentApproved by remember { mutableStateOf(false) }

    // Filter dApps by search query first, then separate favourites/regular and sort
    val filteredDAppList = remember(dAppList, searchQuery) {
        if (searchQuery.isBlank()) dAppList
        else dAppList.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    // Separate favourites and regular apps, then sort each group
    val (favouritesList, regularList) = remember(filteredDAppList, favouriteApps, sortOption) {
        val favs = filteredDAppList.filter { favouriteApps.contains(it.packageName) }
        val regs = filteredDAppList.filter { !favouriteApps.contains(it.packageName) }

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
    val sortedDAppList = remember(filteredDAppList, sortOption) {
        when (sortOption) {
            SortOption.ALPHABETICAL -> filteredDAppList.sortedBy { it.appName.lowercase() }
            SortOption.DATE_DESC -> filteredDAppList.sortedByDescending { it.installedTime }
            SortOption.DATE_ASC -> filteredDAppList.sortedBy { it.installedTime }
        }
    }

    // Filter uninstalled and not-installed lists by search too
    val filteredUninstalledApps = remember(uninstalledApps, searchQuery) {
        if (searchQuery.isBlank()) uninstalledApps
        else uninstalledApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }
    val filteredNotInstalledDApps = remember(notInstalledDApps, searchQuery) {
        if (searchQuery.isBlank()) notInstalledDApps
        else notInstalledDApps.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
    }

    var uninstallProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var currentUninstallingApp by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullToRefreshState()

    // Handle pull-to-refresh
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            isRefreshing = true
            dAppList = packageService.scanInstalledApps(filter = currentFilter)
            val history = uninstallHistory.getHistory()
            val installedPackages = dAppList.map { it.packageName }.toSet()
            uninstalledApps = history
                .filter { !it.reinstalled && it.packageName !in installedPackages }
                .sortedBy { it.appName.lowercase() }
            val dAppStoreDeviceApps = packageService.scanInstalledApps(filter = DAppFilter.DAPP_STORE_ONLY)
            val discoveredExtras = dAppStoreDeviceApps
                .filter { !DAppStoreRegistry.isExcluded(it.packageName) }
                .map { DAppStoreRegistry.DAppEntry(it.packageName, it.appName, "Discovered") }
            val mergedRegistry = DAppStoreRegistry.mergedWith(discoveredExtras)
            val allInstalledPackages = installedPackages + uninstalledApps.map { it.packageName }.toSet()
            notInstalledDApps = mergedRegistry
                .filter { it.packageName !in allInstalledPackages }
                .sortedBy { it.displayName.lowercase() }
            isRefreshing = false
            pullRefreshState.endRefresh()
        }
    }

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

        // Also scan dApp Store apps to discover dApps not in the static registry.
        // Uses DAPP_STORE_ONLY filter to ensure only dApp Store apps appear (no Google/Play Store apps).
        val dAppStoreDeviceApps = packageService.scanInstalledApps(filter = DAppFilter.DAPP_STORE_ONLY)
        val discoveredExtras = dAppStoreDeviceApps
            .filter { !DAppStoreRegistry.isExcluded(it.packageName) }
            .map { DAppStoreRegistry.DAppEntry(it.packageName, it.appName, "Discovered") }
        val mergedRegistry = DAppStoreRegistry.mergedWith(discoveredExtras)

        // Compute dApp Store apps that are NOT installed on the device
        // These are shown as faded items below the installed apps
        val allInstalledPackages = installedPackages + uninstalledApps.map { it.packageName }.toSet()
        notInstalledDApps = mergedRegistry
            .filter { it.packageName !in allInstalledPackages }
            .sortedBy { it.displayName.lowercase() }

        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
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

        Spacer(modifier = Modifier.height(8.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search dApps...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                            if (appSettings.isSelectionHapticsEnabled()) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                        onClick = {
                            if (appSettings.isSelectionHapticsEnabled()) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            selectedDApps = emptySet()
                        }
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
        } else if (sortedDAppList.isEmpty() && filteredNotInstalledDApps.isEmpty()) {
            // Illustrated empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Filled.SearchOff else Icons.Filled.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No Results" else "No dApps Found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty())
                            "No dApps matching \"$searchQuery\""
                        else
                            "Install some apps from the Solana dApp Store to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (sortedDAppList.isEmpty() && filteredNotInstalledDApps.isNotEmpty()) {
            // No installed dApps but we have registry entries to show
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AppShortcut,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No dApp Store apps installed yet",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap any app below to install from the dApp Store",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recently Uninstalled Section
                if (filteredUninstalledApps.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recently Uninstalled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredUninstalledApps, key = { "uninstalled_${it.packageName}" }) { app ->
                        UninstalledDAppItem(app = app)
                    }
                }

                item {
                    Text(
                        text = "Available in dApp Store",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(filteredNotInstalledDApps, key = { "notinstalled_${it.packageName}" }) { entry ->
                    NotInstalledDAppItem(
                        displayName = entry.displayName,
                        category = entry.category,
                        onInstall = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("solanadappstore://details?id=${entry.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("market://details?id=${entry.packageName}")
                                        setPackage("com.solanamobile.dappstore")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
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
                if (filteredUninstalledApps.isNotEmpty()) {
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
                    items(filteredUninstalledApps, key = { "uninstalled_${it.packageName}" }) { app ->
                        UninstalledDAppItem(app = app)
                    }
                }

                // Not-installed dApp Store apps - shown faded at the bottom
                if (filteredNotInstalledDApps.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Available in dApp Store",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredNotInstalledDApps, key = { "notinstalled_${it.packageName}" }) { entry ->
                        NotInstalledDAppItem(
                            displayName = entry.displayName,
                            category = entry.category,
                            onInstall = {
                                // Open dApp Store listing for this app
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("solanadappstore://details?id=${entry.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback: try market intent
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("market://details?id=${entry.packageName}")
                                            setPackage("com.solanamobile.dappstore")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
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

        // SGT-aware banner
        if (selectedDApps.isNotEmpty()) {
            BulkOperationBanner(
                selectedCount = selectedDApps.size,
                isSgtVerified = isSgtVerified,
                operationType = "uninstall"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Uninstall Button
        Button(
            onClick = {
                if (appSettings.isUninstallHapticsEnabled()) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                // Payment gate: 5+ dApps without SGT requires payment
                if (selectedDApps.size >= 5 && !isSgtVerified && !paymentApproved) {
                    showPaymentDialog = true
                    return@Button
                }

                if (appSettings.isConfirmBeforeUninstallEnabled()) {
                    showConfirmDialog = true
                } else {
                    // Skip confirmation — start uninstall immediately
                    scope.launch {
                        paymentApproved = false // Reset for next operation
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
                                dAppList = packageService.scanInstalledApps(filter = currentFilter)
                                val history = uninstallHistory.getHistory()
                                val installedPkgs = dAppList.map { it.packageName }.toSet()
                                uninstalledApps = history
                                    .filter { !it.reinstalled && it.packageName !in installedPkgs }
                                    .sortedBy { it.appName.lowercase() }
                                val dAppStoreApps2 = packageService.scanInstalledApps(filter = DAppFilter.DAPP_STORE_ONLY)
                                val extras2 = dAppStoreApps2
                                    .filter { !DAppStoreRegistry.isExcluded(it.packageName) }
                                    .map { DAppStoreRegistry.DAppEntry(it.packageName, it.appName, "Discovered") }
                                val merged2 = DAppStoreRegistry.mergedWith(extras2)
                                val allKnown = installedPkgs + uninstalledApps.map { it.packageName }.toSet()
                                notInstalledDApps = merged2
                                    .filter { it.packageName !in allKnown }
                                    .sortedBy { it.displayName.lowercase() }
                            }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDApps.isNotEmpty() && !isUninstalling
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Uninstall ${selectedDApps.size} dApps")
        }
    }

    PullToRefreshContainer(
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter)
    )
    } // end Box

    // Confirmation Dialog
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
                                    // Refresh not-installed dApp Store list (merged with device scan)
                                    val dAppStoreApps2 = packageService.scanInstalledApps(filter = DAppFilter.DAPP_STORE_ONLY)
                                    val extras2 = dAppStoreApps2
                                        .filter { !DAppStoreRegistry.isExcluded(it.packageName) }
                                        .map { DAppStoreRegistry.DAppEntry(it.packageName, it.appName, "Discovered") }
                                    val merged2 = DAppStoreRegistry.mergedWith(extras2)
                                    val allKnown = installedPkgs + uninstalledApps.map { it.packageName }.toSet()
                                    notInstalledDApps = merged2
                                        .filter { it.packageName !in allKnown }
                                        .sortedBy { it.displayName.lowercase() }
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

    // Payment choice dialog (non-SGT bulk operations)
    if (showPaymentDialog) {
        BulkPaymentChoiceDialog(
            selectedCount = selectedDApps.size,
            operationType = "uninstall",
            onPayWithSOL = {
                if (activityResultSender == null) {
                    paymentError = "Wallet not available. Please connect a wallet first."
                    showPaymentDialog = false
                    return@BulkPaymentChoiceDialog
                }
                paymentInProgress = true
                paymentMessage = "Requesting SOL payment..."
                scope.launch {
                    try {
                        paymentService.requestSOLPayment(
                            activityResultSender = activityResultSender,
                            appCount = selectedDApps.size,
                            onSuccess = { signature ->
                                paymentInProgress = false
                                showPaymentDialog = false
                                paymentSuccess = signature
                                paymentApproved = true
                            },
                            onError = { error ->
                                paymentInProgress = false
                                showPaymentDialog = false
                                paymentError = error
                            }
                        )
                    } catch (e: Exception) {
                        paymentInProgress = false
                        showPaymentDialog = false
                        paymentError = e.message ?: "Payment failed"
                    }
                }
            },
            onPayWithSKR = {
                if (activityResultSender == null) {
                    paymentError = "Wallet not available. Please connect a wallet first."
                    showPaymentDialog = false
                    return@BulkPaymentChoiceDialog
                }
                paymentInProgress = true
                paymentMessage = "Requesting SKR payment..."
                scope.launch {
                    try {
                        paymentService.requestSKRPayment(
                            activityResultSender = activityResultSender,
                            appCount = selectedDApps.size,
                            onSuccess = { signature ->
                                paymentInProgress = false
                                showPaymentDialog = false
                                paymentSuccess = signature
                                paymentApproved = true
                            },
                            onError = { error ->
                                paymentInProgress = false
                                showPaymentDialog = false
                                paymentError = error
                            }
                        )
                    } catch (e: Exception) {
                        paymentInProgress = false
                        showPaymentDialog = false
                        paymentError = e.message ?: "Payment failed"
                    }
                }
            },
            onCancel = {
                showPaymentDialog = false
                paymentInProgress = false
            },
            isProcessing = paymentInProgress,
            processingMessage = paymentMessage
        )
    }

    // Payment success dialog
    if (paymentSuccess != null) {
        PaymentSuccessDialog(
            transactionSignature = paymentSuccess!!,
            paymentMethod = PaymentMethod.SOL,
            onContinue = {
                paymentSuccess = null
                // Now proceed with the uninstall (payment was approved)
                if (appSettings.isConfirmBeforeUninstallEnabled()) {
                    showConfirmDialog = true
                } else {
                    scope.launch {
                        paymentApproved = false
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
                            onStart = { isUninstalling = true },
                            onProgress = { current, total, packageName ->
                                uninstallProgress = current to total
                                currentUninstallingApp = packageName
                            },
                            onComplete = {
                                isUninstalling = false
                                uninstallProgress = null
                                currentUninstallingApp = null
                                selectedDApps = emptySet()
                                dAppList = packageService.scanInstalledApps(filter = currentFilter)
                            }
                        )
                    }
                }
            }
        )
    }

    // Payment error dialog
    if (paymentError != null) {
        PaymentErrorDialog(
            errorMessage = paymentError!!,
            onRetry = {
                paymentError = null
                showPaymentDialog = true
            },
            onCancel = {
                paymentError = null
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
    val view = LocalView.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onToggle()
            },
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
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onToggleFavourite()
                },
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

@Composable
fun NotInstalledDAppItem(
    displayName: String,
    category: String,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInstall() },
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
                imageVector = Icons.Filled.GetApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "NOT INSTALLED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Execute an ADB shell command via the ADB server reverse port forward.
 * Returns the command output, or null if ADB is not available.
 */
private suspend fun adbShellCommand(command: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val socket = java.net.Socket()
        socket.soTimeout = 10_000
        try {
            socket.connect(java.net.InetSocketAddress("127.0.0.1", 5037), 2000)
        } catch (e: Exception) {
            return@withContext null
        }

        socket.use { sock ->
            val os = sock.getOutputStream()
            val inputStream = sock.getInputStream()

            fun sendMsg(msg: String) {
                val hexLen = String.format("%04x", msg.length)
                os.write(hexLen.toByteArray(Charsets.US_ASCII))
                os.write(msg.toByteArray(Charsets.US_ASCII))
                os.flush()
            }
            fun readStatus(): String {
                val buf = ByteArray(4)
                var off = 0
                while (off < 4) {
                    val n = inputStream.read(buf, off, 4 - off)
                    if (n < 0) return "EOF"
                    off += n
                }
                return String(buf, Charsets.US_ASCII)
            }
            fun readAll(): String {
                val sb = StringBuilder()
                val buf = ByteArray(8192)
                while (true) {
                    val n = try { inputStream.read(buf) } catch (_: Exception) { -1 }
                    if (n <= 0) break
                    sb.append(String(buf, 0, n, Charsets.US_ASCII))
                }
                return sb.toString()
            }

            sendMsg("host:transport-any")
            if (readStatus() != "OKAY") return@withContext null

            sendMsg("shell:$command")
            if (readStatus() != "OKAY") return@withContext null

            readAll()
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Auto-tap the "OK" button on Android's uninstall confirmation dialog via ADB.
 *
 * Uses uiautomator dump to dynamically find the OK button position, making this
 * robust across different dialog layouts and Android versions.
 * Falls back to hardcoded coordinates if uiautomator fails.
 *
 * Seeker screen: 1200x2670 at 480dpi, Android 15.
 */
private suspend fun tapUninstallConfirmButton(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val TAG = "BulkUninstall"

    // Strategy 1: Use uiautomator to find the OK button dynamically
    val uiDump = adbShellCommand("uiautomator dump /dev/tty")
    if (uiDump != null) {
        // Look for the OK button in the UI dump
        // Android's uninstall dialog uses text "OK" or "Ok" on the confirm button
        val okPattern = Regex("""text="OK"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"""", RegexOption.IGNORE_CASE)
        val match = okPattern.find(uiDump)

        if (match != null) {
            val x1 = match.groupValues[1].toInt()
            val y1 = match.groupValues[2].toInt()
            val x2 = match.groupValues[3].toInt()
            val y2 = match.groupValues[4].toInt()
            val tapX = (x1 + x2) / 2
            val tapY = (y1 + y2) / 2

            Log.d(TAG, "Found OK button via uiautomator at ($tapX, $tapY) bounds=[$x1,$y1][$x2,$y2]")

            val tapResult = adbShellCommand("input tap $tapX $tapY")
            if (tapResult != null) {
                Log.d(TAG, "Tapped OK button at ($tapX, $tapY)")
                return@withContext true
            }
        } else {
            Log.d(TAG, "OK button not found in uiautomator dump, trying fallback coordinates")
        }
    }

    // Strategy 2: Fallback to fixed coordinates for Seeker 1200x2670
    // The OK button on the uninstall dialog is typically bottom-right of a centered dialog
    val tapResult = adbShellCommand("input tap 862 1424")
    if (tapResult != null) {
        Log.d(TAG, "Tapped fallback OK position at (862, 1424)")
        return@withContext true
    }

    Log.w(TAG, "ADB not available for auto-tap")
    false
}

/**
 * Perform bulk uninstall.
 *
 * Strategy:
 * 1. Try silent uninstall first (daemon, ADB pm, Shizuku) — no dialogs at all
 * 2. Fall back to ACTION_DELETE intents + auto-tap OK button via ADB
 * 3. If ADB not available, show dialogs and wait for user to manually confirm each
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

    // Filter to only packages that are actually still installed
    val pm = context.packageManager
    val installedPackages = packageNames.filter { packageName ->
        try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    Log.d(TAG, "Starting bulk uninstall: ${installedPackages.size} packages (filtered from ${packageNames.size})")

    // Try silent uninstall first (daemon, ADB TCP pm, Shizuku — no confirmation dialogs)
    val shizukuManager = ShizukuManager(context)
    val canSilent = shizukuManager.canSilentUninstall()
    Log.d(TAG, "Silent uninstall available: $canSilent")

    // Track which packages still need uninstalling after silent attempt
    var remainingPackages = installedPackages

    if (canSilent) {
        // Silent uninstall — no dialogs at all
        Log.d(TAG, "Using silent uninstall (daemon/ADB TCP/Shizuku/direct pm)")
        val results = shizukuManager.bulkUninstall(
            packageNames = installedPackages,
            onProgress = onProgress
        )

        val successCount = results.count { it.second.isSuccess }
        val failCount = results.count { it.second.isFailure }
        Log.d(TAG, "Silent uninstall complete: $successCount succeeded, $failCount failed")

        if (failCount > 0) {
            // Collect packages that failed silent uninstall — they need ACTION_DELETE fallback
            val failedNames = results.filter { it.second.isFailure }.map { it.first }
            Log.w(TAG, "Silent failed, will retry with ACTION_DELETE: ${failedNames.joinToString()}")
            remainingPackages = failedNames
        } else {
            remainingPackages = emptyList()
        }
    }

    // Fallback: ACTION_DELETE intents + auto-tap confirmation via ADB
    // Runs for ALL packages if silent wasn't available, or just failed ones if silent partially worked
    if (remainingPackages.isNotEmpty()) {
        Log.d(TAG, "Using ACTION_DELETE intents with auto-tap for ${remainingPackages.size} packages")

        Runtime.getRuntime().gc()
        delay(500)

        val failedPackages = mutableListOf<String>()
        var autoTapAvailable = false

        remainingPackages.forEachIndexed { index, packageName ->
            val stillInstalled = try {
                pm.getPackageInfo(packageName, 0)
                true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                false
            }

            if (!stillInstalled) {
                Log.d(TAG, "[${index + 1}/${remainingPackages.size}] $packageName already gone, skipping")
                onProgress(index + 1, remainingPackages.size, "$packageName (already removed)")
                return@forEachIndexed
            }

            if (index > 0 && index % 5 == 0) {
                Runtime.getRuntime().gc()
                delay(1500)
            }

            Log.d(TAG, "[${index + 1}/${remainingPackages.size}] Uninstalling: $packageName")
            onProgress(index + 1, remainingPackages.size, packageName)

            // Launch the system uninstall confirmation dialog
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // Wait for the uninstall confirmation dialog to fully render
            delay(1500)

            // Auto-tap the "OK" button on the confirmation dialog via ADB
            val tapped = tapUninstallConfirmButton()
            if (tapped) {
                autoTapAvailable = true
                Log.d(TAG, "Auto-tapped OK for $packageName")
            } else if (!autoTapAvailable) {
                // No ADB — user must manually tap OK each time
                Log.d(TAG, "No ADB tap — waiting for manual confirmation of $packageName")
            }

            // Wait for the uninstall to complete (poll until package is gone)
            val maxWaitMs = if (autoTapAvailable) 8_000L else 15_000L
            val pollIntervalMs = 500L
            var waited = 0L
            var uninstalled = false

            // Give the system a moment to process after tap
            delay(500)
            waited += 500

            while (waited < maxWaitMs) {
                val isGone = try {
                    pm.getPackageInfo(packageName, 0)
                    false
                } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                    true
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

            delay(300)
        }

        if (failedPackages.isNotEmpty()) {
            Log.w(TAG, "${failedPackages.size} packages failed: ${failedPackages.joinToString()}")
        }
    }

    delay(1000)
    Log.d(TAG, "Bulk uninstall complete")

    onComplete()
}
