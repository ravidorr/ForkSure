@file:Suppress("UnstableApiUsage")

import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    alias(libs.plugins.dagger.hilt.android)
    id("jacoco")
    // Firebase plugins for crash reporting
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.ravidor.forksure"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ravidor.forksure"
        minSdk = 29
        targetSdk = 37
        versionCode = 19
        versionName = "1.4.2"

        testInstrumentationRunner = "com.ravidor.forksure.HiltTestRunner"
        
        ndk {
            // Enable 16KB page alignment for better performance on modern devices
            debugSymbolLevel = "SYMBOL_TABLE"
            // Specify supported ABIs for optimal performance
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
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
            // Enable minification and resource shrinking for smaller APK size
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only use release signing if keystore properties exist
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            
            // Generate debug symbols for crash reporting
            isDebuggable = false
            
            // Generate mapping files for deobfuscation (fixes warning 2)
            // This creates mapping.txt for R8/ProGuard deobfuscation
            
            // Enable native debug symbols (fixes warning 3) 
            ndk {
                debugSymbolLevel = "FULL"
            }
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
            // Enable 16KB page alignment for better performance
            keepDebugSymbols += "**/arm64-v8a/*.so"
            keepDebugSymbols += "**/armeabi-v7a/*.so"
        }
        resources {
            // Ensure efficient packaging
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    packaging {
        resources {
            pickFirsts += setOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
        }
    }

    // Optimize dependencies info for better performance
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    
    bundle {
        abi {
            enableSplit = true
        }
        language {
            enableSplit = false
        }
        // Optimize for 16KB page alignment
        density {
            enableSplit = true
        }
    }
    
    lint {
        // Enable UseKtx lint rule to detect opportunities for KTX extensions
        enable += "UseKtx"
        
        // Disable resource shrinking warnings that are not actionable
        disable += "UnusedResources"
        disable += "VectorPath"
        disable += "IconLauncherShape"
        
        abortOnError = false
        checkReleaseBuilds = false
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        unitTests.all {
            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
            }
        }
    }
}

// JaCoCo Configuration
jacoco {
    toolVersion = "0.8.10"
}

