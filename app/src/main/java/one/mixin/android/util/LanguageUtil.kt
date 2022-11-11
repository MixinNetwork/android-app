package one.mixin.android.util

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

fun getLanguage(): String? = AppCompatDelegate.getApplicationLocales().get(0)?.language

fun getCountry(): String? = AppCompatDelegate.getApplicationLocales().get(0)?.country

fun getLocaleString(): String = AppCompatDelegate.getApplicationLocales().get(0).toString()

fun isCurrChinese(): Boolean = getLanguage() == Locale.SIMPLIFIED_CHINESE.language

fun isFollowSystem(): Boolean = AppCompatDelegate.getApplicationLocales().isEmpty
