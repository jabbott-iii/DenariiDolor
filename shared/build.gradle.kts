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

    compilerOptions {
        // PlatformLocale and PlatformCurrency are expect classes; this acknowledges that Kotlin still calls them Beta.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.denariidolor.shared"
        compileSdk = 37
        minSdk = 26

        // JVM tests on the build machine. :app's testDebugUnitTest and coverage report run these too.
        withHostTest {
            enableCoverage = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Shared types use both in their public API (YearMonth, TimeZone, StateFlow).
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
        // Tests for common code that will also run on iOS. Tests that compare against java.* stay in androidHostTest.
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        // The tests that compare against java.time and java.text, or set the JVM's default locale, stay on JUnit 4.
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
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
    source.setFrom("src/commonMain/kotlin", "src/androidMain/kotlin", "src/commonTest/kotlin", "src/androidHostTest/kotlin")
}
