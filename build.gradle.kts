// Top-level build file where you can add configuration options common to all sub-projects/modules.

// CS-22, CS-23: AGP put versions with known advisories on the plugin classpath: protobuf-java, commons-io, Bouncy Castle,
// commons-compress, jdom2 and jose4j (and netty through gRPC, which AGP 9.4.1 no longer brings). None of them ships in the app.
// The constraints are minimums, so a newer version that AGP brings still wins; remove each once AGP brings a patched one.
buildscript {
    dependencies {
        // AGP 9 compiles Kotlin itself (built-in Kotlin) and brings KGP 2.2.10; this raises it to the catalog's Kotlin version.
        classpath(libs.kotlin.gradlePlugin)
        constraints {
            classpath(libs.build.protobuf.java)
            classpath(libs.build.commons.io)
            classpath(libs.build.bouncycastle.bcprov)
            classpath(libs.build.bouncycastle.bcpkix)
            classpath(libs.build.bouncycastle.bcutil)
            classpath(libs.build.commons.compress)
            classpath(libs.build.jdom2)
            classpath(libs.build.jose4j)
        }
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}
