# Add project specific ProGuard rules here.

# Keep Shizuku classes
-keep class rikka.shizuku.** { *; }
-keepclassmembers class * {
    *** onRequestPermissionsResult(...);
}

# Keep Solana MWA classes
-keep class com.solana.mobilewalletadapter.** { *; }

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
