package com.adappvark.toolkit.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.adappvark.toolkit.ui.components.AardvarkIcon

/**
 * Terms and Conditions acceptance screen
 * Must be shown before user can access the app
 */
@Composable
fun TermsScreen(
    onAccept: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var isChecked by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section with logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App Logo
            Card(
                modifier = Modifier.size(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AardvarkIcon(
                        size = 90.dp,
                        color = MaterialTheme.colorScheme.primary,
                        filled = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AardAppvark",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Solana Seeker dApp Toolkit",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Middle section - Important Notice
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Important Notice!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Before entering AardAppvark, you must first agree to our Terms and Conditions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terms and Conditions link
                    TextButton(
                        onClick = { showTermsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Read Terms and Conditions",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Checkbox agreement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I have read and agree to the Terms and Conditions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Bottom section - Buttons
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                enabled = isChecked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accept & Continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    // Exit the app
                    (context as? Activity)?.finish()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.ExitToApp,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exit",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Terms and Conditions Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text(
                    text = "Terms and Conditions",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getTermsAndConditionsText(),
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
}

/**
 * Comprehensive Terms and Conditions text
 * Compliant with USA, EU (GDPR), UK, Australia, and international standards
 */
private fun getTermsAndConditionsText(): String {
    val currentDate = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        .format(java.util.Date())

    return """
AARDAPPVARK TERMS OF SERVICE AND END USER LICENSE AGREEMENT

Effective Date: $currentDate
Version: 1.0

PLEASE READ THESE TERMS CAREFULLY BEFORE USING THIS APPLICATION. BY ACCESSING OR USING AARDAPPVARK, YOU AGREE TO BE BOUND BY THESE TERMS AND ALL APPLICABLE LAWS AND REGULATIONS.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. DEFINITIONS AND INTERPRETATION

1.1 "Application" or "App" refers to AardAppvark, a mobile application for managing decentralized applications (dApps) on Solana Seeker devices.

1.2 "User," "You," or "Your" refers to any individual or entity accessing or using the Application.

1.3 "We," "Us," "Our," or "Provider" refers to the developers and operators of AardAppvark.

1.4 "Services" refers to all features, functions, and capabilities provided through the Application, including but not limited to bulk uninstallation, reinstallation, and management of applications.

1.5 "Digital Assets" refers to cryptocurrencies, tokens, or other blockchain-based assets including but not limited to SOL (Solana) and SKR (Seeker) tokens.

1.6 "Wallet" refers to any digital cryptocurrency wallet used to interact with the Application.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

2. ACCEPTANCE OF TERMS

2.1 By downloading, installing, accessing, or using this Application, you acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions in their entirety.

2.2 If you do not agree to these Terms, you must immediately cease using the Application and delete it from your device.

2.3 Your continued use of the Application constitutes ongoing acceptance of these Terms and any amendments thereto.

2.4 You represent and warrant that you have the legal capacity to enter into this binding agreement in your jurisdiction.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

3. ELIGIBILITY AND GEOGRAPHIC RESTRICTIONS

3.1 AGE REQUIREMENTS
You must be at least eighteen (18) years of age, or the age of legal majority in your jurisdiction, whichever is higher, to use this Application.

3.2 PROHIBITED JURISDICTIONS
This Application is NOT available to users located in, or citizens or residents of:

(a) Countries subject to comprehensive sanctions by the United States, European Union, United Kingdom, United Nations, or other applicable authorities, including but not limited to:
    - Cuba
    - Iran
    - North Korea (DPRK)
    - Syria
    - Crimea, Donetsk, and Luhansk regions of Ukraine
    - Any other jurisdiction subject to comprehensive trade sanctions

(b) Any jurisdiction where the use of blockchain technology, cryptocurrencies, or applications of this nature is prohibited or restricted by law.

3.3 SANCTIONED PERSONS
You represent and warrant that you are NOT:

(a) Listed on any sanctions list maintained by:
    - U.S. Office of Foreign Assets Control (OFAC)
    - U.S. Department of State
    - U.S. Department of Commerce
    - European Union sanctions lists
    - United Kingdom HM Treasury sanctions lists
    - United Nations Security Council sanctions lists
    - Australian Department of Foreign Affairs and Trade sanctions lists

(b) Owned or controlled by, or acting on behalf of, any person or entity on such lists.

(c) Located in, or a national or resident of, any prohibited jurisdiction.

3.4 You agree to immediately discontinue use of the Application if you become subject to any sanctions or relocate to a prohibited jurisdiction.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

4. PROHIBITED USES AND CONDUCT

4.1 GENERAL PROHIBITIONS
You agree NOT to use the Application to:

(a) Violate any applicable local, state, national, or international law or regulation in your jurisdiction or any other jurisdiction.

(b) Engage in any activity that is fraudulent, deceptive, misleading, or harmful.

(c) Facilitate, promote, or engage in money laundering, terrorist financing, or any other financial crime.

(d) Evade or circumvent any sanctions, export controls, or trade restrictions.

(e) Infringe upon the intellectual property rights, privacy rights, or any other rights of third parties.

(f) Distribute malware, viruses, trojans, or any other malicious software or code.

(g) Attempt to gain unauthorized access to any systems, networks, or accounts.

(h) Interfere with or disrupt the Application's infrastructure or any connected services.

(i) Use the Application in any manner that could damage, disable, overburden, or impair its functionality.

(j) Engage in any activity that could harm minors or vulnerable persons.

4.2 SPECIFIC PROHIBITED ACTIVITIES
You expressly agree NOT to:

(a) Use the Application to uninstall, install, or manage applications for any unlawful purpose.

(b) Use the Application to facilitate the distribution of pirated, counterfeit, or unauthorized software.

(c) Use the Application to circumvent digital rights management (DRM) or copy protection mechanisms.

(d) Use the Application to violate any app store terms of service or developer agreements.

(e) Use the Application in connection with any illegal gambling, unauthorized securities offerings, or prohibited financial activities.

(f) Transfer, sell, or otherwise provide access to your account or credits to sanctioned individuals or entities.

(g) Use virtual private networks (VPNs), proxies, or other tools to circumvent geographic restrictions or misrepresent your location.

(h) Create multiple accounts to abuse promotional credits or circumvent usage limitations.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

5. PAYMENTS AND PRICING

5.1 FREE TIER
(a) Operations involving up to four (4) applications are free of charge.
(b) Favoriting applications is free.
(c) One (1) free credit is granted upon first wallet connection.

5.2 PAID OPERATIONS
(a) Bulk operations involving five (5) or more applications require a flat fee of 0.01 SOL per operation.
(b) Each new bulk operation requires a new payment.
(c) Payments are accepted in SOL (Solana) and SKR (Seeker) tokens.
(d) All transactions are processed on the Solana blockchain and are subject to network transaction fees.

5.3 NON-REFUNDABLE POLICY
(a) ALL PAYMENTS ARE FINAL AND NON-REFUNDABLE.
(b) Credits cannot be exchanged, transferred, or converted to cash or any other form of value.
(c) We do not provide refunds for unused credits, accidental purchases, or any other reason.

5.4 CREDIT EXPIRATION
(a) Credits expire twelve (12) months from the date of purchase or grant.
(b) Expired credits cannot be reinstated or refunded.
(c) You are solely responsible for using credits before expiration.

5.5 ANTI-MONEY LAUNDERING (AML) COMPLIANCE
(a) We reserve the right to refuse, suspend, or terminate any transaction that we reasonably believe may involve money laundering, terrorist financing, fraud, or any other illicit activity.
(b) We may implement transaction monitoring and reporting procedures as required by applicable law.
(c) You agree to cooperate with any investigations and provide additional information as requested.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

6. INTELLECTUAL PROPERTY RIGHTS

6.1 The Application, including all content, features, functionality, design elements, source code, and documentation, is owned by the Provider and is protected by international copyright, trademark, patent, trade secret, and other intellectual property laws.

6.2 You are granted a limited, non-exclusive, non-transferable, revocable license to use the Application for personal, non-commercial purposes in accordance with these Terms.

6.3 You may not copy, modify, distribute, sell, lease, sublicense, reverse engineer, decompile, or create derivative works of the Application without express written permission.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

7. DISCLAIMER OF WARRANTIES

7.1 THE APPLICATION IS PROVIDED "AS IS" AND "AS AVAILABLE" WITHOUT ANY WARRANTIES OF ANY KIND, WHETHER EXPRESS, IMPLIED, OR STATUTORY.

7.2 TO THE FULLEST EXTENT PERMITTED BY APPLICABLE LAW, WE EXPRESSLY DISCLAIM ALL WARRANTIES, INCLUDING BUT NOT LIMITED TO:

(a) IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.

(b) WARRANTIES REGARDING THE ACCURACY, RELIABILITY, COMPLETENESS, OR TIMELINESS OF THE APPLICATION OR ANY CONTENT.

(c) WARRANTIES THAT THE APPLICATION WILL BE UNINTERRUPTED, ERROR-FREE, SECURE, OR FREE OF VIRUSES OR OTHER HARMFUL COMPONENTS.

(d) WARRANTIES REGARDING THE RESULTS OBTAINED FROM THE USE OF THE APPLICATION.

7.3 YOU ACKNOWLEDGE THAT THE USE OF BLOCKCHAIN TECHNOLOGY AND CRYPTOCURRENCY INVOLVES INHERENT RISKS, INCLUDING BUT NOT LIMITED TO MARKET VOLATILITY, REGULATORY CHANGES, TECHNOLOGICAL FAILURES, AND LOSS OF DIGITAL ASSETS.

7.4 WE DO NOT WARRANT THAT THE APPLICATION WILL MEET YOUR REQUIREMENTS OR EXPECTATIONS.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

8. LIMITATION OF LIABILITY

8.1 TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL THE PROVIDER, ITS AFFILIATES, DIRECTORS, OFFICERS, EMPLOYEES, AGENTS, OR LICENSORS BE LIABLE FOR:

(a) ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, PUNITIVE, OR EXEMPLARY DAMAGES.

(b) ANY LOSS OF PROFITS, REVENUE, DATA, GOODWILL, OR OTHER INTANGIBLE LOSSES.

(c) ANY LOSS OF OR DAMAGE TO DIGITAL ASSETS, CRYPTOCURRENCIES, OR TOKENS.

(d) ANY UNAUTHORIZED ACCESS TO, ALTERATION OF, OR LOSS OF YOUR DATA OR TRANSMISSIONS.

(e) ANY ERRORS, MISTAKES, OR INACCURACIES IN THE APPLICATION OR CONTENT.

(f) ANY INTERRUPTION OR CESSATION OF THE APPLICATION OR SERVICES.

(g) ANY BUGS, VIRUSES, OR OTHER HARMFUL CODE TRANSMITTED THROUGH THE APPLICATION.

(h) ANY THIRD-PARTY CONDUCT OR CONTENT.

8.2 IN NO EVENT SHALL OUR TOTAL LIABILITY TO YOU FOR ALL CLAIMS EXCEED THE GREATER OF: (A) THE AMOUNT YOU PAID TO US IN THE TWELVE (12) MONTHS PRECEDING THE CLAIM, OR (B) ONE HUNDRED U.S. DOLLARS (USD $100).

8.3 SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR LIMITATION OF CERTAIN DAMAGES. IN SUCH JURISDICTIONS, OUR LIABILITY IS LIMITED TO THE MAXIMUM EXTENT PERMITTED BY LAW.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

9. INDEMNIFICATION

9.1 YOU AGREE TO DEFEND, INDEMNIFY, AND HOLD HARMLESS THE PROVIDER, ITS AFFILIATES, AND THEIR RESPECTIVE DIRECTORS, OFFICERS, EMPLOYEES, AGENTS, LICENSORS, AND SERVICE PROVIDERS FROM AND AGAINST ANY AND ALL CLAIMS, LIABILITIES, DAMAGES, JUDGMENTS, AWARDS, LOSSES, COSTS, EXPENSES, AND FEES (INCLUDING REASONABLE ATTORNEYS' FEES) ARISING OUT OF OR RELATING TO:

(a) Your use or misuse of the Application.

(b) Your violation of these Terms.

(c) Your violation of any applicable law, regulation, or third-party rights.

(d) Your content, data, or information submitted through the Application.

(e) Your negligence, willful misconduct, or fraudulent activities.

(f) Any dispute between you and any third party.

(g) Your failure to comply with applicable sanctions, export controls, or anti-money laundering requirements.

9.2 We reserve the right, at your expense, to assume the exclusive defense and control of any matter for which you are required to indemnify us, and you agree to cooperate with our defense of such claims.

9.3 This indemnification obligation shall survive the termination of these Terms and your use of the Application.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

10. PRIVACY AND DATA PROTECTION

10.1 DATA COLLECTION
We collect and process the following information:
(a) Wallet addresses connected to the Application.
(b) Transaction history and credit balances.
(c) Timestamp of Terms acceptance.
(d) Device and usage information for analytics purposes.

10.2 DATA USE
Your information is used to:
(a) Provide and maintain the Application and Services.
(b) Process transactions and manage credits.
(c) Comply with legal and regulatory obligations.
(d) Detect, prevent, and address fraud or security issues.

10.3 BLOCKCHAIN TRANSPARENCY
You acknowledge that transactions conducted on the Solana blockchain are publicly visible and permanently recorded.

10.4 GDPR COMPLIANCE (European Users)
If you are located in the European Economic Area (EEA), you have rights under the General Data Protection Regulation (GDPR), including the right to access, rectify, erase, restrict processing, and data portability. Contact us to exercise these rights.

10.5 CCPA COMPLIANCE (California Users)
If you are a California resident, you have rights under the California Consumer Privacy Act (CCPA), including the right to know, delete, and opt-out of the sale of personal information.

10.6 We do not sell your personal information to third parties.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

11. TERMINATION

11.1 We may terminate or suspend your access to the Application immediately, without prior notice or liability, for any reason, including but not limited to breach of these Terms.

11.2 Upon termination:
(a) Your right to use the Application ceases immediately.
(b) All unused credits are forfeited without refund.
(c) Provisions that by their nature should survive termination shall survive.

11.3 You may terminate your use of the Application at any time by deleting it from your device. No refunds will be provided for unused credits.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

12. DISPUTE RESOLUTION AND GOVERNING LAW

12.1 GOVERNING LAW
These Terms shall be governed by and construed in accordance with the laws of the jurisdiction where the Provider is established, without regard to conflict of law principles.

12.2 DISPUTE RESOLUTION
(a) Any dispute arising out of or relating to these Terms shall first be attempted to be resolved through good faith negotiation.

(b) If negotiation fails, disputes shall be resolved through binding arbitration in accordance with the rules of a recognized arbitration institution.

(c) The arbitration shall be conducted in English.

(d) The arbitrator's decision shall be final and binding.

12.3 CLASS ACTION WAIVER
YOU AGREE THAT ANY DISPUTES SHALL BE RESOLVED ON AN INDIVIDUAL BASIS AND NOT AS A CLASS ACTION, COLLECTIVE ACTION, OR REPRESENTATIVE ACTION. YOU WAIVE ANY RIGHT TO PARTICIPATE IN A CLASS ACTION LAWSUIT OR CLASS-WIDE ARBITRATION.

12.4 JURY TRIAL WAIVER
TO THE EXTENT PERMITTED BY LAW, YOU WAIVE ANY RIGHT TO A JURY TRIAL IN ANY PROCEEDING ARISING OUT OF OR RELATING TO THESE TERMS.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

13. REGULATORY COMPLIANCE

13.1 You acknowledge that the regulatory status of blockchain technology, cryptocurrencies, and decentralized applications varies by jurisdiction and is subject to change.

13.2 You are solely responsible for determining whether your use of the Application complies with applicable laws in your jurisdiction.

13.3 We make no representations regarding the legality of the Application or Services in your jurisdiction.

13.4 We reserve the right to modify, suspend, or discontinue the Application or Services to comply with applicable laws or regulations.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

14. MODIFICATIONS TO TERMS

14.1 We reserve the right to modify these Terms at any time in our sole discretion.

14.2 Material changes will be communicated through the Application or other reasonable means.

14.3 Your continued use of the Application after any modifications constitutes acceptance of the updated Terms.

14.4 If you do not agree to the modified Terms, you must discontinue use of the Application.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

15. GENERAL PROVISIONS

15.1 ENTIRE AGREEMENT
These Terms constitute the entire agreement between you and the Provider regarding the Application and supersede all prior agreements and understandings.

15.2 SEVERABILITY
If any provision of these Terms is found to be invalid or unenforceable, the remaining provisions shall continue in full force and effect.

15.3 WAIVER
The failure to enforce any right or provision of these Terms shall not constitute a waiver of such right or provision.

15.4 ASSIGNMENT
You may not assign or transfer these Terms or your rights hereunder without our prior written consent. We may assign our rights and obligations without restriction.

15.5 NO THIRD-PARTY BENEFICIARIES
These Terms do not create any third-party beneficiary rights.

15.6 FORCE MAJEURE
We shall not be liable for any failure to perform due to causes beyond our reasonable control, including but not limited to natural disasters, war, terrorism, riots, pandemics, government actions, or failures of third-party services.

15.7 HEADINGS
Section headings are for convenience only and shall not affect the interpretation of these Terms.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

16. ACKNOWLEDGMENT

BY CLICKING "ACCEPT" OR BY USING THE APPLICATION, YOU ACKNOWLEDGE THAT:

(a) You have read and understood these Terms in their entirety.

(b) You agree to be legally bound by these Terms.

(c) You are not located in a prohibited jurisdiction.

(d) You are not a sanctioned person or entity.

(e) You will not use the Application for any unlawful purpose.

(f) You assume all risks associated with the use of the Application and blockchain technology.

(g) You agree to indemnify and hold harmless the Provider as set forth herein.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

CONTACT INFORMATION

For questions, concerns, or requests regarding these Terms, please contact us through the Solana dApp Store or submit an inquiry through our official communication channels.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

End of Terms and Conditions
    """.trimIndent()
}
