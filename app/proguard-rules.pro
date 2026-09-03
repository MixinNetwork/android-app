-allowaccessmodification

# Gson still reads unannotated app fields by their source names.
-keepclassmembers class one.mixin.android.** {
    !transient !static <fields>;
}

# R8 full mode strips unused constructors that Gson instantiates reflectively.
-keepclassmembers class one.mixin.android.** {
    <init>(...);
}

# Retrofit/Gson models are often only constructed reflectively.
-keep,allowoptimization,allowobfuscation class one.mixin.android.api.** { *; }
-keep,allowoptimization,allowobfuscation class one.mixin.android.vo.** { *; }
-keep,allowoptimization,allowobfuscation class one.mixin.android.websocket.** { *; }
-keep,allowoptimization,allowobfuscation class one.mixin.android.crypto.** { *; }
-keep,allowoptimization,allowobfuscation class one.mixin.android.web3.** { *; }
-keep,allowoptimization,allowobfuscation class one.mixin.android.tip.** { *; }
-keep,allowoptimization,allowobfuscation class one.mixin.android.media.** { *; }
-keep,allowoptimization,allowobfuscation class one.mixin.android.webrtc.** { *; }

-keep class com.google.android.gms.internal.mlkit_entity_extraction.** extends java.util.Random { *; }

# Java serialization stores field names and class names across process/app updates.
-keep class one.mixin.android.** implements java.io.Serializable { *; }

# JobQueue persists jobs with Java serialization and looks up classes by name.
-keep class one.mixin.android.job.** { *; }
-keep class com.birbit.android.jobqueue.** { *; }

# These fragment arguments persist nested type names in saved state.
-keepnames class one.mixin.android.ui.wallet.ImportKeyBottomSheetDialogFragment$PopupType$*
-keepnames class one.mixin.android.ui.home.reminder.ReminderBottomSheetDialogFragment$PopupType$*

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

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep generic signature of RxJava2 (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking class io.reactivex.Single
-keep,allowobfuscation,allowshrinking class io.reactivex.Flowable
-keep,allowobfuscation,allowshrinking class io.reactivex.Observable
-keep,allowobfuscation,allowshrinking class io.reactivex.Completable

-keepattributes Signature

# R8 full mode drops TypeToken generic signatures unless the subclass is kept.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

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

# JJWT discovers implementations through META-INF/services.
-keep class io.jsonwebtoken.** { *; }

-keep public class com.reown.walletkit.** { *; }
-keep class com.reown.android.** { *; }

-dontwarn com.sun.jna.**
-keep class com.sun.jna.** { *; }

-dontwarn uniffi.**
-keep class uniffi.** { *; }

# Native Curve25519 providers are loaded by class name.
-keep class org.whispersystems.** { *; }

-dontwarn groovy.lang.GroovyShell

-dontwarn com.yalantis.ucrop**

-dontwarn com.appsflyer.**
