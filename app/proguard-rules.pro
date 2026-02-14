# AardAppvark Toolkit ProGuard Rules

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

# Keep Accessibility Service (required for system registration)
-keep class com.adappvark.toolkit.service.AutoUninstallService { *; }

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

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
