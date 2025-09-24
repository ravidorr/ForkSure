# Firebase Setup Instructions for ForkSure

This document explains how to set up Firebase Crashlytics for the crash prevention system we've just implemented.

## Prerequisites

1. Your project now has all the Firebase dependencies added
2. The crash prevention system is integrated and ready to use Firebase services

## Setup Steps

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Create a project" or "Add project"
3. Enter project name: `ForkSure` (or your preferred name)
4. Enable Google Analytics (recommended for crash reporting insights)

### 2. Add Android App

1. In your Firebase project, click "Add app" and select Android
2. Enter your package name: `com.ravidor.forksure`
3. Enter app nickname: `ForkSure Android`
4. Add debug signing certificate SHA-1 (optional but recommended)

### 3. Download Configuration File

1. Download the `google-services.json` file
2. Place it in your app module directory: `app/google-services.json`
3. **Important**: Add this file to your `.gitignore` if it contains sensitive configuration

### 4. Enable Crashlytics

1. In Firebase Console, go to Crashlytics
2. Click "Set up Crashlytics"
3. Follow the integration steps (you've already done the code integration)

## Testing Your Setup

Once Firebase is configured, you can test the crash prevention system:

```kotlin
// Test crash handler (DEBUG ONLY)
if (BuildConfig.DEBUG) {
    StabilityTestUtils.testCrashHandler(CrashTestType.NULL_POINTER)
}

// Test ANR detection
StabilityTestUtils.testANRDetection()

// Test memory pressure
val result = StabilityTestUtils.testMemoryPressure(context)

// Validate all systems
val validation = StabilityTestUtils.validateStabilitySystems(context)
Log.d("Stability", validation.getReport())
```

## What's Already Implemented

Your app now has comprehensive crash prevention:

### ✅ Firebase Crashlytics Integration
- Automatic crash reporting
- Custom keys and user tracking (anonymized)
- Non-fatal exception logging

### ✅ Global Crash Handler
- Captures any unhandled exceptions
- Logs crashes locally for offline analysis
- Prevents crash loops with smart restart logic
- Clears volatile state on crash

### ✅ Memory Management System
- OutOfMemoryError prevention
- Memory-safe image processing
- Automatic memory cleanup on system requests
- Memory monitoring and alerts

### ✅ ANR Prevention System
- Monitors main thread for blocks
- Early warning at 4 seconds, critical alert at 8 seconds
- Automatic stack trace capture and reporting
- Main thread utilities for safe operations

### ✅ StrictMode Integration (Debug)
- Detects disk I/O, network calls, and leaks in debug builds
- Helps catch potential issues before release

### ✅ Comprehensive Testing Utils
- Test crash scenarios safely
- Validate all stability systems
- Memory pressure testing
- ANR detection testing

## Monitoring and Alerts

With Firebase set up, you'll get:

1. **Real-time crash alerts** via Firebase Console
2. **Detailed crash reports** with stack traces and device info
3. **User impact metrics** (crash-free users percentage)
4. **Custom dashboards** showing app stability trends

## Next Steps

1. Set up Firebase as described above
2. Test with debug builds using `StabilityTestUtils`
3. Monitor crash-free users percentage in Firebase Console
4. Set up alerts for when crash-free rate drops below 99.5%
5. Review weekly crash reports and prioritize fixes

## Security Notes

- Firebase automatically anonymizes user data
- Crash reports don't contain sensitive user information
- Local crash logs are stored securely in app private directory
- All debugging tools only work in DEBUG builds

Your app now has enterprise-level crash prevention and monitoring capabilities!