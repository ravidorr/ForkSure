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

# Specific Generative AI classes (instead of entire package)
-keep class com.google.ai.client.generativeai.GenerativeModel { *; }
-keep class com.google.ai.client.generativeai.type.** { *; }
-keepclassmembers class com.google.ai.client.generativeai.** {
    public <methods>;
    public <fields>;
}
-dontwarn com.google.ai.client.generativeai.**

# Specific CameraX classes (only what's needed)
-keep class androidx.camera.core.ImageProxy { *; }
-keep class androidx.camera.core.ImageCapture { *; }
-keep class androidx.camera.core.Preview { *; }
-keep class androidx.camera.lifecycle.ProcessCameraProvider { *; }
-keepclassmembers class androidx.camera.** {
    public <methods>;
}
-dontwarn androidx.camera.**

# Compose - keep only annotation-driven classes
-keep @androidx.compose.runtime.Stable class *
-keep @androidx.compose.runtime.Immutable class *
-keepclassmembers class androidx.compose.** {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.**

# Accompanist permissions (only what's used)
-keep class com.google.accompanist.permissions.** { *; }
-dontwarn com.google.accompanist.**

# Kotlin coroutines - keep only essential classes
-keepclassmembers class kotlinx.coroutines.CoroutineScope {
    *;
}
-keepclassmembers class kotlinx.coroutines.flow.Flow {
    *;
}
-dontwarn kotlinx.coroutines.**

# App-specific classes - targeted approach
# Keep data models (likely serialized/deserialized)
-keep class com.ravidor.forksure.data.model.** { *; }

# Keep repository interfaces (might be used with reflection)
-keep interface com.ravidor.forksure.repository.** { *; }

# Keep Hilt components and modules
-keep class com.ravidor.forksure.di.** { *; }
-keep @dagger.hilt.** class *

# Keep Activities and Application class
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Keep ViewModel classes and their public methods
-keepclassmembers class com.ravidor.forksure.**ViewModel {
    public <methods>;
}

# Keep classes with @Keep annotation
-keep @androidx.annotation.Keep class * { *; }

# BuildConfig - keep only essential fields
-keep class com.ravidor.forksure.BuildConfig {
    public static final java.lang.String APPLICATION_ID;
    public static final java.lang.String BUILD_TYPE;
    public static final boolean DEBUG;
    public static final int VERSION_CODE;
    public static final java.lang.String VERSION_NAME;
}

# Firebase/Crashlytics - keep specific classes
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.firebase.analytics.** { *; }
-keepattributes *Annotation*
-keepattributes Signature

# Gson/JSON serialization (if used)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Navigation Component
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends androidx.fragment.app.Fragment

# Enhanced Security: Obfuscate sensitive strings
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# Additional string obfuscation
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# Security optimizations
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# Optimization settings - be specific about what to optimize
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

# Additional security measures for sensitive classes
-keep,allowshrinking class com.ravidor.forksure.data.** {
    <fields>;
}

# Keep line numbers for better crash reporting
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
