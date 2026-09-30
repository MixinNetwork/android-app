package one.mixin.android.util

import android.app.Activity
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
@Config(application = Application::class, manifest = Config.NONE, sdk = [35])
class SystemUIManagerTest {
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
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 100, 0, 0))
                .setVisible(WindowInsetsCompat.Type.statusBars(), false)
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 45))
                .build()
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, hiddenInsets)
            assertEquals(0, container.paddingTop)
            container.setBackgroundColor(Color.BLUE)

            val visibleInsets = WindowInsetsCompat.Builder(hiddenInsets)
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 100, 0, 0))
                .setVisible(WindowInsetsCompat.Type.statusBars(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, visibleInsets)
            assertEquals(100, container.paddingTop)
            assertEquals(45, container.paddingBottom)
            assertEquals(Color.BLUE, (container.background as ColorDrawable).color)
        } finally {
            controller.destroy()
        }
    }
}
