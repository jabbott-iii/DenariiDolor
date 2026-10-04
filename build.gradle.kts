// Top-level build file where you can add configuration options common to all sub-projects/modules.

// CS-22: AGP 8.7.3 puts netty (through gRPC), protobuf-java and commons-io versions with known advisories on the plugin
// classpath. None of them ships in the app; these constraints raise them to patched versions until the AGP 9 upgrade.
buildscript {
    dependencies {
        classpath(platform(libs.build.netty.bom))
        constraints {
            classpath(libs.build.protobuf.java)
            classpath(libs.build.commons.io)
        }
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}
