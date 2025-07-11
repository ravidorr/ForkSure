import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("com.google.dagger.hilt.android")
    id("jacoco")
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
        versionCode = 11
        versionName = "1.3.0.rc2"

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
            // Enable 16KB page alignment for better performance
            keepDebugSymbols += "**/arm64-v8a/*.so"
            keepDebugSymbols += "**/armeabi-v7a/*.so"
        }
        resources {
            // Ensure efficient packaging
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/Hilt_*.*",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/*Module.*",
        "**/*Component.*",
        "**/DaggerApplicationComponent*.*"
    )
    
    val debugTree = fileTree("${project.layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug")
    val mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree.exclude(fileFilter)))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get().asFile).include("jacoco/testDebugUnitTest.exec"))
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("jacocoTestReport")
    
    violationRules {
        rule {
            limit {
                minimum = "0.60".toBigDecimal() // 60% minimum coverage
            }
        }
    }
}

// Make check depend on jacoco coverage verification
tasks.named("check") {
    dependsOn("jacocoCoverageVerification")
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
    implementation(libs.androidx.core.splashscreen)
    
    // Hilt dependencies
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
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
    
    // Markdown rendering for Compose
    implementation(libs.compose.markdown)
    
    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    
    // Hilt Testing
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.android.compiler)
    
    // Robolectric for Android unit tests
    testImplementation(libs.robolectric)
    
    // Instrumented Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.truth)
    
    // Hilt Instrumented Testing
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    
    // Compose Testing
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.hilt.navigation.compose)
    
    // Debug implementations
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// KSP configuration for generated code
ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}