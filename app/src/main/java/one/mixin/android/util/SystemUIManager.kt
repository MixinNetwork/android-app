package one.mixin.android.util

import android.annotation.SuppressLint
import android.view.Window
import androidx.annotation.ColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import one.mixin.android.extension.supportsPie

@SuppressLint("InlinedApi")
object SystemUIManager {
    fun fitsSystem(window: Window, @ColorInt color: Int = 0x33000000) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = color
        window.navigationBarColor = color
        supportsPie {
            window.navigationBarDividerColor = color
        }
    }

    fun clearStyle(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    fun fullScreen(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun lightUI(window: Window, light: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    fun hideSystemUI(window: Window) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun showSystemUI(window: Window) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.apply {
            show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun setSystemUiColor(window: Window, color: Int) {
        window.navigationBarColor = color
        window.statusBarColor = color
    }

    fun hasCutOut(window: Window): Boolean {
        supportsPie {
            return window.decorView.rootWindowInsets?.displayCutout?.safeInsetTop != 0
        }
        return false
    }

    fun setAppearanceLightStatusBars(window: Window, isLight: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = isLight
    }
}
