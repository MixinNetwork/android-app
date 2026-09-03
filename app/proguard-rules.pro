-allowaccessmodification

# Gson model fields are addressed by their JSON names at runtime.
-keepclassmembers,allowoptimization class one.mixin.android.api.** {
    !transient !static <fields>;
}
-keepclassmembers,allowoptimization class one.mixin.android.vo.** {
    !transient !static <fields>;
}
-keepclassmembers,allowoptimization class one.mixin.android.websocket.** {
    !transient !static <fields>;
}
-keepclassmembers,allowoptimization class one.mixin.android.db.** {
    !transient !static <fields>;
}
-keepclassmembers,allowoptimization class one.mixin.android.ui.transfer.** {
    !transient !static <fields>;
}

-keep class com.google.android.gms.internal.mlkit_entity_extraction.** extends java.util.Random { *; }

# Preserve classes passed through Android Serializable extras.
-keep,allowoptimization class one.mixin.android.websocket.BlazeMessageData { *; }
-keep,allowoptimization class one.mixin.android.ui.wallet.WalletActivity$Destination { *; }
-keep,allowoptimization class one.mixin.android.ui.wallet.WalletActivity$Destination$* { *; }

# JobQueue persists these objects with Java serialization.
-keepnames class one.mixin.android.job.**
-keepclassmembers,allowshrinking,allowoptimization class one.mixin.android.job.** {
    !transient <fields>;
}

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

-keepclassmembers,allowshrinking,allowoptimization,allowobfuscation enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep generic signature of RxJava2 (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking class io.reactivex.Single
-keep,allowobfuscation,allowshrinking class io.reactivex.Flowable
-keep,allowobfuscation,allowshrinking class io.reactivex.Observable
-keep,allowobfuscation,allowshrinking class io.reactivex.Completable

-keepattributes Signature

# web3j
-dontwarn org.web3j.crypto.**
-dontwarn jnr.unixsocket.**
-dontwarn org.web3j.protocol.ipc.**
-dontwarn org.java_websocket.**
-dontwarn org.web3j.protocol.websocket.**

-dontwarn com.fasterxml.jackson.databind.**
#-dontwarn java.lang.SafeVarargs
-dontwarn org.slf4j.**

# JJWT discovers these implementations through META-INF/services.
-keep class io.jsonwebtoken.impl.compression.DeflateCompressionCodec { *; }
-keep class io.jsonwebtoken.impl.compression.GzipCompressionCodec { *; }
-keep class io.jsonwebtoken.orgjson.io.OrgJsonSerializer { *; }
-keep class io.jsonwebtoken.orgjson.io.OrgJsonDeserializer { *; }

-dontwarn com.sun.jna.**
-keep class com.sun.jna.** { *; }

-dontwarn uniffi.**
-keep class uniffi.** { *; }

-keep,allowshrinking,allowoptimization,allowobfuscation class org.whispersystems.libsignal.** { *; }
-keep class org.whispersystems.curve25519.NativeCurve25519Provider { *; }
-keep class org.whispersystems.curve25519.JavaCurve25519Provider { *; }
-keep class org.whispersystems.curve25519.OpportunisticCurve25519Provider { *; }

-dontwarn groovy.lang.GroovyShell

-dontwarn com.yalantis.ucrop**

-dontwarn com.appsflyer.**
