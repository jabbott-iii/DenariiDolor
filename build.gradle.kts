// Top-level build file where you can add configuration options common to all sub-projects/modules.

// CS-22, CS-23: AGP 8.7.3 puts versions with known advisories on the plugin classpath: netty (through gRPC), protobuf-java,
// commons-io, Bouncy Castle, commons-compress, jdom2 and jose4j. None of them ships in the app; these constraints raise them
// to patched versions until the AGP 9 upgrade.
buildscript {
    dependencies {
        classpath(platform(libs.build.netty.bom))
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
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}
