// Code meant for every platform. It has only an Android target until the iOS phases, so :app builds and ships exactly as
// before; commonMain can't reach java.*, android.* or Hilt, which keeps it portable.
plugins {
    // The root buildscript already puts the catalog's Kotlin Gradle plugin on the classpath, so this one takes no version.
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.denariidolor.shared"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            // Shared types use both in their public API (YearMonth, TimeZone, StateFlow).
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
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

// CS-22: the same build-time patch for ktlint (logback) as in :app.
dependencies {
    constraints {
        add("ktlint", libs.build.logback.classic)
        add("ktlint", libs.build.logback.core)
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
    source.setFrom("src/commonMain/kotlin", "src/androidMain/kotlin")
}
