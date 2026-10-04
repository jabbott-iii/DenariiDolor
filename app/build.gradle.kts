import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.denariidolor"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.denariidolor"
        minSdk = 26
        targetSdk = 36
        // CD passes -PversionCode / -PversionName derived from the release tag.
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "1.0.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing comes only from CI environment variables; the keystore is never committed.
    val releaseKeystore = System.getenv("ANDROID_KEYSTORE_PATH")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        getByName("androidTest").assets.directories += "$projectDir/schemas"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.sqlite)
    implementation(libs.sqlcipher.android)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.material)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // The BOM must be applied to each configuration that uses unversioned Compose artifacts.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    constraints {
        // Room 2.8's schema reader (room-migration, used by MigrationTestHelper) is built against kotlinx-serialization 1.8.1,
        // but navigation and lifecycle bring 1.7.3 into the app. Instrumented tests run against the app's copy, and AGP 9 no
        // longer aligns test dependencies with the app's, so raise the app's copy to match.
        implementation(libs.kotlinx.serialization.core)
    }
}

ktlint {
    version.set(libs.versions.ktlint.get())
    android.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

// CS-22: the same build-time patches for AGP's test platform runner and for ktlint (logback).
configurations.matching { it.name.startsWith("_internal-unified-test-platform") }.configureEach {
    dependencyConstraints.add(project.dependencies.constraints.create(libs.build.protobuf.java.get()))
    dependencyConstraints.add(project.dependencies.constraints.create(libs.build.commons.io.get()))
}
dependencies {
    constraints {
        add("ktlint", libs.build.logback.classic)
        add("ktlint", libs.build.logback.core)
    }
}

// CS-25: lint and AGP's test-engine worker resolve their own classpaths (lint's may be a detached configuration), which the
// root `buildscript` constraints don't reach. A component metadata rule applies to every resolution in this module: it raises
// the listed build-tool libraries wherever a dependency asks for an older version. None of them ships in the app.
@CacheableRule
abstract class RaiseBuildToolDependencies @Inject constructor(private val minimums: Map<String, String>) : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        context.details.allVariants {
            withDependencies {
                forEach { dependency ->
                    val minimum = minimums["${dependency.group}:${dependency.name}"]
                    if (minimum != null && isOlder(dependency.versionConstraint.requiredVersion, minimum)) {
                        dependency.version { require(minimum) }
                        dependency.because("CS-25: patched build-tool version")
                    }
                }
            }
        }
    }

    private fun isOlder(version: String, minimum: String): Boolean {
        val have = version.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val need = minimum.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(have.size, need.size)) {
            val difference = have.getOrElse(i) { 0 } - need.getOrElse(i) { 0 }
            if (difference != 0) return difference < 0
        }
        return false
    }
}

dependencies {
    components {
        all(RaiseBuildToolDependencies::class.java) {
            params(
                mapOf(
                    "org.bouncycastle:bcprov-jdk18on" to libs.versions.buildBouncyCastle.get(),
                    "org.bouncycastle:bcpkix-jdk18on" to libs.versions.buildBouncyCastle.get(),
                    "org.bouncycastle:bcutil-jdk18on" to libs.versions.buildBouncyCastle.get(),
                    "org.apache.commons:commons-lang3" to libs.versions.buildCommonsLang3.get(),
                    "org.apache.httpcomponents:httpclient" to libs.versions.buildHttpClient.get()
                )
            )
        }
    }
}

// detekt 1.23.8 embeds the Kotlin 2.0.21 compiler; keep its own classpath on that version under the newer Kotlin toolchain.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
}
