# ForkSure - Signing Certificates Reference

## 📋 Certificate Fingerprints

### Debug Certificate (Development)
- **SHA-1**: `C1:B4:FC:0F:D8:55:83:13:4E:6B:1F:B2:D5:58:F3:67:F2:B3:0F:A5`
- **SHA-256**: `99:DC:28:2D:4B:0B:AC:3D:D9:66:F4:63:A1:CB:49:70:D7:E5:2B:6F:1F:28:1F:9A:4B:F7:0C:66:25:36:1C:51`
- **Location**: `~/.android/debug.keystore`
- **Alias**: `AndroidDebugKey`
- **Valid Until**: Sunday, 23 May 2055

### Release Certificate (Production)
- **SHA-1**: `0E:67:62:43:B3:08:F0:5B:12:AE:D1:0A:71:11:9B:DF:D9:A6:1B:0C`
- **SHA-256**: `23:81:33:29:B7:B7:55:25:CE:62:93:7C:92:EE:97:2A:A5:EC:96:BB:A5:F1:D9:23:5B:3C:1B:9D:68:B2:BF:F7`
- **Location**: `./forksure-release-key.keystore`
- **Alias**: `forksure`
- **Valid Until**: Wednesday, 16 October 2052

## 🔄 How to Regenerate

To get these fingerprints again anytime:

```bash
# Debug certificate
./gradlew signingReport

# Or manually
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

## 🔥 Firebase Configuration

Both SHA-1 fingerprints have been added to Firebase Console:
- Project: ForkSure
- Package: com.ravidor.forksure

## 📝 Notes

- **Debug SHA-1**: Changes only if you delete `~/.android/debug.keystore`
- **Release SHA-1**: Tied to your specific release keystore file
- **Team Members**: Each developer will have their own debug SHA-1
- **CI/CD**: May need separate SHA-1 if using different keystores

## ⚠️ Security

- SHA-1 fingerprints are **NOT secrets** - they're public identifiers
- The actual **keystore files** and **passwords** ARE secrets
- Safe to commit this file to version control