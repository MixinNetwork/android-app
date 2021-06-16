package one.mixin.android.util

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import one.mixin.android.extension.supportsPie

@SuppressLint("InlinedApi")
object SystemUIManager {

    private const val action_hide_status_bar = View.SYSTEM_UI_FLAG_FULLSCREEN
    private const val action_hide_status_bar_float = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    private const val action_navigation_bar_hide = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    private const val action_navigation_bar_hide_float = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    private const val action_stable = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    private const val action_dim = View.SYSTEM_UI_FLAG_LOW_PROFILE
    private const val action_immersive = View.SYSTEM_UI_FLAG_IMMERSIVE
    private const val action_immersive_sticky = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    private const val flag_draws = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
    fun setStickyStyle(window: Window) {
        val flag =
            action_navigation_bar_hide or action_hide_status_bar or action_hide_status_bar_float or action_navigation_bar_hide_float or action_stable or action_immersive_sticky
        window.decorView.systemUiVisibility = flag
    }

    fun setImmersiveStyle(window: Window) {
        val flag =
            action_navigation_bar_hide or action_hide_status_bar or action_hide_status_bar_float or action_navigation_bar_hide_float or action_stable or action_immersive
        window.decorView.systemUiVisibility = flag
    }

    fun setNavigationBarNormalStyle(window: Window) {
        val flag = action_navigation_bar_hide or action_hide_status_bar
        window.decorView.systemUiVisibility = flag
    }

    fun setNavigationBarFloatStyle(window: Window) {
        val flag = action_navigation_bar_hide or action_hide_status_bar or action_navigation_bar_hide or action_hide_status_bar_float or action_stable
        window.decorView.systemUiVisibility = flag
    }

    fun setStatusNormalStyle(window: Window) {
        val flag = action_hide_status_bar
        window.decorView.systemUiVisibility = flag
    }

    fun setStatusFloatStyle(window: Window) {
        val flag = action_hide_status_bar or action_hide_status_bar_float or action_stable
        window.decorView.systemUiVisibility = flag
    }

    fun transparentDraws(window: Window) {
        window.addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.addFlags(FLAG_TRANSLUCENT_NAVIGATION)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    fun setDimStyle(window: Window) {
        val flag = action_dim
        window.decorView.systemUiVisibility = flag
    }

    fun clearStyle(window: Window) {
        val flag = 0
        window.decorView.systemUiVisibility = flag
    }

    fun lightUI(window: Window, light: Boolean) {
        if (light) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility xor View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR xor View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    fun setSystemUiColor(window: Window, color: Int) {
        window.navigationBarColor = color
        window.statusBarColor = color
    }

    fun generateSafeMarginLayoutParams(window: Window, params: ViewGroup.MarginLayoutParams): ViewGroup.MarginLayoutParams {
        return params.apply {
            window.decorView.rootWindowInsets.displayCutout?.let {
                leftMargin = it.safeInsetLeft
                rightMargin = it.safeInsetRight
                topMargin = it.safeInsetTop
                bottomMargin = it.safeInsetBottom
            }
        }
    }

    fun hasCutOut(window: Window): Boolean {
        supportsPie {
            return window.decorView.rootWindowInsets?.displayCutout?.safeInsetTop != 0
        }
        return false
    }
}
