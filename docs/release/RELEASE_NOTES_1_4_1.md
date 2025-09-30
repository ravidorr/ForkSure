# Release Notes - ForkSure v1.4.1

Release Date: September 28, 2025  
Version Code: 15  
Version Name: 1.4.1  
Build Type: Production Release

## What's New / Improvements

- StrictMode compliance and background-thread I/O for safer, smoother UX
- Dependency upgrades for stability and security
  - Kotlin 2.2.20
  - Hilt 2.57.2
  - Firebase BOM 34.3.0
  - KSP update
- CI/CD and build hygiene
  - Fixed Gradle cache usage and CI workflow issues
  - Updated AAB build workflow and permissions
  - Eliminated dependency-graph submit timeouts
  - ANDROID_HOME → ANDROID_SDK_ROOT correction in CI
- Documentation and assets
  - Documentation reorganization and cleanup
  - Removed obsolete web assets and legacy reports
  - Privacy policy page removed; project docs updated accordingly
  - Splash screen resource updates

## Bug Fixes & Stability

- General fixes for broken strings/translations
- CI fixes to ensure consistent builds across environments
- Additional debug improvements for easier troubleshooting

## Build Information

- Target SDK: 36 (Android 15)
- Minimum SDK: 29 (Android 10)
- Compile SDK: 36
- Gradle Wrapper: 8.14.3
- 16KB Page Alignment: Enabled/maintained

## Installation

Google Play Store: staged rollout recommended.  
Direct testing builds available via CI artifacts or local build.

## Changes since v1.4.0 (selected commits)

- fe6f73a fix-gradle-cache (#65)
- ad22812 fix-broken-string (#64)
- 3228e29 fix-android-home-to-android-sdk-root (#63)
- 7e72d42 adding-debug (#62)
- 4e7e60f fix-all-ci (#61)
- ee80938 Update build-aab-on-merge.yml (#60)
- d555d2b chore: Remove privacy policy and update CI (#59)
- a3fef70 chore: Remove obsolete web assets and update translations (#58)
- d31c5e0 chore: Remove IDE inspection report and update splash screen (#57)
- 39bef7e build(deps): bump Firebase BOM to 34.3.0 (#52)
- e7c925b build(deps): bump hilt to 2.57.2 (#53)
- 4f989ef build(deps): bump com.google.dagger.hilt.android to 2.57.2 (#54)
- 0b9e94b build(deps): bump kotlin to 2.2.20 (#51)
- 194d2a7 fix/dependency submission permissions (#56)
- 0aab26b build(deps): bump KSP (#55)
- 27a5e4d docs: reorganize documentation structure (#50)
- e9df2fe Fix: Eliminate submit-gradle dependency graph timeouts (#49)
- b1b30e3 refactor: complete dependency management and UI improvements (#48)
- ac371fc StrictMode Compliance & Dependency Updates - v1.4.1 (#46)

---

ForkSure v1.4.1 focuses on stability and build reliability, with modernized dependencies and CI fixes to keep the pipeline fast and predictable. 🧁