package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import com.uber.autodispose.android.lifecycle.scope
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultThemeId
import one.mixin.android.extension.isNightMode
import one.mixin.android.util.SystemUIManager

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
    protected val stopScope = scope(Lifecycle.Event.ON_STOP)
    protected val destroyScope = scope(Lifecycle.Event.ON_DESTROY)

    lateinit var lastLang: String
    var lastThemeId: Int = defaultThemeId
    protected var skipSystemUi: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isNightMode()) {
            setTheme(getNightThemeId())
        } else {
            setTheme(getDefaultThemeId())
        }
        enableEdgeToEdge()
        if (!skipSystemUi) {
            SystemUIManager.setSafePadding(window, colorFromAttribute(R.attr.bg_white))
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val fragments = supportFragmentManager.fragments
                    if (fragments.isNotEmpty()) {
                        for (i in fragments.indices.reversed()) {
                            val f = fragments[i]
                            if (f is BaseFragment && f.onBackPressed()) {
                                return
                            }
                        }
                    }
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            },
        )
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        if (!skipSystemUi) {
            window.decorView.doOnPreDraw {
                SystemUIManager.lightUI(window, !(isNightMode()))
            }
        }
    }

    override fun setContentView(
        view: View?,
        params: ViewGroup.LayoutParams?,
    ) {
        super.setContentView(view, params)
        if (!skipSystemUi) {
            view?.doOnPreDraw {
                SystemUIManager.lightUI(window, !(isNightMode()))
            }
        }
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        if (!skipSystemUi) {
            view?.doOnPreDraw {
                SystemUIManager.lightUI(window, !(isNightMode()))
            }
        }
    }

    open fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_NoActionBar
    }

    open fun getDefaultThemeId(): Int {
        return R.style.AppTheme_NoActionBar
    }

}
