package com.adappvark.toolkit.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility Service for automatic uninstall AND install confirmation
 *
 * This service monitors for:
 * 1. Android's package installer dialogs - auto-clicks "OK" or "Uninstall"
 * 2. Solana dApp Store install buttons - auto-clicks "Install"
 *
 * This enables bulk uninstallation and reinstallation without manual confirmation for each app.
 * User must explicitly enable this service in Android Settings.
 */
class AutoUninstallService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoUninstallService"

        // Mode for what action we're auto-clicking
        enum class Mode {
            UNINSTALL,       // Auto-click uninstall confirmations
            INSTALL,         // Auto-click install buttons in dApp Store (single app page)
            CATEGORY_INSTALL // Auto-click install buttons for specific apps in category list view
        }

        // Package installer package names (varies by Android version/manufacturer)
        private val INSTALLER_PACKAGES = setOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.oppo.packageinstaller",
            "com.vivo.packageinstaller",
            "com.huawei.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller"
        )

        // Solana dApp Store package names
        private val DAPP_STORE_PACKAGES = setOf(
            "com.solanamobile.dappstore",
            "com.solanamobile.apps"
        )

        // Keywords that indicate this is a package installer dialog
        private val INSTALLER_KEYWORDS = listOf(
            "packageinstaller",
            "permissioncontroller",
            "installer"
        )

        // Button text for UNINSTALL confirmations
        private val UNINSTALL_BUTTON_TEXTS = listOf(
            "OK", "Ok", "ok",
            "Uninstall", "UNINSTALL", "uninstall",
            "Confirm", "CONFIRM", "confirm",
            "Yes", "YES", "yes",
            "Delete", "DELETE", "delete",
            "Remove", "REMOVE", "remove"
        )

        // Button text for INSTALL actions in dApp Store
        // CRITICAL: Only match EXACT "Install" text - nothing else!
        // Previously had too many matches which caused false positives
        private val INSTALL_BUTTON_TEXTS = listOf(
            "Install"
        )

        // Broader list for permission dialogs only
        private val PERMISSION_BUTTON_TEXTS = listOf(
            "Install", "INSTALL",
            "Allow", "ALLOW",
            "OK", "Ok",
            "Accept", "ACCEPT",
            "Continue", "CONTINUE"
        )

        // Text that indicates the page is still loading - don't click yet!
        private val LOADING_INDICATORS = listOf(
            "Loading app Name",
            "Loading description",
            "Loading"
        )

        // Flag to signal that we found "Open" button (app already installed)
        @Volatile
        var foundOpenButton = false
            private set

        // Flag to signal that installation is in progress (Cancel button visible)
        @Volatile
        var installationInProgress = false
            private set

        // Flag to signal that Install button was successfully clicked
        // This is set IMMEDIATELY when we click Install, before Cancel appears
        @Volatile
        var installButtonClicked = false
            private set

        fun resetOpenButtonFlag() {
            foundOpenButton = false
            installationInProgress = false
            installButtonClicked = false
        }

        // Track if service is currently active
        @Volatile
        var isServiceRunning = false
            private set

        // Flag to enable/disable auto-clicking (controlled by app)
        @Volatile
        var autoClickEnabled = false

        // Current mode (uninstall or install)
        @Volatile
        var currentMode = Mode.UNINSTALL

        // Debug: last seen package name for troubleshooting
        @Volatile
        var lastSeenPackage: String? = null

        // Track if we've already clicked to avoid double-clicks
        @Volatile
        private var lastClickTime = 0L
        private const val CLICK_DEBOUNCE_MS = 1000L

        // For CATEGORY_INSTALL mode: list of app names to install (matched against visible list)
        @Volatile
        var targetAppNames: Set<String> = emptySet()

        // Track which apps have been successfully installed (clicked Install)
        @Volatile
        var installedAppNames: MutableSet<String> = mutableSetOf()
            private set

        // Track which apps were found already installed (Open button)
        @Volatile
        var alreadyInstalledAppNames: MutableSet<String> = mutableSetOf()
            private set

        // Counter for how many Install buttons we've clicked in this session
        @Volatile
        var installClickCount = 0
            private set

        fun setTargetApps(appNames: Set<String>) {
            targetAppNames = appNames.map { it.lowercase().trim() }.toSet()
            installedAppNames = mutableSetOf()
            alreadyInstalledAppNames = mutableSetOf()
            installClickCount = 0
            Log.i(TAG, "=== TARGET APPS SET ===")
            Log.i(TAG, "Original names: $appNames")
            Log.i(TAG, "Lowercase names: $targetAppNames")
            for (name in targetAppNames) {
                val normalized = name.replace(".", "").replace(" ", "").replace("-", "")
                Log.i(TAG, "  '$name' -> normalized: '$normalized'")
            }
        }

        fun clearTargetApps() {
            targetAppNames = emptySet()
            installedAppNames = mutableSetOf()
            alreadyInstalledAppNames = mutableSetOf()
            installClickCount = 0
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true

        // Configure the service to monitor ALL apps (needed for dApp Store)
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
            // Don't filter by package - we need to see dApp Store events
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        lastSeenPackage = packageName

        // Always log events from dApp Store for debugging
        val isDAppStore = packageName in DAPP_STORE_PACKAGES ||
                packageName.contains("solanamobile", ignoreCase = true)
        if (isDAppStore) {
            Log.i(TAG, "dApp Store event: type=${event.eventType}, autoClickEnabled=$autoClickEnabled, mode=$currentMode")
        }

        if (!autoClickEnabled) return

        // Debounce clicks
        val now = System.currentTimeMillis()
        if (now - lastClickTime < CLICK_DEBOUNCE_MS) {
            return
        }

        Log.d(TAG, "Event from package: $packageName, mode: $currentMode, event type: ${event.eventType}")

        // Get the root node of the current window
        val rootNode = rootInActiveWindow ?: run {
            Log.d(TAG, "rootInActiveWindow is null")
            return
        }

        try {
            when (currentMode) {
                Mode.UNINSTALL -> handleUninstallMode(packageName, rootNode)
                Mode.INSTALL -> handleInstallMode(packageName, rootNode)
                Mode.CATEGORY_INSTALL -> handleCategoryInstallMode(packageName, rootNode)
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Handle uninstall mode - click OK/Uninstall in system dialogs
     */
    private fun handleUninstallMode(packageName: String, rootNode: AccessibilityNodeInfo) {
        // Check if this looks like a package installer
        val isInstaller = packageName in INSTALLER_PACKAGES ||
                INSTALLER_KEYWORDS.any { packageName.lowercase().contains(it) }

        if (!isInstaller && !isUninstallDialog(rootNode)) {
            return
        }

        if (isUninstallDialog(rootNode)) {
            Log.d(TAG, "Found uninstall dialog - searching for confirm button...")
            if (findAndClickButton(rootNode, UNINSTALL_BUTTON_TEXTS)) {
                lastClickTime = System.currentTimeMillis()
                Log.d(TAG, "Successfully clicked uninstall confirm button!")
            }
        }
    }

    /**
     * Handle install mode - click Install in dApp Store
     */
    private fun handleInstallMode(packageName: String, rootNode: AccessibilityNodeInfo) {
        // Only act on dApp Store
        val isDAppStore = packageName in DAPP_STORE_PACKAGES ||
                packageName.contains("solanamobile", ignoreCase = true)

        if (!isDAppStore) {
            // Also handle permission dialogs that pop up during install
            if (packageName in INSTALLER_PACKAGES ||
                INSTALLER_KEYWORDS.any { packageName.lowercase().contains(it) }) {
                // Click Allow/OK on permission dialogs
                if (findAndClickButton(rootNode, PERMISSION_BUTTON_TEXTS)) {
                    lastClickTime = System.currentTimeMillis()
                    Log.d(TAG, "Clicked permission/install dialog button")
                }
            }
            return
        }

        // First check if page is still loading - don't click anything yet
        if (isPageStillLoading(rootNode)) {
            Log.i(TAG, "Page still loading - waiting for content...")
            return
        }

        // Log current flag state for debugging
        Log.i(TAG, "In dApp Store - flags: installButtonClicked=$installButtonClicked, foundOpenButton=$foundOpenButton, installationInProgress=$installationInProgress")

        // If we already clicked Install for this app, don't try again
        if (installButtonClicked) {
            Log.i(TAG, "Install already clicked, skipping further attempts")
            return
        }

        // First check if we see "Open" button - means app is already installed
        if (hasOpenButton(rootNode)) {
            Log.i(TAG, "Found 'Open' button - app is ALREADY INSTALLED, skipping...")
            foundOpenButton = true
            lastClickTime = System.currentTimeMillis()
            return
        }

        // Check if "Cancel" button is visible - means installation/download is in progress!
        if (hasCancelButton(rootNode)) {
            Log.i(TAG, "Found 'Cancel' button - installation IN PROGRESS, waiting...")
            installationInProgress = true
            lastClickTime = System.currentTimeMillis()
            return
        }

        // Try to find and click Install button
        Log.i(TAG, "Attempting to find and click Install button...")
        if (findAndClickButton(rootNode, INSTALL_BUTTON_TEXTS)) {
            lastClickTime = System.currentTimeMillis()
            installButtonClicked = true  // Set flag immediately when Install is clicked
            Log.i(TAG, "SUCCESS: Clicked Install button in dApp Store! installButtonClicked=true")
        } else {
            Log.w(TAG, "Install button not found - logging UI for debug...")
            // Log the UI tree to help debug
            logNodeTree(rootNode, 0)
        }
    }

    /**
     * Handle category install mode - find and click Install buttons for target apps in category list view
     * This mode scans the visible app list and clicks Install for apps matching our target list
     *
     * Uses GESTURE-BASED clicking to tap at the exact center of the Install button,
     * avoiding issues where ACTION_CLICK triggers the wrong element.
     */
    private fun handleCategoryInstallMode(packageName: String, rootNode: AccessibilityNodeInfo) {
        // Only act on dApp Store
        val isDAppStore = packageName in DAPP_STORE_PACKAGES ||
                packageName.contains("solanamobile", ignoreCase = true)

        if (!isDAppStore) {
            // Also handle permission dialogs that pop up during install
            if (packageName in INSTALLER_PACKAGES ||
                INSTALLER_KEYWORDS.any { packageName.lowercase().contains(it) }) {
                if (findAndClickButton(rootNode, PERMISSION_BUTTON_TEXTS)) {
                    lastClickTime = System.currentTimeMillis()
                    Log.d(TAG, "Clicked permission/install dialog button")
                }
            }
            return
        }

        if (targetAppNames.isEmpty()) {
            Log.w(TAG, "CATEGORY_INSTALL mode but no target apps set!")
            return
        }

        Log.i(TAG, "CATEGORY_INSTALL: Scanning for ${targetAppNames.size} target apps: $targetAppNames")
        Log.i(TAG, "CATEGORY_INSTALL: Already clicked: ${installedAppNames.size}, Already installed: ${alreadyInstalledAppNames.size}")

        // Find all Install buttons using text search
        val installNodes = rootNode.findAccessibilityNodeInfosByText("Install")
        Log.i(TAG, "CATEGORY_INSTALL: Found ${installNodes.size} Install text nodes")

        for (installNode in installNodes) {
            val nodeText = installNode.text?.toString()?.trim() ?: ""

            // Only process nodes that are exactly "Install"
            if (!nodeText.equals("Install", ignoreCase = true)) {
                installNode.recycle()
                continue
            }

            // Get the bounds of this Install button
            val bounds = Rect()
            installNode.getBoundsInScreen(bounds)
            Log.i(TAG, "CATEGORY_INSTALL: Install button bounds: $bounds")

            // Search up to find the app name
            var appName: String? = null
            var searchNode = installNode.parent
            var level = 0

            while (searchNode != null && level < 5 && appName == null) {
                appName = findAppNameInRow(searchNode)
                if (appName == null) {
                    val nextParent = searchNode.parent
                    searchNode.recycle()
                    searchNode = nextParent
                    level++
                } else {
                    searchNode.recycle()
                }
            }
            searchNode?.recycle()

            if (appName != null) {
                // Skip if already processed
                if (installedAppNames.contains(appName) || alreadyInstalledAppNames.contains(appName)) {
                    Log.i(TAG, "CATEGORY_INSTALL: $appName already processed")
                    installNode.recycle()
                    continue
                }

                Log.i(TAG, "CATEGORY_INSTALL: Found Install for target app: $appName at bounds $bounds")

                // Use GESTURE to tap at the center of the Install button
                // This is more reliable than ACTION_CLICK which can trigger parent containers
                val centerX = bounds.centerX().toFloat()
                val centerY = bounds.centerY().toFloat()

                Log.i(TAG, "CATEGORY_INSTALL: Tapping at coordinates ($centerX, $centerY)")

                val clicked = performTapGesture(centerX, centerY)

                if (clicked) {
                    installedAppNames.add(appName)
                    installClickCount++
                    lastClickTime = System.currentTimeMillis()
                    Log.i(TAG, "CATEGORY_INSTALL: SUCCESS! Gesture tap for $appName. Total: $installClickCount")
                    installNode.recycle()
                    installNodes.forEach { it.recycle() }
                    return  // Exit after one successful click to let UI settle
                } else {
                    Log.w(TAG, "CATEGORY_INSTALL: Gesture tap failed for $appName")
                }
            }

            installNode.recycle()
        }

        // Also scan for Open buttons to mark as already installed
        val openNodes = rootNode.findAccessibilityNodeInfosByText("Open")
        for (openNode in openNodes) {
            val nodeText = openNode.text?.toString()?.trim() ?: ""
            if (!nodeText.equals("Open", ignoreCase = true)) {
                openNode.recycle()
                continue
            }

            var appName: String? = null
            var searchNode = openNode.parent
            var level = 0
            while (searchNode != null && level < 5 && appName == null) {
                appName = findAppNameInRow(searchNode)
                if (appName == null) {
                    val nextParent = searchNode.parent
                    searchNode.recycle()
                    searchNode = nextParent
                    level++
                } else {
                    searchNode.recycle()
                }
            }
            searchNode?.recycle()

            if (appName != null && !alreadyInstalledAppNames.contains(appName)) {
                Log.i(TAG, "CATEGORY_INSTALL: $appName has Open - already installed")
                alreadyInstalledAppNames.add(appName)
            }
            openNode.recycle()
        }
    }

    /**
     * Perform a tap gesture at specific screen coordinates
     * This is more precise than ACTION_CLICK which can be intercepted by parent views
     */
    private fun performTapGesture(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "Gesture API not available on this Android version")
            return false
        }

        try {
            val path = Path()
            path.moveTo(x, y)

            val gestureBuilder = GestureDescription.Builder()
            val stroke = GestureDescription.StrokeDescription(path, 0, 50)  // 50ms tap
            gestureBuilder.addStroke(stroke)

            val gesture = gestureBuilder.build()

            return dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    Log.i(TAG, "Gesture tap completed successfully")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w(TAG, "Gesture tap was cancelled")
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing gesture tap: ${e.message}")
            return false
        }
    }

    /**
     * Search a row/container for an app name that matches our target list
     * Returns the matched target app name (lowercase) or null
     */
    private fun findAppNameInRow(rowNode: AccessibilityNodeInfo): String? {
        return searchForAppNameInRow(rowNode, 0)
    }

    private fun searchForAppNameInRow(node: AccessibilityNodeInfo, depth: Int): String? {
        if (depth > 5) return null

        val nodeText = node.text?.toString()?.trim() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.trim() ?: ""

        // Check if this text matches any target app
        val matched = findMatchingAppName(nodeText, nodeDesc)
        if (matched != null) {
            return matched
        }

        // Search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchForAppNameInRow(child, depth + 1)
            child.recycle()
            if (result != null) return result
        }

        return null
    }

    /**
     * Check if text matches any of our target app names
     * Uses flexible matching to handle variations like:
     * - "pump.fun" vs "PumpFun" vs "Pump.fun"
     * - "URnetwork" vs "urnetwork" vs "UR network"
     */
    private fun findMatchingAppName(text: String, desc: String): String? {
        // Skip empty text
        if (text.isEmpty() && desc.isEmpty()) return null

        // Skip common non-app texts
        val skipTexts = listOf("install", "open", "cancel", "update", "get", "download")
        if (text.lowercase() in skipTexts || desc.lowercase() in skipTexts) return null

        val textLower = text.lowercase().trim()
        val descLower = desc.lowercase().trim()

        // Also create normalized versions (remove dots, spaces, special chars)
        val textNormalized = textLower.replace(".", "").replace(" ", "").replace("-", "")
        val descNormalized = descLower.replace(".", "").replace(" ", "").replace("-", "")

        // DEBUG: Log all text we're checking against targets
        if (text.isNotEmpty() && text.length > 2 && text.length < 50) {
            Log.d(TAG, "MATCH_CHECK: text='$text' normalized='$textNormalized' against targets=$targetAppNames")
        }

        for (targetName in targetAppNames) {
            val targetNormalized = targetName.replace(".", "").replace(" ", "").replace("-", "")

            // Exact match
            if (textLower == targetName || descLower == targetName) {
                Log.i(TAG, "MATCH: Exact match! '$text' == '$targetName'")
                return targetName
            }

            // Starts with match (for "pump.fun - description" style)
            if (textLower.startsWith("$targetName ") ||
                textLower.startsWith("$targetName\n") ||
                descLower.startsWith("$targetName ")) {
                Log.i(TAG, "MATCH: Prefix match! '$text' starts with '$targetName'")
                return targetName
            }

            // Normalized match (handles "pump.fun" vs "PumpFun")
            if (textNormalized == targetNormalized || descNormalized == targetNormalized) {
                Log.i(TAG, "MATCH: Normalized match! '$text' ('$textNormalized') == target '$targetName' ('$targetNormalized')")
                return targetName
            }

            // Normalized starts with match
            if (textNormalized.startsWith(targetNormalized) || descNormalized.startsWith(targetNormalized)) {
                Log.i(TAG, "MATCH: Normalized prefix match! '$text' ('$textNormalized') starts with '$targetNormalized'")
                return targetName
            }

            // REVERSE check: maybe dApp Store name is SHORTER than our stored name
            // e.g., we have "Solana Merge Game" but dApp Store shows "Solana Merge"
            if (targetNormalized.startsWith(textNormalized) && textNormalized.length >= 4) {
                Log.i(TAG, "MATCH: Reverse prefix match! target '$targetName' starts with text '$text'")
                return targetName
            }
        }
        return null
    }

    /**
     * Check if the page has a "Cancel" button, indicating installation is in progress
     */
    private fun hasCancelButton(rootNode: AccessibilityNodeInfo): Boolean {
        val nodes = rootNode.findAccessibilityNodeInfosByText("Cancel")
        if (nodes.isNotEmpty()) {
            val found = nodes.any { node ->
                val nodeText = node.text?.toString()?.trim() ?: ""
                val nodeDesc = node.contentDescription?.toString()?.trim() ?: ""
                // Match "Cancel" text or "Cancel AppName" description
                val result = nodeText.equals("Cancel", ignoreCase = true) ||
                        nodeDesc.startsWith("Cancel ", ignoreCase = true)
                result
            }
            nodes.forEach { it.recycle() }
            if (found) {
                return true
            }
        }
        return false
    }

    /**
     * Check if the page has an "Open" button, indicating the app is already installed
     * Also checks for "Uninstall" button presence which is another indicator
     * Uses recursive search to handle Compose UI where text may be in child nodes
     */
    private fun hasOpenButton(rootNode: AccessibilityNodeInfo): Boolean {
        // First try standard search
        val openTexts = listOf("Open", "OPEN")
        for (text in openTexts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                val nodeText = node.text?.toString()?.trim() ?: ""
                val nodeDesc = node.contentDescription?.toString()?.trim() ?: ""
                // Make sure it's actually the "Open" action button, not just any text
                // Match: "Open", "OPEN", or contentDescription like "Open Jito"
                if (nodeText.equals("Open", ignoreCase = true) ||
                    nodeText.equals("OPEN", ignoreCase = true) ||
                    nodeDesc.equals("Open", ignoreCase = true) ||
                    nodeDesc.startsWith("Open ", ignoreCase = true)) {
                    Log.i(TAG, "Found 'Open' button via standard search: text='$nodeText' desc='$nodeDesc'")
                    node.recycle()
                    nodes.forEach { it.recycle() }
                    return true
                }
                node.recycle()
            }
        }

        // Also check for "Uninstall" in dApp Store - indicates app is installed
        val uninstallNodes = rootNode.findAccessibilityNodeInfosByText("Uninstall")
        if (uninstallNodes.isNotEmpty()) {
            val found = uninstallNodes.any { node ->
                val nodeText = node.text?.toString()?.trim() ?: ""
                val result = nodeText.equals("Uninstall", ignoreCase = true)
                if (result) {
                    Log.i(TAG, "Found 'Uninstall' button - app is already installed")
                }
                result
            }
            uninstallNodes.forEach { it.recycle() }
            if (found) return true
        }

        // Recursive fallback: search entire tree for "Open" text
        // This catches cases where findAccessibilityNodeInfosByText fails
        if (hasOpenButtonRecursive(rootNode, 0)) {
            return true
        }

        return false
    }

    /**
     * Recursively search for "Open" button in the accessibility tree
     * This handles Compose UIs where findAccessibilityNodeInfosByText may fail
     */
    private fun hasOpenButtonRecursive(node: AccessibilityNodeInfo, depth: Int): Boolean {
        if (depth > 10) return false

        val nodeText = node.text?.toString()?.trim() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.trim() ?: ""

        // Check if this node has "Open" text
        if (nodeText.equals("Open", ignoreCase = true) ||
            nodeDesc.equals("Open", ignoreCase = true) ||
            nodeDesc.startsWith("Open ", ignoreCase = true)) {
            Log.i(TAG, "Found 'Open' button via recursive search: text='$nodeText' desc='$nodeDesc'")
            foundOpenButton = true
            return true
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (hasOpenButtonRecursive(child, depth + 1)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    /**
     * Check if the dApp Store page is still loading
     * Uses recursive search since findAccessibilityNodeInfosByText may not work reliably
     */
    private fun isPageStillLoading(rootNode: AccessibilityNodeInfo): Boolean {
        // First try the standard API
        for (indicator in LOADING_INDICATORS) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(indicator)
            if (nodes.isNotEmpty()) {
                Log.d(TAG, "Loading detected via findByText: '$indicator'")
                nodes.forEach { it.recycle() }
                return true
            }
        }

        // Fallback: recursive search through the tree
        if (hasLoadingIndicatorRecursive(rootNode, 0)) {
            return true
        }

        return false
    }

    /**
     * Recursively search for loading indicators in the accessibility tree
     */
    private fun hasLoadingIndicatorRecursive(node: AccessibilityNodeInfo, depth: Int): Boolean {
        if (depth > 8) return false

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        // Check for loading indicators
        for (indicator in LOADING_INDICATORS) {
            if (text.contains(indicator, ignoreCase = true) ||
                desc.contains(indicator, ignoreCase = true)) {
                Log.d(TAG, "Loading detected via recursive: text='$text' indicator='$indicator'")
                return true
            }
        }

        // Check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (hasLoadingIndicatorRecursive(child, depth + 1)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    /**
     * Check if this window contains uninstall-related text
     */
    private fun isUninstallDialog(rootNode: AccessibilityNodeInfo): Boolean {
        val uninstallIndicators = listOf(
            "uninstall",
            "Do you want to uninstall",
            "will be uninstalled",
            "remove this app"
        )

        for (indicator in uninstallIndicators) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(indicator)
            if (nodes.isNotEmpty()) {
                Log.d(TAG, "Found uninstall indicator: '$indicator'")
                nodes.forEach { it.recycle() }
                return true
            }
        }
        return false
    }

    /**
     * Generic button finder and clicker - improved for dApp Store compatibility
     * Uses EXACT matching for "Install" to avoid false positives
     */
    private fun findAndClickButton(rootNode: AccessibilityNodeInfo, buttonTexts: List<String>): Boolean {
        // Try each possible button text
        for (buttonText in buttonTexts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(buttonText)

            for (node in nodes) {
                val nodeText = node.text?.toString()?.trim() ?: ""
                val nodeDesc = node.contentDescription?.toString()?.trim() ?: ""
                val nodeClass = node.className?.toString() ?: ""

                // Use EXACT matching for "Install" to avoid clicking random buttons
                // For "Install", only click if text is exactly "Install" (case insensitive)
                val textMatches = if (buttonText.equals("Install", ignoreCase = true)) {
                    nodeText.equals("Install", ignoreCase = true)
                } else {
                    nodeText.contains(buttonText, ignoreCase = true)
                }

                val descMatches = if (buttonText.equals("Install", ignoreCase = true)) {
                    nodeDesc.equals("Install", ignoreCase = true)
                } else {
                    nodeDesc.contains(buttonText, ignoreCase = true)
                }

                if (!textMatches && !descMatches) {
                    node.recycle()
                    continue
                }

                Log.d(TAG, "Found button: text='$nodeText' desc='$nodeDesc' class='$nodeClass' clickable=${node.isClickable}")

                if (node.isClickable && node.isEnabled) {
                    val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d(TAG, "Click result: $result")
                    node.recycle()
                    if (result) return true
                } else {
                    // Try clicking the parent if the node itself isn't clickable
                    if (tryClickParent(node)) {
                        node.recycle()
                        return true
                    }
                    node.recycle()
                }
            }
        }

        // Try finding by view ID (includes dApp Store IDs)
        if (findAndClickByViewId(rootNode)) {
            return true
        }

        // Last resort: recursive search through entire tree for clickable Install buttons
        return findAndClickButtonRecursive(rootNode, buttonTexts)
    }

    /**
     * Recursively search entire accessibility tree for buttons
     * Uses EXACT matching for "Install" to avoid false positives
     */
    private fun findAndClickButtonRecursive(node: AccessibilityNodeInfo, buttonTexts: List<String>, depth: Int = 0): Boolean {
        if (depth > 10) return false // Prevent infinite recursion

        val nodeText = node.text?.toString()?.trim() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.trim() ?: ""

        // Check if this node matches any of our target button texts
        for (buttonText in buttonTexts) {
            // Use EXACT matching for "Install" to avoid clicking random buttons
            val textMatches = if (buttonText.equals("Install", ignoreCase = true)) {
                nodeText.equals("Install", ignoreCase = true)
            } else {
                nodeText.contains(buttonText, ignoreCase = true)
            }

            val descMatches = if (buttonText.equals("Install", ignoreCase = true)) {
                nodeDesc.equals("Install", ignoreCase = true)
            } else {
                nodeDesc.contains(buttonText, ignoreCase = true)
            }

            if ((textMatches || descMatches) && node.isEnabled) {
                Log.d(TAG, "Recursive found: text='$nodeText' desc='$nodeDesc' clickable=${node.isClickable}")

                if (node.isClickable) {
                    val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (result) {
                        Log.d(TAG, "Recursive click succeeded!")
                        return true
                    }
                }

                // Try parent if this node isn't clickable
                if (tryClickParent(node)) {
                    return true
                }
            }
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickButtonRecursive(child, buttonTexts, depth + 1)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    /**
     * Try clicking parent nodes up to 5 levels
     */
    private fun tryClickParent(node: AccessibilityNodeInfo): Boolean {
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 5) {
            if (parent.isClickable && parent.isEnabled) {
                Log.d(TAG, "Clicking parent at depth $depth")
                val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                if (result) return true
            }
            val grandParent = parent.parent
            parent.recycle()
            parent = grandParent
            depth++
        }
        parent?.recycle()
        return false
    }

    /**
     * Try to find button by common view IDs (includes dApp Store IDs)
     */
    private fun findAndClickByViewId(rootNode: AccessibilityNodeInfo): Boolean {
        val commonIds = listOf(
            // Android system IDs
            "android:id/button1",
            "android:id/button2",
            "android:id/action1",
            // Package installer IDs
            "com.android.packageinstaller:id/ok_button",
            "com.android.packageinstaller:id/uninstall_button",
            "com.google.android.packageinstaller:id/ok_button",
            "com.google.android.packageinstaller:id/uninstall_button",
            // Solana dApp Store IDs (potential)
            "com.solanamobile.dappstore:id/install_button",
            "com.solanamobile.dappstore:id/action_button",
            "com.solanamobile.dappstore:id/primary_button",
            "com.solanamobile.apps:id/install_button",
            "com.solanamobile.apps:id/action_button",
            "com.solanamobile.apps:id/primary_button"
        )

        for (viewId in commonIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)

            for (node in nodes) {
                if (node.isClickable && node.isEnabled) {
                    Log.d(TAG, "Clicking node with ID $viewId")
                    val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    node.recycle()
                    if (result) return true
                }
                node.recycle()
            }
        }

        return false
    }

    /**
     * Log the accessibility node tree for debugging
     * Now includes contentDescription for better Compose UI analysis
     */
    private fun logNodeTree(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > 10) return // Increase depth limit for Compose UIs

        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val className = node.className?.toString()?.substringAfterLast(".") ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val clickable = node.isClickable
        val enabled = node.isEnabled

        // Log ALL clickable nodes (even without text) and nodes with text/desc
        if (clickable || text.isNotEmpty() || desc.isNotEmpty()) {
            Log.i(TAG, "UI: $indent[$className] text='$text' desc='$desc' click=$clickable enabled=$enabled")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                logNodeTree(child, depth + 1)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        // Called when the system wants to interrupt the service
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        autoClickEnabled = false
    }
}
