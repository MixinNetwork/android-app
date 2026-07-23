pluginManagement {
    val androidGradlePluginVersion = "9.2.1"
    val kotlinVersion = "2.4.10"
    val downloadPluginVersion = "5.5.0"
    val secretsGradlePluginVersion = "2.0.1"
    val kspVersion = "2.3.10"
    val firebasePerfPluginVersion = "2.0.2"
    val foojayResolverVersion = "1.0.0"

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version androidGradlePluginVersion
        id("com.android.library") version androidGradlePluginVersion
        id("org.jetbrains.kotlin.plugin.parcelize") version kotlinVersion
        id("de.undercouch.download") version downloadPluginVersion
        id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version secretsGradlePluginVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("com.google.devtools.ksp") version kspVersion
        id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion
        id("com.google.firebase.firebase-perf") version firebasePerfPluginVersion
        id("org.gradle.toolchains.foojay-resolver-convention") version foojayResolverVersion
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

include(":app")
