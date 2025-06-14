# ForkSure - Google Play Store Release Checklist

## ✅ Pre-Release Tasks

### 1. Technical Setup
- [ ] **Update keystore.properties** with your actual keystore password
- [ ] **Test debug APK** on real device to ensure everything works
- [ ] **Build release APK**: `./gradlew assembleRelease`
- [ ] **Build App Bundle** (recommended): `./gradlew bundleRelease`
- [ ] **Test release APK** on real device

### 2. Privacy Policy
- [ ] **Host privacy policy online** (use GitHub Pages, Google Sites, or your website)
- [ ] **Update email contact** in privacy_policy.md
- [ ] **Get privacy policy URL** for Google Play Console

### 3. Store Assets
- [ ] **Take 3-5 app screenshots** (1080x1920 or 1440x2560 pixels)
- [ ] **Create feature graphic** (1024x500 pixels) - use Canva, Figma, or similar
- [ ] **Prepare app icon** ✅ (already done - your custom fork+cupcake icon)

## 🏪 Google Play Console Setup

### 4. Account Setup
- [ ] **Create Google Play Console account** at https://play.google.com/console
- [ ] **Pay $25 registration fee**
- [ ] **Complete account verification**

### 5. App Creation
- [ ] **Create new app** in Google Play Console
- [ ] **App name**: ForkSure - AI Baking Assistant
- [ ] **Default language**: English (US)
- [ ] **App type**: App
- [ ] **Free or paid**: Free

### 6. Store Listing
- [ ] **Upload app icon** (use your custom icon)
- [ ] **Upload feature graphic** (1024x500px)
- [ ] **Upload screenshots** (3-5 images)
- [ ] **Add short description**: "AI-powered baking assistant - snap photos, get instant recipes!"
- [ ] **Add full description** (use content from play_store_listing.md)
- [ ] **Set app category**: Food & Drink
- [ ] **Add contact email**
- [ ] **Add privacy policy URL**

### 7. App Content
- [ ] **Complete content rating questionnaire**
- [ ] **Select target audience**: Everyone
- [ ] **Add app permissions explanation** (camera usage for photo analysis)
- [ ] **Complete data safety section**

### 8. Release Setup
- [ ] **Upload APK or App Bundle** to Internal Testing first
- [ ] **Create release notes** for version 1.0
- [ ] **Test with internal testers**
- [ ] **Review pre-launch report**
- [ ] **Fix any issues found**

### 9. Production Release
- [ ] **Move to Production track**
- [ ] **Set rollout percentage** (start with 20% for safety)
- [ ] **Complete final review**
- [ ] **Submit for review**

## 📋 Build Commands Reference

After updating keystore.properties with your password:

```bash
# Set Java environment
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Build release APK
./gradlew assembleRelease

# Build App Bundle (recommended by Google)
./gradlew bundleRelease

# Install release APK for testing
./gradlew installRelease

# Clean build (if needed)
./gradlew clean assembleRelease
```

## 📁 File Locations After Build

- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **App Bundle**: `app/build/outputs/bundle/release/app-release.aab`

## 🔒 Security Notes

- **Never commit keystore files** to version control (already in .gitignore)
- **Keep keystore password secure** - you'll need it for all future updates
- **Backup your keystore file** - losing it means you can't update your app

## 📞 Support Information

- **Developer email**: [your-email@example.com]
- **Privacy policy**: [your-privacy-policy-url]
- **App version**: 1.0 (versionCode: 1)

## 🎯 Next Steps After Publication

1. **Monitor app performance** in Google Play Console
2. **Respond to user reviews**
3. **Plan future updates** and features
4. **Track download and usage analytics**
5. **Consider adding more baking categories**

---

**Estimated time to complete**: 2-4 hours (excluding Google review time)
**Google review time**: 1-3 days typically 