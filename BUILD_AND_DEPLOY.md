# Build & Deployment Guide - ADappvark Toolkit

## 🎯 Pre-Deployment Checklist

### **CRITICAL CONFIGURATION**

Before building for production, you MUST update:

1. **Payment Wallet Address**
   ```kotlin
   // File: app/src/main/kotlin/com/adappvark/toolkit/AppConfig.kt
   // Line 15
   
   const val WALLET_ADDRESS = "YOUR_MAINNET_WALLET_ADDRESS"
   ```

2. **Network Configuration**
   ```kotlin
   // File: app/src/main/kotlin/com/adappvark/toolkit/AppConfig.kt
   // Lines 18-19
   
   const val CLUSTER = "mainnet-beta"  // NOT devnet!
   const val RPC_ENDPOINT = "https://api.mainnet-beta.solana.com"
   ```

3. **App Identity URLs** (if you have a domain)
   ```kotlin
   // File: app/src/main/kotlin/com/adappvark/toolkit/AppConfig.kt
   // Lines 26-28
   
   const val URI = "https://yourdomain.com"
   const val ICON_URI = "https://yourdomain.com/favicon.ico"
   ```

---

## 🔑 Generate Release Keystore

### **First Time Setup**

```bash
# Navigate to project root
cd ADappvarkToolkit

# Generate keystore
keytool -genkey -v -keystore release-keystore.jks \
  -alias adappvark-release \
  -keyalg RSA -keysize 2048 \
  -validity 10000

# You'll be prompted for:
# - Keystore password (SAVE THIS!)
# - Key password (SAVE THIS!)
# - Your name/organization
# - Location details
```

**CRITICAL:** Save these passwords securely! Without them, you cannot update your app.

### **Create keystore.properties**

```bash
# Create file in project root
touch keystore.properties
```

**Add to keystore.properties:**
```properties
storeFile=release-keystore.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=adappvark-release
keyPassword=YOUR_KEY_PASSWORD
```

**Add to .gitignore:**
```bash
echo "keystore.properties" >> .gitignore
echo "*.jks" >> .gitignore
echo "*.keystore" >> .gitignore
```

---

## 🔧 Configure Build for Release

### **Update app/build.gradle.kts**

Add signing configuration:

```kotlin
android {
    // ... existing config ...
    
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## 🏗️ Build Release APK

### **Clean Build**

```bash
# Clean previous builds
./gradlew clean

# Verify configuration
./gradlew tasks
```

### **Build Signed Release**

```bash
# Build release APK
./gradlew assembleRelease

# Output location:
# app/build/outputs/apk/release/app-release.apk
```

### **Verify APK**

```bash
# Check APK is signed
jarsigner -verify -verbose -certs \
  app/build/outputs/apk/release/app-release.apk

# Should show: "jar verified"
```

### **Get APK SHA256**

```bash
# For dApp Store submission
sha256sum app/build/outputs/apk/release/app-release.apk
```

---

## 📦 Build Android App Bundle (AAB)

**Note:** Solana dApp Store accepts APK. Use AAB only if also publishing to Google Play.

```bash
# Build bundle
./gradlew bundleRelease

# Output:
# app/build/outputs/bundle/release/app-release.aab
```

---

## 🧪 Test Release Build

### **Install on Device**

```bash
# Uninstall debug version first
adb uninstall com.adappvark.toolkit

# Install release APK
adb install app/build/outputs/apk/release/app-release.apk
```

### **Test Checklist**

- [ ] App launches without crash
- [ ] Shizuku permission works
- [ ] Package scanning works
- [ ] Subscription screen loads
- [ ] Payment flow works (CRITICAL - test on mainnet with real SOL)
- [ ] Uninstall feature works
- [ ] No debug logs visible
- [ ] ProGuard not breaking anything

---

## 🚀 Deployment Process

### **Option 1: Solana dApp Store (Recommended)**

1. **Prepare Assets**
   - App icon (512x512)
   - Screenshots (1080x1920, at least 4)
   - Feature graphic (1200x600)

2. **Create Listing**
   - Go to: https://dapp-store.solanamobile.com/publisher
   - Create account if needed
   - Fill in app details (use DAPP_STORE_GUIDE.md)

3. **Upload APK**
   - Upload: `app/build/outputs/apk/release/app-release.apk`
   - Fill in version info (1.0.0, version code 1)

4. **Submit for Review**
   - Add test instructions (see DAPP_STORE_GUIDE.md)
   - Wait 2-3 days for approval

### **Option 2: Self-Distribution**

```bash
# Host APK on your website
# Users can download directly

# Generate QR code for easy install:
# https://www.qr-code-generator.com/
# Link to: https://yourdomain.com/adappvark-toolkit.apk
```

### **Option 3: GitHub Releases**

```bash
# Create GitHub repository
# Upload APK to Releases section
# Users download from GitHub

