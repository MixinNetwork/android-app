package one.mixin.android.extension

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

fun <T> Bundle.getParcelableCompat(key: String?, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }
}

fun <T : Parcelable> Bundle.getParcelableArrayListCompat(key: String?, clazz: Class<out T>): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayList(key)
    }
}

fun <T> Intent.getParcelableExtraCompat(key: String?, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}

fun <T : Parcelable> Intent.getParcelableArrayListCompat(key: String?, clazz: Class<out T>): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra(key)
    }
}

@SuppressWarnings("deprecation")
fun <T : Serializable?> Intent.getSerializableExtraCompat(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(name, clazz)
    } else {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        getSerializableExtra(name) as? T
    }
}
