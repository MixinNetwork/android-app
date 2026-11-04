package one.mixin.android.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import androidx.window.WindowSdkExtensions
import androidx.window.WindowSdkExtensionsDecorator
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowInfoTrackerDecorator
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.WindowMetricsCalculatorDecorator
import one.mixin.android.ui.common.BaseActivity
import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [35], qualifiers = "w800dp-h1000dp")
@SuppressLint("RestrictedApi")
class SystemUIManagerTest {
    @Test
    @Config(sdk = [26, 27, 28, 29])
    fun safePaddingUsesTheActivityContextOnPreAndroidR() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        try {
            val activity = controller.get()
            val container = FrameLayout(activity)
            activity.setContentView(container)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 24, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 48))
                .build()

            SystemUIManager.setSafePadding(activity.window, Color.WHITE)
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, insets)
            assertEquals(24, activity.window.decorView.paddingTop)
            assertEquals(48, activity.window.decorView.paddingBottom)

            SystemUIManager.setSafeContainerPadding(activity.window, container, Color.WHITE)
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, insets)
            assertEquals(24, container.paddingTop)
            assertEquals(48, container.paddingBottom)
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun containerUpdatesPaddingWhenStatusBarBecomesVisible() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        try {
            val activity = controller.get()
            val container = FrameLayout(activity)
            activity.setContentView(container)
            SystemUIManager.setSafeContainerPadding(activity.window, container, Color.WHITE)

            val hiddenInsets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.NONE)
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars(), Insets.of(30, 100, 0, 0))
                .setVisible(WindowInsetsCompat.Type.statusBars(), false)
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 45))
                .build()
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, hiddenInsets)
            assertEquals(0, container.paddingTop)
            container.setBackgroundColor(Color.BLUE)

            val visibleInsets = WindowInsetsCompat.Builder(hiddenInsets)
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(30, 100, 0, 0))
                .setVisible(WindowInsetsCompat.Type.statusBars(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, visibleInsets)
            assertEquals(100, container.paddingTop)
            assertEquals(30, container.paddingLeft)
            assertEquals(45, container.paddingBottom)
            assertEquals(Color.BLUE, (container.background as ColorDrawable).color)
        } finally {
            controller.destroy()
        }
    }

    @Test
    @Config(qualifiers = "sw500dp-w800dp-h500dp-mdpi")
    fun phoneLandscapeSafePaddingMergesCutoutWithoutAccumulation() {
        WindowSdkExtensions.overrideDecorator(object : WindowSdkExtensionsDecorator {
            override fun decorate(windowSdkExtensions: WindowSdkExtensions) = object : WindowSdkExtensions() { override val extensionVersion = 6 }
        })
        WindowInfoTracker.overrideDecorator(object : WindowInfoTrackerDecorator {
            override fun decorate(tracker: WindowInfoTracker) = object : WindowInfoTracker by tracker { override val supportedPostures = emptyList<androidx.window.layout.SupportedPosture>() }
        })
        var bounds = Rect(0, 0, 800, 400)
        WindowMetricsCalculator.overrideDecorator(object : WindowMetricsCalculatorDecorator {
            override fun decorate(calculator: WindowMetricsCalculator) = object : WindowMetricsCalculator by calculator {
                override fun computeCurrentWindowMetrics(context: Context) = WindowMetrics(bounds, 1f)
                override fun computeCurrentWindowMetrics(activity: Activity) = WindowMetrics(bounds, 1f)
            }
        })
        val controller = Robolectric.buildActivity(BaseActivity::class.java).create()
        try {
            val activity = controller.get()
            val container = FrameLayout(activity)
            activity.setContentView(container)
            SystemUIManager.setSafeContainerPadding(activity.window, container, Color.WHITE)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(20, 30, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 10, 40))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(80, 0, 0, 0))
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 180))
                .build()
            activity.window.decorView.setPadding(80, 30, 10, 40)
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, insets)
            val remaining = ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, insets)
            assertEquals(0, activity.window.decorView.paddingLeft)
            assertEquals(0, remaining.getInsets(WindowInsetsCompat.Type.displayCutout()).left)
            assertEquals(180, remaining.getInsets(WindowInsetsCompat.Type.ime()).bottom)
            assertEquals(80, container.paddingLeft)
            assertEquals(10, container.paddingRight)
            assertEquals(30, container.paddingTop)
            assertEquals(40, container.paddingBottom)
            bounds = Rect(0, 0, 400, 800)
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, insets)
            assertEquals(0, container.paddingLeft)
            assertEquals(0, container.paddingRight)
            bounds = Rect(0, 0, 800, 400)
            SystemUIManager.setSafePadding(activity.window, Color.WHITE, onlyStatus = true)
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, insets)
            assertEquals(80, activity.window.decorView.paddingLeft)
            assertEquals(10, activity.window.decorView.paddingRight)
            assertEquals(30, activity.window.decorView.paddingTop)
            assertEquals(0, activity.window.decorView.paddingBottom)
        } finally {
            WindowSdkExtensions.reset()
            WindowInfoTracker.reset()
            WindowMetricsCalculator.reset()
            controller.destroy()
        }
    }
}
