-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

-dontobfuscate

# prevent multi dex caused NoSuchProviderException
-keep class org.whispersystems.** { *; }

-keep class one.mixin.android.** { *; }

-keep class io.jsonwebtoken.** { *; }

# webrtc
-dontwarn org.webrtc.NetworkMonitorAutoDetect
-dontwarn android.net.Network
-keep class org.webrtc.** { *; }

# androidx paging
-keep class androidx.paging.PagedListAdapter.** { *; }
-keep class androidx.paging.AsyncPagedListDiffer.** { *; }

# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

-keep class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keep class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory {}

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

-keepclassmembers,allowobfuscation class * {
 @com.google.gson.annotations.SerializedName <fields>;
}

-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

-keep class kotlin.coroutines.Continuation

# web3j
-keep class org.web3j.protocol.** { *; }
-keep class org.web3j.crypto.** { *; }
-keep class org.web3j.abi.** { *; }

-dontwarn com.fasterxml.jackson.databind.**
-keep class com.fasterxml.jackson.core.** { *; }
-keep interface com.fasterxml.jackson.core.* { *; }
-keep class com.fasterxml.jackson.databind.** { *; }
-keep interface com.fasterxml.jackson.databind.* { *; }
-keep class com.fasterxml.jackson.annotation.** { *; }
-keep interface com.fasterxml.jackson.annotation.** { *; }
#-dontwarn java.lang.SafeVarargs
-dontwarn org.slf4j.**

-keep public class com.walletconnect.android.** { *; }
-keep public class com.walletconnect.web3.** { *; }

-dontwarn groovy.lang.GroovyShell
