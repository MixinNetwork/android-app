plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("de.undercouch.download")
    id("com.google.devtools.ksp")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("com.google.firebase.firebase-perf")
    id("com.bugsnag.android.gradle")
}

apply(plugin = "com.google.gms.google-services")
apply(plugin = "com.google.firebase.crashlytics")

val versionMajor = 5
val versionMinor = 2
val versionPatch = 1
val versionBuild = 1


val bitcoinVersion: String by rootProject.extra
val fragmentVersion: String by rootProject.extra
val activity_version: String by rootProject.extra
val lifecycleVersion: String by rootProject.extra
val appcompatVersion: String by rootProject.extra
val pagingVersion: String by rootProject.extra
val coilVersion: String by rootProject.extra
val collectionx: String by rootProject.extra
val roomVersion: String by rootProject.extra
val navigationVersion: String by rootProject.extra
val workManagerVersion: String by rootProject.extra
val constraintLayoutVersion: String by rootProject.extra
val constraintLayoutComposeVersion: String by rootProject.extra
val supportLibVersion: String by rootProject.extra
val recyclerViewVersion: String by rootProject.extra
val browserVersion: String by rootProject.extra
val biometricVersion: String by rootProject.extra
val mdcVersion: String by rootProject.extra
val exifinterfaceVersion: String by rootProject.extra
val preferenceVersion: String by rootProject.extra
val hiltVersion: String by rootProject.extra
val hiltAndroidxVersion: String by rootProject.extra
val androidxVersion: String by rootProject.extra
val viewpagerVersion: String by rootProject.extra
val sharetargetVersion: String by rootProject.extra
val coordinatorVersion: String by rootProject.extra
val espressoVersion: String by rootProject.extra
val cameraxVersion: String by rootProject.extra
val glideVersion: String by rootProject.extra
val timberVersion: String by rootProject.extra
val okhttpVersion: String by rootProject.extra
val rxJavaVersion: String by rootProject.extra
val rxAndroidVersion: String by rootProject.extra
val rxbindingVersion: String by rootProject.extra
val retrofitVersion: String by rootProject.extra
val coroutineAdapterVersion: String by rootProject.extra
val libphonenumberVersion: String by rootProject.extra
val coroutinesVersion: String by rootProject.extra
val mlkitBarcodeVersion: String by rootProject.extra
val zxingVersion: String by rootProject.extra
val ucropVersion: String by rootProject.extra
val glideTransformationsVersion: String by rootProject.extra
val jobqueueVersion: String by rootProject.extra
val stickyheadersrecyclerviewVersion: String by rootProject.extra
val threetenabpVersion: String by rootProject.extra
val signalVersion: String by rootProject.extra
val playVersion: String by rootProject.extra
val googlePlayServicesVersion: String by rootProject.extra
val svgVersion: String by rootProject.extra
val reboundVersion: String by rootProject.extra
val shimmerVersion: String by rootProject.extra
val composeShimmerVersion: String by rootProject.extra
val media3Version: String by rootProject.extra
val markwonVersion: String by rootProject.extra
val prism4jVersion: String by rootProject.extra
val swirlVersion: String by rootProject.extra
val indicatorseekbarVersion: String by rootProject.extra
val emojiVerison: String by rootProject.extra
val cronetOkhttpVersion: String by rootProject.extra
val diffUtilsVersion: String by rootProject.extra
val argon2ktVersion: String by rootProject.extra
val keccakVersion: String by rootProject.extra
val streetMapVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val testCoreVersion: String by rootProject.extra
val mockitoVersion: String by rootProject.extra
val androidxJunitVersion: String by rootProject.extra
val robolectricVersion: String by rootProject.extra
val gsonVersion: String by rootProject.extra
val serializationVersion: String by rootProject.extra
val autodisposeVersion: String by rootProject.extra
val isoparserVersion: String by rootProject.extra
val bitcoinPaymentURI: String by rootProject.extra
val startupVersion: String by rootProject.extra
val dnsVersion: String by rootProject.extra
val audioSwitchVersion: String by rootProject.extra
val balloonVersion: String by rootProject.extra
val markdownVersion: String by rootProject.extra
val bcVersion: String by rootProject.extra
val jsonVersion: String by rootProject.extra
val composeVersion: String by rootProject.extra
val accompanistVersion: String by rootProject.extra
val sol4kVersion: String by rootProject.extra
val kotsonVersion: String by rootProject.extra
val lottieComposeVersion: String by rootProject.extra
val composeBomVersion: String by rootProject.extra
val activityComposeVersion: String by rootProject.extra
val reownBomVersion: String by rootProject.extra
val playServicesMapsVersion: String by rootProject.extra
val playServicesLocationVersion: String by rootProject.extra
val firebaseBomVersion: String by rootProject.extra
val billingKtxVersion: String by rootProject.extra
val webpdecoderVersion: String by rootProject.extra
val tweetnaclVersion: String by rootProject.extra
val sol4kUtilitiesVersion: String by rootProject.extra
val desugarJdkLibsVersion: String by rootProject.extra
val bugsnagVersion: String by rootProject.extra
val sumsubVersion: String by rootProject.extra
val checkoutFramesVersion: String by rootProject.extra
val checkoutSecureVersion: String by rootProject.extra
val checkoutRiskVersion: String by rootProject.extra
val playWalletVersion: String by rootProject.extra
val playPayVersion: String by rootProject.extra
val datastoreVersion: String by rootProject.extra
val appsFlyerVersion: String by rootProject.extra
val installreferrerVersion: String by rootProject.extra
val billingVersion: String by rootProject.extra

