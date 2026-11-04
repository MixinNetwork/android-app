package one.mixin.android.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.window.WindowSdkExtensions
import androidx.window.WindowSdkExtensionsDecorator
import androidx.window.layout.SupportedPosture
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowInfoTrackerDecorator
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.WindowMetricsCalculatorDecorator
import one.mixin.android.MixinApplication
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.ui.common.BaseActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [35], qualifiers = "sw400dp-w800dp-h400dp-mdpi")
@SuppressLint("RestrictedApi")
class BottomSheetInsetsTest {
    @Test
    fun landscapeScrimCoversWindowWhileContentAvoidsCutoutAndKeyboard() {
        var bounds = Rect(0, 0, 800, 400)
        WindowSdkExtensions.overrideDecorator(object : WindowSdkExtensionsDecorator {
            override fun decorate(windowSdkExtensions: WindowSdkExtensions) = object : WindowSdkExtensions() {
                override val extensionVersion = 6
            }
        })
        WindowInfoTracker.overrideDecorator(object : WindowInfoTrackerDecorator {
            override fun decorate(tracker: WindowInfoTracker) = object : WindowInfoTracker by tracker {
                override val supportedPostures = emptyList<SupportedPosture>()
            }
        })
        WindowMetricsCalculator.overrideDecorator(object : WindowMetricsCalculatorDecorator {
            override fun decorate(calculator: WindowMetricsCalculator) = object : WindowMetricsCalculator by calculator {
                override fun computeCurrentWindowMetrics(context: Context) = WindowMetrics(bounds, 1f)
                override fun computeCurrentWindowMetrics(activity: Activity) = WindowMetrics(bounds, 1f)
            }
        })
        val controller = Robolectric.buildActivity(BaseActivity::class.java).setup()
        val activity = controller.get()
        MixinApplication.appContext = activity.applicationContext
        val content = FrameLayout(activity).apply { minimumHeight = 1000 }
        val background = ColorDrawable(Color.argb(34, 0, 0, 0))
        val placeholder = View(activity).apply {
            tag = "landscape_sheet_placeholder"
            this.background = background
        }
        content.addView(placeholder, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 56))
        val dialog = BottomSheet.Builder(activity, needFocus = true, softInputResize = true).setCustomView(content).create()
        val originalCutout = dialog.window!!.attributes.layoutInDisplayCutoutMode
        try {
            dialog.show()
            dialog.setCustomViewHeightSync(1000)
            val sheet = content.parent as View
            val scrim = sheet.parent as View
            val originalFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            var insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 30, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 20, 24))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(80, 0, 0, 0))
                .build()
            fun layout(height: Int = bounds.height()) {
                scrim.dispatchApplyWindowInsets(insets.toWindowInsets()!!)
                scrim.measure(View.MeasureSpec.makeMeasureSpec(bounds.width(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                scrim.layout(0, 0, scrim.measuredWidth, scrim.measuredHeight)
            }
            layout()
            assertEquals(800, scrim.measuredWidth)
            assertEquals(400, scrim.measuredHeight)
            assertEquals(480, sheet.measuredWidth)
            assertEquals(190, sheet.left)
            assertEquals(376, sheet.bottom)
            assertEquals(376, content.height)
            assertEquals(0, content.top)
            assertEquals(30 + activity.appCompatActionBarHeight(), placeholder.layoutParams.height)
            assertNull(placeholder.background)
            assertEquals(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS, dialog.window!!.attributes.layoutInDisplayCutoutMode)
            assertEquals(0, dialog.window!!.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            insets = WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 180))
                .build()
            layout()
            assertEquals(220, sheet.bottom)
            layout(height = 220)
            assertEquals(220, sheet.bottom)
            assertEquals(220, dialog.getMaxCustomViewHeight())
            assertEquals(220, content.height)
            assertEquals(0, content.top)

            bounds = Rect(0, 0, 400, 800)
            insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 30, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 24))
                .build()
            layout()
            assertEquals(1000, content.layoutParams.height)
            assertSame(background, placeholder.background)
            assertEquals(originalCutout, dialog.window!!.attributes.layoutInDisplayCutoutMode)
            assertEquals(originalFlags, scrim.systemUiVisibility)

            bounds = Rect(0, 0, 800, 400)
            layout()
            assertNull(placeholder.background)
            assertEquals(480, sheet.measuredWidth)
            fun tapOutside() {
                val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
                scrim.onTouchEvent(event)
                event.recycle()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            }
            dialog.setCancelable(false)
            dialog.cancel()
            tapOutside()
            assertTrue(dialog.isShowing)
            dialog.setCancelable(true)
            dialog.dismissClickOutside = false
            tapOutside()
            assertTrue(dialog.isShowing)
            dialog.dismissClickOutside = true
            tapOutside()
            assertFalse(dialog.isShowing)
        } finally {
            dialog.dismiss()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            controller.pause().stop().destroy()
            WindowSdkExtensions.reset()
            WindowInfoTracker.reset()
            WindowMetricsCalculator.reset()
        }
    }
}
