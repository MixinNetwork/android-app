buildscript {
    val jetifierVersion = "1.0.0-beta10"
    val hiltGradlePluginVersion = "2.60.1"
    val googleServicesPluginVersion = "4.5.0"
    val firebaseCrashlyticsPluginVersion = "3.0.7"
    val bugsnagGradlePluginVersion = "8.+"

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.sumsub.com/repository/maven-public/") }
    }
    dependencies {
        classpath("com.android.tools.build.jetifier:jetifier-processor:$jetifierVersion")
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltGradlePluginVersion")
        classpath("com.google.gms:google-services:$googleServicesPluginVersion")
        classpath("com.google.firebase:firebase-crashlytics-gradle:$firebaseCrashlyticsPluginVersion")
        classpath("com.bugsnag:bugsnag-android-gradle-plugin:$bugsnagGradlePluginVersion")
    }
}

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.kotlin.plugin.parcelize") apply false
    id("de.undercouch.download") apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("com.google.devtools.ksp") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
    id("com.google.firebase.firebase-perf") apply false
}


extra.set("androidNdkVersion", "27.0.12077973")
extra.set("jetifierVersion", "1.0.0-beta10")
extra.set("kotlinVersion", "2.4.10")
extra.set("tinkVersion", "0.0.13")
extra.set("securityCryptoVersion", "1.1.0")
extra.set("webkitVersion", "1.16.0")
extra.set("mixinJwtVersion", "2b1c61aa2f")
extra.set("alphabetIndexVersion", "1.0.95")
extra.set("web3jVersion", "4.12.3-android")
extra.set("entityExtractionVersion", "16.0.0-beta6")
extra.set("bitcoinVersion", "0.17.1")
extra.set("fragmentVersion", "1.8.9")
extra.set("activityVersion", "1.13.0")
extra.set("lifecycleVersion", "2.11.0")
extra.set("appcompatVersion", "1.7.1")
extra.set("pagingVersion", "3.5.0")
extra.set("coilVersion", "3.5.0")
extra.set("collectionVersion", "1.6.0")
extra.set("roomVersion", "2.8.4")
extra.set("navigationVersion", "2.9.8")
extra.set("workManagerVersion", "2.11.2")
extra.set("constraintLayoutVersion", "2.2.1")
extra.set("constraintLayoutComposeVersion", "1.1.1")
extra.set("supportLibVersion", "1.0.0")
extra.set("recyclerViewVersion", "1.4.0")
extra.set("browserVersion", "1.10.0")
extra.set("biometricVersion", "1.4.0-alpha07")
extra.set("mdcVersion", "1.14.0")
extra.set("exifinterfaceVersion", "1.4.2")
extra.set("preferenceVersion", "1.2.1")
extra.set("hiltVersion", "2.60.1")
extra.set("hiltAndroidxVersion", "1.4.0")
extra.set("androidxVersion", "1.19.0")
extra.set("viewpagerVersion", "1.1.0")
extra.set("sharetargetVersion", "1.2.0")
extra.set("coordinatorVersion", "1.3.0")
extra.set("espressoVersion", "3.7.0")
extra.set("cameraxVersion", "1.6.1")
extra.set("glideVersion", "5.0.9")
extra.set("timberVersion", "5.0.1")
extra.set("okhttpVersion", "5.4.0")
extra.set("rxJavaVersion", "2.2.21")
extra.set("rxAndroidVersion", "2.1.1")
extra.set("rxbindingVersion", "3.1.0")
extra.set("retrofitVersion", "3.0.0")
extra.set("coroutineAdapterVersion", "0.9.2")
extra.set("libphonenumberVersion", "9.0.30")
extra.set("coroutinesVersion", "1.11.0")
extra.set("mlkitBarcodeVersion", "17.3.0")
extra.set("zxingVersion", "3.5.4")
extra.set("ucropVersion", "2.2.11")
extra.set("glideTransformationsVersion", "4.3.0")
extra.set("jobqueueVersion", "3.1.0")
extra.set("stickyHeadersRecyclerViewVersion", "0.7")
extra.set("threetenabpVersion", "1.4.9")
extra.set("signalVersion", "2.8.1")
extra.set("playVersion", "2.1.0")
extra.set("googlePlayServicesVersion", "18.1.1")
extra.set("svgVersion", "1.4")
extra.set("reboundVersion", "0.3.8")
extra.set("shimmerVersion", "0.5.0")
extra.set("composeShimmerVersion", "1.4.0")
extra.set("media3Version", "1.10.1")
extra.set("markwonVersion", "4.6.2")
extra.set("prism4jVersion", "2.0.0")
extra.set("swirlVersion", "1.3.0")
extra.set("indicatorseekbarVersion", "2.1.2")
extra.set("emojiVersion", "1.6.0")
extra.set("cronetOkhttpVersion", "0.1.1")
extra.set("diffUtilsVersion", "4.16")
extra.set("argon2ktVersion", "0.0.2")
extra.set("keccakVersion", "1.1.3")
extra.set("streetMapVersion", "6.1.20")
extra.set("junitVersion", "4.13.2")
extra.set("testCoreVersion", "1.7.0")
extra.set("mockitoVersion", "1.10.19")
extra.set("androidxJunitVersion", "1.3.0")
extra.set("robolectricVersion", "4.16.1")
extra.set("gsonVersion", "2.14.0")
extra.set("serializationVersion", "1.11.0")
extra.set("autodisposeVersion", "1.4.1")
extra.set("bitcoinPaymentUriVersion", "1.0.3")
extra.set("startupVersion", "1.2.0")
extra.set("dnsVersion", "2.1.9")
extra.set("audioSwitchVersion", "1.1.8")
extra.set("balloonVersion", "1.7.6")
extra.set("markdownVersion", "0.7.3")
extra.set("bcVersion", "1.70")
extra.set("jsonVersion", "20251224")
extra.set("composeVersion", "1.11.4")
extra.set("accompanistVersion", "0.36.0")
extra.set("sol4kVersion", "0.8.2")
extra.set("protobufVersion", "3.11.0")
extra.set("kotsonVersion", "2.5.0")
extra.set("lottieComposeVersion", "6.7.1")
extra.set("composeBomVersion", "2026.05.01")
extra.set("reownBomVersion", "1.6.13")
extra.set("playServicesMapsVersion", "20.0.0")
extra.set("playServicesLocationVersion", "21.4.0")
extra.set("firebaseBomVersion", "34.13.0")
extra.set("webpDecoderVersion", "2.7.4.16.0")
extra.set("tweetnaclVersion", "0.1.6")
extra.set("sol4kUtilitiesVersion", "0.1.0")
extra.set("desugarJdkLibsVersion", "2.1.5")
extra.set("bugsnagVersion", "6.26.0")
extra.set("sumsubVersion", "1.44.1")
extra.set("checkoutFramesVersion", "4.2.3")
extra.set("checkoutSecureVersion", "3.2.6")
extra.set("checkoutRiskVersion", "1.0.6")
extra.set("playWalletVersion", "20.0.0")
extra.set("playPayVersion", "16.5.0")
extra.set("datastoreVersion", "1.2.1")
extra.set("appsFlyerVersion", "6.18.0")
extra.set("installReferrerVersion", "2.2")
extra.set("billingVersion", "8.3.0")

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
