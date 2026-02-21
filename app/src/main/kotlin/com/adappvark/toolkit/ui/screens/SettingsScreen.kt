package com.adappvark.toolkit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.BuildConfig
import com.adappvark.toolkit.service.AppSettingsManager
import com.adappvark.toolkit.service.CreditManager
import com.adappvark.toolkit.service.SeekerVerificationService
import com.adappvark.toolkit.service.TermsAcceptanceService
import com.adappvark.toolkit.service.UserPreferencesManager
import com.adappvark.toolkit.AppConfig
import com.adappvark.toolkit.ui.theme.*
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDisconnectWallet: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesManager(context) }
    val termsService = remember { TermsAcceptanceService(context) }
    val seekerVerifier = remember { SeekerVerificationService(context) }
    val creditManager = remember { CreditManager(context) }
    val appSettings = remember { AppSettingsManager(context) }

    // Preference states
    var darkMode by remember { mutableStateOf(appSettings.getDarkMode()) }
    var selectionHaptics by remember { mutableStateOf(appSettings.isSelectionHapticsEnabled()) }
    var uninstallHaptics by remember { mutableStateOf(appSettings.isUninstallHapticsEnabled()) }
    var reinstallHaptics by remember { mutableStateOf(appSettings.isReinstallHapticsEnabled()) }
    var autoAcceptDeleteSetting by remember { mutableStateOf(appSettings.isAutoAcceptDeleteSettingOn()) }
    var confirmBeforeUninstall by remember { mutableStateOf(appSettings.isConfirmBeforeUninstallEnabled()) }
    var autoAcceptInstallSetting by remember { mutableStateOf(appSettings.isAutoAcceptInstallSettingOn()) }
    var pullToRefresh by remember { mutableStateOf(appSettings.isPullToRefreshEnabled()) }
    var showPackageNames by remember { mutableStateOf(appSettings.isShowPackageNamesEnabled()) }
    var showUninstalledSection by remember { mutableStateOf(appSettings.isShowUninstalledSectionEnabled()) }
    var showNotInstalledSection by remember { mutableStateOf(appSettings.isShowNotInstalledSectionEnabled()) }
    var screenAnimations by remember { mutableStateOf(appSettings.isScreenAnimationsEnabled()) }
    var compactList by remember { mutableStateOf(appSettings.isCompactList()) }
    var defaultFilter by remember { mutableStateOf(appSettings.getDefaultFilter()) }

    // Reactive wallet/SGT state — re-reads on every recomposition so disconnect updates immediately
    val isWalletConnected = userPrefs.isWalletConnected()
    val isSgtVerified = if (isWalletConnected) seekerVerifier.isVerifiedSeeker() else false

    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showConsentRecordDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDisconnectWalletDialog by remember { mutableStateOf(false) }
    var showConnectWalletPrompt by remember { mutableStateOf(false) }

    // Pull-to-refresh for Settings
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            kotlinx.coroutines.delay(500)
            pullRefreshState.endRefresh()
        }
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullRefreshState.nestedScrollConnection)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Wallet Section
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = if (userPrefs.isWalletConnected()) SolanaPurpleGlow else null
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = SolanaPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wallet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (userPrefs.isWalletConnected()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = userPrefs.getWalletName() ?: "Connected Wallet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = userPrefs.getShortWalletAddress() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolanaPurple
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Connected",
                        tint = SolanaGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // T&C acceptance info
                userPrefs.getAcceptanceDateFormatted()?.let { date ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Terms accepted: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // SGT verification status
                if (seekerVerifier.isVerifiedSeeker()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified Seeker",
                            tint = SolanaPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified Seeker",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolanaPurple
                        )
                        seekerVerifier.getSgtMemberNumber()?.let { num ->
                            Text(
                                text = "  #$num",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SolanaGreen
                            )
                        }
                    }
                }

                // App unlock status
                if (seekerVerifier.isVerifiedSeeker()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SolanaGreen.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LockOpen,
                                contentDescription = null,
                                tint = SolanaGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "dApp 100% unlocked",
                                style = MaterialTheme.typography.labelMedium,
                                color = SolanaGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Consent record info
                val consentRecord = termsService.getConsentRecord()
                if (consentRecord != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showConsentRecordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Consent Record")
                    }
                }

                // Disconnect wallet button
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDisconnectWalletDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Wallet")
                }
            } else {
                Text(
                    text = "No wallet connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Connect Seeker Genesis Token for free full functionality",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onDisconnectWallet() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolanaPurple
                    )
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Wallet")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== SEEKER GENESIS TOKEN BENEFITS ====================
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = if (isSgtVerified) SolanaGreenGlow else if (isWalletConnected) SolanaPurpleGlow else null
        ) {
            // ── Section Heading: Seeker Genesis Token ──
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = if (isSgtVerified) SolanaGreen else if (isWalletConnected) SolanaPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Seeker Genesis Token",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!isWalletConnected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))

                if (isSgtVerified) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SolanaGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SolanaGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = SolanaGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (!isWalletConnected) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "No Wallet",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Not Detected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // SGT Connected banner with member number
            if (isSgtVerified) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SolanaGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = SolanaGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Seeker Genesis Token Connected",
                            style = MaterialTheme.typography.labelMedium,
                            color = SolanaGreen,
                            fontWeight = FontWeight.Bold
                        )
                        seekerVerifier.getSgtMemberNumber()?.let { num ->
                            Text(
                                text = "  #$num",
                                style = MaterialTheme.typography.labelMedium,
                                color = SolanaPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (!isWalletConnected) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Connect a wallet to check for SGT. Holders get unlimited free batch operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SGT holders get unlimited batch operations at no cost.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Sub-heading: Auto-Accept ──
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auto-Accept",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Skip system dialogs during batch operations (5+ dApps).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Batch Reinstall Row ──
            BatchOperationRow(
                title = "Batch Reinstall",
                subtitle = "Auto-accept install prompts",
                icon = Icons.Filled.InstallMobile,
                isSgtVerified = isSgtVerified,
                isWalletConnected = isWalletConnected,
                checked = if (isSgtVerified) true else autoAcceptInstallSetting,
                onCheckedChange = {
                    if (!isSgtVerified) {
                        autoAcceptInstallSetting = it
                        appSettings.setAutoAcceptInstall(it)
                    }
                },
                onLockedTap = { showConnectWalletPrompt = true }
            )

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            // ── Batch Uninstall Row ──
            BatchOperationRow(
                title = "Batch Uninstall",
                subtitle = "Auto-accept delete dialogs",
                icon = Icons.Filled.DeleteSweep,
                isSgtVerified = isSgtVerified,
                isWalletConnected = isWalletConnected,
                checked = if (isSgtVerified) true else autoAcceptDeleteSetting,
                onCheckedChange = {
                    if (!isSgtVerified) {
                        autoAcceptDeleteSetting = it
                        appSettings.setAutoAcceptDelete(it)
                    }
                },
                onLockedTap = { showConnectWalletPrompt = true }
            )

            // Pricing info (non-SGT only)
            if (!isSgtVerified) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Token,
                            contentDescription = null,
                            tint = SolanaPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pay per batch operation",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "0.01 SOL or 1 SKR per batch (5+ dApps)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            Spacer(modifier = Modifier.height(12.dp))

            // ── Sub-heading: Behaviour ──
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Behaviour",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isSgtVerified) "SGT holders can disable confirmation prompts."
                       else if (!isWalletConnected) "Connect wallet for SGT benefits or pay per batch."
                       else "Confirmation prompts are always shown.",
                style = MaterialTheme.typography.bodySmall,
                color = if (!isWalletConnected) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Confirm Before Uninstall ──
            SgtBehaviourToggle(
                title = "Confirm Before Uninstall",
                subtitle = if (isSgtVerified) "Show confirmation dialog before bulk uninstall"
                           else if (!isWalletConnected) "Connect wallet to configure"
                           else "Always on without SGT",
                icon = Icons.Filled.Warning,
                isSgtVerified = isSgtVerified,
                isWalletConnected = isWalletConnected,
                checked = if (isSgtVerified) confirmBeforeUninstall else true,
                onCheckedChange = {
                    if (isSgtVerified) {
                        confirmBeforeUninstall = it
                        appSettings.setConfirmBeforeUninstall(it)
                    }
                },
                onLockedTap = { showConnectWalletPrompt = true }
            )

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            // ── Pull to Refresh ──
            SgtBehaviourToggle(
                title = "Pull to Refresh",
                subtitle = if (isSgtVerified) "Swipe down to refresh app lists"
                           else if (!isWalletConnected) "Connect wallet to configure"
                           else "Always on without SGT",
                icon = Icons.Filled.Refresh,
                isSgtVerified = isSgtVerified,
                isWalletConnected = isWalletConnected,
                checked = if (isSgtVerified) pullToRefresh else true,
                onCheckedChange = {
                    if (isSgtVerified) {
                        pullToRefresh = it
                        appSettings.setPullToRefresh(it)
                    }
                },
                onLockedTap = { showConnectWalletPrompt = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== LIST DISPLAY ====================
        GlassSettingsSectionCard(
            title = "List Display",
            icon = Icons.Filled.ViewList
        ) {
            SettingsToggle(
                title = "Show Package Names",
                subtitle = "Display technical package names under app names",
                icon = Icons.Filled.Code,
                checked = showPackageNames,
                onCheckedChange = { showPackageNames = it; appSettings.setShowPackageNames(it) }
            )

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            SettingsToggle(
                title = "Show Uninstalled Section",
                subtitle = "Show recently uninstalled apps as faded items",
                icon = Icons.Filled.History,
                checked = showUninstalledSection,
                onCheckedChange = { showUninstalledSection = it; appSettings.setShowUninstalledSection(it) }
            )

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            SettingsToggle(
                title = "Show Not Installed Section",
                subtitle = "Show dApp Store registry apps you don't have",
                icon = Icons.Filled.Inventory2,
                checked = showNotInstalledSection,
                onCheckedChange = { showNotInstalledSection = it; appSettings.setShowNotInstalledSection(it) }
            )

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            SettingsDropdown(
                title = "Default Filter",
                subtitle = when (defaultFilter) {
                    "all" -> "All apps"
                    "large" -> "Large apps (>100MB)"
                    "old" -> "Old installs (>30 days)"
                    else -> "dApp Store Only"
                },
                icon = Icons.Filled.FilterList,
                selectedValue = defaultFilter,
                options = listOf(
                    "dapp_store" to "dApp Store Only",
                    "all" to "All Apps",
                    "large" to "Large (>100MB)",
                    "old" to "Old (>30 days)"
                ),
                onValueChange = { defaultFilter = it; appSettings.setDefaultFilter(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== HAPTICS ====================
        GlassSettingsSectionCard(
            title = "Haptic Feedback",
            icon = Icons.Filled.Vibration
        ) {
            SettingsToggle(
                title = "Selection Haptics",
                subtitle = "Vibrate on tap, toggle, favourite",
                icon = Icons.Filled.TouchApp,
                checked = selectionHaptics,
                onCheckedChange = { selectionHaptics = it; appSettings.setSelectionHaptics(it) }
            )

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            SettingsToggle(
                title = "Uninstall Haptics",
                subtitle = "Vibrate on uninstall button press",
                icon = Icons.Filled.Delete,
                checked = uninstallHaptics,
                onCheckedChange = { uninstallHaptics = it; appSettings.setUninstallHaptics(it) }
            )

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            SettingsToggle(
                title = "Reinstall Haptics",
                subtitle = "Vibrate on reinstall button press",
                icon = Icons.Filled.Download,
                checked = reinstallHaptics,
                onCheckedChange = { reinstallHaptics = it; appSettings.setReinstallHaptics(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== APPEARANCE ====================
        GlassSettingsSectionCard(
            title = "Appearance",
            icon = Icons.Filled.Palette
        ) {
            // Dark Mode selector
            SettingsDropdown(
                title = "Theme",
                subtitle = when (darkMode) {
                    "dark" -> "Always dark"
                    "light" -> "Always light"
                    else -> "Follow system"
                },
                icon = Icons.Filled.DarkMode,
                selectedValue = darkMode,
                options = listOf("system" to "System Default", "dark" to "Dark", "light" to "Light"),
                onValueChange = { darkMode = it; appSettings.setDarkMode(it) }
            )

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            SettingsToggle(
                title = "Compact List View",
                subtitle = "Smaller cards, more items visible",
                icon = Icons.Filled.ViewCompact,
                checked = compactList,
                onCheckedChange = { compactList = it; appSettings.setCompactList(it) }
            )

            GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))

            SettingsToggle(
                title = "Screen Animations",
                subtitle = "Slide transitions between tabs",
                icon = Icons.Filled.Animation,
                checked = screenAnimations,
                onCheckedChange = { screenAnimations = it; appSettings.setScreenAnimations(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GDPR / Privacy Rights Section
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = SolanaGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Privacy Rights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "GDPR (EU) and CCPA (California) compliant",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Export My Data Button
            OutlinedButton(
                onClick = { showExportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export My Data")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delete My Data Button
            OutlinedButton(
                onClick = { showDeleteDataDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete My Data")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legal Section
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Legal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ListItem(
                headlineContent = { Text("Terms of Service") },
                supportingContent = { Text("v${TermsAcceptanceService.CURRENT_TOS_VERSION}") },
                leadingContent = {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                trailingContent = {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTermsDialog = true },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )

            GlassDivider()

            ListItem(
                headlineContent = { Text("Privacy Policy") },
                supportingContent = { Text("v${TermsAcceptanceService.CURRENT_PRIVACY_VERSION}") },
                leadingContent = {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                trailingContent = {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyDialog = true },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )

            GlassDivider()

            ListItem(
                headlineContent = { Text("Open Source Licenses") },
                supportingContent = { Text("Third-party libraries") },
                leadingContent = {
                    Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                trailingContent = {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLicensesDialog = true },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About AardAppvark",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Filled.Info,
                title = "Version",
                subtitle = BuildConfig.VERSION_NAME
            )

            SettingsItem(
                icon = Icons.Filled.Fingerprint,
                title = "Build",
                subtitle = BuildConfig.VERSION_CODE.toString()
            )

            SettingsItem(
                icon = Icons.Filled.Android,
                title = "Package",
                subtitle = BuildConfig.APPLICATION_ID
            )

            SettingsItem(
                icon = Icons.Filled.Code,
                title = "GitHub",
                subtitle = "github.com/AardAppvark/ADappvarkToolkit"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Branding
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AardAppvark",
                style = MaterialTheme.typography.titleMedium,
                color = SolanaPurple
            )
            Text(
                text = "First in line. Every dApp, every time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Built for Solana Seeker",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Consent Record Dialog
    if (showConsentRecordDialog) {
        val consentRecord = termsService.getConsentRecord()
        AlertDialog(
            onDismissRequest = { showConsentRecordDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Verified,
                    contentDescription = null,
                    tint = SolanaGreen,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Consent Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (consentRecord != null) {
                        ConsentInfoRow("Status", if (consentRecord.isSigned()) "Cryptographically Signed" else "Recorded")
                        ConsentInfoRow("Wallet", consentRecord.walletAddress.take(8) + "..." + consentRecord.walletAddress.takeLast(8))
                        ConsentInfoRow("Date", consentRecord.getFormattedTimestamp())
                        ConsentInfoRow("Terms Version", consentRecord.tosVersion)
                        ConsentInfoRow("Privacy Version", consentRecord.privacyVersion)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "This record proves you agreed to our Terms of Service and Privacy Policy. It is stored locally on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showConsentRecordDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Export Data Dialog
    if (showExportDialog) {
        val exportData = termsService.exportConsentRecordAsJson()
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = {
                Icon(Icons.Filled.Download, contentDescription = null)
            },
            title = { Text("Export My Data", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Your data export (GDPR Article 20 - Data Portability):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = exportData,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "This includes all personal data we have stored about you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Copy to clipboard
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("AardAppvark Data Export", exportData)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Data copied to clipboard", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Data Dialog
    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete My Data", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "GDPR Article 17 - Right to Erasure",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This will delete all locally stored data including:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("• Consent records", style = MaterialTheme.typography.bodySmall)
                    Text("• Wallet connection data", style = MaterialTheme.typography.bodySmall)
                    Text("• App preferences", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "IMPORTANT:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "• Blockchain transactions cannot be deleted\n• You will need to re-accept Terms to use the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDataDialog = false
                        showDeleteConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete My Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Final Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Are you absolutely sure?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This action cannot be undone. All your data will be permanently deleted and you will need to re-accept the Terms of Service to continue using AardAppvark.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Delete all data (GDPR Right to Erasure)
                        termsService.deleteAllConsentData()
                        userPrefs.resetAll()
                        seekerVerifier.resetAll()
                        creditManager.resetAll()
                        appSettings.resetAll()

                        Toast.makeText(context, "All data deleted. Please restart the app.", Toast.LENGTH_LONG).show()
                        showDeleteConfirmDialog = false

                        // In production, you would restart the app or navigate to onboarding
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getTermsOfServiceSummary(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getPrivacyPolicyText(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getOpenSourceLicensesText(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Disconnect Wallet Dialog
    if (showDisconnectWalletDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectWalletDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Disconnect Wallet?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "This will disconnect your wallet from AardAppvark.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "You will need to reconnect and re-accept the Terms of Service to continue using the dApp.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userPrefs.disconnectWallet()
                        showDisconnectWalletDialog = false
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
                Button(
                    onClick = { showDisconnectWalletDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Connect wallet / payment prompt dialog (shown when tapping locked SGT toggles)
    if (showConnectWalletPrompt) {
        AlertDialog(
            onDismissRequest = { showConnectWalletPrompt = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = SolanaPurple,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Wallet Required",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Connect a Solana wallet to access batch operations.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SolanaGreen.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = SolanaGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SGT holders get unlimited free batch operations",
                                style = MaterialTheme.typography.bodySmall,
                                color = SolanaGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SolanaPurple.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Token, contentDescription = null, tint = SolanaPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No SGT? Pay 0.01 SOL or 1 SKR per batch",
                                style = MaterialTheme.typography.bodySmall,
                                color = SolanaPurple,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConnectWalletPrompt = false
                        onDisconnectWallet() // Goes back to TermsAndWalletScreen to connect
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolanaPurple)
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Wallet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectWalletPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    PullToRefreshContainer(
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter)
    )
    } // end Box
}

// ==================== Payment-Gated Toggle ====================

@Composable
fun PaymentGatedToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    isPaid: Boolean,
    hasWallet: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onUnlockClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isPaid) {
                    onCheckedChange(!checked)
                } else {
                    onUnlockClick()
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPaid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPaid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = if (isPaid) subtitle else if (hasWallet) "Unlock to enable" else "Connect wallet to enable",
                style = MaterialTheme.typography.bodySmall,
                color = if (isPaid) MaterialTheme.colorScheme.onSurfaceVariant else SolanaPurple.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        if (isPaid) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SolanaGreen,
                    checkedTrackColor = SolanaGreen.copy(alpha = 0.3f)
                )
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked",
                tint = SolanaPurple.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==================== Batch Operation Row (SGT unlimited / pay-per-use gateway) ====================

@Composable
fun BatchOperationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSgtVerified: Boolean,
    isWalletConnected: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLockedTap: () -> Unit = {}
) {
    val isLocked = !isWalletConnected
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isLocked) {
                    onLockedTap()
                } else if (!isSgtVerified) {
                    onCheckedChange(!checked)
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    isSgtVerified -> subtitle
                    isLocked -> "Connect wallet for free SGT use or pay per batch"
                    else -> "Pay 0.01 SOL or 1 SKR per batch (5+ dApps)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        if (isSgtVerified) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SolanaGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Unlimited",
                    style = MaterialTheme.typography.labelSmall,
                    color = SolanaGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        } else if (isLocked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SolanaPurple.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Token,
                        contentDescription = "Pay per use",
                        tint = SolanaPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "0.01 SOL",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolanaPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==================== SGT Behaviour Toggle (locked ON without SGT, green when SGT) ====================

@Composable
fun SgtBehaviourToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSgtVerified: Boolean,
    isWalletConnected: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLockedTap: () -> Unit = {}
) {
    val isLocked = !isWalletConnected
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isLocked) {
                    onLockedTap()
                } else if (isSgtVerified) {
                    onCheckedChange(!checked)
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                   else if (isSgtVerified) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else if (isSgtVerified) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        else if (isSgtVerified) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        if (isSgtVerified) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SolanaGreen,
                    checkedTrackColor = SolanaGreen.copy(alpha = 0.3f),
                    uncheckedThumbColor = SolanaGreen.copy(alpha = 0.5f),
                    uncheckedTrackColor = SolanaGreen.copy(alpha = 0.1f),
                    uncheckedBorderColor = SolanaGreen.copy(alpha = 0.3f)
                )
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked",
                tint = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==================== Helper Composables ====================

@Composable
fun ConsentInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== Glass Settings Section Card ====================

@Composable
fun GlassSettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

// ==================== Reusable Settings Components ====================

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsDropdown(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Box {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (value == selectedValue) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(label)
                            }
                        },
                        onClick = {
                            onValueChange(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun getTermsOfServiceSummary(): String {
    return """
AARDAPPVARK TERMS OF SERVICE
Summary Version (v${TermsAcceptanceService.CURRENT_TOS_VERSION})

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

IMPORTANT: By using AardAppvark, you agree to be bound by our full Terms of Service which you accepted during onboarding.

KEY TERMS:

1. ELIGIBILITY
• Must be 18+ years of age
• Not located in sanctioned jurisdictions (Cuba, Iran, North Korea, Syria, Crimea)
• Not on any OFAC, EU, UK, UN, or Australian sanctions lists

2. PAYMENTS
• Operations with up to 4 apps are FREE
• Bulk operations (5+ apps) require 0.01 SOL (or 1 SKR)
• One-time fee for unlimited apps per operation
• All payments are processed on-chain

3. PROHIBITED USES
• No illegal activities in any jurisdiction
• No money laundering or terrorist financing
• No malware distribution
• No circumventing app store terms
• No VPN use to bypass restrictions
• No account abuse

4. LIABILITY
• App provided "AS IS" without warranties
• We are not liable for data loss, fund loss, or damages
• Maximum liability capped at ${'$'}100 or 12-month payments

5. INDEMNIFICATION
You agree to defend, indemnify, and hold harmless AardAppvark from any claims arising from your use or misuse of the application.

6. DISPUTE RESOLUTION
• Binding arbitration for disputes
• Class action waiver
• Jury trial waiver

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Your consent was cryptographically recorded with your wallet address during onboarding.
    """.trimIndent()
}

private fun getPrivacyPolicyText(): String {
    return """
AARDAPPVARK PRIVACY POLICY
Version ${TermsAcceptanceService.CURRENT_PRIVACY_VERSION}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. INFORMATION WE COLLECT

• Wallet Address: Your Solana public key
• Consent Record: Proof of Terms acceptance
• Credit Transactions: Purchase and usage history
• Device ID: Hashed for privacy
• Usage Data: App operations performed

We DO NOT collect: names, emails, locations, private keys, or browsing history.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

2. LEGAL BASIS (GDPR)

• Contract: To provide our services
• Consent: Your explicit agreement
• Legitimate Interest: Security and fraud prevention
• Legal Obligation: Regulatory compliance

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

3. DATA SHARING

We DO NOT sell your data.

We may share with:
• Blockchain: Public transaction data
• Law Enforcement: If legally required
• Service Providers: Under strict confidentiality

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

4. YOUR RIGHTS

GDPR (EU): Access, rectification, erasure, portability, objection
CCPA (California): Know, delete, opt-out, non-discrimination

Use "Export My Data" and "Delete My Data" in Settings.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

5. DATA RETENTION

• Consent records: Retained as legal proof
• Credits: Until expiration (12 months)
• Blockchain data: Permanent (cannot be deleted)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

6. BLOCKCHAIN TRANSPARENCY

All Solana transactions are publicly visible and permanent.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Contact: Submit inquiries through the Solana dApp Store.
    """.trimIndent()
}

private fun getOpenSourceLicensesText(): String {
    return """
OPEN SOURCE LICENSES

AardAppvark uses the following open source components:

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ANDROID JETPACK
Copyright Google LLC
Apache License 2.0

• Jetpack Compose UI
• Navigation
• Activity
• Core KTX
• Lifecycle

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

KOTLIN
Copyright JetBrains s.r.o.
Apache License 2.0

• Kotlin Standard Library
• Kotlin Coroutines
• Kotlin Serialization

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

MATERIAL DESIGN 3
Copyright Google LLC
Apache License 2.0

• Material3 Components
• Material Icons

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

SOLANA MOBILE
Copyright Solana Mobile Inc.
Apache License 2.0

• Mobile Wallet Adapter Client

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

APACHE LICENSE 2.0

Licensed under the Apache License, Version 2.0.

http://www.apache.org/licenses/LICENSE-2.0
    """.trimIndent()
}
