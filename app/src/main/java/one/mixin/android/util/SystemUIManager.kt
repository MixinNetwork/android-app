package one.mixin.android.util

import android.annotation.SuppressLint
import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import one.mixin.android.extension.isCompactWindow
import one.mixin.android.extension.isPhoneLandscape
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
            val phoneLandscape = window.context.isPhoneLandscape()
            val compact = window.context.isCompactWindow() && !phoneLandscape
            val statusBarInsets = insets.getInsets(statusBarInsetType(compact))
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(
                if (compact) 0 else maxOf(statusBarInsets.left, navBarInsets.left),
                if (onlyNav) 0 else statusBarInsets.top,
                if (compact) 0 else maxOf(statusBarInsets.right, navBarInsets.right),
                if (onlyStatus) 0 else navBarInsets.bottom + if (imePadding) imeBottom else 0,
            )
            view.setBackgroundColor(color)
            if (phoneLandscape) insets.inset(view.paddingLeft, 0, view.paddingRight, 0) else insets
        }
    }

    fun setSafeContainerPadding(
        window: Window,
        container: View,
        color: Int,
    ) {
        container.setBackgroundColor(color)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val phoneLandscape = window.context.isPhoneLandscape()
            val compact = window.context.isCompactWindow() && !phoneLandscape
            val statusBarInsets = insets.getInsets(statusBarInsetType(compact))
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            container.setPadding(
                if (compact) 0 else maxOf(statusBarInsets.left, navBarInsets.left),
                statusBarInsets.top,
                if (compact) 0 else maxOf(statusBarInsets.right, navBarInsets.right),
                navBarInsets.bottom,
            )
            if (phoneLandscape) {
                if (container !== window.decorView) window.decorView.setPadding(0, 0, 0, 0)
                insets.inset(container.paddingLeft, 0, container.paddingRight, 0)
            } else insets
        }
    }

    private fun statusBarInsetType(compact: Boolean): Int =
        if (compact) {
            WindowInsetsCompat.Type.statusBars()
        } else {
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
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
