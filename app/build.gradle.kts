import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("com.google.dagger.hilt.android")
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.ravidor.forksure"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ravidor.forksure"
        minSdk = 29
        targetSdk = 35
        versionCode = 9
        versionName = "1.2.0-rc3"

        testInstrumentationRunner = "com.ravidor.forksure.HiltTestRunner"
        
        ndk {
            // Enable 16KB page alignment for better performance on modern devices
            debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            
            // Generate debug symbols for crash reporting
            isDebuggable = false
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
    
    bundle {
        abi {
            enableSplit = true
        }
        language {
            enableSplit = false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.generativeai)
    
    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Hilt dependencies
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // CameraX dependencies
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Permissions handling
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Lifecycle compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Markdown rendering for Compose
    implementation("com.github.jeziellago:compose-markdown:0.5.4")
    
    // Unit Testing
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.google.truth:truth:1.1.4")
    
    // Hilt Testing
    testImplementation("com.google.dagger:hilt-android-testing:2.48")
    kspTest("com.google.dagger:hilt-android-compiler:2.48")
    
    // Robolectric for Android unit tests
    testImplementation("org.robolectric:robolectric:4.11.1")
    
    // Instrumented Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("com.google.truth:truth:1.1.4")
    
    // Hilt Instrumented Testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.48")
    
    // Compose Testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.6")
    androidTestImplementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Debug implementations
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// KSP configuration for generated code
ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}