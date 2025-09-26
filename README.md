# ForkSure - AI Baking Assistant
*✨🧁 Transform any photo into a baking opportunity with ForkSure! 🧁✨*

[![Get it on Google Play](https://img.shields.io/badge/Get%20it%20on-Google%20Play-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.ravidor.forksure)

[![CI - Assemble Debug](https://github.com/ravidorr/ForkSure/actions/workflows/ci-build.yml/badge.svg?branch=main)](https://github.com/ravidorr/ForkSure/actions/workflows/ci-build.yml)
[![Verify 16KB page size](https://github.com/ravidorr/ForkSure/actions/workflows/verify-16kb.yml/badge.svg?branch=main)](https://github.com/ravidorr/ForkSure/actions/workflows/verify-16kb.yml)

An Android app that uses AI to analyze photos of baked goods and provide instant recipes and baking tips.

## Features

- **Smart Camera Integration** - Take photos of cupcakes, cookies, cakes, and more
- **AI-Powered Analysis** - Get instant recipe suggestions using Google's Gemini AI
- **Recipe Guidance** - Detailed instructions and ingredient lists
- **Beautiful Interface** - Clean, modern design for bakers of all levels
- **Privacy-First** - Photos are processed securely and never stored permanently

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **AI Service**: Google Gemini AI
- **Camera**: CameraX
- **Architecture**: MVVM with ViewModels
- **Build System**: Gradle with Kotlin DSL

## Documentation

Comprehensive guides and documentation are organized by category in the [`docs/`](docs/README.md) folder:

### Architecture
- **[`docs/architecture/`](docs/architecture/README.md)** - Technical architecture guides and design patterns
  - State management, repository patterns, dependency injection, navigation

### Features  
- **[`docs/features/`](docs/features/README.md)** - Feature implementation details
  - Security, dark mode, splash screen, rate limiting, webhooks

### Testing
- **[`docs/testing/`](docs/testing/README.md)** - Testing strategies and accessibility guides
  - Comprehensive testing, accessibility compliance, and implementation

### Release
- **[`docs/release/`](docs/release/README.md)** - Release management and processes
  - Release checklists, artifacts, and deployment procedures

### Google Play
- **[`docs/google-play/`](docs/google-play/README.md)** - Google Play Store documentation
  - Store listing, data safety, compliance, and asset guidelines

### Legal
- **[`docs/legal/`](docs/legal/README.md)** - Legal and privacy documentation  
  - Privacy policies and legal compliance

### Assets
- **[`docs/assets/`](docs/assets/README.md)** - Design and asset guidelines
  - Visual branding and asset creation specifications

## Getting Started

### Prerequisites
- The latest Android Studio version
- Android SDK 29 or higher
- Google Gemini AI API key

### Setup
1. Clone this repository
2. Open in Android Studio
3. Add your Gemini AI API key to `local.properties`:
   ```
   apiKey=your_gemini_api_key_here
   ```
4. Build and run the project

## Build Release

To build a release version:

```bash
# Set Java environment
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Build App Bundle (for Google Play Store)
./gradlew bundleRelease

# Build APK (for direct installation)
./gradlew assembleRelease
```

## Security Notes

- **Keystore files are excluded** from version control for security
- **API keys are stored** in `local.properties` (not committed)
- **User photos are never stored permanently**

## Privacy Policy

The privacy policy is available at: [Privacy Policy](https://ravidor.github.io/ForkSure/)

## Download

Available on Google Play Store: https://play.google.com/store/apps/details?id=com.ravidor.forksure

## Contact

- Developer: Raanan Avidor
- Email: raanan@avidor.org

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---
