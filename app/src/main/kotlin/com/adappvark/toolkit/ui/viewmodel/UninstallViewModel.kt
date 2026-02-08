package com.adappvark.toolkit.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adappvark.toolkit.data.model.DAppFilter
import com.adappvark.toolkit.data.model.DAppInfo
import com.adappvark.toolkit.data.model.DAppSort
import com.adappvark.toolkit.data.model.Feature
import com.adappvark.toolkit.service.PackageManagerService
import com.adappvark.toolkit.service.ShizukuManager
import com.adappvark.toolkit.service.SubscriptionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for UninstallScreen
 * Manages app list, selection, and uninstall operations
 */
class UninstallViewModel(
    private val context: Context
) : ViewModel() {
    
    private val packageService = PackageManagerService(context)
    private val shizukuManager = ShizukuManager(context)
    private val subscriptionManager = SubscriptionManager(context)
    
    // State flows
    private val _dAppList = MutableStateFlow<List<DAppInfo>>(emptyList())
    val dAppList: StateFlow<List<DAppInfo>> = _dAppList.asStateFlow()
    
    private val _selectedDApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedDApps: StateFlow<Set<String>> = _selectedDApps.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isUninstalling = MutableStateFlow(false)
    val isUninstalling: StateFlow<Boolean> = _isUninstalling.asStateFlow()
    
    private val _uninstallProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val uninstallProgress: StateFlow<Pair<Int, Int>?> = _uninstallProgress.asStateFlow()
    
    private val _currentUninstallingApp = MutableStateFlow<String?>(null)
    val currentUninstallingApp: StateFlow<String?> = _currentUninstallingApp.asStateFlow()
    
    private val _hasUninstallAccess = MutableStateFlow(false)
    val hasUninstallAccess: StateFlow<Boolean> = _hasUninstallAccess.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private var currentFilter = DAppFilter.DAPP_STORE_ONLY
    private var currentSort = DAppSort.NAME_ASC
    
    init {
        checkSubscriptionAccess()
    }
    
    /**
     * Check if user has subscription access
     */
    private fun checkSubscriptionAccess() {
        viewModelScope.launch {
            _hasUninstallAccess.value = subscriptionManager.hasFeature(Feature.BULK_UNINSTALL)
        }
    }
    
    /**
     * Scan installed dApps
     */
    fun scanDApps(filter: DAppFilter = DAppFilter.DAPP_STORE_ONLY) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                currentFilter = filter
                
                val apps = packageService.scanInstalledApps(filter, currentSort)
                _dAppList.value = apps
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to scan apps: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle dApp selection
     */
    fun toggleDAppSelection(packageName: String) {
        val current = _selectedDApps.value
        _selectedDApps.value = if (current.contains(packageName)) {
            current - packageName
        } else {
            current + packageName
        }
    }
    
    /**
     * Select all dApps
     */
    fun selectAll() {
        _selectedDApps.value = _dAppList.value.map { it.packageName }.toSet()
    }
    
    /**
     * Clear selection
     */
    fun clearSelection() {
        _selectedDApps.value = emptySet()
    }
    
    /**
     * Perform bulk uninstall
     */
    fun performBulkUninstall() {
        viewModelScope.launch {
            try {
                _isUninstalling.value = true
                _errorMessage.value = null
                _successMessage.value = null
                
                val packagesToUninstall = _selectedDApps.value.toList()
                
                val results = shizukuManager.bulkUninstall(
                    packageNames = packagesToUninstall,
                    onProgress = { current, total, packageName ->
                        _uninstallProgress.value = current to total
                        _currentUninstallingApp.value = packageName
                    }
                )
                
                // Count successes and failures
                val successCount = results.count { it.second.isSuccess }
                val failCount = results.size - successCount
                
                // Update success message
                _successMessage.value = "Uninstalled $successCount apps" +
                    if (failCount > 0) " ($failCount failed)" else ""
                
                // Clear selection and rescan
                _selectedDApps.value = emptySet()
                scanDApps(currentFilter)
                
            } catch (e: Exception) {
                _errorMessage.value = "Uninstall failed: ${e.message}"
            } finally {
                _isUninstalling.value = false
                _uninstallProgress.value = null
                _currentUninstallingApp.value = null
            }
        }
    }
    
    /**
     * Change filter
     */
    fun changeFilter(filter: DAppFilter) {
        if (currentFilter != filter) {
            scanDApps(filter)
        }
    }
    
    /**
     * Change sort order
     */
    fun changeSort(sort: DAppSort) {
        if (currentSort != sort) {
            currentSort = sort
            scanDApps(currentFilter)
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }
    
    /**
     * Get total size of selected dApps
     */
    fun getSelectedTotalSize(): Long {
        val selected = _selectedDApps.value
        return _dAppList.value
            .filter { selected.contains(it.packageName) }
            .sumOf { it.sizeInBytes }
    }
    
    /**
     * Format bytes to human-readable string
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
