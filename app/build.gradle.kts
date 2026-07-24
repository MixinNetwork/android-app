plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("de.undercouch.download")
    id("com.google.devtools.ksp")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.firebase.firebase-perf")
    id("com.bugsnag.android.gradle")
}

apply(plugin = "com.google.gms.google-services")
apply(plugin = "com.google.firebase.crashlytics")

val versionMajor = 5
val versionMinor = 2
val versionPatch = 1
val versionBuild = 1

val androidNdkVersion = rootProject.extra["androidNdkVersion"] as String
val jetifierVersion = rootProject.extra["jetifierVersion"] as String
val kotlinVersion = rootProject.extra["kotlinVersion"] as String
val tinkVersion = rootProject.extra["tinkVersion"] as String
val securityCryptoVersion = rootProject.extra["securityCryptoVersion"] as String
val webkitVersion = rootProject.extra["webkitVersion"] as String
val lightweightChartsVersion = rootProject.extra["lightweightChartsVersion"] as String
val mixinJwtVersion = rootProject.extra["mixinJwtVersion"] as String
val alphabetIndexVersion = rootProject.extra["alphabetIndexVersion"] as String
val web3jVersion = rootProject.extra["web3jVersion"] as String
val entityExtractionVersion = rootProject.extra["entityExtractionVersion"] as String
val bitcoinVersion = rootProject.extra["bitcoinVersion"] as String
val fragmentVersion = rootProject.extra["fragmentVersion"] as String
val activityVersion = rootProject.extra["activityVersion"] as String
val lifecycleVersion = rootProject.extra["lifecycleVersion"] as String
val appcompatVersion = rootProject.extra["appcompatVersion"] as String
val pagingVersion = rootProject.extra["pagingVersion"] as String
val coilVersion = rootProject.extra["coilVersion"] as String
val collectionVersion = rootProject.extra["collectionVersion"] as String
val roomVersion = rootProject.extra["roomVersion"] as String
val navigationVersion = rootProject.extra["navigationVersion"] as String
val workManagerVersion = rootProject.extra["workManagerVersion"] as String
val constraintLayoutVersion = rootProject.extra["constraintLayoutVersion"] as String
val constraintLayoutComposeVersion = rootProject.extra["constraintLayoutComposeVersion"] as String
val supportLibVersion = rootProject.extra["supportLibVersion"] as String
val recyclerViewVersion = rootProject.extra["recyclerViewVersion"] as String
val browserVersion = rootProject.extra["browserVersion"] as String
val biometricVersion = rootProject.extra["biometricVersion"] as String
val mdcVersion = rootProject.extra["mdcVersion"] as String
val exifinterfaceVersion = rootProject.extra["exifinterfaceVersion"] as String
val preferenceVersion = rootProject.extra["preferenceVersion"] as String
val hiltVersion = rootProject.extra["hiltVersion"] as String
val hiltAndroidxVersion = rootProject.extra["hiltAndroidxVersion"] as String
val androidxVersion = rootProject.extra["androidxVersion"] as String
val viewpagerVersion = rootProject.extra["viewpagerVersion"] as String
val sharetargetVersion = rootProject.extra["sharetargetVersion"] as String
val coordinatorVersion = rootProject.extra["coordinatorVersion"] as String
val espressoVersion = rootProject.extra["espressoVersion"] as String
val cameraxVersion = rootProject.extra["cameraxVersion"] as String
val glideVersion = rootProject.extra["glideVersion"] as String
val timberVersion = rootProject.extra["timberVersion"] as String
val okhttpVersion = rootProject.extra["okhttpVersion"] as String
val rxJavaVersion = rootProject.extra["rxJavaVersion"] as String
val rxAndroidVersion = rootProject.extra["rxAndroidVersion"] as String
val rxbindingVersion = rootProject.extra["rxbindingVersion"] as String
val retrofitVersion = rootProject.extra["retrofitVersion"] as String
val coroutineAdapterVersion = rootProject.extra["coroutineAdapterVersion"] as String
val libphonenumberVersion = rootProject.extra["libphonenumberVersion"] as String
val coroutinesVersion = rootProject.extra["coroutinesVersion"] as String
val mlkitBarcodeVersion = rootProject.extra["mlkitBarcodeVersion"] as String
val zxingVersion = rootProject.extra["zxingVersion"] as String
val ucropVersion = rootProject.extra["ucropVersion"] as String
val glideTransformationsVersion = rootProject.extra["glideTransformationsVersion"] as String
val jobqueueVersion = rootProject.extra["jobqueueVersion"] as String
val stickyHeadersRecyclerViewVersion = rootProject.extra["stickyHeadersRecyclerViewVersion"] as String
val threetenabpVersion = rootProject.extra["threetenabpVersion"] as String
val signalVersion = rootProject.extra["signalVersion"] as String
val playVersion = rootProject.extra["playVersion"] as String
val googlePlayServicesVersion = rootProject.extra["googlePlayServicesVersion"] as String
val svgVersion = rootProject.extra["svgVersion"] as String
val reboundVersion = rootProject.extra["reboundVersion"] as String
val shimmerVersion = rootProject.extra["shimmerVersion"] as String
val composeShimmerVersion = rootProject.extra["composeShimmerVersion"] as String
val media3Version = rootProject.extra["media3Version"] as String
val markwonVersion = rootProject.extra["markwonVersion"] as String
val prism4jVersion = rootProject.extra["prism4jVersion"] as String
val swirlVersion = rootProject.extra["swirlVersion"] as String
val indicatorseekbarVersion = rootProject.extra["indicatorseekbarVersion"] as String
val emojiVersion = rootProject.extra["emojiVersion"] as String
val cronetOkhttpVersion = rootProject.extra["cronetOkhttpVersion"] as String
val diffUtilsVersion = rootProject.extra["diffUtilsVersion"] as String
val argon2ktVersion = rootProject.extra["argon2ktVersion"] as String
val keccakVersion = rootProject.extra["keccakVersion"] as String
val streetMapVersion = rootProject.extra["streetMapVersion"] as String
val junitVersion = rootProject.extra["junitVersion"] as String
val testCoreVersion = rootProject.extra["testCoreVersion"] as String
val mockitoVersion = rootProject.extra["mockitoVersion"] as String
val androidxJunitVersion = rootProject.extra["androidxJunitVersion"] as String
val robolectricVersion = rootProject.extra["robolectricVersion"] as String
val gsonVersion = rootProject.extra["gsonVersion"] as String
val serializationVersion = rootProject.extra["serializationVersion"] as String
val autodisposeVersion = rootProject.extra["autodisposeVersion"] as String
val bitcoinPaymentUriVersion = rootProject.extra["bitcoinPaymentUriVersion"] as String
val startupVersion = rootProject.extra["startupVersion"] as String
val dnsVersion = rootProject.extra["dnsVersion"] as String
val audioSwitchVersion = rootProject.extra["audioSwitchVersion"] as String
val balloonVersion = rootProject.extra["balloonVersion"] as String
val markdownVersion = rootProject.extra["markdownVersion"] as String
val bcVersion = rootProject.extra["bcVersion"] as String
val jsonVersion = rootProject.extra["jsonVersion"] as String
val composeVersion = rootProject.extra["composeVersion"] as String
val accompanistVersion = rootProject.extra["accompanistVersion"] as String
val sol4kVersion = rootProject.extra["sol4kVersion"] as String
val protobufVersion = rootProject.extra["protobufVersion"] as String
val kotsonVersion = rootProject.extra["kotsonVersion"] as String
val lottieComposeVersion = rootProject.extra["lottieComposeVersion"] as String
val composeBomVersion = rootProject.extra["composeBomVersion"] as String
val reownBomVersion = rootProject.extra["reownBomVersion"] as String
val playServicesMapsVersion = rootProject.extra["playServicesMapsVersion"] as String
val playServicesLocationVersion = rootProject.extra["playServicesLocationVersion"] as String
val firebaseBomVersion = rootProject.extra["firebaseBomVersion"] as String
val webpDecoderVersion = rootProject.extra["webpDecoderVersion"] as String
val tweetnaclVersion = rootProject.extra["tweetnaclVersion"] as String
val sol4kUtilitiesVersion = rootProject.extra["sol4kUtilitiesVersion"] as String
val desugarJdkLibsVersion = rootProject.extra["desugarJdkLibsVersion"] as String
val bugsnagVersion = rootProject.extra["bugsnagVersion"] as String
val sumsubVersion = rootProject.extra["sumsubVersion"] as String
val checkoutFramesVersion = rootProject.extra["checkoutFramesVersion"] as String
val checkoutSecureVersion = rootProject.extra["checkoutSecureVersion"] as String
val checkoutRiskVersion = rootProject.extra["checkoutRiskVersion"] as String
val playWalletVersion = rootProject.extra["playWalletVersion"] as String
val playPayVersion = rootProject.extra["playPayVersion"] as String
val datastoreVersion = rootProject.extra["datastoreVersion"] as String
val appsFlyerVersion = rootProject.extra["appsFlyerVersion"] as String
val installReferrerVersion = rootProject.extra["installReferrerVersion"] as String
val billingVersion = rootProject.extra["billingVersion"] as String

