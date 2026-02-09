package com.adappvark.toolkit

/**
 * Build configuration constants
 *
 * IMPORTANT: For release builds, this file should be replaced by Gradle's
 * auto-generated BuildConfig. To enable:
 * 1. Add `buildFeatures { buildConfig = true }` to build.gradle.kts
 * 2. Delete this manual file
 * 3. Gradle will auto-generate BuildConfig with DEBUG=false for release builds
 *
 * SECURITY: DEBUG controls debug logging, payment bypass, and dev features.
 * TODO: Before dApp Store submission, either:
 *   - Switch to Gradle-generated BuildConfig (preferred), or
 *   - Change DEBUG to false and BUILD_TYPE to "release" in this file
 */
object BuildConfig {
    const val APPLICATION_ID = "com.adappvark.toolkit"
    const val VERSION_NAME = "1.0.0"
    const val VERSION_CODE = 1

    // CRITICAL: Set to false for production/release builds
    const val DEBUG = true

    // Build type
    const val BUILD_TYPE = "debug"
}
