# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Generative AI classes
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Keep CameraX classes
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Accompanist classes
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep app-specific classes
-keep class com.ravidor.forksure.** { *; }

# Keep BuildConfig
-keep class com.ravidor.forksure.BuildConfig { *; }# Add to proguard-rules.pro for additional obfuscation

# Add more rules here as needed.

# Add to proguard-rules.pro for additional obfuscation

# Enhanced Security: Obfuscate BuildConfig to make API key extraction harder
-keepclassmembers class **.BuildConfig {
    public static final java.lang.String APPLICATION_ID;
    public static final java.lang.String BUILD_TYPE;
    public static final java.lang.String FLAVOR;
    public static final boolean DEBUG;
    public static final int VERSION_CODE;
    public static final java.lang.String VERSION_NAME;
}

# Obfuscate API key field name (but keep the class accessible)
-keepclassmembers class **.BuildConfig {
    !static final java.lang.String apiKey;
}

# Additional string obfuscation
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# Make reverse engineering more difficult
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# Optimize string constants
-optimizations !code/simplification/string
