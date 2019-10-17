package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.uber.autodispose.android.lifecycle.scope
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import java.util.Locale
import javax.inject.Inject
import one.mixin.android.Constants.Account.PREF_LANGUAGE
import one.mixin.android.Constants.Account.PREF_SET_LANGUAGE
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), HasAndroidInjector {

    protected val stopScope = scope(Lifecycle.Event.ON_STOP)

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector() = dispatchingAndroidInjector

    override fun attachBaseContext(context: Context) {
        val setLanguage = context.defaultSharedPreferences.getBoolean(PREF_SET_LANGUAGE, false)
        if (setLanguage) {
            val conf = context.resources.configuration
            val defaultLang = Locale.getDefault().language
            val language = context.defaultSharedPreferences.getString(PREF_LANGUAGE, defaultLang) ?: defaultLang
            conf.setLocale(Locale(language))
            super.attachBaseContext(context.createConfigurationContext(conf))
        } else {
            super.attachBaseContext(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        if (fragments.size > 0) {
            // Make sure there is a BaseFragment handle this event.
            fragments.indices.reversed()
                .map { fragments[it] }
                .filter { it != null && it is BaseFragment && it.onBackPressed() }
                .forEach { _ -> return }
        }
        super.onBackPressed()
    }
}
