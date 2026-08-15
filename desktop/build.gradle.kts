plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

group = "com.daniel.flighttracker"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.skiko:skiko-awt:0.8.18")
}

kotlin {
    sourceSets {
        main {
            kotlin {
                srcDir("../app/src/main/java/com/daniel/flighttracker/data")
                exclude("**/SettingsRepository.kt")
            }
        }
    }
}

val fatJar by tasks.registering(Jar::class) {
    group = "distribution"
    description = "Builds a single runnable jar (java -jar flight-tracker.jar)."
    archiveFileName.set("flight-tracker.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.daniel.flighttracker.desktop.MainKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    }) {
        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/MANIFEST.MF"
        )
    }
}

tasks.named("build") {
    dependsOn(fatJar)
}

compose.desktop {
    application {
        mainClass = "com.daniel.flighttracker.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "FlightTrackerDesktop"
            packageVersion = "1.0.0"
            description = "Live flight tracker for OpenSky Network"
            vendor = "Daniel"
        }
    }
}
