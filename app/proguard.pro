
# glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ucrop
-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }

# rxpermissions
-dontwarn com.tbruyelle.rxpermissions.**

-dontwarn retrofit2.**
-dontwarn rx.**
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }

-dontwarn com.bugsnag.**
-keep class com.bugsnag.** { *; }

-dontwarn okio.**
-dontwarn com.squareup.okhttp3.**
-dontwarn okhttp3.**
-dontwarn javax.annotation.**

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-dontwarn org.hamcrest.**
-keep class org.hamcrest.** { *; }

-dontwarn org.whispersystems.**
-keep class org.whispersystems.** { *; }

-dontwarn jp.wasabeef.glide.transformations.**
-keep jp.wasabeef.glide.transformations.** { *; }

-dontwarn javax.annotation.**

-keepclasseswithmembers class * implements android.arch.lifecycle.GenericLifecycleObserver {
<init>(...);
}
-keepclassmembers class android.arch.lifecycle.Lifecycle$* { *; }
-keepclassmembers class * {
    @android.arch.lifecycle.OnLifecycleEvent *;
}
-keepclassmembers class * extends android.arch.lifecycle.ViewModel {
<init>(...);
}