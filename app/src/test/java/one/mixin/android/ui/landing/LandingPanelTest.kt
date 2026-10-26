package one.mixin.android.ui.landing

import android.app.Application
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ActivityLandingBinding
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "mdpi")
class LandingPanelTest {
    @Test
    @Config(qualifiers = "w400dp-h800dp-mdpi")
    fun compactLandingCanExitWithBack() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            val activity = controller.get()
            activity.preventLandingDismiss()
            activity.onBackPressedDispatcher.onBackPressed()
            assertTrue(activity.isFinishing)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h1000dp-mdpi")
    fun expandedLandingPopsChildrenButKeepsTheRootPanel() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            val activity = controller.get()
            activity.preventLandingDismiss()
            activity.supportFragmentManager.beginTransaction().add(Fragment(), "child").addToBackStack(null).commit()
            activity.supportFragmentManager.executePendingTransactions()
            activity.onBackPressedDispatcher.onBackPressed()
            activity.supportFragmentManager.executePendingTransactions()
            assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
            assertFalse(activity.isFinishing)
            activity.onBackPressedDispatcher.onBackPressed()
            assertFalse(activity.isFinishing)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun panelFitsLargeSmallAndKeyboardResizedWindows() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.AppTheme)
        MixinApplication.appContext = context.applicationContext
        val binding = ActivityLandingBinding.inflate(LayoutInflater.from(context))
        binding.configurePanel()
        fun resize(width: Int, height: Int, panelWidth: Int, panelHeight: Int, expanded: Boolean) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            binding.root.layout(0, 0, width, height)
            assertEquals(panelWidth, binding.container.layoutParams.width)
            assertEquals(panelHeight, binding.container.layoutParams.height)
            if (expanded) {
                assertEquals(12f, binding.container.elevation, 0f)
                assertNotNull(binding.container.background)
                assertNotNull(binding.root.background)
            } else {
                assertEquals(0f, binding.container.elevation, 0f)
                assertNull(binding.container.background)
                assertNull(binding.root.background)
            }
        }
        resize(1200, 900, 480, 720, true)
        resize(1200, 400, 1200, 400, false)
        resize(360, 760, 360, 760, false)
        resize(900, 500, 900, 500, false)
        resize(800, 700, 480, 652, true)
    }
}