# Release notes in: RELEASE_NOTES.md
```

---

## 📱 Update Process

### **Version Bump**

1. **Update version in build.gradle.kts**
   ```kotlin
   defaultConfig {
       versionCode = 2  // Increment
       versionName = "1.0.1"  // Update
   }
   ```

2. **Update BuildConfig.kt**
   ```kotlin
   const val VERSION_NAME = "1.0.1"
   const val VERSION_CODE = 2
   ```

3. **Build new release**
   ```bash
   ./gradlew clean assembleRelease
   ```

4. **Submit update to dApp Store**

---

## 🔍 Quality Assurance

### **Pre-Release Testing**

```bash
# Run lint checks
./gradlew lint

# Check for common issues
./gradlew lintRelease

# View lint report
open app/build/reports/lint-results-release.html
```

### **ProGuard Verification**

```bash
# Build release
./gradlew assembleRelease

# Test extensively!
# ProGuard can break things silently
```

### **APK Size Optimization**

```bash
# Check APK size
ls -lh app/build/outputs/apk/release/app-release.apk

# Target: < 10MB
# If larger, review dependencies
```

---

## 🐛 Troubleshooting

### **Issue: "Task failed with an exception"**

```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleRelease
```

### **Issue: "Keystore not found"**

- Verify keystore.properties exists
- Check paths are correct
- Ensure keystore.jks is in project root

### **Issue: "ProGuard breaks app"**

- Check proguard-rules.pro
- Add keep rules for affected classes
- Test thoroughly

### **Issue: "APK won't install"**

```bash
# Check signature
jarsigner -verify app/build/outputs/apk/release/app-release.apk

# Check for conflicting versions
adb uninstall com.adappvark.toolkit
```

---

## 📊 Build Variants

### **Debug Build**

```bash
./gradlew assembleDebug
```

**Features:**
- No signing required
- ProGuard disabled
- Debug logs enabled
- Faster build time

### **Release Build**

```bash
./gradlew assembleRelease
```

**Features:**
- Signed with release keystore
- ProGuard enabled
- No debug logs
- Optimized bytecode

---

## 🔐 Security Best Practices

1. **Never commit:**
   - keystore.properties
   - *.jks files
   - Wallet private keys
   - API secrets

2. **Backup securely:**
   - Release keystore
   - Keystore passwords
   - Wallet recovery phrase

3. **Use separate wallets:**
   - Development/testing wallet (devnet)
   - Production wallet (mainnet)
   - Never use personal wallet

---

## 📈 Analytics & Monitoring

### **Post-Launch Monitoring**

```bash
# Check crash reports (if you add Sentry/Crashlytics later)
# Monitor wallet for incoming payments
# Track downloads from dApp Store
# Monitor support email
```

### **Payment Verification**

```bash
# Check mainnet wallet on Solscan
https://solscan.io/account/YOUR_WALLET_ADDRESS

# Verify subscription payments arriving
# Monitor for any issues
```

---

## 🎯 Launch Day Checklist

**24 Hours Before:**
- [ ] Final testing on real device
- [ ] Payment flow tested with real SOL (small amount)
- [ ] All assets uploaded to dApp Store
- [ ] Website live (if applicable)
- [ ] Support email configured
- [ ] Privacy Policy & Terms accessible

**Launch Day:**
- [ ] Monitor dApp Store for approval
- [ ] Respond to any reviewer questions
- [ ] Prepare announcement
- [ ] Monitor support email
- [ ] Watch for payments in wallet

**Post-Launch (Week 1):**
- [ ] Monitor for crashes/bugs
- [ ] Respond to user feedback
- [ ] Track subscription signups
- [ ] Prepare first update (if needed)

---

## 💰 Revenue Tracking

### **Monitor Payments**

```bash
# Check Solscan
https://solscan.io/account/YOUR_WALLET_ADDRESS

# Filter by:
# - Amount (0.005, 0.01, 0.012 SOL)
# - Recent transactions
# - Number of unique payers
```

### **Calculate Revenue**

```python
# Example calculation
uninstall_suite = num_users_tier1 * 0.005 * 4  # per month
reinstall_suite = num_users_tier2 * 0.01 * 4
complete = num_users_tier3 * 0.012 * 4

total_monthly = uninstall_suite + reinstall_suite + complete
```

---

## 🆘 Emergency Procedures

### **Critical Bug Found**

1. Pull APK from dApp Store (if possible)
2. Fix bug immediately
3. Bump version
4. Submit emergency update

### **Payment Issues**

1. Verify wallet address correct
2. Check RPC endpoint responding
3. Test transaction manually
4. Update if needed

### **Shizuku Compatibility**

1. Check Shizuku releases
2. Update integration if needed
3. Test on affected devices
4. Submit patch

---

## 📞 Support Channels

**Build Issues:**
- Check: GitHub Issues (if public repo)
- Email: dev@adappvark.xyz

**Deployment Issues:**
- Solana dApp Store: publisher support
- Email: review@adappvark.xyz

---

**Ready to ship?** Follow this guide step-by-step and you'll have a production-ready release! 🚀

---

**Last Updated:** February 4, 2026
