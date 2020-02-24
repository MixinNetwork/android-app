package one.mixin.android.util.language

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager.GET_META_DATA
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.LocaleList
import java.util.*

/**
 * Lingver is a tool to manage your application locale and language.
 *
 * Once you set a desired locale using [setLocale] method, Lingver will enforce your application
 * to provide correctly localized data via [Resources] class.
 */
class Lingver private constructor(private val store: LocaleStore) {

    /**
     * Creates and sets a [Locale] using language, country and variant information.
     *
     * See the [Locale] class description for more information about valid language, country
     * and variant values.
     */
    @JvmOverloads
    fun setLocale(context: Context, language: String, country: String = "", variant: String = "") {
        setLocale(context, Locale(language, country, variant))
    }

    /**
     * Sets a [locale] which will be used to localize all data coming from [Resources] class.
     *
     * <p>Note that you need to update all already fetched locale-based data manually.
     * [Lingver] is not responsible for that.
     */
    fun setLocale(context: Context, locale: Locale) {
        store.persistLocale(locale)
        update(context, locale)
    }

    /**
     * Returns the active [Locale].
     */
    fun getLocale(): Locale {
        return store.getLocale()
    }

    /**
     * Returns a language code which is a part of the active [Locale].
     *
     * Deprecated ISO language codes "iw", "ji", and "in" are converted
     * to "he", "yi", and "id", respectively.
     */
    fun getLanguage(): String {
        return verifyLanguage(getLocale().language)
    }

    private fun verifyLanguage(language: String): String {
        // get rid of deprecated language tags
        return when (language) {
            "iw" -> "he"
            "ji" -> "yi"
            "in" -> "id"
            else -> language
        }
    }

    private fun setUp(application: Application) {
        application.registerActivityLifecycleCallbacks(LingverActivityLifecycleCallbacks(this))
        application.registerComponentCallbacks(LingverApplicationCallbacks(application, this))
    }

    fun setLocaleInternal(context: Context) {
        update(context, store.getLocale())
    }

    private fun update(context: Context, locale: Locale) {
        updateResources(context, locale)
        val appContext = context.applicationContext
        if (appContext !== context) {
            updateResources(appContext, locale)
        }
    }

    @Suppress("DEPRECATION")
    private fun updateResources(context: Context, locale: Locale) {
        Locale.setDefault(locale)

        val res = context.resources
        val current = res.configuration.getLocaleCompat()

        if (current == locale) return

        val config = Configuration(res.configuration)
        when {
            isAtLeastSdkVersion(VERSION_CODES.N) -> setLocaleForApi24(config, locale)
            isAtLeastSdkVersion(VERSION_CODES.JELLY_BEAN_MR1) -> config.setLocale(locale)
            else -> config.locale = locale
        }
        res.updateConfiguration(config, res.displayMetrics)
    }

    @SuppressLint("NewApi")
    private fun setLocaleForApi24(config: Configuration, locale: Locale) {
        // bring the target locale to the front of the list
        val set = linkedSetOf(locale)

        val defaultLocales = LocaleList.getDefault()
        val all = List<Locale>(defaultLocales.size()) { defaultLocales[it] }
        // append other locales supported by the user
        set.addAll(all)

        config.setLocales(LocaleList(*set.toTypedArray()))
    }

    fun resetActivityTitle(activity: Activity) {
        try {
            val pm = activity.packageManager
            val info = pm.getActivityInfo(activity.componentName, GET_META_DATA)
            if (info.labelRes != 0) {
                activity.setTitle(info.labelRes)
            }
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    private fun Configuration.getLocaleCompat(): Locale {
        return if (isAtLeastSdkVersion(VERSION_CODES.N)) locales.get(0) else locale
    }

    private fun isAtLeastSdkVersion(versionCode: Int): Boolean {
        return Build.VERSION.SDK_INT >= versionCode
    }

    companion object {

        private lateinit var instance: Lingver

        /**
         * Returns the global instance of [Lingver] created via init method.
         *
         * @throws IllegalStateException if it was not initialized properly.
         */
        @JvmStatic
        fun getInstance(): Lingver {
            check(::instance.isInitialized) { "Lingver should be initialized first" }
            return instance
        }

        /**
         * Creates and sets up the global instance using a provided language and the default store.
         */
        @JvmStatic
        fun init(application: Application, defaultLanguage: String): Lingver {
            return init(application, Locale(defaultLanguage))
        }

        /**
         * Creates and sets up the global instance using a provided locale and the default store.
         */
        @JvmStatic
        @JvmOverloads
        fun init(application: Application, defaultLocale: Locale = Locale.getDefault()): Lingver {
            return init(application, PreferenceLocaleStore(application, defaultLocale))
        }

        /**
         * Creates and sets up the global instance.
         *
         * This method must be called before any calls to [Lingver] and may only be called once.
         */
        @JvmStatic
        fun init(application: Application, store: LocaleStore): Lingver {
            check(!::instance.isInitialized) { "Already initialized" }
            val lingver = Lingver(store)
            lingver.setUp(application)
            lingver.setLocale(application, store.getLocale())
            instance = lingver
            return lingver
        }
    }
}
