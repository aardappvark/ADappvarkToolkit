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
import com.adappvark.toolkit.data.DAppStoreRegistry
import androidx.compose.ui.text.style.TextDecoration
import com.adappvark.toolkit.service.PackageManagerService
import com.adappvark.toolkit.service.ShizukuManager
import com.adappvark.toolkit.ui.components.TemporarilyFreeBanner
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UninstallScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val packageService = remember { PackageManagerService(context) }
    val uninstallHistory = remember { UninstallHistory(context) }
    val protectedApps = remember { ProtectedApps(context) }

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
        } else if (sortedDAppList.isEmpty() && notInstalledDApps.isEmpty()) {
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
        } else if (sortedDAppList.isEmpty() && notInstalledDApps.isNotEmpty()) {
            // No installed dApps but we have registry entries to show
            Text(
                text = "No dApp Store apps installed yet. Tap any app below to install from the dApp Store.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recently Uninstalled Section
                if (uninstalledApps.isNotEmpty()) {
                    item {
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

                item {
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

                // Not-installed dApp Store apps - shown faded at the bottom
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

        // Temporarily Free Banner
        if (selectedDApps.isNotEmpty()) {
            TemporarilyFreeBanner(selectedCount = selectedDApps.size)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Uninstall Button
        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDApps.isNotEmpty() && !isUninstalling
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Uninstall ${selectedDApps.size} dApps")
        }
    }

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
 * Perform bulk uninstall.
 *
 * Strategy:
 * 1. Try Shizuku silent uninstall first (pm uninstall --user 0) — no confirmation dialogs
 * 2. Fall back to standard ACTION_DELETE intents if Shizuku is not available
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

    // Try silent uninstall (Shizuku binder, ADB TCP, or direct pm — no confirmation dialogs)
    val shizukuManager = ShizukuManager(context)
    val canSilent = shizukuManager.canSilentUninstall()
    Log.d(TAG, "Shizuku available: ${shizukuManager.isShizukuAvailable()}, ADB TCP available: canSilent=$canSilent")

    if (canSilent) {
        // Silent uninstall — tries Shizuku binder, then ADB TCP, then direct pm
        Log.d(TAG, "Using silent uninstall (Shizuku/ADB TCP/direct pm)")
        val results = shizukuManager.bulkUninstall(
            packageNames = installedPackages,
            onProgress = onProgress
        )

        val successCount = results.count { it.second.isSuccess }
        val failCount = results.count { it.second.isFailure }
        Log.d(TAG, "Shizuku uninstall complete: $successCount succeeded, $failCount failed")

        if (failCount > 0) {
            val failedNames = results.filter { it.second.isFailure }.map { it.first }
            Log.w(TAG, "Failed packages: ${failedNames.joinToString()}")
        }
    } else {
        // Fallback: standard Android uninstall intents (shows confirmation dialog per app)
        Log.d(TAG, "No silent uninstall method available — falling back to ACTION_DELETE intents")

        Runtime.getRuntime().gc()
        delay(500)

        val failedPackages = mutableListOf<String>()

        installedPackages.forEachIndexed { index, packageName ->
            val stillInstalled = try {
                pm.getPackageInfo(packageName, 0)
                true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                false
            }

            if (!stillInstalled) {
                Log.d(TAG, "[${index + 1}/${installedPackages.size}] $packageName already gone, skipping")
                onProgress(index + 1, installedPackages.size, "$packageName (already removed)")
                return@forEachIndexed
            }

            if (index > 0 && index % 5 == 0) {
                Runtime.getRuntime().gc()
                delay(1500)
            }

            Log.d(TAG, "[${index + 1}/${installedPackages.size}] Uninstalling: $packageName")
            onProgress(index + 1, installedPackages.size, packageName)

            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // Adaptive wait: poll until the package is gone or timeout
            val maxWaitMs = 15_000L
            val pollIntervalMs = 500L
            var waited = 0L
            var uninstalled = false

            delay(1000)
            waited += 1000

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

            delay(500)
        }

        if (failedPackages.isNotEmpty()) {
            Log.w(TAG, "${failedPackages.size} packages failed: ${failedPackages.joinToString()}")
        }
    }

    delay(1000)
    Log.d(TAG, "Bulk uninstall complete")

    onComplete()
}
