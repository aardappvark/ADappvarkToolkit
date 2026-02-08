package com.adappvark.toolkit.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.adappvark.toolkit.data.model.Feature
import com.adappvark.toolkit.data.model.SubscriptionPlan
import com.adappvark.toolkit.data.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manager for subscription state and feature access
 */
class SubscriptionManager(private val context: Context) {
    
    companion object {
        private val Context.subscriptionDataStore: DataStore<Preferences> by preferencesDataStore(
            name = "subscription_prefs"
        )
        
        // DataStore keys
        private val KEY_SUBSCRIPTION_PLAN = stringPreferencesKey("subscription_plan")
        private val KEY_IS_ACTIVE = booleanPreferencesKey("is_active")
        private val KEY_EXPIRES_AT = longPreferencesKey("expires_at")
        private val KEY_LAST_PAYMENT_TX = stringPreferencesKey("last_payment_tx")
        
        // Subscription duration
        private const val SUBSCRIPTION_DURATION_MS = 7 * 24 * 60 * 60 * 1000L  // 7 days
    }
    
    private val dataStore = context.subscriptionDataStore
    
    /**
     * Get current subscription status as Flow
     */
    fun getSubscriptionStatusFlow(): Flow<SubscriptionStatus> {
        return dataStore.data.map { preferences ->
            val planId = preferences[KEY_SUBSCRIPTION_PLAN]
            val plan = planId?.let { id ->
                SubscriptionPlan.values().find { it.id == id }
            }
            
            SubscriptionStatus(
                plan = plan,
                isActive = preferences[KEY_IS_ACTIVE] ?: false,
                expiresAt = preferences[KEY_EXPIRES_AT],
                lastPaymentTxId = preferences[KEY_LAST_PAYMENT_TX]
            )
        }
    }
    
    /**
     * Get current subscription status (suspend)
     */
    suspend fun getSubscriptionStatus(): SubscriptionStatus {
        return getSubscriptionStatusFlow().first()
    }
    
    /**
     * Check if subscription is currently valid
     */
    suspend fun hasActiveSubscription(): Boolean {
        val status = getSubscriptionStatus()
        return status.isValid()
    }
    
    /**
     * Check if a specific feature is available
     */
    suspend fun hasFeature(feature: Feature): Boolean {
        val status = getSubscriptionStatus()
        return status.hasFeature(feature)
    }
    
    /**
     * Activate subscription after successful payment
     */
    suspend fun activateSubscription(
        plan: SubscriptionPlan,
        transactionId: String
    ) {
        val expiresAt = System.currentTimeMillis() + SUBSCRIPTION_DURATION_MS
        
        dataStore.edit { preferences ->
            preferences[KEY_SUBSCRIPTION_PLAN] = plan.id
            preferences[KEY_IS_ACTIVE] = true
            preferences[KEY_EXPIRES_AT] = expiresAt
            preferences[KEY_LAST_PAYMENT_TX] = transactionId
        }
    }
    
    /**
     * Manually deactivate subscription (cancel)
     */
    suspend fun deactivateSubscription() {
        dataStore.edit { preferences ->
            preferences[KEY_IS_ACTIVE] = false
        }
    }
    
    /**
     * Check and update subscription expiration
     * Should be called on app start
     */
    suspend fun checkExpiration() {
        val status = getSubscriptionStatus()
        
        if (status.isActive && !status.isValid()) {
            // Subscription expired, deactivate
            deactivateSubscription()
        }
    }
    
    /**
     * Get time remaining on subscription
     */
    suspend fun getTimeRemaining(): Long {
        val status = getSubscriptionStatus()
        val expiresAt = status.expiresAt ?: return 0L
        
        return maxOf(0L, expiresAt - System.currentTimeMillis())
    }
    
    /**
     * Get days remaining on subscription
     */
    suspend fun getDaysRemaining(): Int {
        val timeRemaining = getTimeRemaining()
        return (timeRemaining / (24 * 60 * 60 * 1000)).toInt()
    }
    
    /**
     * Clear all subscription data (for testing/debugging)
     */
    suspend fun clearSubscriptionData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

/**
 * Extension function to get SubscriptionManager
 */
fun Context.getSubscriptionManager(): SubscriptionManager {
    return SubscriptionManager(this)
}
