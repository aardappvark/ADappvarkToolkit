package com.adappvark.toolkit.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.view.WindowManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.adappvark.toolkit.data.UninstallHistory
import com.adappvark.toolkit.data.UninstalledApp
import com.adappvark.toolkit.data.ProtectedApps
import com.adappvark.toolkit.data.DAppStoreRegistry
import com.adappvark.toolkit.data.model.DAppFilter
import com.adappvark.toolkit.service.PackageManagerService
import com.adappvark.toolkit.service.ReinstallForegroundService
import com.adappvark.toolkit.ui.components.TemporarilyFreeBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ReinstallScreen"

/**
 * Sort options for app lists
 */
enum class SortOption {
    ALPHABETICAL,  // A-Z by app name
    DATE_DESC,     // Newest first (most recent uninstall/reinstall)
    DATE_ASC       // Oldest first
}

/**
 * Battery status for pre-reinstall checks
 */
enum class BatteryStatus {
    OK,         // >= 20% or charging
    LOW,        // 10-19% and not charging (warning)
    CRITICAL    // < 10% and not charging (block)
}

/**
 * Get current battery status
 */
private fun getBatteryStatus(context: Context): Triple<Int, Boolean, BatteryStatus> {
    val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 50

    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

    val batteryState = when {
        isCharging -> BatteryStatus.OK
        batteryPct < 10 -> BatteryStatus.CRITICAL
        batteryPct < 20 -> BatteryStatus.LOW
        else -> BatteryStatus.OK
    }

    return Triple(batteryPct, isCharging, batteryState)
}

/**
 * Keep screen on during reinstall process
 */
