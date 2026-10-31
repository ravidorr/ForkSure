// Top-level build file where you can add configuration options common to all subprojects/modules.
buildscript {
    // Pin vulnerable transitive build-tooling dependencies (pulled in by AGP and friends)
    // to patched releases until the plugins that bring them in ship their own fixes.
    dependencies {
        constraints {
            val nettyVersion = "4.1.137.Final"
            listOf(
                "netty-buffer", "netty-codec", "netty-codec-http", "netty-codec-http2",
                "netty-codec-socks", "netty-common", "netty-handler", "netty-handler-proxy",
                "netty-resolver", "netty-transport", "netty-transport-native-unix-common",
            ).forEach { classpath("io.netty:$it:$nettyVersion") }

            val bouncyCastleVersion = "1.85"
            listOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on", "bcpg-jdk18on")
                .forEach { classpath("org.bouncycastle:$it:$bouncyCastleVersion") }

            classpath("org.apache.commons:commons-compress:1.28.0")
            classpath("org.bitbucket.b_c:jose4j:0.9.7")
            classpath("org.jdom:jdom2:2.0.6.1")
            classpath("com.google.protobuf:protobuf-java:3.25.5")
            classpath("com.google.protobuf:protobuf-kotlin:3.25.5")
            classpath("io.ktor:ktor-client-core-jvm:2.3.13")
            classpath("io.opentelemetry:opentelemetry-api:1.67.0")
            classpath("com.google.guava:guava:33.7.2-jre")
            classpath("org.apache.httpcomponents:httpclient:4.5.14")
            classpath("org.apache.commons:commons-lang3:3.18.0")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
    alias(libs.plugins.dagger.hilt.android) apply false
    // Firebase plugins
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}
