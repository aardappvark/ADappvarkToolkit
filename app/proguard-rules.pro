# AardAppvark Toolkit ProGuard Rules

# --- Manifest-declared classes (instantiated by Android framework via reflection) ---

# Activity (declared in AndroidManifest.xml)
-keep class com.adappvark.toolkit.MainActivity { *; }

# Service (declared in AndroidManifest.xml)
-keep class com.adappvark.toolkit.service.ReinstallForegroundService { *; }

# Keep Shizuku classes
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.**
-dontwarn moe.shizuku.**
-keepclassmembers class * {
    *** onRequestPermissionsResult(...);
}

# Keep Solana MWA classes
-keep class com.solana.mobilewalletadapter.** { *; }
-dontwarn com.solana.mobilewalletadapter.**

# Keep seeker-verify (SGT verification) classes
-keep class com.midmightbit.sgt.** { *; }
-dontwarn com.midmightbit.sgt.**

# Keep data classes
-keep class com.adappvark.toolkit.data.model.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep UninstallDaemon (launched externally via app_process, not referenced by app code)
-keep class com.adappvark.toolkit.daemon.UninstallDaemon { *; }

# Keep service classes that use reflection or JSON parsing
-keep class com.adappvark.toolkit.service.Blockhash { *; }
-keep class com.adappvark.toolkit.service.PricingSummary { *; }

# Keep sealed class hierarchies
-keep class com.adappvark.toolkit.service.TransactionStatus { *; }
-keep class com.adappvark.toolkit.service.TransactionStatus$* { *; }
-keep class com.adappvark.toolkit.service.GeoRestrictionService$GeoCheckResult { *; }
-keep class com.adappvark.toolkit.service.GeoRestrictionService$GeoCheckResult$* { *; }
-keep class com.adappvark.toolkit.service.PaymentTransactionResult { *; }
-keep class com.adappvark.toolkit.service.PaymentTransactionResult$* { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# OkHttp + Okio (MWA transitive dependency)
-dontwarn okhttp3.**
-dontwarn okio.**

# Security Crypto + Tink (if present transitively)
-dontwarn com.google.api.client.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.joda.time.**

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
