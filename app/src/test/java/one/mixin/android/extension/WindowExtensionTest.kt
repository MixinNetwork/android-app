package one.mixin.android.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Rect
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.window.embedding.EmbeddingBackend
import androidx.window.embedding.EmbeddingBackendDecorator
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.WindowMetricsCalculatorDecorator
import one.mixin.android.MixinApplication
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [35])
@SuppressLint("RestrictedApi")
class WindowExtensionTest {
    @Test
    @Config(qualifiers = "w1000dp-h800dp-mdpi")
    fun narrowEmbeddedPanesUseContentWidthAndInvalidateCachedHolders() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        var embedded = true
        var paneWidth = 580
        EmbeddingBackend.overrideDecorator(object : EmbeddingBackendDecorator {
            override fun decorate(embeddingBackend: EmbeddingBackend): EmbeddingBackend =
                object : EmbeddingBackend by embeddingBackend {
                    override fun isActivityEmbedded(activity: Activity) = embedded
                }
        })
        WindowMetricsCalculator.overrideDecorator(object : WindowMetricsCalculatorDecorator {
            override fun decorate(calculator: WindowMetricsCalculator): WindowMetricsCalculator =
                object : WindowMetricsCalculator by calculator {
                    override fun computeCurrentWindowMetrics(context: Context) = WindowMetrics(Rect(0, 0, paneWidth, 800), 1f)
                }
        })
        try {
            val activity = controller.get()
            MixinApplication.appContext = activity.applicationContext
            val context = ContextThemeWrapper(activity, 0)
            var attachments = 0
            val recycler = RecyclerView(activity)
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount() = 0
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(TextView(parent.context)) {}
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
                override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
                    attachments++
                }
            }
            activity.setContentView(recycler)
            val content = activity.findViewById<View>(android.R.id.content)
            content.setPadding(16, 0, 16, 0)
            content.layout(0, 0, 580, 800)
            assertTrue(context.isCompactWindow())
            assertTrue(!context.isCompactMessageWindow())
            assertEquals(548, context.messageAreaWidth())
            assertEquals(438, context.maxItemWidth())
            assertEquals(340, context.maxCardWidth())

            recycler.refreshHoldersOnWidthChange()
            recycler.layout(0, 0, 548, 600)
            paneWidth = 300
            content.layout(0, 0, 300, 800)
            recycler.layout(0, 0, 268, 600)
            assertEquals(268, context.messageAreaWidth())
            assertEquals(214, context.maxItemWidth())
            assertEquals(214, context.maxCardWidth())
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(attachments > 1)

            embedded = false
            assertTrue(context.isCompactMessageWindow())
            assertEquals(context.realSize().x, context.messageAreaWidth())
            assertEquals(context.realSize().x * 4 / 5, context.maxItemWidth())
        } finally {
            EmbeddingBackend.reset()
            WindowMetricsCalculator.reset()
            controller.pause().stop().destroy()
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h900dp")
    fun windowHeightChangesRecreateHoldersWithoutChangingRecyclerWidth() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        var windowHeight = 900
        WindowMetricsCalculator.overrideDecorator(object : WindowMetricsCalculatorDecorator {
            override fun decorate(calculator: WindowMetricsCalculator): WindowMetricsCalculator =
                object : WindowMetricsCalculator by calculator {
                    override fun computeCurrentWindowMetrics(context: Context) = WindowMetrics(Rect(0, 0, 800, windowHeight), 1f)
                }
        })
        try {
            val activity = controller.get()
            var attachments = 0
            val recycler = RecyclerView(activity)
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount() = 0
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(TextView(parent.context)) {}
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
                override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
                    attachments++
                }
            }
            recycler.refreshHoldersOnWidthChange()
            activity.setContentView(recycler)
            recycler.layout(0, 0, 400, 600)
            windowHeight = 700
            recycler.layout(0, 0, 400, 500)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(attachments > 1)
        } finally {
            WindowMetricsCalculator.reset()
            controller.pause().stop().destroy()
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h900dp")
    fun resizingRecreatesCachedHoldersAndKeepsTheScrollAnchor() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            assertTrue(!activity.isCompactWindow())
            val recycler = RecyclerView(activity)
            val manager = LinearLayoutManager(activity)
            var created = 0
            val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount() = 100
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    created++
                    return object : RecyclerView.ViewHolder(TextView(parent.context).apply {
                        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80)
                    }) {}
                }
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder.itemView as TextView).text = position.toString()
                }
            }
            recycler.layoutManager = manager
            recycler.adapter = adapter
            recycler.refreshHoldersOnWidthChange()
            activity.setContentView(recycler)
            fun layout(width: Int) {
                recycler.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
                recycler.layout(0, 0, width, 600)
            }
            layout(400)
            manager.scrollToPositionWithOffset(30, 0)
            layout(400)
            val before = manager.findFirstVisibleItemPosition()
            val previousCreated = created
            layout(800)
            shadowOf(Looper.getMainLooper()).idle()
            layout(800)
            assertEquals(before, manager.findFirstVisibleItemPosition())
            assertEquals(100, recycler.adapter?.itemCount)
            assertTrue(created > previousCreated)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun compactResizingDoesNotSwapAdapter() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            assertTrue(activity.isCompactWindow())
            var attachments = 0
            val recycler = RecyclerView(activity)
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount() = 0
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(TextView(parent.context)) {}
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
                override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
                    attachments++
                }
            }
            recycler.refreshHoldersOnWidthChange()
            activity.setContentView(recycler)
            recycler.layout(0, 0, 400, 600)
            recycler.layout(0, 0, 300, 600)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, attachments)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