private fun setKeepScreenOn(context: Context, keepOn: Boolean) {
    (context as? Activity)?.runOnUiThread {
        if (keepOn) {
            context.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            context.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/**
 * Screen for reinstalling previously uninstalled dApps
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReinstallScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uninstallHistory = remember { UninstallHistory(context) }
    val protectedApps = remember { ProtectedApps(context) }
    val packageService = remember { PackageManagerService(context) }

    var historyList by remember { mutableStateOf(uninstallHistory.getHistory()) }
    // Installed dApps shown as faded items (not part of history)
    data class InstalledDApp(val packageName: String, val appName: String, val versionName: String, val sizeInBytes: Long)
    var installedDApps by remember { mutableStateOf<List<InstalledDApp>>(emptyList()) }
    // dApp Store apps not installed on the device
    var notInstalledDApps by remember { mutableStateOf<List<DAppStoreRegistry.DAppEntry>>(emptyList()) }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    var favouriteApps by remember { mutableStateOf(protectedApps.getProtectedPackages()) }
    var isReinstalling by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var reinstallProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var currentReinstallingApp by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showBatteryWarning by remember { mutableStateOf(false) }
    var showBatteryError by remember { mutableStateOf(false) }
    var appsToReinstall by remember { mutableStateOf<List<UninstalledApp>>(emptyList()) }
    var sortOption by remember { mutableStateOf(SortOption.ALPHABETICAL) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    // Separate favourites and regular apps
    val (favouritesList, regularList) = remember(historyList, favouriteApps, sortOption) {
        val favs = historyList.filter { favouriteApps.contains(it.packageName) }
        val regs = historyList.filter { !favouriteApps.contains(it.packageName) }

        // Sort function based on option
        fun sortList(list: List<UninstalledApp>): List<UninstalledApp> {
            return list.sortedWith(
                compareBy<UninstalledApp> { app ->
                    when {
                        app.reinstalled -> 2
                        app.skipReinstall -> 1
                        else -> 0
                    }
                }.let { comparator ->
                    when (sortOption) {
                        SortOption.ALPHABETICAL -> comparator.thenBy { it.appName.lowercase() }
                        SortOption.DATE_DESC -> comparator.thenByDescending { it.reinstalledAt ?: it.uninstalledAt }
                        SortOption.DATE_ASC -> comparator.thenBy { it.reinstalledAt ?: it.uninstalledAt }
                    }
                }
            )
        }

        Pair(sortList(favs), sortList(regs))
    }

    // Combined sorted list for counts
    val sortedHistoryList = remember(historyList, sortOption) {
        historyList.sortedWith(
            compareBy<UninstalledApp> { app ->
                // Status priority: 0 = pending (not reinstalled, not skipped), 1 = skipped, 2 = reinstalled
                when {
                    app.reinstalled -> 2
                    app.skipReinstall -> 1
                    else -> 0
                }
            }.thenBy { app ->
                // Secondary sort by selected option
                when (sortOption) {
                    SortOption.ALPHABETICAL -> app.appName.lowercase()
                    SortOption.DATE_DESC -> "" // handled by thenByDescending below
                    SortOption.DATE_ASC -> "" // handled by thenBy below
                }
            }.let { comparator ->
                // Add date sorting if needed
                when (sortOption) {
                    SortOption.DATE_DESC -> comparator.thenByDescending { it.reinstalledAt ?: it.uninstalledAt }
                    SortOption.DATE_ASC -> comparator.thenBy { it.reinstalledAt ?: it.uninstalledAt }
                    SortOption.ALPHABETICAL -> comparator
                }
            }
        )
    }

    // Function to refresh and sync with device state
    fun refreshList() {
        scope.launch {
            isRefreshing = true
            // Small delay to show the refresh indicator
            delay(500)
            // Sync with actual device installation status
            historyList = uninstallHistory.syncWithDeviceState()
            isRefreshing = false
        }
    }

    // Refresh and sync when screen becomes visible
    LaunchedEffect(Unit) {
        // Sync with device state on screen open
        historyList = uninstallHistory.syncWithDeviceState()

        // Scan installed dApps to show as faded "already installed" items
        val installed = packageService.scanInstalledApps(filter = DAppFilter.DAPP_STORE_ONLY)
        val historyPackages = historyList.map { it.packageName }.toSet()
        installedDApps = installed
            .filter { it.packageName !in historyPackages }
            .map { dApp ->
                InstalledDApp(
                    packageName = dApp.packageName,
                    appName = dApp.appName,
                    sizeInBytes = dApp.sizeInBytes,
                    versionName = dApp.versionName
                )
            }
            .sortedBy { it.appName.lowercase() }

        // Also scan dApp Store apps to discover dApps not in the static registry.
        // Uses DAPP_STORE_ONLY filter to ensure only dApp Store apps appear (no Google/Play Store apps).
        val dAppStoreDeviceApps = packageService.scanInstalledApps(filter = DAppFilter.DAPP_STORE_ONLY)
        val discoveredExtras = dAppStoreDeviceApps
            .filter { !DAppStoreRegistry.isExcluded(it.packageName) }
            .map { DAppStoreRegistry.DAppEntry(it.packageName, it.appName, "Discovered") }
        val mergedRegistry = DAppStoreRegistry.mergedWith(discoveredExtras)

        // Compute dApp Store apps that are NOT installed and NOT in history
        val allKnownPackages = historyPackages +
            installed.map { it.packageName }.toSet()
        notInstalledDApps = mergedRegistry
            .filter { it.packageName !in allKnownPackages }
            .sortedBy { it.displayName.lowercase() }
    }

    // Auto-refresh every 30 seconds during reinstall to show newly installed apps
    LaunchedEffect(isReinstalling) {
        if (isReinstalling) {
            while (isReinstalling) {
                delay(30000) // 30 seconds
                if (isReinstalling) { // Double-check we're still reinstalling
                    Log.d(TAG, "Auto-refresh: syncing device state...")
                    historyList = uninstallHistory.syncWithDeviceState()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with info
        if (historyList.isEmpty() && installedDApps.isEmpty() && notInstalledDApps.isEmpty()) {
            // Truly empty state - no history, no installed apps, no registry
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Uninstall History",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Apps you uninstall using ADappvark will appear here for easy reinstallation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (historyList.isEmpty() && (installedDApps.isNotEmpty() || notInstalledDApps.isNotEmpty())) {
            // No uninstall history yet, but show installed dApps as faded items
            Text(
                text = "No uninstall history yet. Uninstall dApps from the Uninstall tab to see them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Installed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(installedDApps, key = { "installed_${it.packageName}" }) { app ->
                    InstalledDAppItem(appName = app.appName, versionName = app.versionName, sizeInBytes = app.sizeInBytes)
                }

                // Not-installed dApp Store apps
                if (notInstalledDApps.isNotEmpty()) {
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
                    items(notInstalledDApps, key = { "notinstalled_${it.packageName}" }) { entry ->
                        NotInstalledDAppItem(
                            displayName = entry.displayName,
                            category = entry.category,
                            onInstall = {
                                openDAppStore(context, entry.packageName)
                            }
                        )
                    }
                }
            }
        } else {
            // Selection controls - only count non-reinstalled, non-skipped apps
            val pendingApps = sortedHistoryList.filter { !it.reinstalled && !it.skipReinstall }
            val skippedApps = sortedHistoryList.filter { it.skipReinstall }
            val reinstalledApps = sortedHistoryList.filter { it.reinstalled }

            // Filter selectedApps to only include valid pending apps (fixes "5 of 4" bug)
            val validSelectedCount = selectedApps.count { pkg ->
                pendingApps.any { it.packageName == pkg }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$validSelectedCount of ${pendingApps.size} pending selected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (reinstalledApps.isNotEmpty()) {
                        Text(
                            text = "${reinstalledApps.size} already reinstalled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (skippedApps.isNotEmpty()) {
                        Text(
                            text = "${skippedApps.size} marked as skip",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row {
                    TextButton(onClick = {
                        // Only select non-reinstalled apps
                        selectedApps = pendingApps.map { it.packageName }.toSet()
                    }) {
                        Text("Select All")
                    }
                    TextButton(onClick = { selectedApps = emptySet() }) {
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
                    // Refresh button to sync with device state
                    IconButton(
                        onClick = { refreshList() },
                        enabled = !isRefreshing && !isReinstalling
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, "Refresh Status")
                        }
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Filled.DeleteForever, "Clear History")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress indicator - enhanced "Working" overlay
            if (isReinstalling) {
                reinstallProgress?.let { (current, total) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Reinstalling...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { current.toFloat() / total },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "$current of $total apps",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            currentReinstallingApp?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "📱 Tap Install on each dApp Store page",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // List of uninstalled apps with Favourites and Dapps sections
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
                    items(favouritesList, key = { it.packageName }) { app ->
                        UninstalledAppItem(
                            app = app,
                            isSelected = app.packageName in selectedApps,
                            isFavourite = true,
                            onToggle = {
                                selectedApps = if (app.packageName in selectedApps) {
                                    selectedApps - app.packageName
                                } else {
                                    selectedApps + app.packageName
                                }
                            },
                            onSingleReinstall = {
                                openDAppStore(context, app.packageName)
                            },
                            onToggleSkipReinstall = {
                                uninstallHistory.toggleSkipReinstall(app.packageName)
                                historyList = uninstallHistory.getHistory()
                                if (app.packageName in selectedApps) {
                                    selectedApps = selectedApps - app.packageName
                                }
                            },
                            onToggleFavourite = {
                                protectedApps.toggleProtection(app.packageName)
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
                    items(regularList, key = { it.packageName }) { app ->
                        UninstalledAppItem(
                            app = app,
                            isSelected = app.packageName in selectedApps,
                            isFavourite = false,
                            onToggle = {
                                selectedApps = if (app.packageName in selectedApps) {
                                    selectedApps - app.packageName
                                } else {
                                    selectedApps + app.packageName
                                }
                            },
                            onSingleReinstall = {
                                openDAppStore(context, app.packageName)
                            },
                            onToggleSkipReinstall = {
                                uninstallHistory.toggleSkipReinstall(app.packageName)
                                historyList = uninstallHistory.getHistory()
                                if (app.packageName in selectedApps) {
                                    selectedApps = selectedApps - app.packageName
                                }
                            },
                            onToggleFavourite = {
                                protectedApps.toggleProtection(app.packageName)
                                favouriteApps = protectedApps.getProtectedPackages()
                            }
                        )
                    }
                }

                // Installed dApps Section - shown faded as "already installed"
                if (installedDApps.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Installed",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(installedDApps, key = { "installed_${it.packageName}" }) { app ->
                        InstalledDAppItem(appName = app.appName, versionName = app.versionName, sizeInBytes = app.sizeInBytes)
                    }
                }

                // Not-installed dApp Store apps
                if (notInstalledDApps.isNotEmpty()) {
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
                    items(notInstalledDApps, key = { "notinstalled_${it.packageName}" }) { entry ->
                        NotInstalledDAppItem(
                            displayName = entry.displayName,
                            category = entry.category,
                            onInstall = {
                                openDAppStore(context, entry.packageName)
                            }
                        )
                    }
                }
            }

            // Reinstall button - only show if there are eligible apps selected (non-reinstalled, non-skipped)
            val eligibleSelected = selectedApps.filter { pkg ->
                val app = historyList.find { it.packageName == pkg }
                app?.reinstalled != true && app?.skipReinstall != true
            }

            if (eligibleSelected.isNotEmpty()) {
                // Temporarily Free Banner
                TemporarilyFreeBanner(selectedCount = eligibleSelected.size)
                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Check overlay permission first (required on Android 14+ for BAL)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                            !Settings.canDrawOverlays(context)) {
                            showOverlayPermissionDialog = true
                            return@Button
                        }

                        // Check battery before showing confirmation
                        val apps = historyList.filter { it.packageName in eligibleSelected && !it.reinstalled && !it.skipReinstall }

                        // Log what we're about to reinstall (debug builds only)
                        Log.d(TAG, "Reinstall button clicked - ${apps.size} apps selected")

                        appsToReinstall = apps

                        val (_, _, batteryState) = getBatteryStatus(context)
                        when (batteryState) {
                            BatteryStatus.CRITICAL -> showBatteryError = true
                            BatteryStatus.LOW -> showBatteryWarning = true
                            BatteryStatus.OK -> showConfirmDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isReinstalling
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reinstall ${eligibleSelected.size} dApps")
                }
            }
        }
    }

    // Clear history confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History?") },
            text = { Text("This will remove all apps from your reinstall history. You won't be able to reinstall them from here.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        uninstallHistory.clearHistory()
                        historyList = emptyList()
                        selectedApps = emptySet()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Battery critical error dialog (< 10% and not charging)
    if (showBatteryError) {
        val (batteryPct, _, _) = getBatteryStatus(context)
        AlertDialog(
            onDismissRequest = { showBatteryError = false },
            icon = { Icon(Icons.Filled.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Battery Too Low") },
            text = {
                Column {
                    Text(
                        "Your battery is at $batteryPct% and not charging.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please charge your device to at least 10% before reinstalling apps to avoid interruption.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatteryError = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Battery warning dialog (10-19% and not charging)
    if (showBatteryWarning) {
        val (batteryPct, _, _) = getBatteryStatus(context)
        AlertDialog(
            onDismissRequest = { showBatteryWarning = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
            title = { Text("Low Battery Warning") },
            text = {
                Column {
                    Text(
                        "Your battery is at $batteryPct% and not charging.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Reinstalling ${appsToReinstall.size} apps may take several minutes. We recommend plugging in your device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryWarning = false
                        showConfirmDialog = true
                    }
                ) {
                    Text("Continue Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Pre-reinstall confirmation dialog
    if (showConfirmDialog) {
        val (batteryPct, isCharging, _) = getBatteryStatus(context)
        // Filter out skipped apps for accurate estimate
        val appsNotSkipped = appsToReinstall.filter { !it.skipReinstall }
        val estimatedMinutes = (appsNotSkipped.size * 7) / 60 + 1 // ~7 seconds per app with auto-tap

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            title = { Text("Bulk Reinstall") },
            text = {
                Column {
                    Text(
                        "Ready to reinstall ${appsNotSkipped.size} apps from the Solana dApp Store.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (appsToReinstall.size != appsNotSkipped.size) {
                        Text(
                            "(${appsToReinstall.size - appsNotSkipped.size} marked as 'don't reinstall' will be skipped)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "📱 How it works:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("• dApp Store opens for each app", style = MaterialTheme.typography.bodySmall)
                    Text("• Tap Install on each dApp Store page", style = MaterialTheme.typography.bodySmall)
                    Text("• App returns automatically after each one", style = MaterialTheme.typography.bodySmall)
                    Text("• Downloads queue in background", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated time:", style = MaterialTheme.typography.bodySmall)
                        Text("~$estimatedMinutes min", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Battery:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "$batteryPct% ${if (isCharging) "(Charging)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCharging) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        scope.launch {
                            // Use individual app page reinstall
                            performBulkReinstall(
                                context = context,
                                apps = appsToReinstall,
                                uninstallHistory = uninstallHistory,
                                onStart = {
                                    isReinstalling = true
                                    setKeepScreenOn(context, true)
                                },
                                onProgress = { current, total, message ->
                                    reinstallProgress = current to total
                                    currentReinstallingApp = message
                                },
                                onAppReinstalled = { _ ->
                                    historyList = uninstallHistory.getHistory()
                                },
                                onComplete = {
                                    setKeepScreenOn(context, false)
                                    isReinstalling = false
                                    reinstallProgress = null
                                    currentReinstallingApp = null
                                    selectedApps = emptySet()
                                    historyList = uninstallHistory.getHistory()
                                }
                            )
                        }
                    }
                ) {
                    Text("Start Reinstall")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Overlay permission dialog (required on Android 14+ for background activity launches)
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            icon = { Icon(Icons.Filled.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Permission Required") },
            text = {
                Column {
                    Text(
                        "Android requires the \"Display over other apps\" permission for bulk reinstall to work.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This allows ADappvark to switch between the dApp Store and itself during the reinstall cycle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap \"Open Settings\" and enable the toggle for ADappvark Toolkit, then come back and try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        // Open system overlay permission settings for this app
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UninstalledAppItem(
    app: UninstalledApp,
    isSelected: Boolean,
    isFavourite: Boolean = false,
    onToggle: () -> Unit,
    onSingleReinstall: () -> Unit,
    onToggleSkipReinstall: () -> Unit = {},
    onToggleFavourite: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!app.reinstalled && !app.skipReinstall) onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                app.reinstalled -> MaterialTheme.colorScheme.tertiaryContainer
                app.skipReinstall -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
            if (app.reinstalled) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Reinstalled",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            } else if (app.skipReinstall) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = "Skip reinstall",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatSize(app.sizeInBytes) + " • v${app.versionName}",
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
fun InstalledDAppItem(appName: String, versionName: String, sizeInBytes: Long) {
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
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = formatSize(sizeInBytes) + " • v$versionName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "INSTALLED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

/**
 * Open Solana dApp Store to install an app using the official deep link scheme
 * See: https://docs.solanamobile.com/dapp-publishing/link-to-dapp-listing-page
 */
private fun openDAppStore(context: android.content.Context, packageName: String) {
    try {
        // Use the official Solana dApp Store deep link scheme
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("solanadappstore://details?id=$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: try market intent with dApp Store package
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.solanamobile.dappstore")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            // Second fallback: try with apps package name
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=$packageName")
                    setPackage("com.solanamobile.apps")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e3: Exception) {
                // dApp Store not installed - show error
                android.widget.Toast.makeText(
                    context,
                    "Solana dApp Store not found",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

/**
 * Tap the Install/Download button on the dApp Store app detail page via ADB.
 * Uses the same ADB server reverse port forward as silent uninstall.
 *
 * Button position determined from uiautomator dump:
 * bounds="[924,684][1140,828]" → center at (1032, 756)
 * Screen: 1200x2670 (Seeker device)
 */
private suspend fun tapInstallButton(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val socket = java.net.Socket()
        socket.soTimeout = 5000
        try {
            socket.connect(java.net.InetSocketAddress("127.0.0.1", 5037), 2000)
        } catch (e: Exception) {
            Log.w(TAG, "ADB server not available for tap: ${e.message}")
            return@withContext false
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

            sendMsg("host:transport-any")
            if (readStatus() != "OKAY") return@withContext false

            // Tap the Install button on dApp Store detail page
            // Confirmed via uiautomator: bounds=[924,684][1140,828], center=(1032,756)
            sendMsg("shell:input tap 1032 756")
            if (readStatus() != "OKAY") return@withContext false

            // Read response
            val buf = ByteArray(1024)
            try { inputStream.read(buf) } catch (_: Exception) {}
        }
        Log.d(TAG, "Tapped Install button via ADB at (1032, 756)")
        true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to tap Install button: ${e.message}")
        false
    }
}

/**
 * Bring ADappvark back to the foreground
 */
private fun bringAppToForeground(context: android.content.Context) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        if (intent != null) {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        // Ignore - app might already be in foreground
    }
}

/**
 * Reinstall apps via dApp Store deep links.
 *
 * Strategy: Open the dApp Store page for each app so the user can tap Install.
 * 1. Open dApp Store page for each app
 * 2. Wait for user to tap Install
 * 3. Return to ADappvark
 * 4. Downloads continue in background
 * 5. User can tap Refresh to check final status
 */
private suspend fun performBulkReinstall(
    context: android.content.Context,
    apps: List<UninstalledApp>,
    uninstallHistory: UninstallHistory,
    onStart: () -> Unit,
    onProgress: suspend (Int, Int, String) -> Unit,
    onAppReinstalled: suspend (String) -> Unit,
    onComplete: suspend () -> Unit
) {
    onStart()

    // Start foreground service to prevent Android 14+ from blocking
    // background activity launches (BAL) when switching between apps
    ReinstallForegroundService.start(context)

    // Log for debugging (stripped in release builds)
    Log.d(TAG, "Bulk reinstall started - ${apps.size} apps")

    // Also show a toast so we know it started
    android.widget.Toast.makeText(context, "Starting reinstall of ${apps.size} apps...", android.widget.Toast.LENGTH_SHORT).show()

    val appsToProcess = apps.filter { !it.skipReinstall }
    Log.d(TAG, "Processing ${appsToProcess.size} apps after skip filter")

    // Track whether auto-tap is available (affects retry strategy)
    var autoTapAvailable = false

    appsToProcess.forEachIndexed { index, app ->
        Log.d(TAG, "Processing ${index + 1}/${appsToProcess.size}: ${app.appName}")
        onProgress(index + 1, appsToProcess.size, app.appName)
        ReinstallForegroundService.updateProgress(context, index + 1, appsToProcess.size, app.appName)

        // Open dApp Store for this app
        openDAppStore(context, app.packageName)

        // Wait for dApp Store page to load — 5s gives the store time to render
        // even when it's busy processing a large download queue
        delay(5000)

        // Auto-tap the Install/Download button via ADB
        val tapped = tapInstallButton()
        if (tapped) {
            autoTapAvailable = true
            Log.d(TAG, "Auto-tapped Install for ${app.appName}")
            // Just long enough to register the tap — do NOT wait too long
            // or the button changes to Cancel and a retry tap would abort it
            delay(1000)
        } else {
            // ADB not available — give user time to manually tap Install
            Log.d(TAG, "No ADB tap — waiting for manual install of ${app.appName}")
            delay(10000)
        }

        // Return to ADappvark
        bringAppToForeground(context)
        delay(1500)

        // Every 50 apps, take a longer 15s breather for large batches
        // This lets the dApp Store process its download queue
        if ((index + 1) % 50 == 0 && index < appsToProcess.size - 1) {
            Log.d(TAG, "Long pause (15s) after ${index + 1} apps — large batch breather")
            onProgress(index + 1, appsToProcess.size, "Download breather (${index + 1}/${appsToProcess.size})...")
            delay(15000)
        }
        // Every 10 apps, pause for 8 seconds to let downloads catch up
        else if ((index + 1) % 10 == 0 && index < appsToProcess.size - 1) {
            Log.d(TAG, "Pausing 8s after ${index + 1} apps to let downloads catch up...")
            onProgress(index + 1, appsToProcess.size, "Letting downloads catch up...")
            delay(8000)
        }

        // Brief pause before next app
        if (index < appsToProcess.size - 1) {
            delay(500)
        }
    }

    // Wait for downloads to progress, then sync
    delay(5000)
    uninstallHistory.syncWithDeviceState()
    onAppReinstalled("")

    val installedCount = appsToProcess.count { uninstallHistory.isAppInstalled(it.packageName) }
    val pendingCount = appsToProcess.size - installedCount

    // IMPORTANT: Do NOT retry when auto-tap was used.
    // After tapping Install, the button changes to Cancel while downloading.
    // A retry would tap Cancel, aborting the download. Instead, let downloads
    // finish in the background and tell user to tap Refresh later.
    if (pendingCount > 0 && !autoTapAvailable) {
        // Manual mode retry — user might have missed tapping Install on some apps
        Log.d(TAG, "Manual-mode retry: $pendingCount apps not yet installed")
        android.widget.Toast.makeText(
            context,
            "$installedCount installed. Retrying $pendingCount remaining apps...",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        delay(5000)
        val retryApps = appsToProcess.filter { !uninstallHistory.isAppInstalled(it.packageName) }

        retryApps.forEachIndexed { index, app ->
            Log.d(TAG, "Retry [${index + 1}/${retryApps.size}]: ${app.appName}")
            onProgress(appsToProcess.size - retryApps.size + index + 1,
                appsToProcess.size + retryApps.size,
                "${app.appName} (retry)")

            openDAppStore(context, app.packageName)
            // Manual only — give user time to tap
            delay(10000)
            bringAppToForeground(context)
            delay(1500)
        }

        delay(3000)
        uninstallHistory.syncWithDeviceState()
        onAppReinstalled("")

        val finalInstalledCount = appsToProcess.count { uninstallHistory.isAppInstalled(it.packageName) }
        val finalPendingCount = appsToProcess.size - finalInstalledCount

        val message = when {
            finalInstalledCount == appsToProcess.size -> "All ${appsToProcess.size} apps reinstalled!"
            finalPendingCount > 0 -> "$finalInstalledCount installed. $finalPendingCount downloading in background — tap Refresh to check."
            else -> "Reinstall complete. Tap Refresh to check status."
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    } else {
        val message = when {
            installedCount == appsToProcess.size -> "All ${appsToProcess.size} apps reinstalled!"
            pendingCount > 0 -> "$installedCount installed. $pendingCount downloading in background — tap Refresh to check."
            else -> "Reinstall triggered. Tap Refresh to check status."
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }

    // Stop foreground service — reinstall cycle complete
    ReinstallForegroundService.stop(context)

    onComplete()
}

/**
 * dApp Store category definitions with deep links
 */
private val DAPP_STORE_CATEGORIES = listOf(
    "Top Picks" to "solanadappstore://category/top-picks",
    "DeFi & Trading" to "solanadappstore://category/defi",
    "Games" to "solanadappstore://category/games",
    "DePIN" to "solanadappstore://category/depin",
    "NFTs" to "solanadappstore://category/nfts",
    "Privacy & Security" to "solanadappstore://category/privacy",
    "Content & Streaming" to "solanadappstore://category/content",
    "Wallets" to "solanadappstore://category/wallets",
    "Productivity" to "solanadappstore://category/productivity",
    "Social & Identity" to "solanadappstore://category/social",
    "AI & Agents" to "solanadappstore://category/ai",
    "Lifestyle" to "solanadappstore://category/lifestyle"
)

/**
 * Open dApp Store main page
 */
private fun openDAppStoreMain(context: android.content.Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("solanadappstore://")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: launch dApp Store package directly
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.solanamobile.dappstore")
                ?: context.packageManager.getLaunchIntentForPackage("com.solanamobile.apps")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                context.startActivity(intent)
            }
        } catch (e2: Exception) {
            android.widget.Toast.makeText(context, "Could not open dApp Store", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Open a specific category in dApp Store
 */
private fun openDAppStoreCategory(context: android.content.Context, categoryName: String) {
    // Try deep link first
    val deepLink = DAPP_STORE_CATEGORIES.find { it.first == categoryName }?.second

    if (deepLink != null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(deepLink)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            Log.w(TAG, "Deep link failed for $categoryName: ${e.message}")
        }
    }

    // Fallback: just open dApp Store main (user will need to navigate)
    openDAppStoreMain(context)
}

/**
 * Open dApp Store main page
 */
