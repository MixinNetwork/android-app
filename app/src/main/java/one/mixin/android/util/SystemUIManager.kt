package one.mixin.android.util

import android.annotation.SuppressLint
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.ColorInt
import androidx.constraintlayout.compose.Skip
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import one.mixin.android.extension.supportsPie

@SuppressLint("InlinedApi")
object SystemUIManager {

    fun lightUI(
        window: Window,
        light: Boolean,
    ) {
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

    fun setSystemUiColor(
        window: Window,
        color: Int,
        onlyStatus: Boolean = false,
    ) {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsets.Type.navigationBars())
            view.setPadding(
                0,
                statusBarInsets.top,
                0,
                if (onlyStatus) 0 else navBarInsets.bottom,
            )
            view.setBackgroundColor(color)
            insets
        }
    }

    fun hasCutOut(window: Window): Boolean {
        supportsPie {
            return window.decorView.rootWindowInsets?.displayCutout?.safeInsetTop != 0
        }
        return false
    }

    fun setAppearanceLightStatusBars(
        window: Window,
        isLight: Boolean,
    ) {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = isLight
    }
}
