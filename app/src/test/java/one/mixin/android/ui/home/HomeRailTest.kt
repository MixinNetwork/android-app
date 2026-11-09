package one.mixin.android.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.view.ContextThemeWrapper
import androidx.fragment.app.FragmentActivity
import androidx.window.embedding.EmbeddingBackend
import androidx.window.embedding.EmbeddingBackendDecorator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import androidx.window.WindowSdkExtensions
import androidx.window.WindowSdkExtensionsDecorator
import androidx.window.layout.SupportedPosture
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowInfoTrackerDecorator
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.WindowMetricsCalculatorDecorator
import one.mixin.android.extension.isPhoneLandscape
import one.mixin.android.ui.common.BaseActivity
import org.robolectric.Shadows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [35])
@SuppressLint("RestrictedApi")
class HomeRailTest {
    @Test
    @Config(qualifiers = "sw420dp-w420dp-h800dp-mdpi")
    fun narrowEmbeddedPaneKeepsTheRailAndCategoryFiltersAvailable() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        var embedded = true
        EmbeddingBackend.overrideDecorator(object : EmbeddingBackendDecorator {
            override fun decorate(embeddingBackend: EmbeddingBackend): EmbeddingBackend =
                object : EmbeddingBackend by embeddingBackend {
                    override fun isActivityEmbedded(activity: Activity) = embedded
                }
        })
        try {
            val context = ContextThemeWrapper(controller.get(), 0)
            assertTrue(context.shouldShowHomeRail())
            embedded = false
            assertFalse(context.shouldShowHomeRail())
        } finally {
            EmbeddingBackend.reset()
            controller.pause().stop().destroy()
        }
    }

    @Test
    @Config(qualifiers = "sw500dp-w800dp-h500dp-mdpi")
    fun landscapePhonesWithoutPostureCapabilitiesKeepTheBottomNavigation() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            assertFalse(controller.get().shouldShowHomeRail())
            assertEquals(HomeRailMode.None, controller.get().homeRailMode())
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    @Config(qualifiers = "sw800dp-w800dp-h1000dp-mdpi")
    fun expandedUnembeddedWindowsKeepTheRail() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            assertTrue(controller.get().shouldShowHomeRail())
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    @Config(qualifiers = "sw500dp-w800dp-h500dp-mdpi")
    fun landscapePhonesUseCompactRailWithoutEnablingCategoryFilters() {
        val controller = Robolectric.buildActivity(BaseActivity::class.java).setup()
        var version = 6
        var tabletop = false
        var bounds = Rect(0, 0, 800, 400)
        var embedded = false
        var unavailable = false
        EmbeddingBackend.overrideDecorator(object : EmbeddingBackendDecorator {
            override fun decorate(embeddingBackend: EmbeddingBackend) = object : EmbeddingBackend by embeddingBackend {
                override fun isActivityEmbedded(activity: Activity) = embedded
            }
        })
        WindowSdkExtensions.overrideDecorator(object : WindowSdkExtensionsDecorator {
            override fun decorate(windowSdkExtensions: WindowSdkExtensions) = object : WindowSdkExtensions() {
                override val extensionVersion = version
            }
        })
        WindowInfoTracker.overrideDecorator(object : WindowInfoTrackerDecorator {
            override fun decorate(tracker: WindowInfoTracker) = object : WindowInfoTracker by tracker {
                override val supportedPostures: List<SupportedPosture>
                    get() {
                        if (unavailable) throw UnsupportedOperationException()
                        return if (tabletop) listOf(SupportedPosture.TABLETOP) else emptyList()
                    }
            }
        })
        WindowMetricsCalculator.overrideDecorator(object : WindowMetricsCalculatorDecorator {
            override fun decorate(calculator: WindowMetricsCalculator) = object : WindowMetricsCalculator by calculator {
                override fun computeCurrentWindowMetrics(context: Context) = WindowMetrics(bounds, 1f)
                override fun computeCurrentWindowMetrics(activity: Activity) = WindowMetrics(bounds, 1f)
            }
        })
        try {
            val activity = controller.get()
            assertFalse(activity.shouldShowHomeRail())
            assertEquals(HomeRailMode.CompactLandscape, activity.homeRailMode())
            assertTrue(activity.isPhoneLandscape())
            bounds = Rect(0, 0, 400, 800)
            assertEquals(HomeRailMode.None, activity.homeRailMode())
            bounds = Rect(0, 0, 800, 400)
            embedded = true
            assertEquals(HomeRailMode.Full, activity.homeRailMode())
            assertFalse(activity.isPhoneLandscape())
            embedded = false
            Shadows.shadowOf(activity).setInMultiWindowMode(true)
            assertEquals(HomeRailMode.None, activity.homeRailMode())
            Shadows.shadowOf(activity).setInMultiWindowMode(false)
            unavailable = true
            assertEquals(HomeRailMode.None, activity.homeRailMode())
            unavailable = false
            version = 5
            assertEquals(HomeRailMode.None, activity.homeRailMode())
            version = 6
            tabletop = true
            assertEquals(HomeRailMode.None, activity.homeRailMode())
            tabletop = false
            Shadows.shadowOf(activity.packageManager).setSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE, true)
            assertEquals(HomeRailMode.None, activity.homeRailMode())
            Shadows.shadowOf(activity.packageManager).setSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE, false)
            assertEquals(HomeRailMode.CompactLandscape, activity.homeRailMode())
            activity.onFoldObserved()
            assertEquals(HomeRailMode.None, activity.homeRailMode())
        } finally {
            EmbeddingBackend.reset()
            WindowSdkExtensions.reset()
            WindowInfoTracker.reset()
            WindowMetricsCalculator.reset()
            controller.pause().stop().destroy()
        }
    }
}
