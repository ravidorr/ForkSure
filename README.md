# 🍴 ForkSure - AI Baking Assistant

An Android app that uses AI to analyze photos of baked goods and provide instant recipes and baking tips.

## 📱 Features

- **📸 Smart Camera Integration** - Take photos of cupcakes, cookies, cakes, and more
- **🤖 AI-Powered Analysis** - Get instant recipe suggestions using Google's Gemini AI
- **🧁 Recipe Guidance** - Detailed instructions and ingredient lists
- **🎨 Beautiful Interface** - Clean, modern design for bakers of all levels
- **🔒 Privacy-First** - Photos are processed securely and never stored permanently

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **AI Service**: Google Gemini AI
- **Camera**: CameraX
- **Architecture**: MVVM with ViewModels
- **Build System**: Gradle with Kotlin DSL

## 📚 Documentation

Comprehensive guides and documentation are organized by category in the [`docs/`](docs/) folder:

### 🏗️ Architecture
- **[`docs/architecture/`](docs/architecture/)** - Technical architecture guides and design patterns
  - State management, repository patterns, dependency injection, navigation

### ⚡ Features  
- **[`docs/features/`](docs/features/)** - Feature implementation details
  - Security, dark mode, splash screen, rate limiting, webhooks

### 🧪 Testing
- **[`docs/testing/`](docs/testing/)** - Testing strategies and accessibility guides
  - Comprehensive testing, accessibility compliance and implementation

### 🚀 Release
- **[`docs/release/`](docs/release/)** - Release management and processes
  - Release checklists, artifacts, and deployment procedures

### 🏪 Google Play
- **[`docs/google-play/`](docs/google-play/)** - Google Play Store documentation
  - Store listing, data safety, compliance, and asset guidelines

### ⚖️ Legal
- **[`docs/legal/`](docs/legal/)** - Legal and privacy documentation  
  - Privacy policies and legal compliance

### 🎨 Assets
- **[`docs/assets/`](docs/assets/)** - Design and asset guidelines
  - Visual branding and asset creation specifications

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
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

## 📦 Build Release

To build a release version:

```bash
# Set Java environment
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Build App Bundle (for Google Play Store)
./gradlew bundleRelease

# Build APK (for direct installation)
./gradlew assembleRelease
```

## 🔐 Security Notes

- **Keystore files are excluded** from version control for security
- **API keys are stored** in `local.properties` (not committed)
- **User photos are never stored** permanently

## 📄 Privacy Policy

The privacy policy is available at: [Privacy Policy](https://ravidor.github.io/ForkSure/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📱 Download

Available on Google Play Store: [Coming Soon]

## 📧 Contact

- Developer: Raanan Avidor
- Email: raanan@avidor.org

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

*Transform any photo into a baking opportunity with ForkSure! 🧁✨*
// Direct push test
// Direct push test
