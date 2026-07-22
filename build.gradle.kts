buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.sumsub.com/repository/maven-public/") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.3.0")
        classpath("com.android.tools.build.jetifier:jetifier-processor:1.0.0-beta10")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.60.1")
        classpath("com.google.gms:google-services:4.5.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.7")
        classpath("com.google.firebase:perf-plugin:2.0.2")
        classpath("com.bugsnag:bugsnag-android-gradle-plugin:8.+")
    }
}

plugins {
    id("com.android.application") version "9.3.0" apply false
    id("com.android.library") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.4.10" apply false
    id("de.undercouch.download") version "5.5.0" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("com.google.firebase.firebase-perf") version "2.0.2" apply false
}

val bitcoinVersion by extra("0.17.1")
val fragmentVersion by extra("1.8.9")
val activity_version by extra("1.13.0")
val lifecycleVersion by extra("2.10.0")
val appcompatVersion by extra("1.7.1")
val pagingVersion by extra("3.5.0")
val coilVersion by extra("3.5.0")
val collectionx by extra("1.6.0")
val roomVersion by extra("2.8.4")
val navigationVersion by extra("2.9.8")
val workManagerVersion by extra("2.11.2")
val constraintLayoutVersion by extra("2.2.1")
val constraintLayoutComposeVersion by extra("1.1.1")
val supportLibVersion by extra("1.0.0")
val recyclerViewVersion by extra("1.4.0")
val browserVersion by extra("1.10.0")
val biometricVersion by extra("1.4.0-alpha07")
val mdcVersion by extra("1.14.0")
val exifinterfaceVersion by extra("1.4.2")
val preferenceVersion by extra("1.2.1")
val hiltVersion by extra("2.59.2")
val hiltAndroidxVersion by extra("1.3.0")
val androidxVersion by extra("1.18.0")
val viewpagerVersion by extra("1.1.0")
val sharetargetVersion by extra("1.2.0")
val coordinatorVersion by extra("1.3.0")
val espressoVersion by extra("3.7.0")
val cameraxVersion by extra("1.6.1")
val glideVersion by extra("5.0.7")
val timberVersion by extra("5.0.1")
val okhttpVersion by extra("5.4.0")
val rxJavaVersion by extra("2.2.21")
val rxAndroidVersion by extra("2.1.1")
val rxbindingVersion by extra("3.1.0")
val retrofitVersion by extra("3.0.0")
val coroutineAdapterVersion by extra("0.9.2")
val loggerVersion by extra("2.2.0")
val kotlinVersion by extra("2.3.21")
val libphonenumberVersion by extra("9.0.30")
val coroutinesVersion by extra("1.11.0")
val firebaseVersion by extra("18.0.2")
val firebaseMessageVersion by extra("21.0.1")
val firebaseDylinkVersion by extra("19.1.1")
val firebaseMLVersion by extra("24.0.3")
val mlkitBarcodeVersion by extra("17.3.0")
val jwtVersion by extra("0.9.1")
val zxingVersion by extra("3.5.4")
val rxpermissionsVersion by extra("0.10.3")
val ucropVersion by extra("2.2.11")
val glideTransformationsVersion by extra("4.3.0")
val jobqueueVersion by extra("3.1.0")
val stickyheadersrecyclerviewVersion by extra("0.7")
val threetenabpVersion by extra("1.4.9")
val signalVersion by extra("2.8.1")
val fabVersion by extra("1.6")
val playVersion by extra("2.1.0")
val googlePlayServicesVersion by extra("18.1.1")
val svgVersion by extra("1.4")
val reboundVersion by extra("0.3.8")
val shimmerVersion by extra("0.5.0")
val composeShimmerVersion by extra("1.4.0")
val media3Version by extra("1.10.1")
val perfVersion by extra("19.0.10")
val markwonVersion by extra("4.6.2")
val prism4jVersion by extra("2.0.0")
val swirlVersion by extra("1.3.0")
val indicatorseekbarVersion by extra("2.1.2")
val emojiVerison by extra("1.6.0")
val cronetOkhttpVersion by extra("0.1.1")
val diffUtilsVersion by extra("4.16")
val argon2ktVersion by extra("0.0.2")
val keccakVersion by extra("1.1.3")
val streetMapVersion by extra("6.1.20")
val junitVersion by extra("4.13.2")
val testCoreVersion by extra("1.7.0")
val mockitoVersion by extra("1.10.19")
val androidxJunitVersion by extra("1.3.0")
val hamcrestVersion by extra("1.3")
val mockitoCoreVersion by extra("2.7.19")
val testSupVersion by extra("1.0.1")
val robolectricVersion by extra("4.16.1")
val gsonVersion by extra("2.14.0")
val serializationVersion by extra("1.11.0")
val autodisposeVersion by extra("1.4.1")
val jsoupVersion by extra("1.13.1")
val largeImageViewVersion by extra("1.1.1")
val webrtcVersion by extra("1.0.30039")
val bitcoinPaymentURI by extra("1.0.3")
val startupVersion by extra("1.2.0")
val dnsVersion by extra("2.1.9")
val appCenterSdkVersion by extra("4.3.1")
val audioSwitchVersion by extra("1.7.1")
val balloonVersion by extra("1.7.6")
val markdownVersion by extra("0.7.3")
val bcVersion by extra("1.70")
val jsonVersion by extra("20251224")
val composeVersion by extra("1.11.2")
val accompanistVersion by extra("0.36.0")
val web3jVersion by extra("4.8.7-android")
val sol4kVersion by extra("0.7.0")
val sumsubVersion by extra("1.44.1")
val checkoutFramesVersion by extra("4.2.3")
val checkoutSecureVersion by extra("3.2.6")
val checkoutRiskVersion by extra("1.0.6")
val playWalletVersion by extra("20.0.0")
val playPayVersion by extra("16.5.0")
val datastoreVersion by extra("1.2.1")
val protobufVersion by extra("3.11.0")
val appsFlyerVersion by extra("6.18.0")
val installreferrerVersion by extra("2.2")
val billingVersion by extra("8.3.0")
val lottieComposeVersion by extra("6.7.1")
val composeBomVersion by extra("2026.05.01")
val activityComposeVersion by extra("1.13.0")
val reownBomVersion by extra("1.6.13")
val kotsonVersion by extra("2.5.0")
val playServicesMapsVersion by extra("20.0.0")
val playServicesLocationVersion by extra("21.3.0")
val firebaseBomVersion by extra("34.13.0")
val billingKtxVersion by extra("8.3.0")
val webpdecoderVersion by extra("2.7.4.16.0")
val tweetnaclVersion by extra("0.1.6")
val sol4kUtilitiesVersion by extra("0.1.0")
val desugarJdkLibsVersion by extra("2.1.5")
val bugsnagVersion by extra("6.26.0")

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.sumsub.com/repository/maven-public/") }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            name = "OSS-Sonatype"
        }
        maven { url = uri("https://artifacts.consensys.net/public/maven/maven/") }
        maven { url = uri("https://maven.fpregistry.io/releases") }
        maven {
            url = uri("https://maven.pkg.github.com/checkout/checkout-3ds-sdk-android")
            credentials {
                username = project.findProperty("GITHUB_ID") as String? ?: System.getenv("GITHUB_ID") ?: ""
                password = project.findProperty("GITHUB_PACKAGES_TOKEN") as String? ?: System.getenv("GITHUB_PACKAGES_TOKEN") ?: ""
            }
        }
    }
}