kotlin {
    jvmToolchain(17)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    description = ""
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // Exclude generated and boilerplate code from coverage
    val excludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        // Dagger/Hilt/KSP generated
        "**/Hilt_*.*",
        "**/*_Hilt*.*",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/*Module*.*",
        "**/*Component*.*",
        "**/Dagger*.*",
        "**/hilt_aggregated_deps/**",
        // Compose generated singletons and theme boilerplate
        "**/ComposableSingletons*.*",
        "**/ui/theme/**",
        // UI-heavy packages excluded from unit coverage
        "**/com/ravidor/forksure/screens/**",
        "**/com/ravidor/forksure/navigation/**",
        "**/com/ravidor/forksure/state/**",
        // Specific root-level UI helpers (leave MessageDisplayHelper, EnhancedErrorHandler, ErrorHandler, RecipeSharingHelper included)
        "**/com/ravidor/forksure/CameraCapture*.*",
        "**/com/ravidor/forksure/ContentReportDialog*.*",
        "**/com/ravidor/forksure/SecurityStatusIndicator*.*",
        "**/com/ravidor/forksure/ShareButton*.*",
        "**/com/ravidor/forksure/MainActivity*.*",
        "**/com/ravidor/forksure/SplashActivity*.*"
    )

    val buildDirFile = project.layout.buildDirectory.get().asFile
    val builtInKotlinClasses = file("${buildDirFile}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
    val legacyKotlinClasses = file("${buildDirFile}/tmp/kotlin-classes/debug")
    val kotlinClassesDir = if (builtInKotlinClasses.exists()) builtInKotlinClasses else legacyKotlinClasses
    val kotlinDebug = fileTree(kotlinClassesDir).exclude(excludes)
    val javaDebug = fileTree("${buildDirFile}/intermediates/javac/debug/classes").exclude(excludes)
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(kotlinDebug, javaDebug))
    executionData.setFrom(fileTree(buildDirFile).include("jacoco/testDebugUnitTest.exec"))
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    description = ""
    dependsOn("jacocoTestReport")

    val excludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/Hilt_*.*",
        "**/*_Hilt*.*",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/*Module*.*",
        "**/*Component*.*",
        "**/Dagger*.*",
        "**/hilt_aggregated_deps/**",
        "**/ComposableSingletons*.*",
        "**/ui/theme/**",
        // UI-heavy packages excluded from unit coverage
        "**/com/ravidor/forksure/screens/**",
        "**/com/ravidor/forksure/navigation/**",
        "**/com/ravidor/forksure/state/**",
        // Specific root-level UI helpers (leave MessageDisplayHelper, EnhancedErrorHandler, ErrorHandler, RecipeSharingHelper included)
        "**/com/ravidor/forksure/CameraCapture*.*",
        "**/com/ravidor/forksure/ContentReportDialog*.*",
        "**/com/ravidor/forksure/SecurityStatusIndicator*.*",
        "**/com/ravidor/forksure/ShareButton*.*",
        "**/com/ravidor/forksure/MainActivity*.*",
        "**/com/ravidor/forksure/SplashActivity*.*"
    )
    val buildDirFile = project.layout.buildDirectory.get().asFile
    val builtInKotlinClasses = file("${buildDirFile}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
    val legacyKotlinClasses = file("${buildDirFile}/tmp/kotlin-classes/debug")
    val kotlinClassesDir = if (builtInKotlinClasses.exists()) builtInKotlinClasses else legacyKotlinClasses
    val kotlinDebug = fileTree(kotlinClassesDir).exclude(excludes)
    val javaDebug = fileTree("${buildDirFile}/intermediates/javac/debug/classes").exclude(excludes)

    classDirectories.setFrom(files(kotlinDebug, javaDebug))
    executionData.setFrom(fileTree(buildDirFile).include("jacoco/testDebugUnitTest.exec"))

    violationRules {
        rule {
            limit {
                minimum = "0.20".toBigDecimal()
            }
        }
    }
}

// Make check depend on jacoco coverage verification
tasks.named("check") {
    dependsOn("jacocoCoverageVerification")
}

// Force patched versions of vulnerable transitives pulled in by test tooling
// (AGP's unified test platform brings netty/ktor/protobuf, Robolectric brings BouncyCastle).
configurations.configureEach {
    resolutionStrategy.eachDependency {
        when (requested.group) {
            "io.netty" -> if (requested.name.startsWith("netty-")) useVersion("4.1.137.Final")
            "org.bouncycastle" -> if (requested.name.endsWith("-jdk18on")) useVersion("1.85")
            "com.google.guava" -> if (requested.name == "guava") useVersion(if (requested.version.orEmpty().endsWith("-android")) "33.7.2-android" else "33.7.2-jre")
            "com.google.protobuf" -> if (requested.name in listOf("protobuf-java", "protobuf-kotlin")) useVersion("3.25.5")
            "io.ktor" -> if (requested.version == "2.3.2") useVersion("2.3.13")
            "androidx.concurrent" -> useVersion("1.3.0")
        }
    }
}

dependencies {

    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.generativeai)
    implementation(libs.gson)
    
    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)
    
    // Hilt dependencies
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)
    implementation(libs.hilt.navigation.compose)
    
    // CameraX dependencies
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    
    // Permissions handling
    implementation(libs.accompanist.permissions)
    
    // Lifecycle compose
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    
    // Firebase Crashlytics and Analytics
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.analytics.ktx)
    
    // Stability and Performance Monitoring
    debugImplementation(libs.leakcanary.android)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.concurrent.futures.ktx)
    
    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.navigation.testing)
    
    // Hilt Testing
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.android.compiler)
    
    // Robolectric for Android unit tests
    testImplementation(libs.robolectric)
    
    // Instrumented Testing
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.hilt.navigation.compose)
    
    // Hilt Instrumented Testing
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    
    // Debug implementations
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// KSP configuration for generated code
ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}

// Java compilation tasks configured for production builds