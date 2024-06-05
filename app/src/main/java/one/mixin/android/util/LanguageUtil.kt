package one.mixin.android.util

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

fun getLanguage(): String? = AppCompatDelegate.getApplicationLocales().get(0)?.language

fun getCountry(): String? = AppCompatDelegate.getApplicationLocales().get(0)?.country

fun getLocaleString(): String = AppCompatDelegate.getApplicationLocales().get(0).toString()

fun isCurrChinese(): Boolean = (getLanguage() ?: Locale.getDefault().language) == Locale.SIMPLIFIED_CHINESE.language

fun isFollowSystem(): Boolean = AppCompatDelegate.getApplicationLocales().isEmpty

fun needsSpaceBetweenWords(): Boolean {
    return when (getLanguage()) {
        Locale.SIMPLIFIED_CHINESE.language -> false
        Locale.TRADITIONAL_CHINESE.language -> false
        Locale.JAPANESE.language -> false
        else -> true
    }
}

fun getLocalString(
    context: Context,
    @StringRes resId: Int,
): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || isFollowSystem()) return context.getString(resId)
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: return context.getString(resId)
    val configuration = context.resources.configuration
    configuration.setLocales(LocaleList(locale))
    return context.createConfigurationContext(configuration).getString(resId)
}

fun getLocalString(
    context: Context,
    @StringRes resId: Int,
    vararg args: Any,
): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || isFollowSystem()) return context.getString(resId, *args)
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: return context.getString(resId, *args)
    val configuration = context.resources.configuration
    configuration.setLocales(LocaleList(locale))
    return context.createConfigurationContext(configuration).getString(resId, *args)
}
