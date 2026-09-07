-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Preserve third-party names for reflection while allowing shrinking and optimization.
-keep,allowshrinking,allowoptimization class !one.mixin.android.**,!androidx.**,!com.yalantis.ucrop.**,** { *; }

# prevent multi dex caused NoSuchProviderException
-keep class org.whispersystems.** { *; }

# Keep app types and members reachable while allowing optimization and obfuscation.
-keep,allowoptimization,allowobfuscation class one.mixin.android.** { *; }

-keep class com.google.android.gms.internal.mlkit_entity_extraction.** extends java.util.Random { *; }

# Gson still relies on unannotated app fields in API, websocket, database, and cache models.
-keepclassmembers class one.mixin.android.** {
    !transient !static <fields>;
}

# Persisted jobs and payloads must remain Java-serialization compatible across updates.
-keep class one.mixin.android.** implements java.io.Serializable { *; }

# These fragment arguments persist nested type names in saved state.
-keepnames class one.mixin.android.ui.wallet.ImportKeyBottomSheetDialogFragment$PopupType$*
-keepnames class one.mixin.android.ui.home.reminder.ReminderBottomSheetDialogFragment$PopupType$*

-keep class io.jsonwebtoken.** { *; }

# webrtc
-dontwarn org.webrtc.NetworkMonitorAutoDetect
-dontwarn android.net.Network
-keep class org.webrtc.** { *; }

-keep class org.jni_zero.** { *; }
-dontwarn org.jni_zero.**

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-dontwarn org.webrtc.**

-dontwarn sun.net.spi.nameservice.**

-keep class com.birbit.android.jobqueue.** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep generic signature of RxJava2 (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking class io.reactivex.Single
-keep,allowobfuscation,allowshrinking class io.reactivex.Flowable
-keep,allowobfuscation,allowshrinking class io.reactivex.Observable
-keep,allowobfuscation,allowshrinking class io.reactivex.Completable


# https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md#r8-full-mode

-keepattributes Signature
-keep class * extends com.google.gson.reflect.TypeToken

# web3j
-keep class org.web3j.protocol.** { *; }
-keep class org.web3j.abi.** { *; }
-keep class org.web3j.crypto.** { *; }
-dontwarn org.web3j.crypto.**
-dontwarn jnr.unixsocket.**
-dontwarn org.web3j.protocol.ipc.**
-dontwarn org.java_websocket.**
-dontwarn org.web3j.protocol.websocket.**

-dontwarn com.fasterxml.jackson.databind.**
-keep class com.fasterxml.jackson.core.** { *; }
-keep class com.fasterxml.jackson.databind.** { *; }
-keep class com.fasterxml.jackson.annotation.** { *; }
#-dontwarn java.lang.SafeVarargs
-dontwarn org.slf4j.**

-keep public class com.reown.walletkit.** { *; }

-dontwarn com.sun.jna.**
-keep class com.sun.jna.** { *; }

-dontwarn uniffi.**
-keep class uniffi.** { *; }

-dontwarn groovy.lang.GroovyShell

-dontwarn com.yalantis.ucrop**

-dontwarn com.appsflyer.**
-keep class kotlin.jvm.internal.** { *; }
