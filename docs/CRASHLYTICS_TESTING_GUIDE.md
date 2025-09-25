# Crashlytics Testing Guide - ForkSure

This guide will help you test Firebase Crashlytics integration and verify that crash reports are being sent correctly.

## Quick Test (Automatic)

The app automatically sends a test crash 3 seconds after startup (debug builds only). Look for this log message:
```
CrashlyticsQuickTest: Test crash sent successfully! Check Firebase Console in 2-5 minutes.
```

## Manual Testing Options

### Option 1: Debug UI (Recommended)

1. **Build and run the app** in debug mode
2. **Stay on the main screen** (don't analyze any images)
3. **Scroll down** to the bottom of the screen to find the "Debug Crash Testing" section
4. **Use the Crashlytics Testing buttons**:
   - **Non-Fatal**: Sends a test non-fatal exception
   - **Diagnostic**: Sends device/app diagnostic information
   - **Verify Setup**: Checks Crashlytics configuration (check logcat)
   - **Delayed Test**: Sends a test crash after 2 seconds

### Option 2: Code-based Testing

Add this line anywhere in your code to send a test crash:
```kotlin
CrashlyticsQuickTest.sendTestCrash()
```

## Verifying Results

### 1. Check Logcat
Look for these log messages:
```
CrashlyticsQuickTest: Sending test crash to Firebase Crashlytics...
CrashlyticsQuickTest: Test crash sent successfully! Check Firebase Console in 2-5 minutes.
```

### 2. Check Firebase Console

1. **Open Firebase Console**: https://console.firebase.google.com
2. **Select your project**: "forksure-74375"
3. **Navigate to Crashlytics**: Left sidebar → Quality → Crashlytics
4. **Check sections**:
   - **Non-fatals**: Test crashes appear here (RuntimeException)
   - **Crashes**: Real app crashes appear here

### 3. Expected Timeline

- **Immediate**: Log messages appear in Android Studio logcat
- **2-5 minutes**: Crashes appear in Firebase Console
- **Up to 15 minutes**: Sometimes takes longer for first-time setup

## Troubleshooting

### Debug UI Not Visible

**Problem**: Can't see the debug crash testing section
**Solutions**:
1. Make sure you're running a **debug build** (not release)
2. Stay on the **main screen** (not in results view)
3. **Scroll down** to the bottom of the screen
4. Check logcat for: `DebugCrashSection: Showing debug crash testing UI`

### No Crashes in Firebase

**Problem**: Sent test crashes but don't see them in Firebase Console
**Solutions**:
1. **Wait longer**: Can take up to 15 minutes
2. **Check correct section**: Non-fatal exceptions go to "Non-fatals", not "Crashes"
3. **Verify setup**: Use "Verify Setup" button and check logcat
4. **Check internet**: Device needs internet connection
5. **Force close app**: Close and reopen the app to trigger upload

### Configuration Issues

**Problem**: Setup verification shows issues
**Solutions**:
1. **Check google-services.json**: Must be in `app/` directory
2. **Verify Firebase project**: Project ID should match
3. **Check build.gradle**: Firebase plugins should be applied
4. **Clean rebuild**: `./gradlew clean assembleDebug`

## Debug Logging

Enable verbose logging by checking logcat for these tags:
- `CrashlyticsQuickTest`: Quick test results
- `CrashlyticsTestHelper`: Detailed test helper logs
- `DebugCrashSection`: Debug UI visibility
- `ForkSureApplication`: App initialization

## Firebase Console Navigation

1. **Firebase Console** → https://console.firebase.google.com
2. **Select Project** → forksure-74375
3. **Crashlytics** → Left sidebar → Quality → Crashlytics
4. **View Reports**:
   - **Dashboard**: Overview of crashes
   - **Crashes**: Fatal crashes that crash the app
   - **Non-fatals**: Handled exceptions (test crashes go here)
   - **ANRs**: App Not Responding events

## Test Crash Types

The debug UI includes several test types:

### Crashlytics Tests
- **Non-Fatal**: `RuntimeException` with test metadata
- **Diagnostic**: Device info with `RuntimeException`
- **Delayed Test**: Delayed test crash

### System Tests
- **NPE Test**: NullPointerException
- **OOM Test**: OutOfMemoryError simulation
- **Array Test**: ArrayIndexOutOfBoundsException
- **ANR Test**: Application Not Responding simulation
- **Memory Test**: Memory pressure testing

## Success Indicators

**✅ Working correctly if you see:**
1. Log message: "Test crash sent successfully!"
2. Firebase Console shows new non-fatal exceptions
3. Custom keys visible in crash details (test_timestamp, app_version, etc.)
4. Recent crashes have your device info

**❌ Issues if you see:**
1. Log message: "Failed to send test crash"
2. No crashes appear in Firebase Console after 15 minutes
3. "Crashlytics Instance: Not Available" in setup verification
4. Network errors in logcat