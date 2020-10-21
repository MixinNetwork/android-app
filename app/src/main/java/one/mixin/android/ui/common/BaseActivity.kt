package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.uber.autodispose.android.lifecycle.scope
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultThemeId
import one.mixin.android.extension.isNightMode
import one.mixin.android.ui.web.WebFragment
import one.mixin.android.ui.web.refreshClip
import one.mixin.android.util.SystemUIManager

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {

    protected val stopScope = scope(Lifecycle.Event.ON_STOP)

    lateinit var lastLang: String
    var lastThemeId: Int = defaultThemeId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MixinApplication.get().activitiesCount ++
        if (isNightMode()) {
            setTheme(getNightThemeId())
            SystemUIManager.lightUI(window, false)
        } else {
            setTheme(getDefaultThemeId())
            SystemUIManager.lightUI(window, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = colorFromAttribute(R.attr.bg_white)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MixinApplication.get().activitiesCount --
        refreshClip()
    }

    open fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_NoActionBar
    }

    open fun getDefaultThemeId(): Int {
        return R.style.AppTheme_NoActionBar
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        if (fragments.size > 0) {
            // Make sure there is a BaseFragment handle this event.
            fragments.indices.reversed()
                .map { fragments[it] }
                .filter { it != null && ((it is BaseFragment && it.onBackPressed()) || (it is WebFragment && it.onBackPressed())) }
                .forEach { _ -> return }
        }
        super.onBackPressed()
    }
}
