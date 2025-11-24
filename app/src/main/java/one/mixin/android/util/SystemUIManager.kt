package one.mixin.android.util

import android.annotation.SuppressLint
import android.view.Window
import androidx.core.view.ViewCompat
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

    fun setSafePadding(
        window: Window,
        color: Int,
        onlyStatus: Boolean = false,
        onlyNav: Boolean = false,
        imePadding: Boolean = false
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(
                0,
                if (onlyNav) 0 else statusBarInsets.top,
                0,
                if (onlyStatus) 0 else navBarInsets.bottom + if (imePadding) imeBottom else 0,
            )
            view.setBackgroundColor(color)
            insets
        }
    }

    fun setSafePaddingOnce(
        window: Window,
        color: Int,
        onlyStatus: Boolean = false,
        onlyNav: Boolean = false,
        imePadding: Boolean = false
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(
                0,
                if (onlyNav) 0 else statusBarInsets.top,
                0,
                if (onlyStatus) 0 else navBarInsets.bottom + if (imePadding) imeBottom else 0
            )
            view.setBackgroundColor(color)
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
            insets
        }
        ViewCompat.requestApplyInsets(window.decorView)
    }

    fun fullScreen(window: Window) {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            view.setPadding(0, 0, 0, 0)
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