val includeDebugX86_64 = project.findProperty("includeDebugX86_64")?.toString()?.toBoolean() ?: false

android {
    compileSdk = 37
    ndkVersion = androidNdkVersion
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
            force("com.android.tools.build.jetifier:jetifier-core:$jetifierVersion")
            force("com.github.mixinnetwork:tink-eddsa:$tinkVersion")
            force("junit:junit:$junitVersion")
            force("org.bouncycastle:bcprov-jdk15to18:$bcVersion")
            force("org.bouncycastle:bcutil-jdk15to18:$bcVersion")
            force("org.bouncycastle:bcpkix-jdk15to18:$bcVersion")
            force("org.jetbrains.kotlin:kotlin-metadata-jvm:$kotlinVersion")
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
    implementation("androidx.activity:activity-ktx:$activityVersion")
    implementation("androidx.appcompat:appcompat:$appcompatVersion")
    implementation("androidx.legacy:legacy-support-v4:$supportLibVersion")
    implementation("com.google.android.material:material:$mdcVersion")
    implementation("androidx.recyclerview:recyclerview:$recyclerViewVersion")
    implementation("androidx.exifinterface:exifinterface:$exifinterfaceVersion")
    implementation("androidx.browser:browser:$browserVersion")
    implementation("androidx.constraintlayout:constraintlayout:$constraintLayoutVersion")
    implementation("androidx.core:core-ktx:$androidxVersion")
    implementation("androidx.collection:collection-ktx:$collectionVersion")
    implementation("androidx.preference:preference-ktx:$preferenceVersion")
    implementation("androidx.viewpager2:viewpager2:$viewpagerVersion")
    implementation("androidx.sharetarget:sharetarget:$sharetargetVersion")
    implementation("androidx.coordinatorlayout:coordinatorlayout:$coordinatorVersion")
    implementation("androidx.biometric:biometric:$biometricVersion")
    implementation("androidx.security:security-crypto:$securityCryptoVersion")
    implementation("androidx.webkit:webkit:$webkitVersion")
    implementation("com.tradingview:lightweightcharts:$lightweightChartsVersion")

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
    implementation("androidx.emoji2:emoji2:$emojiVersion")
    implementation("androidx.emoji2:emoji2-views:$emojiVersion")
    implementation("androidx.emoji2:emoji2-bundled:$emojiVersion")
    implementation("androidx.emoji2:emoji2-views-helper:$emojiVersion")

    // DI
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-work:$hiltAndroidxVersion")
    implementation("androidx.hilt:hilt-navigation-compose:$hiltAndroidxVersion")
    ksp("androidx.hilt:hilt-compiler:$hiltAndroidxVersion")

    // RxJava
    implementation("io.reactivex.rxjava2:rxjava:$rxJavaVersion")
    implementation("io.reactivex.rxjava2:rxandroid:$rxAndroidVersion")

    implementation("com.github.mixinnetwork:tink-eddsa:$tinkVersion")
    implementation("com.github.mixinnetwork.jjwt:jjwt-api:$mixinJwtVersion")
    runtimeOnly("com.github.mixinnetwork.jjwt:jjwt-impl:$mixinJwtVersion")
    runtimeOnly("com.github.mixinnetwork.jjwt:jjwt-orgjson:$mixinJwtVersion") {
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
            strictly(protobufVersion)
        }
    }

    implementation("com.google.mlkit:barcode-scanning:$mlkitBarcodeVersion")
    implementation("com.google.android.play:app-update-ktx:$playVersion")
    implementation("com.google.android.gms:play-services-maps:$playServicesMapsVersion")
    implementation("com.google.android.gms:play-services-location:$playServicesLocationVersion")
    implementation("com.google.android.gms:play-services-cronet:$googlePlayServicesVersion")
    implementation("com.google.zxing:core:$zxingVersion")
    implementation("com.github.tougee:sticky-headers-recyclerview:$stickyHeadersRecyclerViewVersion")
    implementation("org.whispersystems:signal-protocol-android:$signalVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion")
    implementation("com.github.zjupure:webpdecoder:$webpDecoderVersion")
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    implementation("com.github.bumptech.glide:okhttp3-integration:$glideVersion")
    ksp("com.github.bumptech.glide:ksp:$glideVersion")
    implementation("jp.wasabeef:glide-transformations:$glideTransformationsVersion")
    implementation("com.jakewharton.timber:timber:$timberVersion")
    implementation("com.github.myinnos:AlphabetIndex-Fast-Scroll-RecyclerView:$alphabetIndexVersion")
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
    implementation("com.github.SandroMachado:BitcoinPaymentURI:$bitcoinPaymentUriVersion")
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
    implementation("androidx.activity:activity-compose:$activityVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")

    // accompanist
    implementation("com.google.accompanist:accompanist-navigation-animation:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-pager:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-pager-indicators:$accompanistVersion")

    // wallet connect && web3j
    implementation("org.web3j:core:$web3jVersion")
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
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
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
    implementation("com.google.mlkit:entity-extraction:$entityExtractionVersion")

    testImplementation("com.google.protobuf:protobuf-javalite") {
        version {
            strictly(protobufVersion)
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
    implementation("com.android.installreferrer:installreferrer:$installReferrerVersion")
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
