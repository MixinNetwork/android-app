-allowaccessmodification

# Play requires >= 25% obfuscation, optimization, and shrinking (DEX > 10 MB).
# Do not use `-keep class x.** { *; }` without allow* unless the name is a
# runtime identifier (SPI, JNI, or persisted Java serialization).

# Gson/Room models that still use source field names as JSON/column keys.
-keepclassmembers class one.mixin.android.api.**,
                         one.mixin.android.vo.**,
                         one.mixin.android.websocket.**,
                         one.mixin.android.db.**,
                         one.mixin.android.crypto.**,
                         one.mixin.android.web3.**,
                         one.mixin.android.tip.**,
                         one.mixin.android.media.**,
                         one.mixin.android.webrtc.**,
                         one.mixin.android.ui.web.**,
                         one.mixin.android.ui.wallet.**,
                         one.mixin.android.ui.landing.**,
                         one.mixin.android.ui.transfer.** {
    !transient !static <fields>;
    <init>(...);
}

# JSON keys come from the annotation, so field names may be renamed.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# R8 full mode can replace reflectively constructed model types with null.
-keep,allowoptimization,allowobfuscation class one.mixin.android.api.**
-keep,allowoptimization,allowobfuscation class one.mixin.android.vo.**
-keep,allowoptimization,allowobfuscation class one.mixin.android.websocket.**
-keep,allowoptimization,allowobfuscation class one.mixin.android.crypto.**
-keep,allowoptimization,allowobfuscation class one.mixin.android.web3.**
-keep,allowoptimization,allowobfuscation class one.mixin.android.tip.**
-keep,allowoptimization,allowobfuscation class one.mixin.android.media.**
-keep,allowoptimization,allowobfuscation class one.mixin.android.webrtc.**

-keep class com.google.android.gms.internal.mlkit_entity_extraction.** extends java.util.Random { *; }

# Java serialization stores class names and field names across app updates.
-keep class one.mixin.android.** implements java.io.Serializable
-keepclassmembers class one.mixin.android.** implements java.io.Serializable {
    static final long serialVersionUID;
    !transient <fields>;
}

# JobQueue persists jobs by class name and serializes non-transient fields.
-keep class one.mixin.android.job.**
-keepclassmembers class one.mixin.android.job.** {
    static final long serialVersionUID;
    !transient <fields>;
    <init>(...);
}
-keep class com.birbit.android.jobqueue.Job
-keepclassmembers class com.birbit.android.jobqueue.Job {
    static final long serialVersionUID;
    !transient <fields>;
}
-keep,allowoptimization,allowobfuscation class com.birbit.android.jobqueue.messaging.message.** {
    public <init>();
}

# These fragment arguments persist nested type names in saved state.
-keepnames class one.mixin.android.ui.wallet.ImportKeyBottomSheetDialogFragment$PopupType$*
-keepnames class one.mixin.android.ui.home.reminder.ReminderBottomSheetDialogFragment$PopupType$*

# Protocol strings use Enum.name; constant names must stay.
-keepclassmembers enum one.mixin.android.** {
    <fields>;
}

# webrtc JNI looks up classes by their original names.
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
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken { *; }
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# web3j Type.getTypeAsString() uses the datatype class simple name.
-keep class org.web3j.abi.datatypes.** { *; }
-keep class org.web3j.abi.TypeReference { *; }
-keep class * extends org.web3j.abi.TypeReference
-keep,allowoptimization,allowobfuscation,allowshrinking class org.web3j.crypto.** { *; }
-keep,allowoptimization,allowobfuscation,allowshrinking class org.web3j.protocol.** { *; }
-dontwarn org.web3j.crypto.**
-dontwarn jnr.unixsocket.**
-dontwarn org.web3j.protocol.ipc.**
-dontwarn org.java_websocket.**
-dontwarn org.web3j.protocol.websocket.**

-dontwarn com.fasterxml.jackson.databind.**
-keep,allowoptimization,allowobfuscation,allowshrinking class com.fasterxml.jackson.** { *; }
#-dontwarn java.lang.SafeVarargs
-dontwarn org.slf4j.**

# JJWT discovers implementations through META-INF/services.
-keep class io.jsonwebtoken.impl.** { *; }
-keep class io.jsonwebtoken.orgjson.** { *; }

-keep,allowoptimization,allowobfuscation,allowshrinking class com.reown.** { *; }

-dontwarn com.sun.jna.**
-keep class com.sun.jna.** { *; }

-dontwarn uniffi.**
-keep class uniffi.** { *; }

# Native Curve25519 providers are loaded by class name.
-keep class org.whispersystems.curve25519.NativeCurve25519Provider { *; }
-keep class org.whispersystems.curve25519.JavaCurve25519Provider { *; }
-keep class org.whispersystems.curve25519.OpportunisticCurve25519Provider { *; }
-keep,allowoptimization,allowobfuscation,allowshrinking class org.whispersystems.libsignal.** { *; }

-dontwarn groovy.lang.GroovyShell

-dontwarn com.yalantis.ucrop**

-dontwarn com.appsflyer.**
-keep class com.appsflyer.** { *; }
-keep class com.android.installreferrer.** { *; }

-keep,allowoptimization,allowobfuscation,allowshrinking class com.checkout.** { *; }