val includeDebugX86_64 = project.findProperty("includeDebugX86_64")?.toString()?.toBoolean() ?: false

android {
    compileSdk = 37
    ndkVersion = "27.0.12077973"
    namespace = "one.mixin.android"
    defaultConfig {
        applicationId = "one.mixin.messenger"
        minSdk = 26
        targetSdk = 36
        versionCode = versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild
        versionName = "$versionMajor.$versionMinor.$versionPatch"
        multiDexEnabled = true
        testInstrumentationRunner = "one.mixin.android.CustomTestRunner"
        vectorDrawables.useSupportLibrary = true
    }

    androidResources {
        localeFilters += listOf("en", "es", "in", "ja", "ms", "ru", "zh-rCN", "zh-rTW")
    }

    packaging {
        resources {
            excludes += "**/*.kotlin_metadata"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/*.version"
            excludes += "META-INF/DISCLAIMER"
            excludes += "META-INF/NOTICE.md"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
            excludes += setOf("lib/x86_64/libcurve25519.so")
        }
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
        abi {
            enableSplit = true
        }
        storeArchive {
            enable = false
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    sourceSets {
        val sharedTestDir = "src/sharedTest/java"
        getByName("test") {
            java.directories.add(sharedTestDir)
        }
        getByName("androidTest") {
            java.directories.add(sharedTestDir)
            assets.directories.add("$projectDir/schemas")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    flavorDimensions += "channel"
    productFlavors {
        create("googlePlay") {
            dimension = "channel"
            buildConfigField("boolean", "IS_GOOGLE_PLAY", "true")
        }
        create("otherChannel") {
            dimension = "channel"
            buildConfigField("boolean", "IS_GOOGLE_PLAY", "false")
        }
    }

    testBuildType = "debug"

    signingConfigs {
        getByName("debug") {
            storeFile = file("keys/debug.keystore")
            storePassword = "android"
            keyAlias = "AndroidDebugKey"
            keyPassword = "android"
        }
        if (project.hasProperty("RELEASE_STORE_FILE")) {
            create("release") {
                keyAlias = project.property("RELEASE_KEY_ALIAS") as String
                keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
                storeFile = file(project.property("RELEASE_STORE_FILE") as String)
                storePassword = project.property("RELEASE_STORE_PASSWORD") as String
            }
        }
    }

    buildTypes {
        release {
            ndk {
                abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86_64")
            }
            isDebuggable = false
            isJniDebuggable = false
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (project.hasProperty("RELEASE_STORE_FILE")) {
                signingConfig = signingConfigs.getByName("release")
                configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                    mappingFileUploadEnabled = true
                }
            } else {
                configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                    mappingFileUploadEnabled = false
                }
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isJniDebuggable = false
            configure<com.google.firebase.perf.plugin.FirebasePerfExtension> {
                setInstrumentationEnabled(false)
            }
            ndk {
                if (includeDebugX86_64) {
                    abiFilters += setOf("arm64-v8a", "x86_64")
                } else {
                    abiFilters += setOf("arm64-v8a")
                }
            }
        }
        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".test"
            matchingFallbacks += listOf("debug")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = false
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = false
        disable += setOf("ObjectAnimatorBinding", "AnimatorKeep", "MissingTranslation")
    }

    configurations.configureEach {
        resolutionStrategy {
            force("com.android.tools.build.jetifier:jetifier-core:1.0.0-beta10")
            force("com.github.mixinnetwork:tink-eddsa:0.0.13")
            force("junit:junit:4.13.2")
            force("org.bouncycastle:bcprov-jdk15to18:$bcVersion")
            force("org.bouncycastle:bcutil-jdk15to18:$bcVersion")
            force("org.bouncycastle:bcpkix-jdk15to18:$bcVersion")
            force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.10")
            dependencySubstitution {
                substitute(module("org.bouncycastle:bcprov-jdk15on")).using(module("org.bouncycastle:bcprov-jdk15to18:$bcVersion"))
                substitute(module("org.bouncycastle:bcutil-jdk15on")).using(module("org.bouncycastle:bcutil-jdk15to18:$bcVersion"))
            }
        }
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }
}

bugsnag {
    uploadNdkMappings = false
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation("org.bitcoinj:bitcoinj-core:$bitcoinVersion") {
        exclude(group = "net.jcip", module = "jcip-annotations")
    }
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:$desugarJdkLibsVersion")
    implementation(platform("com.google.firebase:firebase-bom:$firebaseBomVersion"))
    implementation("com.google.firebase:firebase-perf")
    implementation(fileTree(mapOf("include" to listOf("*.aar"), "dir" to "libs")))
    implementation("androidx.fragment:fragment-ktx:$fragmentVersion")
    implementation("androidx.activity:activity-ktx:$activity_version")
    implementation("androidx.appcompat:appcompat:$appcompatVersion")
    implementation("androidx.legacy:legacy-support-v4:$supportLibVersion")
    implementation("com.google.android.material:material:$mdcVersion")
    implementation("androidx.recyclerview:recyclerview:$recyclerViewVersion")
    implementation("androidx.exifinterface:exifinterface:$exifinterfaceVersion")
    implementation("androidx.browser:browser:$browserVersion")
    implementation("androidx.constraintlayout:constraintlayout:$constraintLayoutVersion")
    implementation("androidx.core:core-ktx:$androidxVersion")
    implementation("androidx.collection:collection-ktx:$collectionx")
    implementation("androidx.preference:preference-ktx:$preferenceVersion")
    implementation("androidx.viewpager2:viewpager2:$viewpagerVersion")
    implementation("androidx.sharetarget:sharetarget:$sharetargetVersion")
    implementation("androidx.coordinatorlayout:coordinatorlayout:$coordinatorVersion")
    implementation("androidx.biometric:biometric:$biometricVersion")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("androidx.webkit:webkit:1.16.0")

    implementation("org.sol4k:tweetnacl:$tweetnaclVersion")
    implementation("org.sol4k:utilities:$sol4kUtilitiesVersion")

    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")

    implementation("androidx.work:work-runtime-ktx:$workManagerVersion")
    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVersion")
    implementation("androidx.navigation:navigation-compose:$navigationVersion")
    implementation("io.coil-kt.coil3:coil-compose:$coilVersion")
    implementation("io.coil-kt.coil3:coil-network-okhttp:$coilVersion")
    implementation("io.coil-kt.coil3:coil-svg:$coilVersion")
    implementation("io.coil-kt.coil3:coil-network-cache-control:$coilVersion")
    implementation("io.coil-kt.coil3:coil-video:$coilVersion")
    implementation("io.coil-kt.coil3:coil-gif:$coilVersion")
    // Architecture components
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")
    implementation("androidx.paging:paging-common-ktx:$pagingVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-rxjava2:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")

    // media3
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-transformer:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")

    // emoji
    implementation("androidx.emoji2:emoji2:$emojiVerison")
    implementation("androidx.emoji2:emoji2-views:$emojiVerison")
    implementation("androidx.emoji2:emoji2-bundled:$emojiVerison")
    implementation("androidx.emoji2:emoji2-views-helper:$emojiVerison")

    // DI
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-work:$hiltAndroidxVersion")
    implementation("androidx.hilt:hilt-navigation-compose:$hiltAndroidxVersion")
    ksp("androidx.hilt:hilt-compiler:$hiltAndroidxVersion")

    // RxJava
    implementation("io.reactivex.rxjava2:rxjava:$rxJavaVersion")
    implementation("io.reactivex.rxjava2:rxandroid:$rxAndroidVersion")

    implementation("com.github.mixinnetwork:tink-eddsa:0.0.13")
    implementation("com.github.mixinnetwork.jjwt:jjwt-api:2b1c61aa2f")
    runtimeOnly("com.github.mixinnetwork.jjwt:jjwt-impl:2b1c61aa2f")
    runtimeOnly("com.github.mixinnetwork.jjwt:jjwt-orgjson:2b1c61aa2f") {
        exclude(group = "org.json", module = "json")
    }

    implementation("com.jakewharton.threetenabp:threetenabp:$threetenabpVersion")

    // retrofit
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("com.squareup.okhttp3:okhttp-tls:$okhttpVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.retrofit2:adapter-rxjava2:$retrofitVersion")
    implementation("com.jakewharton.rxbinding3:rxbinding:$rxbindingVersion")
    implementation("com.jakewharton.rxbinding3:rxbinding-material:$rxbindingVersion")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:$coroutineAdapterVersion")

    implementation("com.google.net.cronet:cronet-okhttp:$cronetOkhttpVersion")

    implementation("com.google.code.gson:gson:$gsonVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("com.google.protobuf:protobuf-javalite") {
        version {
            strictly("3.11.0")
        }
    }

    implementation("com.android.billingclient:billing-ktx:$billingKtxVersion")
    implementation("com.google.mlkit:barcode-scanning:$mlkitBarcodeVersion")
    implementation("com.google.android.play:app-update-ktx:$playVersion")
    implementation("com.google.android.gms:play-services-maps:$playServicesMapsVersion")
    implementation("com.google.android.gms:play-services-location:$playServicesLocationVersion")
    implementation("com.google.android.gms:play-services-cronet:$googlePlayServicesVersion")
    implementation("com.google.zxing:core:$zxingVersion")
    implementation("com.github.tougee:sticky-headers-recyclerview:$stickyheadersrecyclerviewVersion")
    implementation("org.whispersystems:signal-protocol-android:$signalVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion")
    implementation("com.github.zjupure:webpdecoder:$webpdecoderVersion")
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    implementation("com.github.bumptech.glide:okhttp3-integration:$glideVersion")
    ksp("com.github.bumptech.glide:ksp:$glideVersion")
    implementation("jp.wasabeef:glide-transformations:$glideTransformationsVersion")
    implementation("com.jakewharton.timber:timber:$timberVersion")
    implementation("com.github.myinnos:AlphabetIndex-Fast-Scroll-RecyclerView:1.0.95")
    implementation("com.googlecode.libphonenumber:libphonenumber:$libphonenumberVersion")
    implementation("com.github.tougee:android-priority-jobqueue:$jobqueueVersion")
    implementation("com.github.yalantis:ucrop:$ucropVersion")
    implementation("com.facebook.rebound:rebound:$reboundVersion")
    implementation("com.facebook.shimmer:shimmer:$shimmerVersion")
    implementation("com.valentinilk.shimmer:compose-shimmer:$composeShimmerVersion")

    implementation("com.uber.autodispose:autodispose:$autodisposeVersion")
    implementation("com.uber.autodispose:autodispose-android:$autodisposeVersion")
    implementation("com.uber.autodispose:autodispose-android-archcomponents:$autodisposeVersion")
    implementation("com.uber.autodispose:autodispose-lifecycle:$autodisposeVersion")
    implementation("com.googlecode.mp4parser:isoparser:$isoparserVersion")
    implementation("io.noties.markwon:core:$markwonVersion")
    implementation("io.noties.markwon:image:$markwonVersion")
    implementation("io.noties.markwon:image-glide:$markwonVersion")
    implementation("io.noties.markwon:ext-tables:$markwonVersion")
    implementation("io.noties.markwon:recycler:$markwonVersion")
    implementation("io.noties.markwon:ext-strikethrough:$markwonVersion")
    implementation("io.noties.markwon:html:$markwonVersion")
    implementation("io.noties.markwon:editor:$markwonVersion")
    implementation("io.noties:prism4j:$prism4jVersion")
    implementation("io.noties.markwon:syntax-highlight:$markwonVersion")
    implementation("io.noties.markwon:ext-tasklist:$markwonVersion")
    implementation("com.github.SandroMachado:BitcoinPaymentURI:$bitcoinPaymentURI")
    implementation("com.caverock:androidsvg-aar:$svgVersion")
    implementation("androidx.startup:startup-runtime:$startupVersion")
    implementation("dnsjava:dnsjava:$dnsVersion")
    implementation("com.github.SeniorZhai:audioswitch:$audioSwitchVersion")
    implementation("com.github.skydoves:balloon:$balloonVersion")
    implementation("org.osmdroid:osmdroid-android:$streetMapVersion")
    implementation("com.mattprecious.swirl:swirl:$swirlVersion")
    implementation("com.github.SeniorZhai:IndicatorSeekBar:$indicatorseekbarVersion")
    implementation("org.jetbrains:markdown:$markdownVersion")
    implementation("io.github.java-diff-utils:java-diff-utils:$diffUtilsVersion")
    implementation("com.github.tougee:argon2kt:$argon2ktVersion")
    implementation("com.github.komputing.khash:keccak-jvm:$keccakVersion")

    // compose
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.runtime:runtime-livedata:$composeVersion")
    implementation("androidx.compose.runtime:runtime-rxjava2:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.paging:paging-compose:$pagingVersion")
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.constraintlayout:constraintlayout-compose:$constraintLayoutComposeVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")

    // accompanist
    implementation("com.google.accompanist:accompanist-navigation-animation:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-pager:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-pager-indicators:$accompanistVersion")

    // wallet connect && web3j
    implementation("org.web3j:core:4.12.3-android")
    implementation(platform("com.reown:android-bom:$reownBomVersion"))
    implementation("com.reown:android-core")
    testImplementation("com.reown:android-core")
    implementation("com.reown:walletkit")

    implementation("org.sol4k:sol4k:$sol4kVersion")

    implementation("com.github.salomonbrys.kotson:kotson:$kotsonVersion")

    implementation("com.airbnb.android:lottie-compose:$lottieComposeVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("androidx.test:core:$testCoreVersion")
    androidTestImplementation("org.mockito:mockito-core:$mockitoVersion")

    testImplementation("org.robolectric:robolectric:$robolectricVersion") {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    }

    // for jwt unit test
    testImplementation("org.json:json:$jsonVersion")

    androidTestImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    androidTestImplementation("androidx.test.espresso:espresso-contrib:$espressoVersion") {
        exclude(group = "com.android.support", module = "support-annotations")
        exclude(group = "org.checkerframework", module = "checker")
    }
    androidTestImplementation("androidx.test.espresso:espresso-idling-resource:$espressoVersion")
    androidTestImplementation("androidx.test.ext:junit:$androidxJunitVersion")
    androidTestImplementation("androidx.fragment:fragment-testing:$fragmentVersion")
    androidTestImplementation("androidx.navigation:navigation-testing:$navigationVersion")

    // Hilt testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:$hiltVersion")
    kspAndroidTest("androidx.hilt:hilt-compiler:$hiltAndroidxVersion")

    // ML Kit
    implementation("com.google.mlkit:entity-extraction:16.0.0-beta6")

    testImplementation("com.google.protobuf:protobuf-javalite") {
        version {
            strictly("3.11.0")
        }
    }

    // SumSub
    implementation("com.sumsub.sns:idensic-mobile-sdk:$sumsubVersion") {
        exclude(group = "com.twilio.audioswitch", module = "AudioDevice")
    }
    // checkout
    implementation("com.github.checkout:frames-android:$checkoutFramesVersion")
    implementation("com.checkout:checkout-sdk-3ds-android:$checkoutSecureVersion")
    implementation("com.github.checkout:checkout-risk-sdk-android:$checkoutRiskVersion")

    // google play
    implementation("com.google.android.gms:play-services-wallet:$playWalletVersion")
    implementation("com.google.android.gms:play-services-pay:$playPayVersion")

    // Jetpack Datastore - Proto Datastore
    implementation("androidx.datastore:datastore:$datastoreVersion")

    // AppsFlyer
    implementation("com.appsflyer:af-android-sdk:$appsFlyerVersion") {
        exclude(group = "com.squareup.leakcanary", module = "leakcanary-android-process")
    }
    implementation("com.android.installreferrer:installreferrer:$installreferrerVersion")
    implementation("com.android.billingclient:billing:$billingVersion")
    implementation("com.android.billingclient:billing-ktx:$billingVersion")
    implementation("com.bugsnag:bugsnag-android:$bugsnagVersion")
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}

secrets {
    defaultPropertiesFileName = "local.defaults.properties"
}

configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

tasks.register("allTests") {
    dependsOn("testStagingUnitTest", "connectedStagingAndroidTest")
    description = "Run unit tests and instrumentation tests"
}

tasks.register("syncStrings") {
    doLast {
        listOf("en", "zh", "zh-TW", "ja", "ru", "in", "ms").forEach { lang ->
            project.extensions.getByName("download").let { ext ->
                val downloadExt = ext as de.undercouch.gradle.tasks.download.DownloadExtension
                downloadExt.run {
                    src("https://raw.githubusercontent.com/Tougee/sync-google-sheet/master/generated/output/Android/value-$lang/strings.xml")
                    dest(
                        when (lang) {
                            "en" -> "src/main/res/values"
                            "zh" -> "src/main/res/values-zh-rCN"
                            "zh-TW" -> "src/main/res/values-zh-rTW"
                            "zh-HK" -> "src/main/res/values-zh-rHK"
                            else -> "src/main/res/values-$lang"
                        }
                    )
                }
            }
        }
    }
}
