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
    fun landscapePhonesKeepTheBottomNavigation() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            assertFalse(controller.get().shouldShowHomeRail())
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
}
