package com.adappvark.toolkit

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.adappvark.toolkit.service.CreditManager
import com.adappvark.toolkit.service.GeoRestrictionService
import com.adappvark.toolkit.service.SeekerVerificationService
import com.adappvark.toolkit.service.UserPreferencesManager
import com.adappvark.toolkit.ui.navigation.AppNavigation
import com.adappvark.toolkit.ui.screens.TermsAndWalletScreen
import com.adappvark.toolkit.ui.theme.ADappvarkToolkitTheme
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    // ActivityResultSender for MWA - must be created at activity level
    private lateinit var activityResultSender: ActivityResultSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MWA activity result sender
        activityResultSender = ActivityResultSender(this)

        setContent {
            ADappvarkToolkitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AardAppvarkApp(activityResultSender)
                }
            }
        }
    }
}

@Composable
fun AardAppvarkApp(activityResultSender: ActivityResultSender) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesManager(context) }
    val seekerVerifier = remember { SeekerVerificationService(context) }
    val creditManager = remember { CreditManager(context) }

    // Check if onboarding is complete (T&C accepted AND wallet connected)
    var isOnboardingComplete by remember {
        mutableStateOf(userPrefs.hasAcceptedTerms() && userPrefs.isWalletConnected())
    }

    if (!isOnboardingComplete) {
        // Show combined Terms & Wallet screen
        TermsAndWalletScreen(
            activityResultSender = activityResultSender,
            onComplete = { publicKey, walletName ->
                // Record T&C acceptance with wallet address
                userPrefs.acceptTerms(publicKey)

                // Save wallet connection
                userPrefs.saveWalletConnection(publicKey, walletName)

                // Mark onboarding complete
                userPrefs.setOnboardingComplete()
                isOnboardingComplete = true
            },
            onExit = {
                // Activity will handle exit via finish()
            }
        )
    } else {
        // Verify SGT on-chain after wallet connect (non-blocking for UI)
        // This checks if the user's wallet holds a Seeker Genesis Token
        // and grants bonus credits accordingly
        LaunchedEffect(Unit) {
            val walletAddress = userPrefs.getWalletPublicKey()
            if (walletAddress != null) {
                try {
                    val sgtResult = seekerVerifier.verifySeekerDevice(walletAddress)

                    if (sgtResult.hasSgt) {
                        Log.i("AardAppvark", "Seeker verified! SGT #${sgtResult.memberNumber}")

                        // Grant 2 free credits for verified Seeker (or upgrade if already got 1)
                        if (!creditManager.hasReceivedFreeCredit()) {
                            creditManager.grantWalletConnectCredit(walletAddress, isVerifiedSeeker = true)
                            seekerVerifier.markSeekerBonusGranted()
                            Log.i("AardAppvark", "Granted 2 Seeker bonus credits")
                        } else if (!seekerVerifier.hasSeekerBonusBeenGranted()) {
                            // User already got 1 generic credit — upgrade to 2
                            if (creditManager.upgradeToSeekerBonus(walletAddress)) {
                                seekerVerifier.markSeekerBonusGranted()
                                Log.i("AardAppvark", "Upgraded to Seeker bonus (1 → 2 credits)")
                            }
                        }
                    } else {
                        Log.d("AardAppvark", "No SGT found — standard wallet")
                        // Grant 1 generic credit if not already granted
                        if (!creditManager.hasReceivedFreeCredit()) {
                            creditManager.grantWalletConnectCredit(walletAddress, isVerifiedSeeker = false)
                            Log.i("AardAppvark", "Granted 1 standard credit")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("AardAppvark", "SGT verification error: ${e.message}")
                    // On error, still grant 1 generic credit so user isn't penalised
                    if (!creditManager.hasReceivedFreeCredit()) {
                        creditManager.grantWalletConnectCredit(walletAddress, isVerifiedSeeker = false)
                    }
                }
            }
        }

        // Periodic geo re-check every 30 minutes while app is in use
        // Detects if user enables VPN mid-session
        LaunchedEffect(Unit) {
            val geoService = GeoRestrictionService(context)
            while (true) {
                delay(30 * 60 * 1000L) // 30 minutes
                val result = geoService.checkGeoRestriction()
                if (result is GeoRestrictionService.GeoCheckResult.Blocked) {
                    // User is now in a blocked jurisdiction — force exit
                    Log.w("AardAppvark", "Periodic geo-check: BLOCKED in ${result.countryName}")
                    isOnboardingComplete = false // Will show blocked screen on TermsAndWalletScreen
                }
            }
        }

        // Show main app
        AppNavigation(
            activityResultSender = activityResultSender,
            onDisconnectWallet = {
                // Reset onboarding state to show Terms & Wallet screen again
                isOnboardingComplete = false
            }
        )
    }
}
